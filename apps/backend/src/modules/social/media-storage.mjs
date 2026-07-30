import { randomUUID } from "node:crypto";
import { PutObjectCommand, S3Client } from "@aws-sdk/client-s3";
import { loadRootEnv } from "../../../../../packages/config/src/index.mjs";

const MAX_SOCIAL_IMAGE_BYTES = 4 * 1024 * 1024;
const MAX_SOCIAL_VIDEO_BYTES = 40 * 1024 * 1024;

const IMAGE_MIME_TYPES = new Set(["image/jpeg", "image/png", "image/webp", "image/gif", "image/heic", "image/heif"]);
const VIDEO_MIME_TYPES = new Set(["video/mp4", "video/webm", "video/quicktime"]);

function normalizeMimeType(value) {
  return String(value ?? "").split(";")[0]?.trim().toLowerCase() || "application/octet-stream";
}

function getSocialMediaKind(mimeType) {
  const normalized = normalizeMimeType(mimeType);

  if (IMAGE_MIME_TYPES.has(normalized)) {
    return "image";
  }

  if (VIDEO_MIME_TYPES.has(normalized)) {
    return "video";
  }

  return null;
}

function extensionFromMimeType(mimeType, fallback = "bin") {
  const explicit = {
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

function getR2Config() {
  loadRootEnv();
  const config = {
    accountId: process.env.R2_ACCOUNT_ID?.trim(),
    accessKeyId: process.env.R2_ACCESS_KEY_ID?.trim(),
    secretAccessKey: process.env.R2_SECRET_ACCESS_KEY?.trim(),
    bucket: process.env.R2_BUCKET?.trim(),
    publicBaseUrl: process.env.R2_PUBLIC_BASE_URL?.trim()?.replace(/\/+$/u, "")
  };
  const missing = Object.entries(config)
    .filter(([, value]) => !value)
    .map(([key]) => key);
  if (missing.length > 0) {
    throw new Error(`R2 media storage is not configured. Missing: ${missing.join(", ")}.`);
  }
  return config;
}

let cachedR2Client = null;
let cachedR2ClientKey = null;

function getR2Client(config) {
  const clientKey = `${config.accountId}:${config.accessKeyId}`;
  if (cachedR2Client && cachedR2ClientKey === clientKey) {
    return cachedR2Client;
  }

  cachedR2Client = new S3Client({
    region: "auto",
    endpoint: `https://${config.accountId}.r2.cloudflarestorage.com`,
    credentials: {
      accessKeyId: config.accessKeyId,
      secretAccessKey: config.secretAccessKey
    }
  });
  cachedR2ClientKey = clientKey;
  return cachedR2Client;
}

function resolveAssetType(intent) {
  if (intent === "avatar") {
    return {
      assetType: "profiles",
      placement: "avatar"
    };
  }

  if (intent === "story") {
    return {
      assetType: "stories",
      placement: "feed"
    };
  }

  return {
    assetType: "posts",
    placement: intent === "vibe" ? "vibe" : "feed"
  };
}

function sanitizeFileName(value, fallback) {
  const cleaned = String(value ?? "")
    .trim()
    .replace(/\s+/g, "-")
    .replace(/[^a-zA-Z0-9._-]/g, "")
    .replace(/-+/g, "-");

  return cleaned || fallback;
}

export async function persistSocialMediaAsset(input) {
  const r2 = getR2Config();

  const mimeType = normalizeMimeType(input.mimeType);
  const mediaType = getSocialMediaKind(mimeType);

  if (!mediaType) {
    throw new Error("Only image and video uploads are supported right now.");
  }

  if (typeof input.base64Data !== "string" || !input.base64Data.trim()) {
    throw new Error("Upload payload is missing media bytes.");
  }

  const buffer = Buffer.from(input.base64Data, "base64");
  if (buffer.byteLength <= 0) {
    throw new Error("Upload payload is empty.");
  }

  const maxBytes = mediaType === "video" ? MAX_SOCIAL_VIDEO_BYTES : MAX_SOCIAL_IMAGE_BYTES;
  if (buffer.byteLength > maxBytes) {
    throw new Error(
      mediaType === "video"
        ? "Video is still too large after optimization. Keep it under 40 MB."
        : "Image is too large right now. Keep it under 4 MB."
    );
  }

  const { assetType, placement } = resolveAssetType(input.intent);
  const assetId = randomUUID();
  const extension = extensionFromMimeType(mimeType, mediaType === "video" ? "mp4" : "jpg");
  const originalFileName = sanitizeFileName(input.fileName, `${assetType}.${extension}`);
  const storagePath = `social/${input.tenantId}/${assetType}/${placement}/${input.userId}/${assetId}.${extension}`;
  await getR2Client(r2).send(
    new PutObjectCommand({
      Bucket: r2.bucket,
      Key: storagePath,
      Body: buffer,
      ContentType: mimeType,
      CacheControl: "public, max-age=31536000, immutable",
      Metadata: { originalFileName }
    })
  );

  return {
    mediaType,
    mimeType,
    sizeBytes: buffer.byteLength,
    storagePath,
    url: `${r2.publicBaseUrl}/${storagePath.split("/").map(encodeURIComponent).join("/")}`
  };
}
