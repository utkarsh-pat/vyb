import { createHmac, randomUUID } from "node:crypto";
import { getFirebaseDataConnect } from "../../../../../packages/config/src/index.mjs";
import {
  connectorConfig,
  createContentDailyInsight,
  createContentEvent,
  createContentUniqueViewer,
  createContentUniqueViewerDay,
  deleteContentDailyInsight,
  deleteContentEvent,
  deleteContentPurgeRequest,
  deleteContentUniqueViewer,
  deleteContentUniqueViewerDay,
  getContentDailyInsightByKey,
  getContentEventByKey,
  getContentMeasurementPreference,
  getContentUniqueViewerByKey,
  getContentUniqueViewerDayByKey,
  listContentDailyInsightsByPost,
  listContentDailyInsightIdsByPost,
  listContentEventIdsByPost,
  listContentEventIdsByViewerKey,
  listContentEventsByPostWindow,
  listReadyContentPurgeRequests,
  listContentUniqueViewerDayIdsByPost,
  listContentUniqueViewerDayIdsByViewerKey,
  listContentUniqueViewerIdsByPost,
  listContentUniqueViewerIdsByViewerKey,
  listContentUniqueViewersByPost,
  listContentUniqueViewersByPostSince,
  listExpiredContentEvents,
  listExpiredContentUniqueViewerDays,
  listRecentContentEventsForViewer,
  listUnrolledContentEvents,
  markContentEventRolledUp,
  updateContentDailyInsight,
  upsertContentMeasurementPreference
} from "../../../../../packages/dataconnect/social-admin-sdk/esm/index.esm.js";

const RAW_RETENTION_DAYS = Math.min(14, Math.max(3, Number(process.env.VYB_ANALYTICS_RAW_RETENTION_DAYS ?? 14)));
const UNIQUE_DAY_RETENTION_DAYS = 90;
const MAX_ROLLUP_EVENTS_PER_DAY = 50_000;
const DELETE_BATCH_SIZE = 1_000;

const EVENT_COOLDOWN_MS = Object.freeze({
  impression: 30 * 60_000,
  qualified_view: 30 * 60_000,
  video_play: 5 * 60_000,
  video_view: 5 * 60_000,
  video_complete: 5 * 60_000,
  video_replay: 60_000,
  carousel_slide: 5_000
});

export function contentEventCooldownMs(eventType) {
  return EVENT_COOLDOWN_MS[eventType] ?? 30 * 60_000;
}

function dc() {
  return getFirebaseDataConnect(connectorConfig);
}

export function deriveViewerKey(userId, tenantId = "local") {
  const secret = process.env.VYB_ANALYTICS_VIEWER_KEY_SECRET?.trim() ||
    (process.env.NODE_ENV === "production" ? "" : "local-vyb-analytics-viewer-key");
  if (!secret || secret.length < 24) {
    throw new Error("VYB_ANALYTICS_VIEWER_KEY_SECRET must contain at least 24 characters in production.");
  }
  return createHmac("sha256", secret)
    .update(`${String(tenantId)}:${String(userId)}`)
    .digest("base64url");
}

function measurementPreferenceKey(tenantId, userId) {
  return `${tenantId}:${userId}`;
}

export async function getContentMeasurementPreferenceForUser({ tenantId, userId }) {
  const preferenceKey = measurementPreferenceKey(tenantId, userId);
  const response = await getContentMeasurementPreference(dc(), { preferenceKey });
  const row = response.data.contentMeasurementPreferences[0] ?? null;
  return {
    measurementEnabled: row?.measurementEnabled !== false,
    updatedAt: row?.updatedAt ?? null
  };
}

export async function setContentMeasurementPreferenceForUser({ tenantId, userId, measurementEnabled }) {
  const preferenceKey = measurementPreferenceKey(tenantId, userId);
  const existing = await getContentMeasurementPreference(dc(), { preferenceKey });
  const id = existing.data.contentMeasurementPreferences[0]?.id ?? randomUUID();
  await upsertContentMeasurementPreference(dc(), {
    id,
    preferenceKey,
    tenantId,
    userId,
    measurementEnabled: measurementEnabled !== false
  });
  return { measurementEnabled: measurementEnabled !== false };
}

function addDays(value, days) {
  return new Date(value.getTime() + days * 86_400_000);
}

export function utcDate(value) {
  return new Date(value).toISOString().slice(0, 10);
}

function utcWindow(date) {
  const from = new Date(`${date}T00:00:00.000Z`);
  return { from, until: addDays(from, 1) };
}

function isAlreadyExists(error) {
  const message = error instanceof Error ? error.message : String(error);
  return /already exists|duplicate key|unique constraint|ALREADY_EXISTS/iu.test(message);
}

