import { DurableObject } from "cloudflare:workers";
import {
  createChessState,
  createLudoState,
  createUnoState,
  drawUnoCard,
  isOnlineGameSlug,
  moveChess,
  moveLudo,
  playUnoCard,
  redactGameState,
  rollLudo,
  type GamePlayer,
  type GameState,
  type OnlineGameSlug,
  type UnoCard
} from "@vyb/game-engine";

type SessionClaims = {
  tenantId: string;
  userId: string;
  membershipId: string;
  displayName: string;
  username: string;
  blockedUserIds: string[];
  game: OnlineGameSlug;
  roomCode: string;
  exp: number;
};

type SocialSessionClaims = {
  scope: "social.feed";
  tenantId: string;
  userId: string;
  membershipId: string;
  exp: number;
};

type SocialConnectionAttachment = Pick<SocialSessionClaims, "tenantId" | "userId" | "membershipId">;

type SocialFanoutEvent = {
  type: "social.feed.invalidated";
  tenantId: string;
  reason: string;
  excludeMembershipId: string | null;
  emittedAt: number;
};

type ConnectionAttachment = Pick<SessionClaims, "userId" | "displayName" | "username"> & {
  seat: number;
  actionWindowStartedAt?: number;
  actionCount?: number;
  lastChatAt?: number;
};

type StoredPlayer = GamePlayer & {
  blockedUserIds: string[];
};

type StoredRoom = {
  game: OnlineGameSlug;
  roomCode: string;
  hostId: string;
  players: StoredPlayer[];
  state: GameState | null;
  createdAt: number;
};

type ClientMessage =
  | { type: "start" }
  | { type: "rematch" }
  | { type: "chat"; text: string }
  | { type: "emoji"; emoji: string }
  | { type: "chess.move"; from: string; to: string; promotion?: string }
  | { type: "ludo.roll" }
  | { type: "ludo.move"; tokenIndex: number }
  | { type: "uno.play"; cardId: string; color?: UnoCard["color"] }
  | { type: "uno.draw" };

const MAX_CHAT_LENGTH = 280;
const MAX_CHAT_ROWS = 50;
const ROOM_LIMITS: Record<OnlineGameSlug, number> = { chess: 2, ludo: 4, uno: 4 };

function json(value: unknown, status = 200, headers?: HeadersInit) {
  return new Response(JSON.stringify(value), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", ...headers }
  });
}

function decodeBase64Url(value: string) {
  const normalized = value.replace(/-/gu, "+").replace(/_/gu, "/");
  const padding = "=".repeat((4 - normalized.length % 4) % 4);
  return Uint8Array.from(atob(normalized + padding), (character) => character.charCodeAt(0));
}

function encodeBase64Url(value: ArrayBuffer) {
  const bytes = new Uint8Array(value);
  let binary = "";
  bytes.forEach((byte) => { binary += String.fromCharCode(byte); });
  return btoa(binary).replace(/\+/gu, "-").replace(/\//gu, "_").replace(/=+$/gu, "");
}

async function verifySessionToken(token: string | null, secret: string): Promise<SessionClaims | null> {
  if (!token || !secret) return null;
  const [payload, signature, extra] = token.split(".");
  if (!payload || !signature || extra) return null;
  const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const expected = encodeBase64Url(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(payload)));
  if (expected.length !== signature.length) return null;
  let mismatch = 0;
  for (let index = 0; index < expected.length; index += 1) mismatch |= expected.charCodeAt(index) ^ signature.charCodeAt(index);
  if (mismatch !== 0) return null;
  try {
    const claims = JSON.parse(new TextDecoder().decode(decodeBase64Url(payload))) as Partial<SessionClaims>;
    if (
      typeof claims.tenantId !== "string" || typeof claims.userId !== "string" ||
      typeof claims.membershipId !== "string" || typeof claims.displayName !== "string" ||
      typeof claims.username !== "string" || typeof claims.game !== "string" ||
      typeof claims.roomCode !== "string" || typeof claims.exp !== "number" ||
      (claims.blockedUserIds !== undefined &&
        (!Array.isArray(claims.blockedUserIds) || claims.blockedUserIds.some((value) => typeof value !== "string"))) ||
      !isOnlineGameSlug(claims.game) || claims.exp <= Date.now()
    ) return null;
    return { ...claims, blockedUserIds: claims.blockedUserIds ?? [] } as SessionClaims;
  } catch {
    return null;
  }
}

