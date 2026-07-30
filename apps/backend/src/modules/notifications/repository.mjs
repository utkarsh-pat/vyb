import { randomUUID } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { getMessaging } from "firebase-admin/messaging";
import { getWorkspaceRoot } from "../../../../../packages/config/src/index.mjs";
import { getFirebaseAdminApp } from "../../../../../packages/config/src/firebase-admin.mjs";

const storePath = process.env.VYB_NOTIFICATION_STORE_PATH
  ? path.resolve(process.env.VYB_NOTIFICATION_STORE_PATH)
  : process.env.VYB_SERVERLESS_RUNTIME === "vercel"
    ? path.join(os.tmpdir(), "vyb", "notifications-store.json")
    : path.join(getWorkspaceRoot(), "data", "notifications-store.json");
const FCM_DELIVERY_MAX_ATTEMPTS = 5;
const DEFAULT_PUSH_TTL_SECONDS = 60 * 60 * 24 * 14;
const DEFAULT_FCM_WORKER_INTERVAL_MS = 15_000;
let writeQueue = Promise.resolve();
let fcmWorkerStarted = false;

async function load() {
  try {
    const parsed = JSON.parse(await readFile(storePath, "utf8"));
    return {
      notifications: Array.isArray(parsed.notifications) ? parsed.notifications : [],
      scheduled: Array.isArray(parsed.scheduled) ? parsed.scheduled : [],
      devices: Array.isArray(parsed.devices) ? parsed.devices : [],
      pushDeliveries: Array.isArray(parsed.pushDeliveries) ? parsed.pushDeliveries : [],
      liveModes: parsed.liveModes && typeof parsed.liveModes === "object" ? parsed.liveModes : {}
    };
  } catch {
    return { notifications: [], scheduled: [], devices: [], pushDeliveries: [], liveModes: {} };
  }
}

async function save(store) {
  await mkdir(path.dirname(storePath), { recursive: true });
  const snapshot = JSON.stringify(store, null, 2);
  writeQueue = writeQueue.then(() => writeFile(storePath, snapshot, "utf8"));
  await writeQueue;
}

function belongs(item, viewer) {
  return item.tenant_id === viewer.tenantId && Array.isArray(item.recipient_user_ids) && item.recipient_user_ids.includes(viewer.userId);
}

function emptyNotificationState() {
  return { read_at: null, seen_at: null, archived_at: null };
}

function getViewerNotificationState(item, viewer) {
  const ownState = item.recipient_states?.[viewer.userId];
  if (ownState && typeof ownState === "object") {
    return { ...emptyNotificationState(), ...ownState };
  }

  // Legacy records with one recipient used the top-level state field. It is
  // safe to preserve that state only when it cannot belong to another user.
  if (item.recipient_user_ids?.length === 1 && item.state && typeof item.state === "object") {
    return { ...emptyNotificationState(), ...item.state };
  }

  return emptyNotificationState();
}

function ensureViewerNotificationState(item, viewer) {
  const current = getViewerNotificationState(item, viewer);
  item.recipient_states =
    item.recipient_states && typeof item.recipient_states === "object"
      ? item.recipient_states
      : {};
  item.recipient_states[viewer.userId] = current;
  return current;
}

function buildViewerNotification(item, viewer) {
  const { recipient_states: _recipientStates, ...safeItem } = item;
  return {
    ...safeItem,
    state: getViewerNotificationState(item, viewer)
  };
}

