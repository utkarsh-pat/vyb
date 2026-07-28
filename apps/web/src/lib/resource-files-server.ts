import "server-only";

import { randomUUID } from "node:crypto";
import { mkdir, readFile, rm, writeFile } from "node:fs/promises";
import path from "node:path";
import { DeleteObjectCommand, GetObjectCommand, PutObjectCommand, S3Client } from "@aws-sdk/client-s3";
import type { ResourceFileSummary } from "@vyb/contracts";
import { loadWorkspaceRootEnv } from "./server-env";

const MAX_RESOURCE_FILES = 6;
const MAX_RESOURCE_FILE_BYTES = 25 * 1024 * 1024;
const RESOURCE_FILE_CACHE_CONTROL = "private, max-age=3600";
const GENERAL_RESOURCE_SCOPE = "_general";

const MIME_BY_EXTENSION: Record<string, string> = {
  ".csv": "text/csv",
  ".doc": "application/msword",
  ".docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  ".gif": "image/gif",
  ".jpeg": "image/jpeg",
  ".jpg": "image/jpeg",
  ".md": "text/markdown",
  ".pdf": "application/pdf",
  ".png": "image/png",
  ".ppt": "application/vnd.ms-powerpoint",
  ".pptx": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
  ".txt": "text/plain",
  ".webp": "image/webp",
  ".xls": "application/vnd.ms-excel",
  ".xlsx": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
};

const EXTENSION_BY_MIME: Record<string, string> = Object.fromEntries(
  Object.entries(MIME_BY_EXTENSION).map(([extension, mimeType]) => [mimeType, extension.slice(1)])
);

const ALLOWED_RESOURCE_MIME_TYPES = new Set(Object.values(MIME_BY_EXTENSION));

type R2Config = {
  accountId: string;
  accessKeyId: string;
  secretAccessKey: string;
  bucket: string;
};

export type PersistedResourceFile = ResourceFileSummary & {
  storagePath: string;
  url: string;
};

export type ParsedResourceStoragePath = {
  tenantId: string;
  communityId: string | null;
  uploaderUserId: string;
  fileName: string;
};

function normalizeMimeType(value: string) {
  return value.split(";")[0]?.trim().toLowerCase() || "application/octet-stream";
}

function getR2Config() {
  loadWorkspaceRootEnv();
  const config = {
    accountId: process.env.R2_ACCOUNT_ID?.trim(),
    accessKeyId: process.env.R2_ACCESS_KEY_ID?.trim(),
    secretAccessKey: process.env.R2_SECRET_ACCESS_KEY?.trim(),
    bucket: process.env.R2_BUCKET?.trim()
  };
  const missing = Object.entries(config)
    .filter(([, value]) => !value)
    .map(([key]) => key);
  if (missing.length > 0) {
    throw new Error(`R2 media storage is not configured. Missing: ${missing.join(", ")}.`);
  }
  return config as R2Config;
}

function getR2Client(config: R2Config) {
  return new S3Client({
    region: "auto",
    endpoint: `https://${config.accountId}.r2.cloudflarestorage.com`,
    credentials: {
      accessKeyId: config.accessKeyId,
      secretAccessKey: config.secretAccessKey
    }
  });
}

function getLocalResourceFileRoot() {
  loadWorkspaceRootEnv();

  const configuredRoot =
    process.env.VYB_LOCAL_MEDIA_ROOT ??
    process.env.TMPDIR ??
    process.env.TEMP ??
    process.env.TMP ??
    path.join(process.cwd(), ".tmp");

  return path.join(configuredRoot, "vyb-resource-files");
}

function isLocalDevelopmentStorageFailure(error: unknown) {
  if (process.env.NODE_ENV === "production") {
    return false;
  }

  if (!(error instanceof Error)) {
    return false;
  }

  const message = error.message.toLowerCase();
  return (
    message.includes("r2 media storage is not configured") ||
    message.includes("enoent") ||
    message.includes("permission") ||
    message.includes("unauthorized") ||
    message.includes("credentials")
  );
}

function sanitizeFileName(value: string) {
  const cleaned = value
    .trim()
    .replace(/\s+/g, "-")
    .replace(/[^a-zA-Z0-9._-]/g, "")
    .replace(/-+/g, "-");

  return cleaned || "campus-resource";
}

function normalizeStorageSegment(value: string) {
  return value.trim().replace(/[^a-zA-Z0-9_-]/g, "_") || GENERAL_RESOURCE_SCOPE;
}

function inferMimeType(file: File) {
  const explicit = normalizeMimeType(file.type || "");
  if (ALLOWED_RESOURCE_MIME_TYPES.has(explicit)) {
    return explicit;
  }

  const byExtension = MIME_BY_EXTENSION[path.extname(file.name).toLowerCase()];
  return byExtension ?? explicit;
}

function extensionForFile(fileName: string, mimeType: string) {
  const extension = path.extname(fileName).toLowerCase().replace(/^\./, "");
  if (extension && MIME_BY_EXTENSION[`.${extension}`]) {
    return extension;
  }

  return EXTENSION_BY_MIME[mimeType] ?? "bin";
}

function validateResourceFile(file: File) {
  const mimeType = inferMimeType(file);

  if (!ALLOWED_RESOURCE_MIME_TYPES.has(mimeType)) {
    throw new Error(`"${file.name || "resource file"}" is not a supported notes, document, spreadsheet, slide, image, or PDF file.`);
  }

  if (file.size <= 0) {
    throw new Error(`"${file.name || "resource file"}" is empty.`);
  }

  if (file.size > MAX_RESOURCE_FILE_BYTES) {
    throw new Error(`"${file.name || "resource file"}" is too large. Keep each resource file under 25 MB.`);
  }

  return {
    mimeType,
    fileName: sanitizeFileName(file.name || `resource.${extensionForFile("resource", mimeType)}`)
  };
}

