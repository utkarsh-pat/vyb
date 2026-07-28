import { readJson, sendError, sendJson } from "../../lib/http.mjs";
import { resolveLiveContext } from "../shared/viewer-context.mjs";
import {
  listNotifications,
  markAllRead,
  markRead,
  registerDevice,
  startFcmNotificationDeliveryWorker
} from "./repository.mjs";

if (process.env.VYB_SERVERLESS_RUNTIME !== "vercel") {
  startFcmNotificationDeliveryWorker();
}

export function getNotificationsModuleHealth() {
  return { module: "notifications", status: "ok" };
}

export async function handleNotificationsRoute({ request, response, url, context }) {
  if (!url.pathname.startsWith("/v1/notifications")) return false;
  if (!context.actor) return false;
  const resolved = await resolveLiveContext(context.actor);
  if (!resolved?.live?.tenant || !resolved.live.user || !resolved.live.membership) {
    sendError(response, 401, "UNAUTHENTICATED", "An authenticated membership is required.");
    return true;
  }
  const viewer = { tenantId: resolved.live.tenant.id, userId: resolved.live.user.id };

  try {
    if (request.method === "GET" && url.pathname === "/v1/notifications") {
      const state = ["all", "unread", "read", "archived"].includes(url.searchParams.get("state"))
        ? url.searchParams.get("state")
        : "all";
      const limit = Math.max(1, Math.min(100, Number(url.searchParams.get("limit")) || 30));
      const cursor = Math.max(0, Number(url.searchParams.get("cursor")) || 0);
      sendJson(response, 200, await listNotifications(viewer, {
        state,
        category: url.searchParams.get("category"),
        limit,
        cursor
      }));
      return true;
    }
    const readMatch = request.method === "PUT" ? url.pathname.match(/^\/v1\/notifications\/([^/]+)\/read$/u) : null;
    if (readMatch) {
      sendJson(response, 200, await markRead(viewer, readMatch[1]));
      return true;
    }
    if (request.method === "PUT" && url.pathname === "/v1/notifications/read-all") {
      const payload = await readJson(request);
      sendJson(response, 200, await markAllRead(viewer, typeof payload?.category === "string" ? payload.category.trim() : null));
      return true;
    }
    if (request.method === "POST" && url.pathname === "/v1/notifications/register-device") {
      const payload = await readJson(request);
      if (!payload || typeof payload !== "object") {
        sendError(response, 400, "INVALID_JSON", "Request body must be valid JSON.");
        return true;
      }
      sendJson(response, 201, await registerDevice(viewer, payload));
      return true;
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : "Notifications service is unavailable.";
    sendError(response, /not found/i.test(message) ? 404 : 400, "NOTIFICATION_REQUEST_FAILED", message);
    return true;
  }
  return false;
}
