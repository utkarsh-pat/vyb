import { randomUUID } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { getFirebaseDataConnect } from "../../../../../packages/config/src/index.mjs";
import {
  connectorConfig,
  createCampusEventStore,
  getCampusEventStoreByTenant,
  updateCampusEventStore
} from "../../../../../packages/dataconnect/campus-admin-sdk/esm/index.esm.js";
import { getR2Bucket } from "../../lib/r2-bucket.mjs";

const queues = new Map();
const clone = (value) => JSON.parse(JSON.stringify(value));
const text = (value, fallback = "") => (typeof value === "string" && value.trim() ? value.trim() : fallback);
const makeId = (prefix) => `${prefix}-${randomUUID()}`;

function localPath(tenantId) {
  const defaultRoot =
    process.env.VYB_SERVERLESS_RUNTIME === "vercel"
      ? path.join(os.tmpdir(), "vyb", "campus-events")
      : path.join(process.cwd(), ".tmp", "vyb-campus-events");
  const root = path.resolve(process.env.VYB_EVENTS_STORE_ROOT?.trim() || defaultRoot);
  return path.join(root, `${tenantId}.json`);
}

function isFallbackError(error) {
  if (process.env.NODE_ENV === "production") {
    return false;
  }
  const message = String(error?.message ?? error).toLowerCase();
  return [
    "credential",
    "oauth2",
    "metadata",
    "econnrefused",
    "enotfound",
    "fetch failed",
    "unrecognized operation",
    "failed_precondition",
    "database is not available",
    "billing",
    "paused",
    "unavailable"
  ].some((part) =>
    message.includes(part)
  );
}

async function readLocal(tenantId) {
  try {
    const parsed = JSON.parse(await readFile(localPath(tenantId), "utf8"));
    return { events: Array.isArray(parsed?.events) ? parsed.events : [] };
  } catch {
    return { events: [] };
  }
}

async function readStore(tenantId) {
  try {
    const response = await getCampusEventStoreByTenant(getFirebaseDataConnect(connectorConfig), { id: tenantId });
    const raw = response.data.campusEventStore?.eventsJson;
    return raw ? JSON.parse(raw) : { events: [] };
  } catch (error) {
    if (!isFallbackError(error)) throw error;
    return readLocal(tenantId);
  }
}

async function writeStore(tenantId, store) {
  const eventsJson = JSON.stringify({ events: store.events });
  try {
    const dc = getFirebaseDataConnect(connectorConfig);
    const current = await getCampusEventStoreByTenant(dc, { id: tenantId });
    if (current.data.campusEventStore) {
      await updateCampusEventStore(dc, { id: tenantId, eventsJson });
    } else {
      await createCampusEventStore(dc, { id: tenantId, tenantId, eventsJson });
    }
  } catch (error) {
    if (!isFallbackError(error)) throw error;
    const file = localPath(tenantId);
    await mkdir(path.dirname(file), { recursive: true });
    await writeFile(file, eventsJson, "utf8");
  }
}

async function transact(tenantId, mutator) {
  const previous = queues.get(tenantId) ?? Promise.resolve();
  let release;
  const gate = new Promise((resolve) => (release = resolve));
  queues.set(tenantId, previous.then(() => gate));
  await previous;
  try {
    const store = await readStore(tenantId);
    const result = await mutator(store);
    await writeStore(tenantId, store);
    return { store, result };
  } finally {
    release();
  }
}

function canRead(event, viewer) {
  return !event.communityId || event.host?.userId === viewer.userId || viewer.communityIds.has(event.communityId);
}

function findEvent(store, viewer, eventId) {
  return store.events.find(
    (item) => item.id === eventId && item.tenantId === viewer.tenantId && item.status !== "deleted"
  ) ?? null;
}

function ensureHost(event, viewer) {
  if (!event || event.host?.userId !== viewer.userId) {
    throw new Error("Only the host can manage this event.");
  }
}

function ownedMediaAssets(assets, viewer, maxItems) {
  if (!Array.isArray(assets)) return [];
  if (assets.length > maxItems) {
    throw new Error(`You can attach up to ${maxItems} media files here.`);
  }
  const requiredPrefix = `events/${viewer.tenantId}/${viewer.userId}/`;
  for (const asset of assets) {
    if (
      !asset ||
      typeof asset !== "object" ||
      !text(asset.id) ||
      !text(asset.url) ||
      !text(asset.fileName) ||
      !text(asset.mimeType) ||
      !text(asset.storagePath).startsWith(requiredPrefix)
    ) {
      throw new Error("Event media must belong to the signed-in uploader.");
    }
  }
  return clone(assets);
}

