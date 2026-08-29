import { createHmac, randomBytes } from "node:crypto";
import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { isOnlineGameSlug } from "@vyb/game-engine";
import { readDevSessionFromCookieStore } from "../../../../../src/lib/dev-session";
import { getInternalApiKey } from "../../../../../src/lib/internal-api-key";
import { getInternalRelationshipBlockedUserIds } from "../../../../../src/lib/backend";

const TOKEN_TTL_MS = 5 * 60 * 1000;
const ROOM_CODE_PATTERN = /^[A-Z2-9]{6}$/u;

function gameSecret() {
  const configured = process.env.VYB_GAME_SESSION_SECRET?.trim();
  if (configured) return configured;
  if (process.env.NODE_ENV !== "production") return getInternalApiKey();
  throw new Error("VYB_GAME_SESSION_SECRET must be configured in production.");
}

function socketOrigin(request: Request) {
  const configured = process.env.VYB_GAMES_WS_URL?.trim();
  if (configured) return configured.replace(/\/$/u, "");
  if (process.env.NODE_ENV !== "production") return "ws://localhost:8787";
  throw new Error("VYB_GAMES_WS_URL must be configured in production.");
}

function createRoomCode() {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  return [...randomBytes(6)].map((byte) => alphabet[byte % alphabet.length]).join("");
}

function sign(payload: Record<string, unknown>) {
  const encoded = Buffer.from(JSON.stringify(payload), "utf8").toString("base64url");
  const signature = createHmac("sha256", gameSecret()).update(encoded).digest("base64url");
  return `${encoded}.${signature}`;
}

export async function POST(request: Request) {
  const viewer = readDevSessionFromCookieStore(await cookies());
  if (!viewer) return NextResponse.json({ error: { code: "UNAUTHENTICATED", message: "Sign in to play online." } }, { status: 401 });
  try {
    const input = await request.json() as { game?: unknown; roomCode?: unknown };
    if (typeof input.game !== "string" || !isOnlineGameSlug(input.game)) {
      return NextResponse.json({ error: { code: "INVALID_GAME", message: "Choose Chess, Ludo or UNO." } }, { status: 400 });
    }
    const requestedCode = typeof input.roomCode === "string" ? input.roomCode.trim().toUpperCase() : "";
    if (requestedCode && !ROOM_CODE_PATTERN.test(requestedCode)) {
      return NextResponse.json({ error: { code: "INVALID_ROOM", message: "Enter a valid 6-character room code." } }, { status: 400 });
    }
    const roomCode = requestedCode || createRoomCode();
    const blockedUsers = await getInternalRelationshipBlockedUserIds({
      tenantId: viewer.tenantId,
      viewerUserId: viewer.userId
    });
    const username = viewer.email.split("@")[0]?.replace(/[^a-z0-9._-]/giu, "").toLowerCase() || "player";
    const expiresAt = Date.now() + TOKEN_TTL_MS;
    const token = sign({
      tenantId: viewer.tenantId,
      userId: viewer.userId,
      membershipId: viewer.membershipId,
      displayName: viewer.displayName || username,
      username,
      blockedUserIds: blockedUsers.blockedUserIds,
      game: input.game,
      roomCode,
      exp: expiresAt
    });
    const wsUrl = new URL("/ws", socketOrigin(request));
    wsUrl.searchParams.set("token", token);
    return NextResponse.json({ roomCode, wsUrl: wsUrl.toString(), expiresAt });
  } catch (error) {
    console.error("[games-session] create.failed", error);
    return NextResponse.json({ error: { code: "SESSION_FAILED", message: "Could not prepare the online game room." } }, { status: 503 });
  }
}