function cleanString(value) {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

function getFcmTarget(device) {
  if (device?.platform !== "android") return null;
  const subscription = device.pushSubscription;
  if (!subscription || typeof subscription !== "object") return null;
  if (subscription.provider === "fcm-fid") {
    const fid = cleanString(subscription.fid) ?? cleanString(device.endpoint);
    return fid ? { field: "fid", value: fid } : null;
  }
  if (subscription.provider === "fcm") {
    const token = cleanString(subscription.token) ?? cleanString(device.endpoint);
    return token ? { field: "token", value: token } : null;
  }
  return null;
}

function isFcmDelivery(delivery) {
  return delivery?.provider === "fcm" || cleanString(delivery?.id)?.startsWith("notif_fcm_");
}

function buildFcmDeliveryId() {
  return `notif_fcm_${Date.now().toString(36)}_${randomUUID().replaceAll("-", "").slice(0, 10)}`;
}

function normalizeCollapseKey(value) {
  return (cleanString(value) ?? "vyb_update").replace(/[^A-Za-z0-9_.:-]/gu, "_").slice(0, 64);
}

function notificationCanPush(item) {
  return (
    Array.isArray(item?.channels) &&
    item.channels.includes("push") &&
    item.delivery_policy?.silent !== true &&
    item.privacy?.push_body_safe === true
  );
}

function notificationHasNotExpired(item, now) {
  const createdAt = Date.parse(item.created_at);
  if (!Number.isFinite(createdAt)) return false;
  const ttlSeconds = Number.isInteger(item.delivery_policy?.ttl_seconds) && item.delivery_policy.ttl_seconds > 0
    ? item.delivery_policy.ttl_seconds
    : DEFAULT_PUSH_TTL_SECONDS;
  return createdAt + ttlSeconds * 1000 > now;
}

function buildFcmPayload(item) {
  return {
    title: cleanString(item.copy?.title) ?? "Vyb update",
    body: cleanString(item.copy?.body) ?? "Open Vyb to check the latest update.",
    href: cleanString(item.copy?.href)?.startsWith("/") ? item.copy.href.trim() : "/home",
    collapseKey: normalizeCollapseKey(item.delivery_policy?.collapse_key),
    notificationId: item.id,
    eventKey: cleanString(item.event_key) ?? "system.update"
  };
}

function queueFcmDeliveries(store, now = Date.now()) {
  let queued = 0;
  for (const item of store.notifications) {
    if (!notificationCanPush(item) || !notificationHasNotExpired(item, now)) continue;

    const recipients = Array.isArray(item.recipient_user_ids) ? item.recipient_user_ids : [];
    for (const userId of recipients) {
      const devices = store.devices.filter(
        (device) =>
          device.tenantId === item.tenant_id &&
          device.userId === userId &&
          Boolean(getFcmTarget(device)) &&
          Date.parse(item.created_at) >= Date.parse(device.updatedAt)
      );
      for (const device of devices) {
        const alreadyQueued = store.pushDeliveries.some(
          (delivery) =>
            delivery.notificationId === item.id &&
            delivery.tenantId === item.tenant_id &&
            delivery.userId === userId &&
            delivery.deviceId === device.deviceId
        );
        if (alreadyQueued) continue;

        store.pushDeliveries.push({
          id: buildFcmDeliveryId(),
          provider: "fcm",
          notificationId: item.id,
          tenantId: item.tenant_id,
          userId,
          deviceId: device.deviceId,
          payload: buildFcmPayload(item),
          status: "pending",
          attempts: 0,
          nextAttemptAt: new Date(now).toISOString(),
          lastAttemptAt: null,
          deliveredAt: null,
          lastError: null
        });
        queued += 1;
      }
    }
  }
  return queued;
}

function getFcmRetryDelayMs(attempts) {
  return Math.min(60 * 60 * 1000, 2 ** Math.max(0, attempts - 1) * 60 * 1000);
}

function getFcmErrorCode(error) {
  return typeof error === "object" && error !== null && typeof error.code === "string" ? error.code : null;
}

function getFcmErrorMessage(error) {
  return error instanceof Error ? error.message : "FCM delivery failed.";
}

function isInvalidFcmTokenError(error) {
  const code = getFcmErrorCode(error);
  return (
    code === "messaging/installation-id-not-registered" ||
    code === "messaging/registration-token-not-registered" ||
    code === "messaging/invalid-registration-token"
  );
}

function buildFcmMessage(delivery, device, item) {
  const ttlSeconds = Number.isInteger(item?.delivery_policy?.ttl_seconds) && item.delivery_policy.ttl_seconds > 0
    ? item.delivery_policy.ttl_seconds
    : DEFAULT_PUSH_TTL_SECONDS;
  const collapseKey = normalizeCollapseKey(delivery.payload.collapseKey);
  const target = getFcmTarget(device);
  if (!target) throw new Error("FCM device registration is unavailable.");
  return {
    [target.field]: target.value,
    notification: {
      title: delivery.payload.title,
      body: delivery.payload.body
    },
    data: {
      href: delivery.payload.href,
      collapseKey,
      notificationId: delivery.payload.notificationId,
      eventKey: delivery.payload.eventKey
    },
    android: {
      collapseKey,
      ttl: ttlSeconds * 1000,
      priority: Number(item?.priority_score) >= 10 ? "high" : "normal",
      notification: {
        tag: collapseKey
      }
    }
  };
}

async function sendFcmMessage(message) {
  return getMessaging(getFirebaseAdminApp()).send(message);
}

export async function runFcmNotificationDeliveryOutbox({ tenantId = null, limit = 25, sendMessage = sendFcmMessage } = {}) {
  const store = await load();
  const now = Date.now();
  const queued = queueFcmDeliveries(store, now);
  const boundedLimit = Math.max(1, Math.min(100, Number(limit) || 25));
  const due = store.pushDeliveries
    .filter((delivery) => {
      if (!isFcmDelivery(delivery) || delivery.status === "sent" || delivery.attempts >= FCM_DELIVERY_MAX_ATTEMPTS) {
        return false;
      }
      if (tenantId && delivery.tenantId !== tenantId) return false;
      return Date.parse(delivery.nextAttemptAt) <= now;
    })
    .slice(0, boundedLimit);

  let sent = 0;
  let failed = 0;
  let invalidTokensRemoved = 0;
  for (const delivery of due) {
    const device = store.devices.find(
      (candidate) =>
        candidate.tenantId === delivery.tenantId &&
        candidate.userId === delivery.userId &&
        candidate.deviceId === delivery.deviceId &&
        Boolean(getFcmTarget(candidate))
    );
    const item = store.notifications.find((candidate) => candidate.id === delivery.notificationId);
    if (!device || !item) {
      delivery.status = "failed";
      delivery.lastAttemptAt = new Date().toISOString();
      delivery.lastError = !device ? "FCM device is no longer registered." : "Notification is no longer available.";
      failed += 1;
      continue;
    }

    delivery.attempts += 1;
    delivery.lastAttemptAt = new Date().toISOString();
    try {
      await sendMessage(buildFcmMessage(delivery, device, item));
      delivery.status = "sent";
      delivery.deliveredAt = new Date().toISOString();
      delivery.lastError = null;
      sent += 1;
    } catch (error) {
      delivery.lastError = getFcmErrorMessage(error);
      failed += 1;
      if (isInvalidFcmTokenError(error)) {
        delivery.status = "failed";
        store.devices = store.devices.filter(
          (candidate) =>
            candidate.tenantId !== device.tenantId ||
            candidate.userId !== device.userId ||
            candidate.deviceId !== device.deviceId
        );
        invalidTokensRemoved += 1;
        continue;
      }
      delivery.status = delivery.attempts >= FCM_DELIVERY_MAX_ATTEMPTS ? "failed" : "pending";
      delivery.nextAttemptAt = new Date(Date.now() + getFcmRetryDelayMs(delivery.attempts)).toISOString();
    }
  }

  if (queued > 0 || due.length > 0) await save(store);
  return { queued, attempted: due.length, sent, failed, invalidTokensRemoved };
}

export function startFcmNotificationDeliveryWorker() {
  if (fcmWorkerStarted || process.env.VYB_FCM_WORKER_DISABLED === "1") return;
  fcmWorkerStarted = true;
  const configured = Number(process.env.VYB_FCM_WORKER_INTERVAL_MS);
  const intervalMs = Number.isFinite(configured) && configured >= 5_000 ? configured : DEFAULT_FCM_WORKER_INTERVAL_MS;
  let running = false;
  const run = async () => {
    if (running) return;
    running = true;
    try {
      await runFcmNotificationDeliveryOutbox();
    } catch (error) {
      console.warn("[notifications] FCM outbox run failed", {
        message: getFcmErrorMessage(error)
      });
    } finally {
      running = false;
    }
  };
  const initial = setTimeout(run, 1_000);
  const timer = setInterval(run, intervalMs);
  initial.unref?.();
  timer.unref?.();
}

export async function listNotifications(viewer, { state = "all", category = null, limit = 30, cursor = 0 } = {}) {
  const store = await load();
  const filtered = store.notifications
    .filter((item) => belongs(item, viewer))
    .filter((item) => !category || item.category === category)
    .filter((item) => {
      const viewerState = getViewerNotificationState(item, viewer);
      if (state === "unread") return !viewerState.read_at && !viewerState.archived_at;
      if (state === "read") return Boolean(viewerState.read_at) && !viewerState.archived_at;
      if (state === "archived") return Boolean(viewerState.archived_at);
      return !viewerState.archived_at;
    })
    .sort((a, b) => Date.parse(b.created_at) - Date.parse(a.created_at));
  return {
    tenantId: viewer.tenantId,
    items: filtered.slice(cursor, cursor + limit).map((item) => buildViewerNotification(item, viewer)),
    unreadCount: store.notifications.filter((item) => {
      if (!belongs(item, viewer)) return false;
      const viewerState = getViewerNotificationState(item, viewer);
      return !viewerState.read_at && !viewerState.archived_at;
    }).length,
    nextCursor: cursor + limit < filtered.length ? String(cursor + limit) : null
  };
}

export async function markRead(viewer, notificationId) {
  const store = await load();
  const item = store.notifications.find((candidate) => candidate.id === notificationId && belongs(candidate, viewer));
  if (!item) throw new Error("Notification not found.");
  const now = new Date().toISOString();
  const viewerState = ensureViewerNotificationState(item, viewer);
  viewerState.read_at ??= now;
  viewerState.seen_at ??= now;
  await save(store);
  return { item: buildViewerNotification(item, viewer) };
}

export async function markAllRead(viewer, category = null) {
  const store = await load();
  const now = new Date().toISOString();
  let updatedCount = 0;
  for (const item of store.notifications) {
    const viewerState = getViewerNotificationState(item, viewer);
    if (belongs(item, viewer) && !viewerState.read_at && !viewerState.archived_at && (!category || item.category === category)) {
      const mutableViewerState = ensureViewerNotificationState(item, viewer);
      mutableViewerState.read_at = now;
      mutableViewerState.seen_at ??= now;
      updatedCount += 1;
    }
  }
  if (updatedCount) await save(store);
  return { updatedCount, readAt: now };
}

export async function registerDevice(viewer, payload) {
  const deviceId = typeof payload.deviceId === "string" ? payload.deviceId.trim() : "";
  if (!deviceId) throw new Error("A notification device id is required.");
  const allowedPlatforms = new Set(["web", "ios", "android", "desktop", "unknown"]);
  const updatedAt = new Date().toISOString();
  const store = await load();
  const device = {
    userId: viewer.userId,
    tenantId: viewer.tenantId,
    deviceId,
    platform: allowedPlatforms.has(payload.platform) ? payload.platform : "unknown",
    endpoint: typeof payload.endpoint === "string" && payload.endpoint.trim() ? payload.endpoint.trim() : null,
    pushSubscription: payload.pushSubscription && typeof payload.pushSubscription === "object" ? payload.pushSubscription : null,
    updatedAt
  };
  const index = store.devices.findIndex(
    (item) => item.userId === viewer.userId && item.tenantId === viewer.tenantId && item.deviceId === deviceId
  );
  if (index >= 0) store.devices[index] = device;
  else store.devices.push(device);
  await save(store);
  return { deviceId, registered: true, updatedAt };
}
