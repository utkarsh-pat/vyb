import "server-only";

import { randomUUID } from "node:crypto";
import { DeleteObjectCommand, PutObjectCommand, S3Client } from "@aws-sdk/client-s3";
import type { CampusEventMediaAsset } from "@vyb/contracts";
import { loadWorkspaceRootEnv } from "./server-env";

const MAX_EVENT_MEDIA_ITEMS = 4;
const MAX_EVENT_IMAGE_BYTES = 12 * 1024 * 1024;
const MAX_EVENT_VIDEO_BYTES = 60 * 1024 * 1024;
const MAX_REGISTRATION_MEDIA_ITEMS = 3;
const MAX_REGISTRATION_IMAGE_BYTES = 10 * 1024 * 1024;
const CACHE_CONTROL = "public, max-age=31536000, immutable";

const IMAGE_MIME_TYPES = new Set(["image/jpeg", "image/png", "image/webp", "image/gif", "image/heic", "image/heif"]);
const VIDEO_MIME_TYPES = new Set(["video/mp4", "video/webm", "video/quicktime", "video/x-matroska"]);

type R2Config = {
  accountId: string;
  accessKeyId: string;
  secretAccessKey: string;
  bucket: string;
  publicBaseUrl: string;
};

function getR2Config() {
  loadWorkspaceRootEnv();
  const config = {
    accountId: process.env.R2_ACCOUNT_ID?.trim(),
    accessKeyId: process.env.R2_ACCESS_KEY_ID?.trim(),
    secretAccessKey: process.env.R2_SECRET_ACCESS_KEY?.trim(),
    bucket: process.env.R2_BUCKET?.trim(),
    publicBaseUrl: process.env.R2_PUBLIC_BASE_URL?.trim().replace(/\/+$/u, "")
  };
  const missing = Object.entries(config)
    .filter(([, value]) => !value)
    .map(([key]) => key);

  if (missing.length > 0) {
    throw new Error(`R2 event storage is not configured. Missing: ${missing.join(", ")}.`);
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

function buildR2PublicUrl(publicBaseUrl: string, storagePath: string) {
  return `${publicBaseUrl}/${storagePath.split("/").map(encodeURIComponent).join("/")}`;
}

function normalizeMimeType(value: string) {
  return value.trim().toLowerCase();
}

function getEventMediaKind(mimeType: string) {
  const normalized = normalizeMimeType(mimeType);
  if (IMAGE_MIME_TYPES.has(normalized)) {
    return "image" as const;
  }
  if (VIDEO_MIME_TYPES.has(normalized)) {
    return "video" as const;
  }
  return null;
}

function extensionFromMimeType(mimeType: string, fallback = "bin") {
  const explicit: Record<string, string> = {
    "image/jpeg": "jpg",
    "image/png": "png",
    "image/webp": "webp",
    "image/gif": "gif",
    "image/heic": "heic",
    "image/heif": "heif",
    "video/mp4": "mp4",
    "video/webm": "webm",
    "video/quicktime": "mov",
    "video/x-matroska": "mkv"
  };
  return explicit[mimeType] ?? mimeType.split("/")[1] ?? fallback;
}

function sanitizeFileName(value: string) {
  const cleaned = value
    .trim()
    .replace(/\s+/g, "-")
    .replace(/[^a-zA-Z0-9._-]/g, "")
    .replace(/-+/g, "-");
  return cleaned || "event-media";
}

function validateFile(file: File) {
  const mimeType = normalizeMimeType(file.type || "application/octet-stream");
  const kind = getEventMediaKind(mimeType);
  if (!kind) {
    throw new Error(`"${file.name}" is not a supported event poster or video format.`);
  }
  const maxBytes = kind === "video" ? MAX_EVENT_VIDEO_BYTES : MAX_EVENT_IMAGE_BYTES;
  if (file.size > maxBytes) {
    throw new Error(
      kind === "video"
        ? `"${file.name}" is too large. Keep each event video under 60 MB.`
        : `"${file.name}" is too large. Keep each event image under 12 MB.`
    );
  }
  return { kind, mimeType };
}

function validateRegistrationFile(file: File) {
  const mimeType = normalizeMimeType(file.type || "application/octet-stream");
  const kind = getEventMediaKind(mimeType);
  if (kind !== "image") {
    throw new Error(`"${file.name}" is not a supported registration image format.`);
  }
  if (file.size > MAX_REGISTRATION_IMAGE_BYTES) {
    throw new Error(`"${file.name}" is too large. Keep each registration image under 10 MB.`);
  }
  return { kind, mimeType };
}

async function persistAssets(input: {
  tenantId: string;
  userId: string;
  scope: string;
  files: File[];
  maxItems: number;
  validate: (file: File) => { kind: "image" | "video"; mimeType: string };
}): Promise<CampusEventMediaAsset[]> {
  if (input.files.length === 0) {
    return [];
  }
  if (input.files.length > input.maxItems) {
    throw new Error(`You can upload up to ${input.maxItems} files here.`);
  }

  const r2 = getR2Config();
  const client = getR2Client(r2);

  return Promise.all(
    input.files.map(async (file) => {
      const { kind, mimeType } = input.validate(file);
      const assetId = randomUUID();
      const buffer = Buffer.from(await file.arrayBuffer());
      const extension = extensionFromMimeType(mimeType, kind === "video" ? "mp4" : "jpg");
      const fileName = sanitizeFileName(file.name || `${kind}.${extension}`);
      const storagePath = `${input.scope}/${assetId}.${extension}`;

      await client.send(
        new PutObjectCommand({
          Bucket: r2.bucket,
          Key: storagePath,
          Body: buffer,
          ContentType: mimeType,
          CacheControl: CACHE_CONTROL,
          Metadata: {
            originalFileName: fileName,
            tenantId: input.tenantId,
            uploaderId: input.userId,
            originModule: "events"
          }
        })
      );

      return {
        id: assetId,
        kind,
        url: buildR2PublicUrl(r2.publicBaseUrl, storagePath),
        fileName,
        mimeType,
        sizeBytes: buffer.byteLength,
        storagePath
      } satisfies CampusEventMediaAsset;
    })
  );
}

export async function deleteEventMediaAssets(assets: CampusEventMediaAsset[]) {
  const storagePaths = assets
    .map((asset) => asset.storagePath?.trim())
    .filter((storagePath): storagePath is string => Boolean(storagePath));
  if (storagePaths.length === 0) {
    return;
  }

  const r2 = getR2Config();
  const client = getR2Client(r2);
  await Promise.allSettled(
    storagePaths.map((storagePath) =>
      client.send(new DeleteObjectCommand({ Bucket: r2.bucket, Key: storagePath }))
    )
  );
}

export async function persistEventMediaAssets(input: {
  tenantId: string;
  userId: string;
  eventId: string;
  files: File[];
}): Promise<CampusEventMediaAsset[]> {
  return persistAssets({
    tenantId: input.tenantId,
    userId: input.userId,
    scope: `events/${input.tenantId}/${input.userId}/${input.eventId}`,
    files: input.files,
    maxItems: MAX_EVENT_MEDIA_ITEMS,
    validate: validateFile
  });
}

export async function persistEventRegistrationAssets(input: {
  tenantId: string;
  userId: string;
  eventId: string;
  registrationId: string;
  files: File[];
}): Promise<CampusEventMediaAsset[]> {
  return persistAssets({
    tenantId: input.tenantId,
    userId: input.userId,
    scope: `events/${input.tenantId}/${input.userId}/${input.eventId}/registrations/${input.registrationId}`,
    files: input.files,
    maxItems: MAX_REGISTRATION_MEDIA_ITEMS,
    validate: validateRegistrationFile
  });
}
