import { GetObjectCommand, S3Client } from "@aws-sdk/client-s3";
import { NextResponse } from "next/server";
import { loadWorkspaceRootEnv } from "../../../../src/lib/server-env";

export const runtime = "nodejs";

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

  return config as {
    accountId: string;
    accessKeyId: string;
    secretAccessKey: string;
    bucket: string;
  };
}

function getR2Client(config: ReturnType<typeof getR2Config>) {
  return new S3Client({
    region: "auto",
    endpoint: `https://${config.accountId}.r2.cloudflarestorage.com`,
    credentials: {
      accessKeyId: config.accessKeyId,
      secretAccessKey: config.secretAccessKey
    }
  });
}

function isNotFound(error: unknown) {
  if (!error || typeof error !== "object") {
    return false;
  }

  const candidate = error as {
    name?: string;
    $metadata?: { httpStatusCode?: number };
  };
  return candidate.name === "NoSuchKey" || candidate.name === "NotFound" || candidate.$metadata?.httpStatusCode === 404;
}

export async function GET(
  _request: Request,
  context: {
    params: Promise<{
      segments?: string[];
    }>;
  }
) {
  const { segments = [] } = await context.params;
  const normalizedSegments = segments.filter((segment) => segment && segment !== "." && segment !== "..");

  if (normalizedSegments.length === 0 || normalizedSegments.length !== segments.length) {
    return NextResponse.json(
      {
        error: {
          code: "INVALID_MEDIA_PATH",
          message: "Media file path is invalid."
        }
      },
      { status: 400 }
    );
  }

  try {
    const config = getR2Config();
    const result = await getR2Client(config).send(
      new GetObjectCommand({
        Bucket: config.bucket,
        Key: normalizedSegments.join("/")
      })
    );

    if (!result.Body) {
      throw new Error("R2 returned an empty media response.");
    }

    const body = Buffer.from(await result.Body.transformToByteArray());
    return new Response(body, {
      status: 200,
      headers: {
        "content-type": result.ContentType ?? "application/octet-stream",
        "content-length": String(body.byteLength),
        "cache-control": result.CacheControl ?? "public, max-age=31536000, immutable",
        "x-content-type-options": "nosniff"
      }
    });
  } catch (error) {
    if (isNotFound(error)) {
      return NextResponse.json(
        {
          error: {
            code: "MEDIA_NOT_FOUND",
            message: "That media file could not be found."
          }
        },
        { status: 404 }
      );
    }

    console.error("R2 media proxy failed.", error);
    return NextResponse.json(
      {
        error: {
          code: "MEDIA_UNAVAILABLE",
          message: "Media is temporarily unavailable."
        }
      },
      { status: 503 }
    );
  }
}
