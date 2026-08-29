import { createHmac } from "node:crypto";

const SOCIAL_REALTIME_SESSION_TTL_MS = 5 * 60 * 1000;
const SOCIAL_REALTIME_PUBLISH_TIMEOUT_MS = 1_500;

function requireNonEmptyString(value, name) {
  if (typeof value !== "string" || !value.trim()) {
    throw new Error(`${name} is required.`);
  }
  return value.trim();
}

function getRealtimeOrigin(env) {
  const configured = requireNonEmptyString(
    env.VYB_SOCIAL_REALTIME_PUBLIC_ORIGIN,
    "VYB_SOCIAL_REALTIME_PUBLIC_ORIGIN"
  );
  const url = new URL(configured);
  if (!new Set(["http:", "https:"]).has(url.protocol) || url.username || url.password) {
    throw new Error("VYB_SOCIAL_REALTIME_PUBLIC_ORIGIN must be an HTTP(S) origin.");
  }
  if (env.NODE_ENV === "production" && url.protocol !== "https:") {
    throw new Error("VYB_SOCIAL_REALTIME_PUBLIC_ORIGIN must use HTTPS in production.");
  }
  return url.origin;
}

function getRealtimeSecret(env) {
  const secret = requireNonEmptyString(env.VYB_SOCIAL_REALTIME_SECRET, "VYB_SOCIAL_REALTIME_SECRET");
  if (env.NODE_ENV === "production" && secret.length < 24) {
    throw new Error("VYB_SOCIAL_REALTIME_SECRET must contain at least 24 characters in production.");
  }
  return secret;
}

function signClaims(claims, secret) {
  const payload = Buffer.from(JSON.stringify(claims), "utf8").toString("base64url");
  const signature = createHmac("sha256", secret).update(payload).digest("base64url");
  return `${payload}.${signature}`;
}

export function buildSocialRealtimeSession({ tenantId, userId, membershipId, env = process.env, now = Date.now() }) {
  const origin = getRealtimeOrigin(env);
  const secret = getRealtimeSecret(env);
  const expiresAt = now + SOCIAL_REALTIME_SESSION_TTL_MS;
  const token = signClaims({
    scope: "social.feed",
    tenantId: requireNonEmptyString(tenantId, "tenantId"),
    userId: requireNonEmptyString(userId, "userId"),
    membershipId: requireNonEmptyString(membershipId, "membershipId"),
    exp: expiresAt
  }, secret);
  const url = new URL("/ws/social", origin);
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
  url.searchParams.set("token", token);
  return { wsUrl: url.toString(), expiresAt };
}

export async function publishSocialFeedInvalidation({
  tenantId,
  reason = "feed.changed",
  excludeMembershipId = null,
  env = process.env,
  fetchImpl = fetch
}) {
  if (!env.VYB_SOCIAL_REALTIME_PUBLIC_ORIGIN?.trim() || !env.VYB_SOCIAL_REALTIME_SECRET?.trim()) {
    return { delivered: false, reason: "not-configured" };
  }
  const origin = getRealtimeOrigin(env);
  const secret = getRealtimeSecret(env);
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), SOCIAL_REALTIME_PUBLISH_TIMEOUT_MS);
  try {
    const response = await fetchImpl(new URL("/internal/social/publish", origin), {
      method: "POST",
      headers: {
        authorization: `Bearer ${secret}`,
        "content-type": "application/json"
      },
      body: JSON.stringify({
        tenantId: requireNonEmptyString(tenantId, "tenantId"),
        reason: typeof reason === "string" && reason.trim() ? reason.trim().slice(0, 80) : "feed.changed",
        excludeMembershipId: typeof excludeMembershipId === "string" && excludeMembershipId.trim()
          ? excludeMembershipId.trim()
          : null
      }),
      signal: controller.signal
    });
    if (!response.ok) {
      throw new Error(`Social realtime fanout returned HTTP ${response.status}.`);
    }
    return { delivered: true, status: response.status };
  } finally {
    clearTimeout(timeout);
  }
}
