import assert from "node:assert/strict";
import { createHmac } from "node:crypto";
import test from "node:test";
import { buildSocialRealtimeSession, publishSocialFeedInvalidation } from "./fanout.mjs";

const env = {
  NODE_ENV: "production",
  VYB_SOCIAL_REALTIME_PUBLIC_ORIGIN: "https://realtime.example.workers.dev",
  VYB_SOCIAL_REALTIME_SECRET: "social-realtime-secret-for-tests"
};

test("buildSocialRealtimeSession creates a short-lived verifiable WebSocket token", () => {
  const session = buildSocialRealtimeSession({
    tenantId: "tenant-1",
    userId: "user-1",
    membershipId: "membership-1",
    env,
    now: 1_000
  });
  const url = new URL(session.wsUrl);
  assert.equal(url.protocol, "wss:");
  assert.equal(url.pathname, "/ws/social");
  assert.equal(session.expiresAt, 301_000);
  const [payload, signature] = url.searchParams.get("token").split(".");
  assert.equal(createHmac("sha256", env.VYB_SOCIAL_REALTIME_SECRET).update(payload).digest("base64url"), signature);
  assert.deepEqual(JSON.parse(Buffer.from(payload, "base64url").toString("utf8")), {
    scope: "social.feed",
    tenantId: "tenant-1",
    userId: "user-1",
    membershipId: "membership-1",
    exp: 301_000
  });
});

test("buildSocialRealtimeSession rejects insecure production origins", () => {
  assert.throws(() => buildSocialRealtimeSession({
    tenantId: "tenant-1",
    userId: "user-1",
    membershipId: "membership-1",
    env: { ...env, VYB_SOCIAL_REALTIME_PUBLIC_ORIGIN: "http://example.test" }
  }), /HTTPS/u);
});

test("publishSocialFeedInvalidation authenticates and sends only an invalidation envelope", async () => {
  let captured;
  const result = await publishSocialFeedInvalidation({
    tenantId: "tenant-1",
    reason: "social.post.created",
    excludeMembershipId: "membership-1",
    env,
    fetchImpl: async (url, options) => {
      captured = { url: String(url), options };
      return new Response(null, { status: 202 });
    }
  });
  assert.deepEqual(result, { delivered: true, status: 202 });
  assert.equal(captured.url, "https://realtime.example.workers.dev/internal/social/publish");
  assert.equal(captured.options.headers.authorization, `Bearer ${env.VYB_SOCIAL_REALTIME_SECRET}`);
  assert.deepEqual(JSON.parse(captured.options.body), {
    tenantId: "tenant-1",
    reason: "social.post.created",
    excludeMembershipId: "membership-1"
  });
});

test("publishSocialFeedInvalidation degrades cleanly until shared fanout is configured", async () => {
  assert.deepEqual(await publishSocialFeedInvalidation({ tenantId: "tenant-1", env: {} }), {
    delivered: false,
    reason: "not-configured"
  });
});