async function deleteMediaAssets(assets) {
  const storagePaths = (assets ?? [])
    .map((asset) => asset?.storagePath?.trim())
    .filter(Boolean);
  if (storagePaths.length === 0) return;
  const bucket = getR2Bucket();
  await Promise.allSettled(
    storagePaths.map((storagePath) => bucket.file(storagePath).delete({ ignoreNotFound: true }))
  );
}

function registrationSummary(event) {
  const registrations = event.registrations ?? [];
  return {
    total: registrations.length,
    submitted: registrations.filter((item) => item.status === "submitted").length,
    approved: registrations.filter((item) => item.status === "approved").length,
    waitlisted: registrations.filter((item) => item.status === "waitlisted").length,
    rejected: registrations.filter((item) => item.status === "rejected").length
  };
}

function toEvent(event, viewer) {
  const registrations = event.registrations ?? [];
  const viewerRegistration = registrations.find((item) => item.attendee?.userId === viewer.userId) ?? null;
  const approvedSeats = registrations
    .filter((item) => item.status === "approved")
    .reduce((sum, item) => sum + Number(item.teamSize ?? 1), 0);
  const closesAt = event.registrationConfig?.closesAt;
  const registrationOpen =
    event.status === "published" &&
    event.responseMode !== "interest" &&
    (!closesAt || Date.parse(closesAt) > Date.now()) &&
    (!event.capacity || approvedSeats < event.capacity);
  return {
    ...clone(event),
    savedCount: (event.savedByUserIds ?? []).length,
    interestCount: (event.interestedUserIds ?? []).length,
    spotsLeft: event.capacity ? Math.max(0, event.capacity - approvedSeats) : null,
    isRegistrationOpen: registrationOpen,
    registrationSummary: registrationSummary(event),
    viewerRegistration: viewerRegistration
      ? {
          id: viewerRegistration.id,
          status: viewerRegistration.status,
          submittedAt: viewerRegistration.submittedAt,
          updatedAt: viewerRegistration.updatedAt,
          teamName: viewerRegistration.teamName ?? null,
          teamSize: viewerRegistration.teamSize ?? 1,
          note: viewerRegistration.note ?? null,
          reviewNote: viewerRegistration.reviewNote ?? null,
          attachmentCount: viewerRegistration.attachments?.length ?? 0
        }
      : null,
    isSaved: (event.savedByUserIds ?? []).includes(viewer.userId),
    isInterested: (event.interestedUserIds ?? []).includes(viewer.userId),
    isHostedByViewer: event.host?.userId === viewer.userId
  };
}

export function dashboard(store, viewer) {
  const visible = store.events
    .filter((event) => event.tenantId === viewer.tenantId && event.status !== "deleted" && canRead(event, viewer));
  const events = visible
    .filter((event) => event.status === "published")
    .map((event) => toEvent(event, viewer))
    .sort((a, b) => Date.parse(a.startsAt) - Date.parse(b.startsAt));
  const hostedEvents = visible.filter((event) => event.host?.userId === viewer.userId).map((event) => toEvent(event, viewer));
  return {
    tenantId: viewer.tenantId,
    viewer: {
      userId: viewer.userId,
      username: viewer.username,
      savedCount: events.filter((event) => event.isSaved).length,
      interestedCount: events.filter((event) => event.isInterested || event.viewerRegistration).length,
      hostedCount: hostedEvents.length,
      hostedPendingCount: hostedEvents.reduce((sum, event) => sum + event.registrationSummary.submitted, 0),
      hostedRegistrationCount: hostedEvents.reduce((sum, event) => sum + event.registrationSummary.total, 0)
    },
    events,
    hostedEvents,
    categories: [...new Set(events.map((event) => event.category).filter(Boolean))].sort()
  };
}

export async function listEvents(viewer) {
  return dashboard(await readStore(viewer.tenantId), viewer);
}

export async function getEvent(viewer, eventId) {
  const event = findEvent(await readStore(viewer.tenantId), viewer, eventId);
  return event && canRead(event, viewer) ? toEvent(event, viewer) : null;
}

