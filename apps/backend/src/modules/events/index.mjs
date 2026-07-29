import { readJson, sendError, sendJson } from "../../lib/http.mjs";
import { getProfileByUserId } from "../identity/profile-repository.mjs";
import { resolveLiveContext } from "../shared/viewer-context.mjs";
import {
  cancelEvent,
  createEvent,
  deleteEvent,
  getEvent,
  getViewerRegistration,
  listEvents,
  listRegistrations,
  manageRegistration,
  registerEvent,
  toggleEventField,
  updateEvent
} from "./repository.mjs";

const buckets = new Map();
const rules = [
  { method: "POST", pattern: /^\/v1\/events$/u, max: 10 },
  { method: "PUT", pattern: /^\/v1\/events\/[^/]+\/(?:save|interest)$/u, max: 60 },
  { method: "POST", pattern: /^\/v1\/events\/[^/]+\/register$/u, max: 20 },
  { method: "POST", pattern: /^\/v1\/events\/[^/]+\/cancel$/u, max: 10 }
];

function nonEmpty(value) {
  return typeof value === "string" && value.trim().length > 0;
}

function enforceRate(response, method, pathname, userId) {
  const rule = rules.find((item) => item.method === method && item.pattern.test(pathname));
  if (!rule) return true;
  const now = Date.now();
  const key = `${method}:${pathname.replace(/\/[^/]+\/(save|interest|register|cancel)$/u, "/:id/$1")}:${userId}`;
  const bucket = buckets.get(key);
  const current = bucket && bucket.resetAt > now ? bucket : { count: 0, resetAt: now + 60_000 };
  if (current.count >= rule.max) {
    sendError(response, 429, "RATE_LIMITED", "Slow down before updating more events.", null, {
      "retry-after": String(Math.max(1, Math.ceil((current.resetAt - now) / 1000)))
    });
    return false;
  }
  current.count += 1;
  buckets.set(key, current);
  return true;
}

function sendFailure(response, error) {
  const message = error instanceof Error ? error.message : "The events service is unavailable.";
  const forbidden = /only the host|communities you belong|hosts cannot/i.test(message);
  const notFound = /could not be found/i.test(message);
  sendError(response, forbidden ? 403 : notFound ? 404 : 400, forbidden ? "FORBIDDEN" : notFound ? "EVENT_NOT_FOUND" : "EVENT_REQUEST_FAILED", message);
}

function validateCreate(payload) {
  if (!payload || typeof payload !== "object") return "Request body must be valid JSON.";
  for (const field of ["title", "club", "category", "description", "location", "startsAt"]) {
    if (!nonEmpty(payload[field])) return `${field} is required.`;
  }
  if (!Number.isFinite(Date.parse(payload.startsAt))) return "startsAt must be a valid ISO date.";
  if (!["free", "rsvp", "paid"].includes(payload.passKind)) return "passKind must be free, rsvp, or paid.";
  if (!["interest", "register", "apply"].includes(payload.responseMode)) return "responseMode must be interest, register, or apply.";
  return null;
}

export function getEventsModuleHealth() {
  return { module: "events", status: "ok" };
}

