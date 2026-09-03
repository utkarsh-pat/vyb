import assert from "node:assert/strict";
import { createHmac } from "node:crypto";
import { test } from "node:test";
import {
  createMultiplayerSessionService,
  MultiplayerSessionError,
  normalizeRoomCode
} from "./multiplayer-session.mjs";

const NOW = Date.parse("2026-09-03T08:00:00.000Z");
const SECRET = "test-game-secret-with-enough-entropy";
const viewer = {
  tenantId: "tenant-1",
  userId: "user-1",
  membershipId: "membership-1",
  email: "Student.One@college.edu",
  displayName: "Student One"
};

function decodeAndVerify(token) {
  const [encoded, signature] = token.split(".");
  const expected = createHmac("sha256", SECRET).update(encoded).digest("base64url");
  assert.equal(signature, expected);
  return JSON.parse(Buffer.from(encoded, "base64url").toString("utf8"));
}

function createFixture() {
  return createMultiplayerSessionService({
    now: () => NOW,
    getSecret: () => SECRET,
    getSocketOrigin: () => "wss://games.example.test",
    generateRoomCode: () => "ABC234",
    hydratePolicy: async (value) => ({
      ...value,
      blockedUserIds: new Set(["blocked-user"])
    })
  });
}

test("room codes are normalized and ambiguous digits are rejected", () => {
  assert.equal(normalizeRoomCode(" abc234 "), "ABC234");
  assert.equal(normalizeRoomCode("ABC10Z"), null);
  assert.equal(normalizeRoomCode("short"), null);
  assert.equal(normalizeRoomCode(undefined), "");
});

test("multiplayer sessions are tenant-bound, short-lived and relationship-aware", async () => {
  const session = await createFixture().create(viewer, { game: "chess" });
  const url = new URL(session.wsUrl);
  const claims = decodeAndVerify(url.searchParams.get("token"));

  assert.equal(session.roomCode, "ABC234");
  assert.equal(url.protocol, "wss:");
  assert.equal(url.pathname, "/ws");
  assert.equal(session.expiresAt, NOW + 5 * 60 * 1000);
  assert.deepEqual(claims, {
    tenantId: "tenant-1",
    userId: "user-1",
    membershipId: "membership-1",
    displayName: "Student One",
    username: "student.one",
    blockedUserIds: ["blocked-user"],
    game: "chess",
    roomCode: "ABC234",
    exp: NOW + 5 * 60 * 1000
  });
});

test("multiplayer sessions reject unsupported games and malformed room codes", async () => {
  await assert.rejects(
    createFixture().create(viewer, { game: "connect4" }),
    (error) => error instanceof MultiplayerSessionError && error.code === "INVALID_GAME"
  );
  await assert.rejects(
    createFixture().create(viewer, { game: "uno", roomCode: "BAD" }),
    (error) => error instanceof MultiplayerSessionError && error.code === "INVALID_ROOM"
  );
});
