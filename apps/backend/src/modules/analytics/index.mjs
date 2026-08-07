import { sendError, sendJson } from "../../lib/http.mjs";
import {
  processContentPurgeRequests,
  purgeExpiredContentMeasurement,
  rollupContentEvents
} from "../social/analytics-repository.mjs";

export function getAnalyticsModuleHealth() {
  return { module: "analytics", status: "ok", rollupIntervalMinutes: 15 };
}

export async function handleAnalyticsRoute({ request, response, url, context }) {
  if (url.pathname !== "/v1/internal/analytics/rollup") return false;
  if (request.method !== "POST") {
    sendError(response, 405, "METHOD_NOT_ALLOWED", "Use POST for analytics maintenance.", null, { allow: "POST" });
    return true;
  }
  if (!context.isTrustedInternalRequest) {
    sendError(response, 401, "UNAUTHENTICATED", "A trusted internal service credential is required.");
    return true;
  }

  const deletionPurge = await processContentPurgeRequests({ limit: 200 });
  const rollup = await rollupContentEvents({ limit: 2000 });
  const retention = await purgeExpiredContentMeasurement({ limit: 2000 });
  sendJson(response, 200, { deletionPurge, rollup, retention, completedAt: new Date().toISOString() }, { "cache-control": "no-store" });
  return true;
}
