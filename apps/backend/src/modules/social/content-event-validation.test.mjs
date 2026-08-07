import test from "node:test";
import assert from "node:assert/strict";
import { normalizeContentEvent } from "./index.mjs";

function valid(overrides = {}) {
  return {
    eventKey: "11111111-1111-4111-8111-111111111111",
    postId: "22222222-2222-4222-8222-222222222222",
    sessionKey: "0123456789abcdef",
    eventType: "qualified_view",
    source: "android",
    visibleMs: 1000,
    occurredAt: new Date().toISOString(),
    ...overrides
  };
}

test("accepts a threshold-qualified event", () => assert.ok(normalizeContentEvent(valid())));
test("rejects client-only view inflation below threshold", () => assert.equal(normalizeContentEvent(valid({ visibleMs: 999 })), null));
test("rejects stale replay batches", () => assert.equal(normalizeContentEvent(valid({ occurredAt: new Date(Date.now() - 86_500_000).toISOString() })), null));
test("requires three seconds or 30 percent for a video view", () => {
  assert.equal(normalizeContentEvent(valid({ eventType: "video_view", visibleMs: 0, watchMs: 2999, progressBasisPoints: 2999 })), null);
  assert.ok(normalizeContentEvent(valid({ eventType: "video_view", visibleMs: 0, watchMs: 3000 })));
});
