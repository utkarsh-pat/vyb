import assert from "node:assert/strict";
import test from "node:test";
import { evaluateRuntimeReadiness } from "./readiness.mjs";

test("readiness accepts Cloud Run project identity without long-lived credentials", () => {
  const result = evaluateRuntimeReadiness({
    GOOGLE_CLOUD_PROJECT: "vybnet",
    R2_ACCOUNT_ID: "account",
    R2_ACCESS_KEY_ID: "key",
    R2_SECRET_ACCESS_KEY: "secret",
    R2_BUCKET: "bucket",
    R2_PUBLIC_BASE_URL: "https://media.example.test",
    VYB_GAMES_SESSION_SECRET: "session-secret",
    VYB_SOCIAL_REALTIME_PUBLIC_ORIGIN: "https://realtime.example.test",
    VYB_SOCIAL_REALTIME_SECRET: "social-realtime-secret-for-tests"
  });

  assert.equal(result.ready, true);
  assert.deepEqual(result.missingRequired, []);
  assert.deepEqual(result.degradedFeatures, []);
});

test("readiness fails only for core project configuration and reports optional degradation", () => {
  const result = evaluateRuntimeReadiness({});

  assert.equal(result.ready, false);
  assert.deepEqual(result.missingRequired, ["firebase-project"]);
  assert.deepEqual(result.degradedFeatures, ["r2-media", "signed-games", "social-realtime-fanout"]);
  assert.equal(result.checks.r2Media, false);
});

test("partial R2 credentials never report media ready", () => {
  const result = evaluateRuntimeReadiness({
    FIREBASE_PROJECT_ID: "vybnet",
    R2_ACCOUNT_ID: "account",
    R2_BUCKET: "bucket"
  });

  assert.equal(result.ready, true);
  assert.equal(result.checks.r2Media, false);
  assert.deepEqual(result.degradedFeatures, ["r2-media", "signed-games", "social-realtime-fanout"]);
});

test("production fails closed without analytics and internal-job secrets", () => {
  const result = evaluateRuntimeReadiness({ NODE_ENV: "production", FIREBASE_PROJECT_ID: "vybnet" });
  assert.equal(result.ready, false);
  assert.deepEqual(result.missingRequired, ["analytics-viewer-key", "internal-jobs-key"]);
});

test("production rejects short internal-job secrets", () => {
  const result = evaluateRuntimeReadiness({
    NODE_ENV: "production",
    FIREBASE_PROJECT_ID: "vybnet",
    VYB_ANALYTICS_VIEWER_KEY_SECRET: "analytics-key-longer-than-24-characters",
    VYB_INTERNAL_API_KEY: "too-short"
  });
  assert.equal(result.ready, false);
  assert.deepEqual(result.missingRequired, ["internal-jobs-key"]);
});