export function aggregateContentEvents(events) {
  const qualifiedViewers = new Set();
  const totals = {
    impressionCount: 0,
    qualifiedViewCount: 0,
    uniqueReachCount: 0,
    videoPlayCount: 0,
    videoViewCount: 0,
    replayCount: 0,
    watchMsTotal: 0,
    completionCount: 0,
    carouselSlideCount: 0
  };

  for (const event of events) {
    switch (event.eventType) {
      case "impression":
        totals.impressionCount += 1;
        break;
      case "qualified_view":
        totals.qualifiedViewCount += 1;
        qualifiedViewers.add(event.viewerKey);
        break;
      case "video_play":
        totals.videoPlayCount += 1;
        break;
      case "video_view":
        totals.videoViewCount += 1;
        totals.watchMsTotal += Math.max(0, Number(event.watchMs ?? 0));
        break;
      case "video_replay":
        totals.replayCount += 1;
        break;
      case "video_complete":
        totals.completionCount += 1;
        totals.watchMsTotal += Math.max(0, Number(event.watchMs ?? 0));
        break;
      case "carousel_slide":
        totals.carouselSlideCount += 1;
        break;
      default:
        break;
    }
  }
  totals.uniqueReachCount = qualifiedViewers.size;
  return { totals, qualifiedViewers };
}

export async function ingestContentEvents({ tenantId, viewerUserId, events }) {
  const preference = await getContentMeasurementPreferenceForUser({ tenantId, userId: viewerUserId });
  if (!preference.measurementEnabled) {
    return { accepted: 0, duplicates: 0, throttled: 0, disabled: true, retentionDays: RAW_RETENTION_DAYS };
  }
  const instance = dc();
  const viewerKey = deriveViewerKey(viewerUserId, tenantId);
  const expiresAt = addDays(new Date(), RAW_RETENTION_DAYS).toISOString();
  let accepted = 0;
  let duplicates = 0;
  let throttled = 0;

  for (const event of events) {
    const existing = await getContentEventByKey(instance, { eventKey: event.eventKey });
    if (existing.data.contentEvents.length > 0) {
      duplicates += 1;
      continue;
    }
    const cooldownMs = contentEventCooldownMs(event.eventType);
    if (cooldownMs > 0) {
      const recent = await listRecentContentEventsForViewer(instance, {
        postId: event.postId,
        viewerKey,
        eventType: event.eventType,
        from: new Date(new Date(event.occurredAt).getTime() - cooldownMs).toISOString(),
        limit: 1
      });
      if (recent.data.contentEvents.length > 0) {
        throttled += 1;
        continue;
      }
    }
    try {
      await createContentEvent(instance, {
        id: randomUUID(),
        eventKey: event.eventKey,
        tenantId,
        postId: event.postId,
        viewerKey,
        sessionKey: event.sessionKey,
        eventType: event.eventType,
        source: event.source,
        visibleMs: event.visibleMs,
        watchMs: event.watchMs,
        progressBasisPoints: event.progressBasisPoints,
        occurredAt: event.occurredAt,
        expiresAt
      });
      accepted += 1;
    } catch (error) {
      if (!isAlreadyExists(error)) throw error;
      duplicates += 1;
    }
  }
  return { accepted, duplicates, throttled, retentionDays: RAW_RETENTION_DAYS };
}

async function deleteAllByPost({ postId, list, remove, field }) {
  const instance = dc();
  let deleted = 0;
  for (;;) {
    const result = await list(instance, { postId, limit: DELETE_BATCH_SIZE });
    const rows = result.data[field];
    if (rows.length === 0) return deleted;
    for (const row of rows) {
      await remove(instance, { id: row.id });
      deleted += 1;
    }
  }
}

export async function purgeContentMeasurementForPost(postId) {
  const contentEvents = await deleteAllByPost({
    postId, list: listContentEventIdsByPost, remove: deleteContentEvent, field: "contentEvents"
  });
  const uniqueViewerDays = await deleteAllByPost({
    postId,
    list: listContentUniqueViewerDayIdsByPost,
    remove: deleteContentUniqueViewerDay,
    field: "contentUniqueViewerDays"
  });
  const uniqueViewers = await deleteAllByPost({
    postId,
    list: listContentUniqueViewerIdsByPost,
    remove: deleteContentUniqueViewer,
    field: "contentUniqueViewers"
  });
  const dailyInsights = await deleteAllByPost({
    postId,
    list: listContentDailyInsightIdsByPost,
    remove: deleteContentDailyInsight,
    field: "contentDailyInsights"
  });
  return { contentEvents, uniqueViewerDays, uniqueViewers, dailyInsights };
}

