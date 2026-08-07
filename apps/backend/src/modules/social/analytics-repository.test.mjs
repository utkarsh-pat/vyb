import test from "node:test";
import assert from "node:assert/strict";
import {
  aggregateContentEvents,
  aggregateInsightRows,
  contentEventCooldownMs,
  deriveViewerKey,
  utcDate
} from "./analytics-repository.mjs";

test("qualified views remain non-unique while reach is unique", () => {
  const result = aggregateContentEvents([
    { eventType: "qualified_view", viewerKey: "u1" },
    { eventType: "qualified_view", viewerKey: "u1" },
    { eventType: "qualified_view", viewerKey: "u2" },
    { eventType: "impression", viewerKey: "u2" }
  ]);
  assert.equal(result.totals.qualifiedViewCount, 3);
  assert.equal(result.totals.uniqueReachCount, 2);
});

test("video watch and engagement totals roll up independently", () => {
  const result = aggregateContentEvents([
    { eventType: "video_play", viewerKey: "u1" },
    { eventType: "video_view", viewerKey: "u1", watchMs: 3000 },
    { eventType: "video_complete", viewerKey: "u1", watchMs: 7000 },
    { eventType: "video_replay", viewerKey: "u1" },
    { eventType: "carousel_slide", viewerKey: "u1" }
  ]).totals;
  assert.equal(result.watchMsTotal, 10000);
  assert.equal(result.videoViewCount, 1);
  assert.equal(result.completionCount, 1);
  assert.equal(result.replayCount, 1);
  assert.equal(result.carouselSlideCount, 1);
});

test("insight row aggregation handles Int64 strings", () => {
  const result = aggregateInsightRows([
    { qualifiedViewCount: 2, watchMsTotal: "3000" },
    { qualifiedViewCount: 3, watchMsTotal: "7000" }
  ]);
  assert.equal(result.qualifiedViewCount, 5);
  assert.equal(result.watchMsTotal, 10000);
});

test("UTC date keys do not depend on server timezone", () => {
  assert.equal(utcDate("2026-08-01T23:59:59.999Z"), "2026-08-01");
});

test("viewer identifiers are pseudonymized deterministically", () => {
  const key = deriveViewerKey("user-123", "tenant-1");
  assert.equal(key, deriveViewerKey("user-123", "tenant-1"));
  assert.notEqual(key, "user-123");
  assert.notEqual(key, deriveViewerKey("user-456", "tenant-1"));
  assert.notEqual(key, deriveViewerKey("user-123", "tenant-2"));
});

test("server cooldowns cap ranking and high-volume playback event inflation", () => {
  assert.equal(contentEventCooldownMs("qualified_view"), 30 * 60_000);
  assert.equal(contentEventCooldownMs("video_view"), 5 * 60_000);
  assert.equal(contentEventCooldownMs("video_replay"), 60_000);
  assert.equal(contentEventCooldownMs("carousel_slide"), 5_000);
});
