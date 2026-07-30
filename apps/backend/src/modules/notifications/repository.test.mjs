import assert from "node:assert/strict";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { after, beforeEach, test } from "node:test";

const fixtureRoot = await mkdtemp(path.join(os.tmpdir(), "vyb-fcm-outbox-"));
const fixturePath = path.join(fixtureRoot, "notifications-store.json");
process.env.VYB_NOTIFICATION_STORE_PATH = fixturePath;
process.env.VYB_FCM_WORKER_DISABLED = "1";

const {
  listNotifications,
  markRead,
  runFcmNotificationDeliveryOutbox
} = await import("./repository.mjs");

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
        endpoint: "fcm-installation-id",
        pushSubscription: { provider: "fcm-fid", fid: "fcm-installation-id" },
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
  assert.equal(messages[0].fid, "fcm-installation-id");
  assert.equal("token" in messages[0], false);
  assert.equal(messages[0].data.href, "/messages/conversation-1");
  assert.equal(messages[0].android.priority, "high");

  const persisted = JSON.parse(await readFile(fixturePath, "utf8"));
  assert.equal(persisted.pushDeliveries[0].provider, "fcm");
  assert.equal(persisted.pushDeliveries[0].status, "sent");
});

test("removes a device whose Firebase Installation ID is no longer registered", async () => {
  const invalidInstallation = Object.assign(new Error("Installation is no longer registered."), {
    code: "messaging/installation-id-not-registered"
  });
  const result = await runFcmNotificationDeliveryOutbox({
    sendMessage: async () => {
      throw invalidInstallation;
    }
  });

  assert.equal(result.failed, 1);
  assert.equal(result.invalidTokensRemoved, 1);
  const persisted = JSON.parse(await readFile(fixturePath, "utf8"));
  assert.equal(persisted.devices.length, 0);
  assert.equal(persisted.pushDeliveries[0].status, "failed");
});

test("keeps legacy Android registration tokens deliverable during the FID rollout", async () => {
  const fixture = buildFixture();
  fixture.notifications[0].created_at = new Date().toISOString();
  fixture.devices[0] = {
    ...fixture.devices[0],
    endpoint: "legacy-token",
    pushSubscription: { provider: "fcm", token: "legacy-token" },
    updatedAt: new Date(Date.now() - 60_000).toISOString()
  };
  await writeFile(fixturePath, JSON.stringify(fixture), "utf8");

  const messages = [];
  const result = await runFcmNotificationDeliveryOutbox({
    sendMessage: async (message) => {
      messages.push(message);
      return "projects/vyb/messages/legacy";
    }
  });

  assert.equal(result.sent, 1);
  assert.equal(messages[0].token, "legacy-token");
  assert.equal("fid" in messages[0], false);
});

test("reading a multi-recipient notification does not mark it read for another user", async () => {
  const fixture = buildFixture();
  fixture.notifications[0].recipient_user_ids = ["user-1", "user-2"];
  await writeFile(fixturePath, JSON.stringify(fixture), "utf8");

  const firstViewer = { tenantId: "tenant-1", userId: "user-1" };
  const secondViewer = { tenantId: "tenant-1", userId: "user-2" };
  await markRead(firstViewer, "notif_test");

  const firstResult = await listNotifications(firstViewer, { state: "read" });
  const secondResult = await listNotifications(secondViewer, { state: "unread" });

  assert.equal(firstResult.items.length, 1);
  assert.ok(firstResult.items[0].state.read_at);
  assert.equal(secondResult.items.length, 1);
  assert.equal(secondResult.items[0].state.read_at, null);
  assert.equal(secondResult.unreadCount, 1);
  assert.equal("recipient_states" in firstResult.items[0], false);
});