async function verifySocialSessionToken(token: string | null, secret: string): Promise<SocialSessionClaims | null> {
  if (!token || !secret) return null;
  const [payload, signature, extra] = token.split(".");
  if (!payload || !signature || extra) return null;
  const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const expected = encodeBase64Url(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(payload)));
  if (expected.length !== signature.length) return null;
  let mismatch = 0;
  for (let index = 0; index < expected.length; index += 1) mismatch |= expected.charCodeAt(index) ^ signature.charCodeAt(index);
  if (mismatch !== 0) return null;
  try {
    const claims = JSON.parse(new TextDecoder().decode(decodeBase64Url(payload))) as Partial<SocialSessionClaims>;
    if (
      claims.scope !== "social.feed" || typeof claims.tenantId !== "string" ||
      typeof claims.userId !== "string" || typeof claims.membershipId !== "string" ||
      typeof claims.exp !== "number" || claims.exp <= Date.now()
    ) return null;
    return claims as SocialSessionClaims;
  } catch {
    return null;
  }
}

function constantTimeStringEqual(left: string, right: string) {
  if (left.length !== right.length) return false;
  let mismatch = 0;
  for (let index = 0; index < left.length; index += 1) mismatch |= left.charCodeAt(index) ^ right.charCodeAt(index);
  return mismatch === 0;
}

function socialFanoutShardCount(env: Env) {
  const parsed = Number(env.SOCIAL_FANOUT_SHARDS);
  return Number.isInteger(parsed) && parsed >= 1 && parsed <= 64 ? parsed : 16;
}

function socialFanoutShard(membershipId: string, shardCount: number) {
  let hash = 2166136261;
  for (let index = 0; index < membershipId.length; index += 1) {
    hash ^= membershipId.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return (hash >>> 0) % shardCount;
}

async function publishSocialInvalidation(request: Request, env: Env) {
  const authorization = request.headers.get("authorization") ?? "";
  const providedSecret = authorization.startsWith("Bearer ") ? authorization.slice(7) : "";
  if (!providedSecret || !env.SOCIAL_FANOUT_SECRET || !constantTimeStringEqual(providedSecret, env.SOCIAL_FANOUT_SECRET)) {
    return json({ error: { code: "UNAUTHENTICATED", message: "A valid fanout credential is required." } }, 401);
  }
  const contentLength = Number(request.headers.get("content-length") ?? 0);
  if (contentLength > 4_096) {
    return json({ error: { code: "PAYLOAD_TOO_LARGE", message: "Fanout payload is too large." } }, 413);
  }
  const rawBody = await request.text();
  if (rawBody.length > 4_096) {
    return json({ error: { code: "PAYLOAD_TOO_LARGE", message: "Fanout payload is too large." } }, 413);
  }
  let body: { tenantId?: unknown; reason?: unknown; excludeMembershipId?: unknown };
  try {
    body = JSON.parse(rawBody) as typeof body;
  } catch {
    return json({ error: { code: "INVALID_JSON", message: "Fanout payload must be valid JSON." } }, 400);
  }
  const tenantId = typeof body.tenantId === "string" ? body.tenantId.trim() : "";
  const reason = typeof body.reason === "string" ? body.reason.trim().slice(0, 80) : "feed.changed";
  const excludeMembershipId = typeof body.excludeMembershipId === "string" && body.excludeMembershipId.trim()
    ? body.excludeMembershipId.trim()
    : null;
  if (!tenantId || tenantId.length > 128) {
    return json({ error: { code: "INVALID_TENANT", message: "tenantId is required." } }, 400);
  }
  const event: SocialFanoutEvent = {
    type: "social.feed.invalidated",
    tenantId,
    reason,
    excludeMembershipId,
    emittedAt: Date.now()
  };
  const shardCount = socialFanoutShardCount(env);
  const delivered = await Promise.all(
    Array.from({ length: shardCount }, (_, shard) =>
      env.SOCIAL_FEED_HUBS.getByName(`${tenantId}:${shard}`).publish(event)
    )
  );
  return json({ accepted: true, shards: shardCount, sockets: delivered.reduce((sum, count) => sum + count, 0) }, 202);
}

function allowedOrigin(request: Request, env: Env) {
  const origin = request.headers.get("origin");
  if (!origin) return null;
  return env.GAME_ALLOWED_ORIGINS.split(",").map((value) => value.trim()).includes(origin) ? origin : null;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    if (url.pathname === "/health") return json({ ok: true, service: "vyb-games-realtime" });
    if (url.pathname === "/internal/social/publish" && request.method === "POST") {
      return publishSocialInvalidation(request, env);
    }
    if (url.pathname === "/ws/social" && request.headers.get("upgrade")?.toLowerCase() === "websocket") {
      const origin = allowedOrigin(request, env);
      if (!origin) return json({ error: { code: "INVALID_ORIGIN", message: "This app origin is not allowed." } }, 403);
      const claims = await verifySocialSessionToken(url.searchParams.get("token"), env.SOCIAL_FANOUT_SECRET);
      if (!claims) return json({ error: { code: "INVALID_SESSION", message: "The social session is invalid or expired." } }, 401);
      const shard = socialFanoutShard(claims.membershipId, socialFanoutShardCount(env));
      const headers = new Headers(request.headers);
      headers.set("x-vyb-social-claims", btoa(JSON.stringify(claims)));
      return env.SOCIAL_FEED_HUBS.getByName(`${claims.tenantId}:${shard}`).fetch(new Request(request, { headers }));
    }
    if (url.pathname !== "/ws" || request.headers.get("upgrade")?.toLowerCase() !== "websocket") {
      return json({ error: { code: "NOT_FOUND", message: "Game socket endpoint not found." } }, 404);
    }
    const origin = allowedOrigin(request, env);
    if (!origin) return json({ error: { code: "INVALID_ORIGIN", message: "This app origin is not allowed." } }, 403);
    const claims = await verifySessionToken(url.searchParams.get("token"), env.GAME_SESSION_SECRET);
    if (!claims) return json({ error: { code: "INVALID_SESSION", message: "The game session is invalid or expired." } }, 401);
    const roomName = `${claims.tenantId}:${claims.game}:${claims.roomCode}`;
    const stub = env.GAME_ROOMS.getByName(roomName);
    const headers = new Headers(request.headers);
    headers.set("x-vyb-game-claims", btoa(JSON.stringify(claims)));
    return stub.fetch(new Request(request, { headers }));
  }
} satisfies ExportedHandler<Env>;

