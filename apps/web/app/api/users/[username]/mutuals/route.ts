import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { getCampusUserConnections } from "../../../../../src/lib/backend";
import { readDevSessionFromCookieStore } from "../../../../../src/lib/dev-session";

export async function GET(_request: Request, context: { params: Promise<{ username: string }> }) {
  const viewer = readDevSessionFromCookieStore(await cookies());
  if (!viewer) {
    return NextResponse.json({ error: { code: "UNAUTHENTICATED", message: "Sign in to view mutual connections." } }, { status: 401 });
  }
  return NextResponse.json(
    await getCampusUserConnections(viewer, (await context.params).username, "mutuals"),
    { headers: { "cache-control": "private, no-store" } }
  );
}