export async function createEvent(viewer, payload) {
  if (payload.communityId && !viewer.communityIds.has(payload.communityId)) {
    throw new Error("You can only attach events to communities you belong to.");
  }
  const eventId = makeId("event");
  const now = new Date().toISOString();
  const { store } = await transact(viewer.tenantId, (store) => {
    store.events.unshift({
      id: eventId,
      tenantId: viewer.tenantId,
      communityId: text(payload.communityId) || null,
      host: { userId: viewer.userId, username: viewer.username, displayName: viewer.displayName, role: viewer.role },
      title: text(payload.title),
      club: text(payload.club),
      category: text(payload.category),
      description: text(payload.description),
      location: text(payload.location),
      startsAt: payload.startsAt,
      endsAt: text(payload.endsAt) || null,
      media: ownedMediaAssets(payload.media, viewer, 4),
      passKind: payload.passKind,
      passLabel: text(payload.passLabel, payload.passKind === "paid" ? "Paid" : "Free"),
      capacity: Number.isFinite(Number(payload.capacity)) && Number(payload.capacity) > 0 ? Math.round(Number(payload.capacity)) : null,
      commentCount: 0,
      status: "published",
      createdAt: now,
      savedByUserIds: [],
      interestedUserIds: [],
      responseMode: payload.responseMode,
      registrationConfig: {
        mode: payload.responseMode,
        entryMode: payload.entryMode === "team" ? "team" : "individual",
        closesAt: text(payload.registrationClosesAt) || null,
        requiresApproval: payload.responseMode === "apply",
        teamSizeMin: payload.teamSizeMin ?? null,
        teamSizeMax: payload.teamSizeMax ?? null,
        allowAttachments: Boolean(payload.allowAttachments),
        attachmentLabel: text(payload.attachmentLabel) || null,
        formFields: Array.isArray(payload.formFields) ? payload.formFields : []
      },
      registrations: []
    });
  });
  return { dashboard: dashboard(store, viewer), eventId };
}

export async function updateEvent(viewer, eventId, payload) {
  if (payload.communityId && !viewer.communityIds.has(payload.communityId)) {
    throw new Error("You can only attach events to communities you belong to.");
  }
  let removableMedia = [];
  const { store } = await transact(viewer.tenantId, (store) => {
    const event = findEvent(store, viewer, eventId);
    ensureHost(event, viewer);
    const keepIds = new Set(Array.isArray(payload.keepMediaIds) ? payload.keepMediaIds : []);
    const retainedMedia = (event.media ?? []).filter((asset) => keepIds.has(asset.id));
    removableMedia = (event.media ?? []).filter((asset) => !keepIds.has(asset.id));
    event.communityId = text(payload.communityId) || null;
    event.title = text(payload.title);
    event.club = text(payload.club);
    event.category = text(payload.category);
    event.description = text(payload.description);
    event.location = text(payload.location);
    event.startsAt = payload.startsAt;
    event.endsAt = text(payload.endsAt) || null;
    event.media = [
      ...retainedMedia,
      ...ownedMediaAssets(payload.media, viewer, Math.max(0, 4 - retainedMedia.length))
    ];
    event.passKind = payload.passKind;
    event.passLabel = text(payload.passLabel, payload.passKind === "paid" ? "Paid" : "Free");
    event.capacity =
      Number.isFinite(Number(payload.capacity)) && Number(payload.capacity) > 0
        ? Math.round(Number(payload.capacity))
        : null;
    event.responseMode = payload.responseMode;
    event.registrationConfig = {
      mode: payload.responseMode,
      entryMode: payload.entryMode === "team" ? "team" : "individual",
      closesAt: text(payload.registrationClosesAt) || null,
      requiresApproval: payload.responseMode === "apply",
      teamSizeMin: payload.teamSizeMin ?? null,
      teamSizeMax: payload.teamSizeMax ?? null,
      allowAttachments: Boolean(payload.allowAttachments),
      attachmentLabel: text(payload.attachmentLabel) || null,
      formFields: Array.isArray(payload.formFields) ? payload.formFields : []
    };
  });
  await deleteMediaAssets(removableMedia);
  return { dashboard: dashboard(store, viewer), eventId };
}

export async function toggleEventField(viewer, eventId, field) {
  const { store, result } = await transact(viewer.tenantId, (store) => {
    const event = findEvent(store, viewer, eventId);
    if (!event || event.status !== "published" || !canRead(event, viewer)) throw new Error("This event could not be found.");
    if (field === "interestedUserIds" && event.responseMode !== "interest") {
      throw new Error("This event requires registration instead of simple interest.");
    }
    const values = event[field] ?? [];
    const active = !values.includes(viewer.userId);
    event[field] = active ? [...values, viewer.userId] : values.filter((id) => id !== viewer.userId);
    return active;
  });
  return {
    dashboard: dashboard(store, viewer),
    eventId,
    ...(field === "savedByUserIds" ? { isSaved: result } : { isInterested: result })
  };
}

