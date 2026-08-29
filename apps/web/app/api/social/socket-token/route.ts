import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { getSocialRealtimeSession, isBackendRequestError } from "../../../../src/lib/backend";
import { readDevSessionFromCookieStore } from "../../../../src/lib/dev-session";

function buildError(status: number, code: string, message: string) {
  return NextResponse.json({ error: { code, message } }, { status });
}

export async function GET() {
  const viewer = readDevSessionFromCookieStore(await cookies());
  if (!viewer) {
    return buildError(401, "UNAUTHENTICATED", "You must sign in before opening realtime social updates.");
  }

  try {
    return NextResponse.json(await getSocialRealtimeSession(viewer), {
      headers: { "cache-control": "no-store, no-cache, must-revalidate" }
    });
  } catch (error) {
    if (isBackendRequestError(error)) {
      return buildError(error.statusCode, error.code, error.message);
    }
    return buildError(503, "SOCIAL_REALTIME_UNAVAILABLE", "Feed realtime is temporarily unavailable.");
  }
}
