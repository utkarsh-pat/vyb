import assert from "node:assert/strict";
import { test } from "node:test";
import { dashboard } from "./repository.mjs";

function event(id, hostUserId, registrations = []) {
  return {
    id,
    tenantId: "tenant-1",
    status: "published",
    host: { userId: hostUserId, username: hostUserId, displayName: hostUserId, role: "student" },
    title: id,
    category: "Campus",
    startsAt: "2026-09-01T10:00:00.000Z",
    responseMode: "register",
    registrationConfig: {},
    registrations,
    savedByUserIds: [],
    interestedUserIds: []
  };
}

test("events hide blocked hosts and blocked attendee registration details", () => {
  const result = dashboard({
    events: [
      event("hidden-event", "user-b"),
      event("hosted-event", "user-a", [
        { id: "hidden-registration", attendee: { userId: "user-b" }, status: "submitted", teamSize: 1 },
        { id: "visible-registration", attendee: { userId: "user-c" }, status: "approved", teamSize: 1 }
      ])
    ]
  }, {
    tenantId: "tenant-1",
    userId: "user-a",
    username: "a",
    blockedUserIds: new Set(["user-b"])
  });

  assert.deepEqual(result.events.map((item) => item.id), ["hosted-event"]);
  assert.equal(result.hostedEvents[0].registrations.length, 1);
  assert.equal(result.hostedEvents[0].registrationSummary.total, 1);
  assert.equal(result.hostedEvents[0].spotsLeft, null);
});