async function deleteAllByViewerKey({ viewerKey, list, remove, field }) {
  const instance = dc();
  let deleted = 0;
  for (;;) {
    const result = await list(instance, { viewerKey, limit: DELETE_BATCH_SIZE });
    const rows = result.data[field];
    if (rows.length === 0) return deleted;
    for (const row of rows) {
      await remove(instance, { id: row.id });
      deleted += 1;
    }
  }
}

export async function purgeContentMeasurementForViewer({ tenantId, userId }) {
  const viewerKey = deriveViewerKey(userId, tenantId);
  const contentEvents = await deleteAllByViewerKey({
    viewerKey, list: listContentEventIdsByViewerKey, remove: deleteContentEvent, field: "contentEvents"
  });
  const uniqueViewerDays = await deleteAllByViewerKey({
    viewerKey,
    list: listContentUniqueViewerDayIdsByViewerKey,
    remove: deleteContentUniqueViewerDay,
    field: "contentUniqueViewerDays"
  });
  const uniqueViewers = await deleteAllByViewerKey({
    viewerKey,
    list: listContentUniqueViewerIdsByViewerKey,
    remove: deleteContentUniqueViewer,
    field: "contentUniqueViewers"
  });
  return { contentEvents, uniqueViewerDays, uniqueViewers };
}

async function ensureUniqueLedgers({ tenantId, postId, date, events }) {
  const instance = dc();
  const viewedBy = new Map();
  for (const event of events) {
    if (event.eventType === "qualified_view") {
      const current = viewedBy.get(event.viewerKey);
      if (!current || new Date(event.occurredAt).getTime() > new Date(current).getTime()) {
        viewedBy.set(event.viewerKey, event.occurredAt);
      }
    }
  }

  for (const [viewerKey, viewedAt] of viewedBy) {
    const dayKey = `${postId}:${viewerKey}:${date}`;
    const day = await getContentUniqueViewerDayByKey(instance, { uniqueKey: dayKey });
    if (day.data.contentUniqueViewerDays.length === 0) {
      try {
        await createContentUniqueViewerDay(instance, {
          id: randomUUID(), uniqueKey: dayKey, tenantId, postId, viewerKey,
          viewDate: date, viewedAt,
          expiresAt: addDays(new Date(`${date}T00:00:00.000Z`), UNIQUE_DAY_RETENTION_DAYS).toISOString()
        });
      } catch (error) {
        if (!isAlreadyExists(error)) throw error;
      }
    }

    const lifetimeKey = `${postId}:${viewerKey}`;
    const lifetime = await getContentUniqueViewerByKey(instance, { uniqueKey: lifetimeKey });
    const lifetimeRow = lifetime.data.contentUniqueViewers[0];
    if (!lifetimeRow) {
      try {
        await createContentUniqueViewer(instance, {
          id: randomUUID(), uniqueKey: lifetimeKey, tenantId, postId, viewerKey, viewedAt
        });
      } catch (error) {
        if (!isAlreadyExists(error)) throw error;
      }
    } else if (new Date(viewedAt).getTime() > new Date(lifetimeRow.lastViewedAt).getTime()) {
      await updateContentUniqueViewer(instance, {
        id: lifetimeRow.id,
        qualifiedViewCount: lifetimeRow.qualifiedViewCount,
        viewedAt
      });
    }
  }
}

async function replaceDailyInsight({ tenantId, postId, date, totals }) {
  const instance = dc();
  const insightKey = `${postId}:${date}`;
  const existing = await getContentDailyInsightByKey(instance, { insightKey });
  const values = { ...totals, watchMsTotal: String(totals.watchMsTotal) };
  const row = existing.data.contentDailyInsights[0];
  if (row) {
    await updateContentDailyInsight(instance, { id: row.id, ...values });
    return;
  }
  try {
    await createContentDailyInsight(instance, {
      id: randomUUID(), insightKey, tenantId, postId, insightDate: date, ...values
    });
  } catch (error) {
    if (!isAlreadyExists(error)) throw error;
    const raced = await getContentDailyInsightByKey(instance, { insightKey });
    const racedRow = raced.data.contentDailyInsights[0];
    if (!racedRow) throw error;
    await updateContentDailyInsight(instance, { id: racedRow.id, ...values });
  }
}

