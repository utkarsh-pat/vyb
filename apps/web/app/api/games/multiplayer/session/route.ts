import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { readDevSessionFromCookieStore } from "../../../../../src/lib/dev-session";
import {
  isBackendRequestError,
  postBackendJson
} from "../../../../../src/lib/backend";

export async function POST(request: Request) {
  const viewer = readDevSessionFromCookieStore(await cookies());
  if (!viewer) return NextResponse.json({ error: { code: "UNAUTHENTICATED", message: "Sign in to play online." } }, { status: 401 });
  try {
    const input = await request.json() as { game?: unknown; roomCode?: unknown };
    const session = await postBackendJson<{
      roomCode: string;
      wsUrl: string;
      expiresAt: number;
    }>("/v1/games/multiplayer/session", input, viewer);
    return NextResponse.json(session, {
      headers: { "cache-control": "private, no-store" }
    });
  } catch (error) {
    console.error("[games-session] create.failed", error);
    if (isBackendRequestError(error)) {
      return NextResponse.json({
        error: { code: error.code, message: error.message }
      }, { status: error.statusCode });
    }
    return NextResponse.json({ error: { code: "SESSION_FAILED", message: "Could not prepare the online game room." } }, { status: 503 });
  }
}