export class SocialFeedHub extends DurableObject<Env> {
  async fetch(request: Request): Promise<Response> {
    const encodedClaims = request.headers.get("x-vyb-social-claims");
    if (!encodedClaims) return json({ error: { code: "UNAUTHENTICATED", message: "Missing social identity." } }, 401);
    const claims = JSON.parse(atob(encodedClaims)) as SocialSessionClaims;
    const pair = new WebSocketPair();
    const [client, server] = Object.values(pair);
    const attachment: SocialConnectionAttachment = {
      tenantId: claims.tenantId,
      userId: claims.userId,
      membershipId: claims.membershipId
    };
    server.serializeAttachment(attachment);
    this.ctx.acceptWebSocket(server);
    server.send(JSON.stringify({ type: "social.connected", tenantId: claims.tenantId }));
    return new Response(null, { status: 101, webSocket: client });
  }

  publish(event: SocialFanoutEvent) {
    let delivered = 0;
    const frame = JSON.stringify({
      type: event.type,
      tenantId: event.tenantId,
      payload: { reason: event.reason, emittedAt: event.emittedAt }
    });
    for (const socket of this.ctx.getWebSockets()) {
      const attachment = socket.deserializeAttachment() as SocialConnectionAttachment | null;
      if (!attachment || (event.excludeMembershipId && attachment.membershipId === event.excludeMembershipId)) continue;
      try {
        socket.send(frame);
        delivered += 1;
      } catch {
        socket.close(1011, "Realtime fanout failed");
      }
    }
    return delivered;
  }

  webSocketMessage(socket: WebSocket, message: string | ArrayBuffer) {
    if (typeof message === "string" && message === "ping") {
      socket.send("pong");
    }
  }

  webSocketError(socket: WebSocket) {
    socket.close(1011, "Realtime connection failed");
  }
}