export async function handleEventsRoute({ request, response, url, context }) {
  if (!url.pathname.startsWith("/v1/events")) return false;
  if (!context.actor) return false;
  const resolved = await resolveLiveContext(context.actor);
  if (!resolved?.live?.tenant || !resolved.live.user || !resolved.live.membership) {
    sendError(response, 401, "UNAUTHENTICATED", "An authenticated membership is required.");
    return true;
  }
  if (!enforceRate(response, request.method, url.pathname, resolved.live.user.id)) return true;

  const profile = await getProfileByUserId({
    tenantId: resolved.live.tenant.id,
    userId: resolved.live.user.id
  }).catch(() => null);
  const viewer = {
    tenantId: resolved.live.tenant.id,
    userId: resolved.live.user.id,
    username: profile?.username ?? context.actor.email?.split("@")[0] ?? resolved.live.user.id,
    displayName: profile?.fullName ?? context.actor.displayName ?? "Vyb Student",
    role: resolved.live.membership.role ?? "student",
    communityIds: new Set(
      (resolved.live.communities ?? [])
        .filter((item) => item.community?.tenantId === resolved.live.tenant.id)
        .map((item) => item.community?.id)
        .filter(Boolean)
    )
  };

  try {
    if (request.method === "GET" && url.pathname === "/v1/events") {
      sendJson(response, 200, await listEvents(viewer));
      return true;
    }
    if (request.method === "POST" && url.pathname === "/v1/events") {
      const payload = await readJson(request);
      const error = validateCreate(payload);
      if (error) {
        sendError(response, 400, "INVALID_EVENT", error);
        return true;
      }
      sendJson(response, 201, await createEvent(viewer, payload));
      return true;
    }

    const update = request.method === "PUT" ? url.pathname.match(/^\/v1\/events\/([^/]+)$/u) : null;
    if (update) {
      const payload = await readJson(request);
      const error = validateCreate(payload);
      if (error) {
        sendError(response, 400, "INVALID_EVENT", error);
        return true;
      }
      sendJson(response, 200, await updateEvent(viewer, update[1], payload));
      return true;
    }

    const remove = request.method === "DELETE" ? url.pathname.match(/^\/v1\/events\/([^/]+)$/u) : null;
    if (remove) {
      sendJson(response, 200, await deleteEvent(viewer, remove[1]));
      return true;
    }

    const detail = request.method === "GET" ? url.pathname.match(/^\/v1\/events\/([^/]+)$/u) : null;
    if (detail) {
      const event = await getEvent(viewer, detail[1]);
      if (!event) sendError(response, 404, "EVENT_NOT_FOUND", "This event could not be found.");
      else sendJson(response, 200, { event });
      return true;
    }

    const toggle = request.method === "PUT" ? url.pathname.match(/^\/v1\/events\/([^/]+)\/(save|interest)$/u) : null;
    if (toggle) {
      sendJson(response, 200, await toggleEventField(viewer, toggle[1], toggle[2] === "save" ? "savedByUserIds" : "interestedUserIds"));
      return true;
    }

    const register = request.method === "POST" ? url.pathname.match(/^\/v1\/events\/([^/]+)\/register$/u) : null;
    if (register) {
      const payload = await readJson(request);
      if (!payload || typeof payload !== "object") {
        sendError(response, 400, "INVALID_JSON", "Request body must be valid JSON.");
        return true;
      }
      sendJson(response, 200, await registerEvent(viewer, register[1], payload));
      return true;
    }

    const viewerRegistration =
      request.method === "GET" ? url.pathname.match(/^\/v1\/events\/([^/]+)\/register$/u) : null;
    if (viewerRegistration) {
      sendJson(response, 200, await getViewerRegistration(viewer, viewerRegistration[1]));
      return true;
    }

    const registrations = request.method === "GET" ? url.pathname.match(/^\/v1\/events\/([^/]+)\/registrations$/u) : null;
    if (registrations) {
      sendJson(response, 200, await listRegistrations(viewer, registrations[1]));
      return true;
    }

    const manageRegistrationMatch =
      request.method === "PUT"
        ? url.pathname.match(/^\/v1\/events\/([^/]+)\/registrations\/([^/]+)$/u)
        : null;
    if (manageRegistrationMatch) {
      const payload = await readJson(request);
      if (
        !payload ||
        typeof payload !== "object" ||
        !["approved", "waitlisted", "rejected"].includes(payload.status)
      ) {
        sendError(response, 400, "INVALID_REGISTRATION_STATUS", "Choose a valid registration status.");
        return true;
      }
      sendJson(
        response,
        200,
        await manageRegistration(
          viewer,
          manageRegistrationMatch[1],
          manageRegistrationMatch[2],
          payload
        )
      );
      return true;
    }

    const cancel = request.method === "POST" ? url.pathname.match(/^\/v1\/events\/([^/]+)\/cancel$/u) : null;
    if (cancel) {
      sendJson(response, 200, await cancelEvent(viewer, cancel[1]));
      return true;
    }
  } catch (error) {
    sendFailure(response, error);
    return true;
  }

  return false;
}
