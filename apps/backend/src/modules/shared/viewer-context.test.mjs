import assert from "node:assert/strict";
import test from "node:test";
import { canUseDemoViewerFallback } from "./viewer-context.mjs";

test("production never exposes demo membership data", () => {
  assert.equal(
    canUseDemoViewerFallback({ isTrustedInternalRequest: true }, "production"),
    false
  );
});

test("Firebase and anonymous requests never receive demo membership data", () => {
  assert.equal(
    canUseDemoViewerFallback({ isTrustedInternalRequest: false }, "development"),
    false
  );
  assert.equal(canUseDemoViewerFallback({}, "development"), false);
});

test("trusted internal development sessions may use demo fixtures", () => {
  assert.equal(
    canUseDemoViewerFallback({ isTrustedInternalRequest: true }, "development"),
    true
  );
});
