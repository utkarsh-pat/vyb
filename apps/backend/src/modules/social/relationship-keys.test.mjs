import test from "node:test";
import assert from "node:assert/strict";

import { buildFollowKey, buildUserBlockKey } from "./repository.mjs";

test("follow keys are isolated by tenant", () => {
  const first = buildFollowKey("campus-a", "viewer", "peer");
  const second = buildFollowKey("campus-b", "viewer", "peer");

  assert.notEqual(first, second);
  assert.equal(first, "campus-a:viewer:peer");
});

test("block keys are isolated by tenant and direction", () => {
  const outgoing = buildUserBlockKey("campus-a", "viewer", "peer");
  const incoming = buildUserBlockKey("campus-a", "peer", "viewer");
  const otherCampus = buildUserBlockKey("campus-b", "viewer", "peer");

  assert.notEqual(outgoing, incoming);
  assert.notEqual(outgoing, otherCampus);
  assert.equal(outgoing, "campus-a:viewer:peer");
});
