import assert from "node:assert/strict";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { after, beforeEach, test } from "node:test";

const fixtureRoot = await mkdtemp(path.join(os.tmpdir(), "vyb-fcm-outbox-"));
const fixturePath = path.join(fixtureRoot, "notifications-store.json");
process.env.VYB_NOTIFICATION_STORE_PATH = fixturePath;
process.env.VYB_FCM_WORKER_DISABLED = "1";

const { runFcmNotificationDeliveryOutbox } = await import("./repository.mjs");

function buildFixture() {
  return {
    notifications: [
      {
        id: "notif_test",
        event_key: "chat.message",
        tenant_id: "tenant-1",
        recipient_user_ids: ["user-1"],
        priority_score: 10,
        channels: ["in_app", "push"],
        delivery_policy: {
          collapse_key: "chat:conversation-1:user-1",
          ttl_seconds: 3600,
          silent: false
        },
        copy: {
          title: "New message",
          body: "A classmate sent you a message.",
          href: "/messages/conversation-1"
        },
        privacy: { push_body_safe: true },
        created_at: "2026-07-27T12:00:00.000Z"
      }
    ],
    scheduled: [],
    devices: [
      {
        userId: "user-1",
        tenantId: "tenant-1",
        deviceId: "android-test",
        platform: "android",
        endpoint: "fcm-token",
        pushSubscription: { provider: "fcm", token: "fcm-token" },
        updatedAt: "2026-07-27T11:00:00.000Z"
      }
    ],
    pushDeliveries: [],
    liveModes: {}
  };
}

beforeEach(async () => {
  const fixture = buildFixture();
  fixture.notifications[0].created_at = new Date().toISOString();
  fixture.devices[0].updatedAt = new Date(Date.now() - 60_000).toISOString();
  await writeFile(fixturePath, JSON.stringify(fixture), "utf8");
});

after(async () => {
  await rm(fixtureRoot, { recursive: true, force: true });
});

test("queues and sends an Android FCM notification with deep-link data", async () => {
  const messages = [];
  const result = await runFcmNotificationDeliveryOutbox({
    sendMessage: async (message) => {
      messages.push(message);
      return "projects/vyb/messages/test";
    }
  });

  assert.deepEqual(result, {
    queued: 1,
    attempted: 1,
    sent: 1,
    failed: 0,
    invalidTokensRemoved: 0
  });
  assert.equal(messages[0].token, "fcm-token");
  assert.equal(messages[0].data.href, "/messages/conversation-1");
  assert.equal(messages[0].android.priority, "high");

  const persisted = JSON.parse(await readFile(fixturePath, "utf8"));
  assert.equal(persisted.pushDeliveries[0].provider, "fcm");
  assert.equal(persisted.pushDeliveries[0].status, "sent");
});

test("removes a device whose FCM registration token is no longer valid", async () => {
  const invalidToken = Object.assign(new Error("Token is no longer registered."), {
    code: "messaging/registration-token-not-registered"
  });
  const result = await runFcmNotificationDeliveryOutbox({
    sendMessage: async () => {
      throw invalidToken;
    }
  });

  assert.equal(result.failed, 1);
  assert.equal(result.invalidTokensRemoved, 1);
  const persisted = JSON.parse(await readFile(fixturePath, "utf8"));
  assert.equal(persisted.devices.length, 0);
  assert.equal(persisted.pushDeliveries[0].status, "failed");
});
