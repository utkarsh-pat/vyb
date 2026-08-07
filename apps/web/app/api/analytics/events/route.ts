import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { readDevSessionFromCookieStore } from "../../../../src/lib/dev-session";
import { proxyBackendMutation } from "../../../../src/lib/backend";

const MAX_ANALYTICS_BODY_BYTES = 32 * 1024;

async function readBoundedJson(request: Request) {
  if (!request.body) return {};
  const reader = request.body.getReader();
  const chunks: Uint8Array[] = [];
  let total = 0;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    total += value.byteLength;
    if (total > MAX_ANALYTICS_BODY_BYTES) {
      await reader.cancel();
      return { tooLarge: true } as const;
    }
    chunks.push(value);
  }
  const body = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) {
    body.set(chunk, offset);
    offset += chunk.byteLength;
  }
  try {
    return { payload: JSON.parse(new TextDecoder().decode(body)) as unknown } as const;
  } catch {
    return { payload: null } as const;
  }
}

export async function POST(request: Request) {
  const viewer = readDevSessionFromCookieStore(await cookies());
  if (!viewer) {
    return NextResponse.json({ error: { code: "UNAUTHENTICATED", message: "Sign in is required." } }, { status: 401 });
  }
  if (!(request.headers.get("content-type") ?? "").toLowerCase().startsWith("application/json")) {
    return NextResponse.json({ error: { code: "UNSUPPORTED_MEDIA_TYPE", message: "Use application/json." } }, { status: 415 });
  }
  const declaredLength = Number(request.headers.get("content-length") ?? 0);
  if (Number.isFinite(declaredLength) && declaredLength > MAX_ANALYTICS_BODY_BYTES) {
    return NextResponse.json({ error: { code: "PAYLOAD_TOO_LARGE", message: "Analytics batch is too large." } }, { status: 413 });
  }
  const parsed = await readBoundedJson(request);
  if ("tooLarge" in parsed) {
    return NextResponse.json({ error: { code: "PAYLOAD_TOO_LARGE", message: "Analytics batch is too large." } }, { status: 413 });
  }
  const payload = parsed.payload;
  if (!payload) {
    return NextResponse.json({ error: { code: "INVALID_JSON", message: "Request body must be valid JSON." } }, { status: 400 });
  }
  const upstream = await proxyBackendMutation("/v1/analytics/events", "POST", payload, viewer);
  return new Response(await upstream.text(), {
    status: upstream.status,
    headers: { "content-type": upstream.headers.get("content-type") ?? "application/json; charset=utf-8" }
  });
}
