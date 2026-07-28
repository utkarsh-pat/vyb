import {
  DeleteObjectCommand,
  GetObjectCommand,
  HeadObjectCommand,
  PutObjectCommand,
  S3Client
} from "@aws-sdk/client-s3";
import { loadRootEnv } from "../../../../packages/config/src/index.mjs";

function getConfig() {
  loadRootEnv();
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
  return config;
}

let cachedClient = null;
let cachedAccountId = null;

function getClient(config) {
  if (cachedClient && cachedAccountId === config.accountId) {
    return cachedClient;
  }

  cachedClient = new S3Client({
    region: "auto",
    endpoint: `https://${config.accountId}.r2.cloudflarestorage.com`,
    credentials: {
      accessKeyId: config.accessKeyId,
      secretAccessKey: config.secretAccessKey
    }
  });
  cachedAccountId = config.accountId;
  return cachedClient;
}

function isNotFound(error) {
  return error?.name === "NotFound" || error?.$metadata?.httpStatusCode === 404;
}

function encodeCustomMetadata(value) {
  return Buffer.from(JSON.stringify(value ?? {}), "utf8").toString("base64");
}

function decodeCustomMetadata(value) {
  if (!value) {
    return {};
  }

  try {
    return JSON.parse(Buffer.from(value, "base64").toString("utf8"));
  } catch {
    return {};
  }
}

function createFile(client, bucket, key) {
  return {
    async save(body, options = {}) {
      const metadata = options.metadata ?? {};
      const customMetadata = metadata.metadata ?? {};
      await client.send(
        new PutObjectCommand({
          Bucket: bucket,
          Key: key,
          Body: body,
          ContentType: metadata.contentType,
          CacheControl: metadata.cacheControl,
          Metadata: {
            "vyb-metadata": encodeCustomMetadata(customMetadata)
          }
        })
      );
    },

    async download() {
      const result = await client.send(new GetObjectCommand({ Bucket: bucket, Key: key }));
      if (!result.Body) {
        throw new Error(`R2 object ${key} has no body.`);
      }
      return [Buffer.from(await result.Body.transformToByteArray())];
    },

    async delete({ ignoreNotFound = false } = {}) {
      try {
        await client.send(new DeleteObjectCommand({ Bucket: bucket, Key: key }));
      } catch (error) {
        if (!ignoreNotFound || !isNotFound(error)) {
          throw error;
        }
      }
    },

    async exists() {
      try {
        await client.send(new HeadObjectCommand({ Bucket: bucket, Key: key }));
        return [true];
      } catch (error) {
        if (isNotFound(error)) {
          return [false];
        }
        throw error;
      }
    },

    async getMetadata() {
      const result = await client.send(new HeadObjectCommand({ Bucket: bucket, Key: key }));
      return [
        {
          size: result.ContentLength,
          contentType: result.ContentType,
          cacheControl: result.CacheControl,
          metadata: decodeCustomMetadata(result.Metadata?.["vyb-metadata"])
        }
      ];
    }
  };
}

export function getR2Bucket() {
  const config = getConfig();
  const client = getClient(config);
  return {
    name: config.bucket,
    file(key) {
      return createFile(client, config.bucket, key);
    }
  };
}