export function buildResourceFileDownloadUrl(storagePath: string) {
  const encodedPath = storagePath
    .split("/")
    .filter(Boolean)
    .map((segment) => encodeURIComponent(segment))
    .join("/");

  return `/api/resources/files/${encodedPath}`;
}

export function resolveLocalResourceFilePath(storagePath: string) {
  const rootPath = path.resolve(getLocalResourceFileRoot());
  const relativePath = storagePath
    .split("/")
    .filter(Boolean)
    .join(path.sep);
  const absolutePath = path.resolve(rootPath, relativePath);
  const relativeCheck = path.relative(rootPath, absolutePath);

  if (relativeCheck.startsWith("..") || path.isAbsolute(relativeCheck)) {
    throw new Error("Invalid local resource file path.");
  }

  return absolutePath;
}

export function inferResourceFileContentType(storagePath: string) {
  return MIME_BY_EXTENSION[path.extname(storagePath).toLowerCase()] ?? "application/octet-stream";
}

export function parseResourceStoragePath(storagePath: string): ParsedResourceStoragePath | null {
  const segments = storagePath.split("/").filter(Boolean);
  if (segments.length !== 5 || segments[0] !== "resources") {
    return null;
  }

  return {
    tenantId: segments[1],
    communityId: segments[2] === GENERAL_RESOURCE_SCOPE ? null : segments[2],
    uploaderUserId: segments[3],
    fileName: segments[4]
  };
}

async function persistLocalResourceFile(input: {
  buffer: Buffer;
  fileName: string;
  mimeType: string;
  storagePath: string;
}): Promise<PersistedResourceFile> {
  const filePath = resolveLocalResourceFilePath(input.storagePath);
  await mkdir(path.dirname(filePath), { recursive: true });
  await writeFile(filePath, input.buffer);

  return {
    id: path.basename(input.storagePath, path.extname(input.storagePath)),
    fileName: input.fileName,
    mimeType: input.mimeType,
    sizeBytes: input.buffer.byteLength,
    storagePath: input.storagePath,
    url: buildResourceFileDownloadUrl(input.storagePath)
  };
}

export async function persistResourceFiles(input: {
  tenantId: string;
  userId: string;
  communityId?: string | null;
  files: File[];
}): Promise<PersistedResourceFile[]> {
  if (input.files.length === 0) {
    return [];
  }

  if (input.files.length > MAX_RESOURCE_FILES) {
    throw new Error(`You can upload up to ${MAX_RESOURCE_FILES} files in one resource.`);
  }

  return Promise.all(
    input.files.map(async (file) => {
      const { fileName, mimeType } = validateResourceFile(file);
      const buffer = Buffer.from(await file.arrayBuffer());
      const assetId = randomUUID();
      const extension = extensionForFile(fileName, mimeType);
      const communitySegment = input.communityId ? normalizeStorageSegment(input.communityId) : GENERAL_RESOURCE_SCOPE;
      const storagePath = `resources/${input.tenantId}/${communitySegment}/${input.userId}/${assetId}.${extension}`;

      try {
        const r2 = getR2Config();
        await getR2Client(r2).send(
          new PutObjectCommand({
            Bucket: r2.bucket,
            Key: storagePath,
            Body: buffer,
            ContentType: mimeType,
            CacheControl: RESOURCE_FILE_CACHE_CONTROL,
            Metadata: {
              originalFileName: fileName,
              tenant_id: input.tenantId,
              uploader_id: input.userId,
              community_id: input.communityId ?? ""
            }
          })
        );

        return {
          id: assetId,
          fileName,
          mimeType,
          sizeBytes: buffer.byteLength,
          storagePath,
          url: buildResourceFileDownloadUrl(storagePath)
        };
      } catch (error) {
        if (!isLocalDevelopmentStorageFailure(error)) {
          throw error;
        }

        console.warn("[web/resource-files] falling back to local resource storage", {
          tenantId: input.tenantId,
          userId: input.userId,
          communityId: input.communityId ?? null,
          fileName,
          message: error instanceof Error ? error.message : "unknown"
        });

        return persistLocalResourceFile({
          buffer,
          fileName,
          mimeType,
          storagePath
        });
      }
    })
  );
}

export async function readResourceFileBuffer(storagePath: string) {
  try {
    return await readFile(resolveLocalResourceFilePath(storagePath));
  } catch {
    const r2 = getR2Config();
    const result = await getR2Client(r2).send(new GetObjectCommand({ Bucket: r2.bucket, Key: storagePath }));
    if (!result.Body) {
      throw new Error("Resource file body is missing.");
    }
    return Buffer.from(await result.Body.transformToByteArray());
  }
}

export async function deleteResourceFiles(files: Array<{ storagePath?: string | null; url?: string | null }>) {
  const removable = files.filter((file) => typeof file.storagePath === "string" && file.storagePath.length > 0);

  if (removable.length === 0) {
    return;
  }

  await Promise.allSettled(
    removable.map((file) =>
      rm(resolveLocalResourceFilePath(file.storagePath as string), {
        force: true
      }).catch(() => null)
    )
  );

  try {
    const r2 = getR2Config();
    const client = getR2Client(r2);
    await Promise.allSettled(
      removable.map((file) =>
        client.send(new DeleteObjectCommand({ Bucket: r2.bucket, Key: file.storagePath as string }))
      )
    );
  } catch (error) {
    if (!isLocalDevelopmentStorageFailure(error)) {
      throw error;
    }
  }
}
