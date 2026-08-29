import assert from "node:assert/strict";
import test from "node:test";
import { buildFeedChangeSummary, isPublicPostRealtimeEligible, isValidSocialUploadMimeType } from "./index.mjs";

test("feed change summary advances without leaking changed entity ids", () => {
  const summary = buildFeedChangeSummary({
    items: [{ entityType: "post", entityId: "private-post", eventType: "post.updated" }],
    highWater: "cursor-new",
    nextCursor: "cursor-page",
    resetRequired: false
  }, "cursor-old");
  assert.deepEqual(summary.items, []);
  assert.equal(summary.nextCursor, null);
  assert.equal(summary.highWater, "cursor-new");
  assert.equal(summary.hasChanges, true);
});

test("avatar uploads accept images and reject videos", () => {
  assert.equal(isValidSocialUploadMimeType("avatar", "image/jpeg"), true);
  assert.equal(isValidSocialUploadMimeType("avatar", " IMAGE/PNG "), true);
  assert.equal(isValidSocialUploadMimeType("avatar", "video/mp4"), false);
});

test("post, story, and vibe uploads retain their supported media flexibility", () => {
  assert.equal(isValidSocialUploadMimeType("post", "video/mp4"), true);
  assert.equal(isValidSocialUploadMimeType("story", "video/webm"), true);
  assert.equal(isValidSocialUploadMimeType("vibe", "video/mp4"), true);
});

test("all upload intents reject missing MIME types", () => {
  assert.equal(isValidSocialUploadMimeType("post", ""), false);
  assert.equal(isValidSocialUploadMimeType("avatar", null), false);
});

test("tenant-wide realtime broadcasts never expose restricted posts", () => {
  assert.equal(isPublicPostRealtimeEligible({ visibility: "public" }), true);
  assert.equal(isPublicPostRealtimeEligible({ visibility: "followers" }), false);
  assert.equal(isPublicPostRealtimeEligible({ visibility: "community" }), false);
  assert.equal(isPublicPostRealtimeEligible(null), false);
});
