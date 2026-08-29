function hasValue(value) {
  return typeof value === "string" && value.trim().length > 0;
}

function hasAll(env, names) {
  return names.every((name) => hasValue(env[name]));
}

export function evaluateRuntimeReadiness(env = process.env) {
  const production = env.NODE_ENV === "production";
  const checks = {
    firebaseProject: hasValue(env.FIREBASE_PROJECT_ID) || hasValue(env.GOOGLE_CLOUD_PROJECT),
    dataConnectSdk: true,
    r2Media: hasAll(env, [
      "R2_ACCOUNT_ID",
      "R2_ACCESS_KEY_ID",
      "R2_SECRET_ACCESS_KEY",
      "R2_BUCKET",
      "R2_PUBLIC_BASE_URL"
    ]),
    signedGames: hasValue(env.VYB_GAMES_SESSION_SECRET) || hasValue(env.VYB_SESSION_SECRET),
    socialRealtimeFanout: hasAll(env, [
      "VYB_SOCIAL_REALTIME_PUBLIC_ORIGIN",
      "VYB_SOCIAL_REALTIME_SECRET"
    ]),
    analyticsMeasurement:
      hasValue(env.VYB_ANALYTICS_VIEWER_KEY_SECRET) && env.VYB_ANALYTICS_VIEWER_KEY_SECRET.trim().length >= 24,
    internalJobs:
      hasValue(env.VYB_INTERNAL_API_KEY) &&
      env.VYB_INTERNAL_API_KEY.trim() !== "local-vyb-internal-key" &&
      env.VYB_INTERNAL_API_KEY.trim().length >= 24
  };

  const missingRequired = [];
  if (!checks.firebaseProject) missingRequired.push("firebase-project");
  if (production && !checks.analyticsMeasurement) missingRequired.push("analytics-viewer-key");
  if (production && !checks.internalJobs) missingRequired.push("internal-jobs-key");

  const degradedFeatures = [];
  if (!checks.r2Media) degradedFeatures.push("r2-media");
  if (!checks.signedGames) degradedFeatures.push("signed-games");
  if (!checks.socialRealtimeFanout) degradedFeatures.push("social-realtime-fanout");

  return {
    ready: missingRequired.length === 0,
    checks,
    missingRequired,
    degradedFeatures
  };
}
