import {
  HeadObjectCommand,
  ListObjectsV2Command,
  PutObjectCommand,
  S3Client
} from "@aws-sdk/client-s3";
import { createHash } from "node:crypto";

const execute = process.argv.includes("--execute");
const sourceBucket = requiredEnv("GCS_SOURCE_BUCKET");
const googleAccessToken = requiredEnv("GOOGLE_OAUTH_ACCESS_TOKEN");
const r2AccountId = requiredEnv("R2_ACCOUNT_ID");
const r2AccessKeyId = requiredEnv("R2_ACCESS_KEY_ID");
const r2SecretAccessKey = requiredEnv("R2_SECRET_ACCESS_KEY");
const r2Bucket = requiredEnv("R2_BUCKET");
const concurrency = positiveInteger(process.env.MEDIA_MIGRATION_CONCURRENCY, 3);

const r2 = new S3Client({
  region: "auto",
  endpoint: `https://${r2AccountId}.r2.cloudflarestorage.com`,
  credentials: {
    accessKeyId: r2AccessKeyId,
    secretAccessKey: r2SecretAccessKey
  }
});

function requiredEnv(name) {
  const value = process.env[name]?.trim();
  if (!value) {
    throw new Error(`${name} is required.`);
  }
  return value;
}

function positiveInteger(value, fallback) {
  const parsed = Number.parseInt(value ?? "", 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function googleObjectUrl(object) {
  const bucket = encodeURIComponent(sourceBucket);
  const name = encodeURIComponent(object.name);
  const generation = object.generation ? `&generation=${encodeURIComponent(object.generation)}` : "";
  return `https://storage.googleapis.com/download/storage/v1/b/${bucket}/o/${name}?alt=media${generation}`;
}

async function listGoogleObjects() {
  const objects = [];
  let pageToken;
  do {
    const params = new URLSearchParams({
      maxResults: "1000",
      fields: "items(name,size,md5Hash,contentType,generation,cacheControl),nextPageToken"
    });
    if (pageToken) {
      params.set("pageToken", pageToken);
    }
    const response = await fetch(
      `https://storage.googleapis.com/storage/v1/b/${encodeURIComponent(sourceBucket)}/o?${params}`,
      { headers: { authorization: `Bearer ${googleAccessToken}` } }
    );
    if (!response.ok) {
      throw new Error(`Google Storage list failed with HTTP ${response.status}.`);
    }
    const page = await response.json();
    objects.push(...(page.items ?? []));
    pageToken = page.nextPageToken;
  } while (pageToken);
  return objects;
}

async function listR2Objects() {
  const objects = new Map();
  let continuationToken;
  do {
    const page = await r2.send(
      new ListObjectsV2Command({
        Bucket: r2Bucket,
        ContinuationToken: continuationToken
      })
    );
    for (const object of page.Contents ?? []) {
      objects.set(object.Key, Number(object.Size ?? 0));
    }
    continuationToken = page.IsTruncated ? page.NextContinuationToken : undefined;
  } while (continuationToken);
  return objects;
}

async function destinationMatches(source) {
  try {
    const head = await r2.send(new HeadObjectCommand({ Bucket: r2Bucket, Key: source.name }));
    return (
      Number(head.ContentLength ?? -1) === Number(source.size) &&
      (!source.md5Hash || head.Metadata?.["source-md5"] === source.md5Hash)
    );
  } catch (error) {
    if (error?.name === "NotFound" || error?.$metadata?.httpStatusCode === 404) {
      return false;
    }
    throw error;
  }
}

async function migrateObject(source) {
  if (await destinationMatches(source)) {
    return { status: "skipped", bytes: Number(source.size) };
  }
  if (!execute) {
    return { status: "planned", bytes: Number(source.size) };
  }

  const response = await fetch(googleObjectUrl(source), {
    headers: { authorization: `Bearer ${googleAccessToken}` }
  });
  if (!response.ok) {
    throw new Error(`Google Storage download failed for ${source.name}: HTTP ${response.status}.`);
  }
  const body = Buffer.from(await response.arrayBuffer());
  if (body.byteLength !== Number(source.size)) {
    throw new Error(
      `Source size mismatch for ${source.name}: expected ${source.size}, received ${body.byteLength}.`
    );
  }
  if (source.md5Hash) {
    const actualMd5 = createHash("md5").update(body).digest("base64");
    if (actualMd5 !== source.md5Hash) {
      throw new Error(`Source MD5 mismatch for ${source.name}.`);
    }
  }

  await r2.send(
    new PutObjectCommand({
      Bucket: r2Bucket,
      Key: source.name,
      Body: body,
      ContentLength: body.byteLength,
      ContentType: source.contentType || "application/octet-stream",
      CacheControl: source.cacheControl || "public, max-age=31536000, immutable",
      Metadata: {
        "source-bucket": sourceBucket,
        "source-generation": String(source.generation ?? ""),
        "source-md5": String(source.md5Hash ?? "")
      }
    })
  );

  if (!(await destinationMatches(source))) {
    throw new Error(`R2 verification failed for ${source.name}.`);
  }
  return { status: "migrated", bytes: body.byteLength };
}

async function runPool(items, workerCount, worker) {
  const results = new Array(items.length);
  let nextIndex = 0;
  async function runWorker() {
    while (nextIndex < items.length) {
      const index = nextIndex++;
      results[index] = await worker(items[index], index);
    }
  }
  await Promise.all(Array.from({ length: Math.min(workerCount, items.length) }, runWorker));
  return results;
}

const sourceObjects = await listGoogleObjects();
const sourceBytes = sourceObjects.reduce((total, object) => total + Number(object.size), 0);
const before = await listR2Objects();

console.log(
  JSON.stringify({
    mode: execute ? "execute" : "plan",
    sourceBucket,
    destinationBucket: r2Bucket,
    sourceObjects: sourceObjects.length,
    sourceBytes,
    destinationObjectsBefore: before.size
  })
);

let completed = 0;
const results = await runPool(sourceObjects, concurrency, async (object) => {
  const result = await migrateObject(object);
  completed += 1;
  console.log(
    JSON.stringify({
      progress: `${completed}/${sourceObjects.length}`,
      status: result.status,
      key: object.name,
      bytes: result.bytes
    })
  );
  return result;
});

const after = await listR2Objects();
const summary = results.reduce(
  (totals, result) => {
    totals[result.status] = (totals[result.status] ?? 0) + 1;
    totals.bytes += result.bytes;
    return totals;
  },
  { bytes: 0 }
);

const missing = sourceObjects.filter(
  (object) => after.get(object.name) !== Number(object.size)
);
if (execute && missing.length > 0) {
  throw new Error(`${missing.length} R2 objects are missing or have an incorrect size after migration.`);
}

console.log(
  JSON.stringify({
    ...summary,
    destinationObjectsAfter: after.size,
    verifiedSourceObjects: sourceObjects.length - missing.length,
    verificationFailures: missing.length
  })
);
