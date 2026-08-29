import assert from "node:assert/strict";
import { test } from "node:test";
import {
  assertRelationshipAllowed,
  filterRelationshipVisible,
  isRelationshipBlocked,
  normalizeBlockedUserIds,
  RelationshipBlockedError
} from "./relationship-policy.mjs";

test("normalizes relationship ids and enforces the same boundary in either block direction", () => {
  const firstViewer = {
    tenantId: "tenant-1",
    userId: "user-a",
    blockedUserIds: normalizeBlockedUserIds(["user-b", " user-c ", ""])
  };
  const secondViewer = {
    tenantId: "tenant-1",
    userId: "user-b",
    blockedUserIds: normalizeBlockedUserIds(["user-a"])
  };

  assert.equal(isRelationshipBlocked(firstViewer, "user-b"), true);
  assert.equal(isRelationshipBlocked(secondViewer, "user-a"), true);
  assert.equal(isRelationshipBlocked(firstViewer, "user-a"), false);
  assert.throws(
    () => assertRelationshipAllowed(firstViewer, "user-b"),
    (error) => error instanceof RelationshipBlockedError && error.code === "RELATIONSHIP_BLOCKED" && error.status === 404
  );
});

test("filters records when any linked actor is hidden by relationship policy", () => {
  const viewer = { userId: "user-a", blockedUserIds: new Set(["user-b"]) };
  const items = [
    { id: "visible", actors: ["user-c"] },
    { id: "hidden", actors: ["user-c", "user-b"] }
  ];

  assert.deepEqual(
    filterRelationshipVisible(items, viewer, (item) => item.actors).map((item) => item.id),
    ["visible"]
  );
});
