import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import {
  getContentMeasurementPreference,
  proxyBackendMutation
} from "../../../../src/lib/backend";
import { readDevSessionFromCookieStore } from "../../../../src/lib/dev-session";

async function viewerOrResponse() {
  const viewer = readDevSessionFromCookieStore(await cookies());
  return viewer ?? NextResponse.json(
    { error: { code: "UNAUTHENTICATED", message: "Sign in to manage analytics privacy." } },
    { status: 401 }
  );
}

export async function GET() {
  const resolved = await viewerOrResponse();
  if (resolved instanceof NextResponse) return resolved;
  return NextResponse.json(await getContentMeasurementPreference(resolved), {
    headers: { "cache-control": "private, no-store" }
  });
}

export async function PUT(request: Request) {
  const resolved = await viewerOrResponse();
  if (resolved instanceof NextResponse) return resolved;
  const payload = await request.json().catch(() => null);
  return proxyBackendMutation("/v1/users/me/content-measurement", "PUT", payload ?? {}, resolved);
}

export async function DELETE() {
  const resolved = await viewerOrResponse();
  if (resolved instanceof NextResponse) return resolved;
  return proxyBackendMutation("/v1/users/me/content-measurement", "DELETE", {}, resolved);
}
