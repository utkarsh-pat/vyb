import { readJson, sendError, sendJson } from "../../lib/http.mjs";
import { hydrateViewerRelationshipPolicy, isRelationshipBlocked } from "./relationship-policy.mjs";

const MAX_CANDIDATES = 200;

function normalizeId(value) {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

export async function handleRelationshipPolicyRoute({ request, response, url, context }) {
  if (request.method !== "POST" || url.pathname !== "/v1/internal/relationships/visibility") {
    return false;
  }
  if (!context.isTrustedInternalRequest) {
    sendError(response, 404, "ROUTE_NOT_FOUND", "Unknown route.");
    return true;
  }

  const payload = await readJson(request);
  const tenantId = normalizeId(payload?.tenantId);
  const viewerUserId = normalizeId(payload?.viewerUserId);
  const candidateUserIds = Array.isArray(payload?.candidateUserIds)
    ? [...new Set(payload.candidateUserIds.map(normalizeId).filter(Boolean))]
    : [];
  const includeBlockedUserIds = payload?.includeBlockedUserIds === true;
  if (!tenantId || !viewerUserId || candidateUserIds.length > MAX_CANDIDATES) {
    sendError(response, 400, "INVALID_RELATIONSHIP_POLICY_REQUEST", "A tenant, viewer and up to 200 candidate users are required.");
    return true;
  }

  const viewer = await hydrateViewerRelationshipPolicy({ tenantId, userId: viewerUserId });
  sendJson(response, 200, {
    visibleUserIds: candidateUserIds.filter((userId) => !isRelationshipBlocked(viewer, userId)),
    ...(includeBlockedUserIds ? { blockedUserIds: [...viewer.blockedUserIds] } : {})
  }, { "cache-control": "private, no-store" });
  return true;
}
