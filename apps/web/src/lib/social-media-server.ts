import "server-only";

import { randomUUID } from "node:crypto";
import path from "node:path";
import { loadWorkspaceRootEnv } from "./server-env";

const MAX_SOCIAL_IMAGE_BYTES = 4 * 1024 * 1024;
const MAX_SOCIAL_VIDEO_BYTES = 40 * 1024 * 1024;
const IMAGE_MIME_TYPES = new Set(["image/jpeg", "image/png", "image/webp", "image/gif", "image/heic", "image/heif"]);
const VIDEO_MIME_TYPES = new Set(["video/mp4", "video/webm", "video/quicktime"]);
const SOCIAL_MEDIA_CACHE_CONTROL = "public, max-age=31536000, immutable";

function normalizeMimeType(value: string) {
  return value.split(";")[0]?.trim().toLowerCase() || "application/octet-stream";
}

function getLocalMediaRoot() {
  loadWorkspaceRootEnv();
  const configuredRoot =
    process.env.VYB_LOCAL_MEDIA_ROOT ??
    process.env.TMPDIR ??
    process.env.TEMP ??
    process.env.TMP ??
    path.join(process.cwd(), ".tmp");
  return path.join(configuredRoot, "vyb-social-media");
}

export function resolveLocalSocialMediaFilePath(storagePath: string) {
  const rootPath = path.resolve(getLocalMediaRoot());
  const relativePath = storagePath.split("/").filter(Boolean).join(path.sep);
  const absolutePath = path.resolve(rootPath, relativePath);
  const relativeCheck = path.relative(rootPath, absolutePath);
  if (relativeCheck.startsWith("..") || path.isAbsolute(relativeCheck)) {
    throw new Error("Invalid local media path.");
  }
  return absolutePath;
}

function getSocialMediaKind(mimeType: string) {
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
    "video/quicktime": "mov"
  };
  return explicit[mimeType] ?? mimeType.split("/")[1] ?? fallback;
}

function resolveMaxBytesForMediaType(mediaType: "image" | "video") {
  return mediaType === "video" ? MAX_SOCIAL_VIDEO_BYTES : MAX_SOCIAL_IMAGE_BYTES;
}

function resolveAssetType(intent: "post" | "story" | "vibe") {
  return intent === "story"
    ? { assetType: "stories", placement: "feed" }
    : { assetType: "posts", placement: intent === "vibe" ? "vibe" : "feed" };
}

function sanitizeFileName(value: string, fallback: string) {
  const cleaned = value
    .trim()
    .replace(/\s+/g, "-")
    .replace(/[^a-zA-Z0-9._-]/g, "")
    .replace(/-+/g, "-");
  return cleaned || fallback;
}

export function inferSocialMediaContentType(storagePath: string) {
  const extension = path.extname(storagePath).toLowerCase();
  const byExtension: Record<string, string> = {
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".png": "image/png",
    ".webp": "image/webp",
    ".gif": "image/gif",
    ".heic": "image/heic",
    ".heif": "image/heif",
    ".mp4": "video/mp4",
    ".webm": "video/webm",
    ".mov": "video/quicktime"
  };
  return byExtension[extension] ?? "application/octet-stream";
}

export function planSocialMediaAssetUpload(input: {
  tenantId: string;
  userId: string;
  intent: "post" | "story" | "vibe";
  fileName: string;
  mimeType: string;
  sizeBytes: number;
}) {
  const mimeType = normalizeMimeType(input.mimeType || "application/octet-stream");
  const mediaType = getSocialMediaKind(mimeType);
  if (!mediaType) {
    throw new Error("Only image and video uploads are supported right now.");
  }
  if (!Number.isFinite(input.sizeBytes) || input.sizeBytes <= 0) {
    throw new Error("Upload payload is empty.");
  }

  const maxBytes = resolveMaxBytesForMediaType(mediaType);
  if (input.sizeBytes > maxBytes) {
    throw new Error(
      mediaType === "video"
        ? "Video is still too large after optimization. Keep it under 40 MB."
        : "Image is too large right now. Keep it under 4 MB."
    );
  }

  const { assetType, placement } = resolveAssetType(input.intent);
  const assetId = randomUUID();
  const extension = extensionFromMimeType(mimeType, mediaType === "video" ? "mp4" : "jpg");
  const originalFileName = sanitizeFileName(input.fileName || `${assetType}.${extension}`, `${assetType}.${extension}`);
  const storagePath = `social/${input.tenantId}/${assetType}/${placement}/${input.userId}/${assetId}.${extension}`;

  return {
    mediaType,
    mimeType,
    sizeBytes: input.sizeBytes,
    originalFileName,
    storagePath,
    cacheControl: SOCIAL_MEDIA_CACHE_CONTROL,
    customMetadata: {
      tenant_id: input.tenantId,
      uploader_id: input.userId,
      origin_module: "social",
      upload_intent: input.intent,
      original_file_name: originalFileName
    }
  };
}