export async function rollupContentEvents({ limit = 1000 } = {}) {
  const instance = dc();
  const pending = await listUnrolledContentEvents(instance, { limit: Math.min(5000, Math.max(1, limit)) });
  const groups = new Map();
  for (const event of pending.data.contentEvents) {
    const date = utcDate(event.occurredAt);
    const key = `${event.postId}:${date}`;
    const group = groups.get(key) ?? { tenantId: event.tenantId, postId: event.postId, date, pendingIds: [] };
    group.pendingIds.push(event.id);
    groups.set(key, group);
  }

  let rolledUp = 0;
  for (const group of groups.values()) {
    const { from, until } = utcWindow(group.date);
    const response = await listContentEventsByPostWindow(instance, {
      postId: group.postId, from: from.toISOString(), until: until.toISOString(), limit: MAX_ROLLUP_EVENTS_PER_DAY
    });
    const allEvents = response.data.contentEvents;
    if (allEvents.length >= MAX_ROLLUP_EVENTS_PER_DAY) {
      throw new Error(`Content rollup safety limit reached for post ${group.postId} on ${group.date}.`);
    }
    const { totals } = aggregateContentEvents(allEvents);
    await ensureUniqueLedgers({ ...group, events: allEvents });
    await replaceDailyInsight({ ...group, totals });
    for (const id of group.pendingIds) {
      await markContentEventRolledUp(instance, { id });
      rolledUp += 1;
    }
  }
  return { selected: pending.data.contentEvents.length, rolledUp, groups: groups.size };
}

export async function purgeExpiredContentMeasurement({ limit = 2000 } = {}) {
  const instance = dc();
  const before = new Date().toISOString();
  const bounded = Math.min(5000, Math.max(1, limit));
  const [raw, day] = await Promise.all([
    listExpiredContentEvents(instance, { before, limit: bounded }),
    listExpiredContentUniqueViewerDays(instance, { before, limit: bounded })
  ]);
  await Promise.all(raw.data.contentEvents.map(({ id }) => deleteContentEvent(instance, { id })));
  await Promise.all(day.data.contentUniqueViewerDays.map(({ id }) => deleteContentUniqueViewerDay(instance, { id })));
  return { rawDeleted: raw.data.contentEvents.length, uniqueDayDeleted: day.data.contentUniqueViewerDays.length };
}

export async function processContentPurgeRequests({ limit = 200 } = {}) {
  const instance = dc();
  const before = new Date(Date.now() - 5 * 60_000).toISOString();
  const requests = await listReadyContentPurgeRequests(instance, {
    before,
    limit: Math.min(1000, Math.max(1, limit))
  });
  let completed = 0;
  for (const request of requests.data.contentPurgeRequests) {
    await purgeContentMeasurementForPost(request.postId);
    await deleteContentPurgeRequest(instance, { id: request.id });
    completed += 1;
  }
  return { selected: requests.data.contentPurgeRequests.length, completed };
}

export async function getContentInsights({ postId, range = "7d" }) {
  const instance = dc();
  if (range === "24h") {
    const until = new Date();
    const from = new Date(until.getTime() - 86_400_000);
    const raw = await listContentEventsByPostWindow(instance, {
      postId, from: from.toISOString(), until: until.toISOString(), limit: MAX_ROLLUP_EVENTS_PER_DAY
    });
    if (raw.data.contentEvents.length >= MAX_ROLLUP_EVENTS_PER_DAY) {
      throw new Error(`24-hour insight safety limit reached for post ${postId}.`);
    }
    const { totals, qualifiedViewers } = aggregateContentEvents(raw.data.contentEvents);
    return { range, views: totals.qualifiedViewCount, reach: qualifiedViewers.size, ...totals, daily: [] };
  }
  const days = range === "30d" ? 30 : range === "lifetime" ? 36500 : 7;
  const since = utcDate(addDays(new Date(), -(days - 1)));
  const daily = await listContentDailyInsightsByPost(instance, { postId, since, limit: Math.min(days + 2, 36600) });
  const totals = aggregateInsightRows(daily.data.contentDailyInsights);
  let reach;
  if (range === "lifetime") {
    const rows = await listContentUniqueViewersByPost(instance, { postId, limit: 50_000 });
    reach = new Set(rows.data.contentUniqueViewers.map((row) => row.viewerKey)).size;
  } else {
    const rows = await listContentUniqueViewersByPostSince(instance, {
      postId, since: new Date(`${since}T00:00:00.000Z`).toISOString(), limit: 50_000
    });
    reach = new Set(rows.data.contentUniqueViewers.map((row) => row.viewerKey)).size;
  }
  return { range, views: totals.qualifiedViewCount, reach, ...totals, daily: daily.data.contentDailyInsights };
}

export function aggregateInsightRows(rows) {
  return rows.reduce((acc, row) => {
    for (const key of ["impressionCount", "qualifiedViewCount", "videoPlayCount", "videoViewCount", "replayCount", "completionCount", "carouselSlideCount"]) {
      acc[key] += Number(row[key] ?? 0);
    }
    acc.watchMsTotal += Number(row.watchMsTotal ?? 0);
    return acc;
  }, { impressionCount: 0, qualifiedViewCount: 0, videoPlayCount: 0, videoViewCount: 0, replayCount: 0, watchMsTotal: 0, completionCount: 0, carouselSlideCount: 0 });
}
