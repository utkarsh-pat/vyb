import assert from "node:assert/strict";
import test from "node:test";
import { parseBackendErrorBody } from "./backend-error.ts";

test("parses structured backend errors", () => {
  assert.deepEqual(
    parseBackendErrorBody(
      JSON.stringify({
        error: {
          code: "PROFILE_INCOMPLETE",
          message: "Complete your profile.",
          details: { field: "username" }
        }
      }),
      "fallback"
    ),
    {
      code: "PROFILE_INCOMPLETE",
      message: "Complete your profile.",
      details: { field: "username" }
    }
  );
});

test("preserves plain-text upstream errors instead of losing the consumed body", () => {
  assert.deepEqual(
    parseBackendErrorBody("Gateway timed out while saving.", "fallback"),
    {
      code: "BACKEND_REQUEST_FAILED",
      message: "Gateway timed out while saving.",
      details: null
    }
  );
});
