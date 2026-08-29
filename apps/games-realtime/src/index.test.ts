import { SELF } from "cloudflare:test";
import { describe, expect, it } from "vitest";

async function gameToken(input: { userId: string; blockedUserIds?: string[]; roomCode: string }) {
  const json = JSON.stringify({
    tenantId: "tenant-test",
    userId: input.userId,
    membershipId: `membership-${input.userId}`,
    displayName: input.userId,
    username: input.userId,
    blockedUserIds: input.blockedUserIds ?? [],
    game: "chess",
    roomCode: input.roomCode,
    exp: Date.now() + 60_000
  });
  const encoded = btoa(json).replace(/\+/gu, "-").replace(/\//gu, "_").replace(/=+$/gu, "");
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode("test-game-session-secret"),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signatureBytes = new Uint8Array(
    await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(encoded))
  );
  let signatureBinary = "";
  signatureBytes.forEach((byte) => { signatureBinary += String.fromCharCode(byte); });
  const signature = btoa(signatureBinary).replace(/\+/gu, "-").replace(/\//gu, "_").replace(/=+$/gu, "");
  return `${encoded}.${signature}`;
}

async function socialToken(input: { tenantId?: string; userId: string; membershipId?: string }) {
  const json = JSON.stringify({
    scope: "social.feed",
    tenantId: input.tenantId ?? "tenant-test",
    userId: input.userId,
    membershipId: input.membershipId ?? `membership-${input.userId}`,
    exp: Date.now() + 60_000
  });
  const encoded = btoa(json).replace(/\+/gu, "-").replace(/\//gu, "_").replace(/=+$/gu, "");
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode("test-social-fanout-secret"),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signatureBytes = new Uint8Array(
    await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(encoded))
  );
  let signatureBinary = "";
  signatureBytes.forEach((byte) => { signatureBinary += String.fromCharCode(byte); });
  const signature = btoa(signatureBinary).replace(/\+/gu, "-").replace(/\//gu, "_").replace(/=+$/gu, "");
  return `${encoded}.${signature}`;
}

async function socialSocketRequest(token: string) {
  return SELF.fetch(`https://games.test/ws/social?token=${encodeURIComponent(token)}`, {
    headers: {
      origin: "http://localhost:3000",
      upgrade: "websocket"
    }
  });
}

function nextSocketJson(socket: WebSocket) {
  return new Promise<Record<string, unknown>>((resolve, reject) => {
    const onMessage = (event: MessageEvent) => {
      cleanup();
      try {
        resolve(JSON.parse(String(event.data)) as Record<string, unknown>);
      } catch (error) {
        reject(error);
      }
    };
    const onError = () => {
      cleanup();
      reject(new Error("WebSocket failed before a message arrived."));
    };
    const cleanup = () => {
      socket.removeEventListener("message", onMessage);
      socket.removeEventListener("error", onError);
    };
    socket.addEventListener("message", onMessage);
    socket.addEventListener("error", onError);
  });
}

function socketRequest(token: string) {
  return SELF.fetch(`https://games.test/ws?token=${encodeURIComponent(token)}`, {
    headers: {
      origin: "http://localhost:3000",
      upgrade: "websocket"
    }
  });
}

describe("games realtime worker", () => {
  it("reports a healthy service without exposing bindings", async () => {
    const response = await SELF.fetch("https://games.test/health");
    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toEqual({ ok: true, service: "vyb-games-realtime" });
  });

  it("rejects non-websocket game routes", async () => {
    const response = await SELF.fetch("https://games.test/ws", { headers: { origin: "http://localhost:3000" } });
    expect(response.status).toBe(404);
  });

  it("conceals rooms when either player has blocked the other", async () => {
    const roomCode = "BLK123";
    const first = await socketRequest(await gameToken({
      userId: "user-a",
      blockedUserIds: ["user-b"],
      roomCode
    }));
    expect(first.status).toBe(101);
    first.webSocket?.accept();

    const second = await socketRequest(await gameToken({ userId: "user-b", roomCode }));
    expect(second.status).toBe(404);
    await expect(second.json()).resolves.toEqual({
      error: { code: "ROOM_NOT_FOUND", message: "That room is not available." }
    });
    first.webSocket?.close(1000, "test complete");
  });

  it("requires the internal social fanout credential", async () => {
    const response = await SELF.fetch("https://games.test/internal/social/publish", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ tenantId: "tenant-test" })
    });
    expect(response.status).toBe(401);
    await expect(response.json()).resolves.toEqual({
      error: { code: "UNAUTHENTICATED", message: "A valid fanout credential is required." }
    });
  });

  it("fans out content-free feed invalidations to another authenticated member", async () => {
    const response = await socialSocketRequest(await socialToken({ userId: "user-b" }));
    expect(response.status).toBe(101);
    const socket = response.webSocket;
    expect(socket).toBeDefined();
    socket?.accept();
    await expect(nextSocketJson(socket!)).resolves.toMatchObject({
      type: "social.connected",
      tenantId: "tenant-test"
    });

    const nextMessage = nextSocketJson(socket!);
    const publish = await SELF.fetch("https://games.test/internal/social/publish", {
      method: "POST",
      headers: {
        authorization: "Bearer test-social-fanout-secret",
        "content-type": "application/json"
      },
      body: JSON.stringify({
        tenantId: "tenant-test",
        reason: "social.post.created",
        excludeMembershipId: "membership-user-a"
      })
    });
    expect(publish.status).toBe(202);
    await expect(publish.json()).resolves.toMatchObject({ accepted: true, shards: 16, sockets: 1 });
    await expect(nextMessage).resolves.toMatchObject({
      type: "social.feed.invalidated",
      tenantId: "tenant-test",
      payload: { reason: "social.post.created" }
    });
    socket?.close(1000, "test complete");
  });
});