export class GameRoom extends DurableObject<Env> {
  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);
    this.ctx.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS room_state (
        id INTEGER PRIMARY KEY CHECK (id = 1),
        value TEXT NOT NULL,
        updated_at INTEGER NOT NULL
      );
      CREATE TABLE IF NOT EXISTS room_messages (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        display_name TEXT NOT NULL,
        kind TEXT NOT NULL,
        body TEXT NOT NULL,
        created_at INTEGER NOT NULL
      );
    `);
  }

  private readRoom(): StoredRoom | null {
    const row = [...this.ctx.storage.sql.exec<{ value: string }>("SELECT value FROM room_state WHERE id = 1")][0];
    return row ? JSON.parse(row.value) as StoredRoom : null;
  }

  private writeRoom(room: StoredRoom) {
    this.ctx.storage.sql.exec(
      "INSERT INTO room_state (id, value, updated_at) VALUES (1, ?, ?) ON CONFLICT(id) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at",
      JSON.stringify(room),
      Date.now()
    );
  }

  private messages() {
    return [...this.ctx.storage.sql.exec<{ id: number; user_id: string; display_name: string; kind: string; body: string; created_at: number }>(
      "SELECT id, user_id, display_name, kind, body, created_at FROM room_messages ORDER BY id DESC LIMIT ?",
      MAX_CHAT_ROWS
    )].reverse().map((row) => ({
      id: row.id,
      userId: row.user_id,
      displayName: row.display_name,
      kind: row.kind,
      body: row.body,
      createdAt: row.created_at
    }));
  }

  private sendSnapshot(socket: WebSocket, room: StoredRoom) {
    const attachment = socket.deserializeAttachment() as ConnectionAttachment;
    socket.send(JSON.stringify({
      type: "snapshot",
      room: {
        ...room,
        players: room.players.map(({ blockedUserIds: _blockedUserIds, ...player }) => player),
        state: redactGameState(room.state, attachment.seat)
      },
      viewer: attachment,
      messages: this.messages()
    }));
  }

  private broadcast(room: StoredRoom) {
    this.ctx.getWebSockets().forEach((socket) => this.sendSnapshot(socket, room));
  }

  async fetch(request: Request): Promise<Response> {
    const encodedClaims = request.headers.get("x-vyb-game-claims");
    if (!encodedClaims) return json({ error: { code: "UNAUTHENTICATED", message: "Missing room identity." } }, 401);
    const claims = JSON.parse(atob(encodedClaims)) as SessionClaims;
    let room = this.readRoom();
    if (!room) {
      room = { game: claims.game, roomCode: claims.roomCode, hostId: claims.userId, players: [], state: null, createdAt: Date.now() };
    }
    if (room.game !== claims.game || room.roomCode !== claims.roomCode) {
      return json({ error: { code: "ROOM_MISMATCH", message: "Room identity does not match." } }, 409);
    }
    const blockedUserIds = new Set(claims.blockedUserIds);
    const hasRelationshipConflict = room.players.some(
      (candidate) =>
        candidate.id !== claims.userId &&
        (blockedUserIds.has(candidate.id) || new Set(candidate.blockedUserIds ?? []).has(claims.userId))
    );
    if (hasRelationshipConflict) {
      return json({ error: { code: "ROOM_NOT_FOUND", message: "That room is not available." } }, 404);
    }
    let player = room.players.find((candidate) => candidate.id === claims.userId);
    if (!player) {
      if (room.state || room.players.length >= ROOM_LIMITS[room.game]) {
        return json({ error: { code: "ROOM_FULL", message: "This room is full or the match already started." } }, 409);
      }
      player = {
        id: claims.userId,
        name: claims.displayName.slice(0, 48),
        username: claims.username.slice(0, 48),
        seat: room.players.length,
        connected: true,
        blockedUserIds: [...blockedUserIds]
      };
      room.players.push(player);
    } else {
      player.connected = true;
      player.name = claims.displayName.slice(0, 48);
      player.username = claims.username.slice(0, 48);
      player.blockedUserIds = [...blockedUserIds];
    }
    this.writeRoom(room);

    const pair = new WebSocketPair();
    const [client, server] = Object.values(pair);
    const attachment: ConnectionAttachment = { userId: player.id, displayName: player.name, username: player.username, seat: player.seat };
    server.serializeAttachment(attachment);
    this.ctx.acceptWebSocket(server);
    this.sendSnapshot(server, room);
    this.broadcast(room);
    return new Response(null, { status: 101, webSocket: client });
  }

  private addMessage(actor: ConnectionAttachment, kind: "chat" | "emoji", body: string) {
    this.ctx.storage.sql.exec(
      "INSERT INTO room_messages (user_id, display_name, kind, body, created_at) VALUES (?, ?, ?, ?, ?)",
      actor.userId,
      actor.displayName,
      kind,
      body,
      Date.now()
    );
    this.ctx.storage.sql.exec(
      "DELETE FROM room_messages WHERE id NOT IN (SELECT id FROM room_messages ORDER BY id DESC LIMIT ?)",
      MAX_CHAT_ROWS
    );
  }

  private start(room: StoredRoom, actor: ConnectionAttachment) {
    if (room.hostId !== actor.userId) throw new Error("Only the room host can start the match.");
    if (room.players.length < 2) throw new Error("Invite at least one more player.");
    room.players = room.players.map((player, seat) => ({ ...player, seat }));
    room.state = room.game === "chess"
      ? createChessState()
      : room.game === "ludo"
        ? createLudoState(room.players.length)
        : createUnoState(room.players.length, crypto.getRandomValues(new Uint32Array(1))[0]);
  }

  async webSocketMessage(socket: WebSocket, rawMessage: string | ArrayBuffer) {
    const actor = socket.deserializeAttachment() as ConnectionAttachment;
    try {
      const encodedMessage = typeof rawMessage === "string" ? rawMessage : new TextDecoder().decode(rawMessage);
      if (encodedMessage.length > 8_192) throw new Error("Game message is too large.");
      const now = Date.now();
      if (!actor.actionWindowStartedAt || now - actor.actionWindowStartedAt >= 1_000) {
        actor.actionWindowStartedAt = now;
        actor.actionCount = 0;
      }
      actor.actionCount = (actor.actionCount ?? 0) + 1;
      if (actor.actionCount > 20) throw new Error("Too many actions. Slow down for a moment.");
      const message = JSON.parse(encodedMessage) as ClientMessage;
      const room = this.readRoom();
      if (!room) throw new Error("Room state is unavailable.");
      if (message.type === "chat") {
        if (actor.lastChatAt && now - actor.lastChatAt < 500) throw new Error("Please wait before sending another message.");
        const text = message.text.trim().slice(0, MAX_CHAT_LENGTH);
        if (!text) throw new Error("Write a message first.");
        this.addMessage(actor, "chat", text);
        actor.lastChatAt = now;
      } else if (message.type === "emoji") {
        const emoji = message.emoji.trim().slice(0, 16);
        if (!emoji) throw new Error("Choose an emoji first.");
        this.addMessage(actor, "emoji", emoji);
      } else if (message.type === "start" || message.type === "rematch") {
        this.start(room, actor);
      } else if (message.type === "chess.move" && room.state?.kind === "chess") {
        room.state = moveChess(room.state, room.players, actor.userId, message.from, message.to, message.promotion);
      } else if (message.type === "ludo.roll" && room.state?.kind === "ludo") {
        const dice = crypto.getRandomValues(new Uint8Array(1))[0] % 6 + 1;
        room.state = rollLudo(room.state, room.players, actor.userId, dice);
      } else if (message.type === "ludo.move" && room.state?.kind === "ludo") {
        room.state = moveLudo(room.state, room.players, actor.userId, message.tokenIndex);
      } else if (message.type === "uno.play" && room.state?.kind === "uno") {
        room.state = playUnoCard(room.state, room.players, actor.userId, message.cardId, message.color);
      } else if (message.type === "uno.draw" && room.state?.kind === "uno") {
        room.state = drawUnoCard(room.state, room.players, actor.userId);
      } else {
        throw new Error("That action is not available in this match.");
      }
      socket.serializeAttachment(actor);
      this.writeRoom(room);
      this.broadcast(room);
    } catch (error) {
      socket.send(JSON.stringify({ type: "error", message: error instanceof Error ? error.message : "Game action failed." }));
    }
  }

  async webSocketClose(socket: WebSocket) {
    const actor = socket.deserializeAttachment() as ConnectionAttachment | null;
    const room = this.readRoom();
    if (!actor || !room) return;
    const hasAnotherSocket = this.ctx.getWebSockets().some((candidate) => {
      if (candidate === socket) return false;
      return (candidate.deserializeAttachment() as ConnectionAttachment | null)?.userId === actor.userId;
    });
    if (!hasAnotherSocket) {
      room.players = room.players.map((player) => player.id === actor.userId ? { ...player, connected: false } : player);
      this.writeRoom(room);
      this.broadcast(room);
    }
  }

  webSocketError(socket: WebSocket) {
    socket.close(1011, "Realtime connection failed");
  }
}