export async function registerEvent(viewer, eventId, payload) {
  let savedRegistration;
  let removableAttachments = [];
  const { store } = await transact(viewer.tenantId, (store) => {
    const event = findEvent(store, viewer, eventId);
    if (!event || !canRead(event, viewer) || event.status !== "published") throw new Error("This event could not be found.");
    if (event.host?.userId === viewer.userId) throw new Error("Hosts cannot register for their own event.");
    if (event.responseMode === "interest") throw new Error("This event only supports interest.");
    const existing = (event.registrations ?? []).find((item) => item.attendee?.userId === viewer.userId);
    const keepAttachmentIds = new Set(
      Array.isArray(payload.keepAttachmentIds) ? payload.keepAttachmentIds : []
    );
    removableAttachments = (existing?.attachments ?? []).filter(
      (asset) => !keepAttachmentIds.has(asset.id)
    );
    const retainedAttachments = (existing?.attachments ?? []).filter(
      (asset) => keepAttachmentIds.has(asset.id)
    );
    const attachments = [
      ...retainedAttachments,
      ...ownedMediaAssets(
        payload.attachments,
        viewer,
        Math.max(0, 3 - retainedAttachments.length)
      )
    ];
    const now = new Date().toISOString();
    savedRegistration = {
      id: existing?.id ?? makeId("event-reg"),
      eventId,
      attendee: { userId: viewer.userId, username: viewer.username, displayName: viewer.displayName, role: viewer.role },
      status: event.responseMode === "apply" ? "submitted" : "approved",
      submittedAt: existing?.submittedAt ?? now,
      updatedAt: now,
      teamName: text(payload.teamName) || null,
      teamSize: Math.max(1, (payload.teamMembers?.length ?? 0) + 1),
      teamMembers: Array.isArray(payload.teamMembers) ? payload.teamMembers : [],
      answers: Array.isArray(payload.answers) ? payload.answers : [],
      attachments,
      note: text(payload.note) || null,
      reviewNote: null
    };
    event.registrations = existing
      ? event.registrations.map((item) => (item.id === existing.id ? savedRegistration : item))
      : [savedRegistration, ...(event.registrations ?? [])];
    if (!(event.interestedUserIds ?? []).includes(viewer.userId)) event.interestedUserIds.push(viewer.userId);
  });
  await deleteMediaAssets(removableAttachments);
  const event = findEvent(store, viewer, eventId);
  return { dashboard: dashboard(store, viewer), event: toEvent(event, viewer), registration: toEvent(event, viewer).viewerRegistration };
}

export async function getViewerRegistration(viewer, eventId) {
  const event = findEvent(await readStore(viewer.tenantId), viewer, eventId);
  if (!event || !canRead(event, viewer)) throw new Error("This event could not be found.");
  if (event.host?.userId === viewer.userId) {
    throw new Error("Hosts do not have attendee registrations for their own event.");
  }
  return {
    event: toEvent(event, viewer),
    registration:
      clone((event.registrations ?? []).find((item) => item.attendee?.userId === viewer.userId) ?? null)
  };
}

export async function listRegistrations(viewer, eventId) {
  const event = findEvent(await readStore(viewer.tenantId), viewer, eventId);
  if (!event || event.host?.userId !== viewer.userId) throw new Error("Only the host can view registrations.");
  return { event: toEvent(event, viewer), registrations: clone(event.registrations ?? []) };
}

export async function manageRegistration(viewer, eventId, registrationId, payload) {
  let nextStatus = null;
  const { store } = await transact(viewer.tenantId, (store) => {
    const event = findEvent(store, viewer, eventId);
    ensureHost(event, viewer);
    const registration = (event.registrations ?? []).find((item) => item.id === registrationId);
    if (!registration) throw new Error("This registration could not be found.");
    if (payload.status === "approved" && event.capacity) {
      const approvedSeats = (event.registrations ?? [])
        .filter((item) => item.status === "approved" && item.id !== registration.id)
        .reduce((sum, item) => sum + Number(item.teamSize ?? 1), 0);
      if (approvedSeats + Number(registration.teamSize ?? 1) > event.capacity) {
        throw new Error("There are not enough spots left to approve this registration.");
      }
    }
    registration.status = payload.status;
    registration.reviewNote = text(payload.reviewNote) || null;
    registration.updatedAt = new Date().toISOString();
    nextStatus = registration.status;
  });
  const event = findEvent(store, viewer, eventId);
  return {
    dashboard: dashboard(store, viewer),
    event: toEvent(event, viewer),
    registrations: clone(
      [...(event.registrations ?? [])].sort(
        (left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt)
      )
    ),
    registrationId,
    status: nextStatus
  };
}

export async function cancelEvent(viewer, eventId) {
  const { store } = await transact(viewer.tenantId, (store) => {
    const event = findEvent(store, viewer, eventId);
    if (!event || event.host?.userId !== viewer.userId) throw new Error("Only the host can cancel this event.");
    event.status = "cancelled";
  });
  return { dashboard: dashboard(store, viewer), eventId, action: "cancelled" };
}

export async function deleteEvent(viewer, eventId) {
  let removableMedia = [];
  const { store } = await transact(viewer.tenantId, (store) => {
    const event = findEvent(store, viewer, eventId);
    ensureHost(event, viewer);
    event.status = "deleted";
    removableMedia = [
      ...(event.media ?? []),
      ...(event.registrations ?? []).flatMap((registration) => registration.attachments ?? [])
    ];
  });
  await deleteMediaAssets(removableMedia);
  return { dashboard: dashboard(store, viewer), eventId, action: "deleted" };
}
