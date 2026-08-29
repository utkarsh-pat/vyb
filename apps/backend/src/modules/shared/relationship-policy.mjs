import { getRelationshipBlockedUserIds } from "../social/block-policy-repository.mjs";

export class RelationshipBlockedError extends Error {
  constructor(message = "This action is unavailable because one of these accounts has blocked the other.") {
    super(message);
    this.name = "RelationshipBlockedError";
    this.code = "RELATIONSHIP_BLOCKED";
    this.status = 404;
  }
}

function normalizeUserId(value) {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

export function normalizeBlockedUserIds(value) {
  const values = value instanceof Set ? [...value] : Array.isArray(value) ? value : [];
  return new Set(values.map(normalizeUserId).filter(Boolean));
}

export async function hydrateViewerRelationshipPolicy(viewer) {
  if (!viewer?.tenantId || !viewer?.userId) {
    return {
      ...viewer,
      blockedUserIds: new Set()
    };
  }

  return {
    ...viewer,
    blockedUserIds: await getRelationshipBlockedUserIds({
      tenantId: viewer.tenantId,
      userId: viewer.userId
    })
  };
}

export function isRelationshipBlocked(viewer, otherUserId) {
  const normalizedOtherUserId = normalizeUserId(otherUserId);
  if (!normalizedOtherUserId || normalizedOtherUserId === viewer?.userId) {
    return false;
  }

  return normalizeBlockedUserIds(viewer?.blockedUserIds).has(normalizedOtherUserId);
}

export function assertRelationshipAllowed(viewer, otherUserId, message) {
  if (isRelationshipBlocked(viewer, otherUserId)) {
    throw new RelationshipBlockedError(message);
  }
}

export function filterRelationshipVisible(items, viewer, getUserIds) {
  if (!Array.isArray(items)) return [];
  return items.filter((item) => {
    const resolved = getUserIds(item);
    const userIds = Array.isArray(resolved) ? resolved : [resolved];
    return userIds.filter(Boolean).every((userId) => !isRelationshipBlocked(viewer, userId));
  });
}
