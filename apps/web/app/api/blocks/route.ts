import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { getBlockedCampusUsers } from "../../../src/lib/backend";
import { readDevSessionFromCookieStore } from "../../../src/lib/dev-session";

export async function GET() {
  const viewer = readDevSessionFromCookieStore(await cookies());
  if (!viewer) {
    return NextResponse.json({ error: { code: "UNAUTHENTICATED", message: "Sign in to view blocked accounts." } }, { status: 401 });
  }
  return NextResponse.json(await getBlockedCampusUsers(viewer), {
    headers: { "cache-control": "private, no-store" }
  });
}
