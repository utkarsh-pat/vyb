import { createHmac, randomBytes } from "node:crypto";
import { hydrateViewerRelationshipPolicy } from "../shared/relationship-policy.mjs";

const TOKEN_TTL_MS = 5 * 60 * 1000;
const ROOM_CODE_PATTERN = /^[A-Z2-9]{6}$/u;
const ONLINE_GAMES = new Set(["chess", "ludo", "uno"]);

export class MultiplayerSessionError extends Error {
  constructor(status, code, message) {
    super(message);
    this.name = "MultiplayerSessionError";
    this.status = status;
    this.code = code;
  }
}

export function isOnlineGame(value) {
  return typeof value === "string" && ONLINE_GAMES.has(value);
}

export function normalizeRoomCode(value) {
  if (value === undefined || value === null || value === "") return "";
  if (typeof value !== "string") return null;
  const normalized = value.trim().toUpperCase();
  return ROOM_CODE_PATTERN.test(normalized) ? normalized : null;
}

function createRoomCode() {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  return [...randomBytes(6)].map((byte) => alphabet[byte % alphabet.length]).join("");
}

function defaultSecret() {
  const secret = process.env.VYB_GAME_SESSION_SECRET?.trim();
  if (secret) return secret;
  if (process.env.NODE_ENV !== "production") {
    const fallback = process.env.VYB_INTERNAL_API_KEY?.trim();
    if (fallback) return fallback;
  }
  throw new Error("VYB_GAME_SESSION_SECRET is not configured.");
}

function defaultSocketOrigin() {
  const configured = process.env.VYB_GAMES_WS_URL?.trim();
  if (configured) return configured.replace(/\/$/u, "");
  if (process.env.NODE_ENV !== "production") return "ws://localhost:8787";
  throw new Error("VYB_GAMES_WS_URL is not configured.");
}

function sign(payload, secret) {
  const encoded = Buffer.from(JSON.stringify(payload), "utf8").toString("base64url");
  const signature = createHmac("sha256", secret).update(encoded).digest("base64url");
  return `${encoded}.${signature}`;
}

export function createMultiplayerSessionService({
  now = () => Date.now(),
  getSecret = defaultSecret,
  getSocketOrigin = defaultSocketOrigin,
  hydratePolicy = hydrateViewerRelationshipPolicy,
  generateRoomCode = createRoomCode
} = {}) {
  return {
    async create(viewer, input) {
      if (!isOnlineGame(input?.game)) {
        throw new MultiplayerSessionError(400, "INVALID_GAME", "Choose Chess, Ludo or UNO.");
      }
      const requestedCode = normalizeRoomCode(input?.roomCode);
      if (requestedCode === null) {
        throw new MultiplayerSessionError(400, "INVALID_ROOM", "Enter a valid 6-character room code.");
      }

      const policy = await hydratePolicy(viewer);
      const roomCode = requestedCode || generateRoomCode();
      const expiresAt = now() + TOKEN_TTL_MS;
      const username = viewer.email.split("@")[0]?.replace(/[^a-z0-9._-]/giu, "").toLowerCase() || "player";
      const token = sign({
        tenantId: viewer.tenantId,
        userId: viewer.userId,
        membershipId: viewer.membershipId,
        displayName: viewer.displayName || username,
        username,
        blockedUserIds: [...policy.blockedUserIds],
        game: input.game,
        roomCode,
        exp: expiresAt
      }, getSecret());
      const wsUrl = new URL("/ws", getSocketOrigin());
      wsUrl.searchParams.set("token", token);

      return { roomCode, wsUrl: wsUrl.toString(), expiresAt };
    }
  };
}

export const multiplayerSessionService = createMultiplayerSessionService();
