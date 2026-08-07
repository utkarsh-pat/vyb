"use client";

export type ContentEventType =
  | "impression" | "qualified_view" | "video_play" | "video_view"
  | "video_replay" | "video_complete" | "carousel_slide";

type QueuedEvent = {
  eventKey: string;
  postId: string;
  sessionKey: string;
  eventType: ContentEventType;
  source: "web";
  visibleMs: number;
  watchMs: number;
  progressBasisPoints: number;
  occurredAt: string;
};

const queue: QueuedEvent[] = [];
const QUEUE_STORAGE_KEY = "vyb.measurement.queue.v1";
const MAX_QUEUED_EVENTS = 200;
let flushTimer: ReturnType<typeof setTimeout> | null = null;
let flushing = false;
let hydrated = false;

function persistQueue() {
  try {
    window.localStorage.setItem(QUEUE_STORAGE_KEY, JSON.stringify(queue.slice(-MAX_QUEUED_EVENTS)));
  } catch {
    // Storage may be unavailable; the in-memory queue still works for this page.
  }
}

function hydrateQueue() {
  if (hydrated || typeof window === "undefined") return;
  hydrated = true;
  try {
    const value = JSON.parse(window.localStorage.getItem(QUEUE_STORAGE_KEY) ?? "[]") as unknown;
    if (Array.isArray(value)) queue.push(...(value as QueuedEvent[]).slice(-MAX_QUEUED_EVENTS));
  } catch {
    window.localStorage.removeItem(QUEUE_STORAGE_KEY);
  }
}

function sessionKey() {
  const storageKey = "vyb.measurement.session";
  let value = sessionStorage.getItem(storageKey);
  if (!value) {
    value = crypto.randomUUID().replaceAll("-", "");
    sessionStorage.setItem(storageKey, value);
  }
  return value;
}

export function recordContentEvent(
  postId: string,
  eventType: ContentEventType,
  metrics: { visibleMs?: number; watchMs?: number; progressBasisPoints?: number } = {}
) {
  if (typeof window === "undefined" || document.visibilityState === "hidden") return;
  hydrateQueue();
  queue.push({
    eventKey: crypto.randomUUID(), postId, sessionKey: sessionKey(), eventType, source: "web",
    visibleMs: Math.max(0, Math.trunc(metrics.visibleMs ?? 0)),
    watchMs: Math.max(0, Math.trunc(metrics.watchMs ?? 0)),
    progressBasisPoints: Math.max(0, Math.min(10_000, Math.trunc(metrics.progressBasisPoints ?? 0))),
    occurredAt: new Date().toISOString()
  });
  if (queue.length > MAX_QUEUED_EVENTS) queue.splice(0, queue.length - MAX_QUEUED_EVENTS);
  persistQueue();
  if (queue.length >= 20) void flushContentEvents();
  else if (!flushTimer) flushTimer = setTimeout(() => void flushContentEvents(), 10_000);
}

export async function flushContentEvents() {
  hydrateQueue();
  if (flushing || queue.length === 0) return;
  flushing = true;
  if (flushTimer) clearTimeout(flushTimer);
  flushTimer = null;
  const batch = queue.splice(0, 20);
  persistQueue();
  try {
    const response = await fetch("/api/analytics/events", {
      method: "POST", headers: { "content-type": "application/json" },
      body: JSON.stringify({ events: batch }), keepalive: true, credentials: "same-origin"
    });
    if (!response.ok && response.status >= 500) queue.unshift(...batch);
  } catch {
    queue.unshift(...batch);
  } finally {
    if (queue.length > MAX_QUEUED_EVENTS) queue.length = MAX_QUEUED_EVENTS;
    persistQueue();
    flushing = false;
    if (queue.length > 0 && !flushTimer) flushTimer = setTimeout(() => void flushContentEvents(), 15_000);
  }
}

if (typeof window !== "undefined") {
  window.addEventListener("pagehide", () => void flushContentEvents());
}
