function hasValue(value) {
  return typeof value === "string" && value.trim().length > 0;
}

function hasAll(env, names) {
  return names.every((name) => hasValue(env[name]));
}

export function evaluateRuntimeReadiness(env = process.env) {
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
    signedGames: hasValue(env.VYB_GAMES_SESSION_SECRET) || hasValue(env.VYB_SESSION_SECRET)
  };

  const missingRequired = [];
  if (!checks.firebaseProject) missingRequired.push("firebase-project");

  const degradedFeatures = [];
  if (!checks.r2Media) degradedFeatures.push("r2-media");
  if (!checks.signedGames) degradedFeatures.push("signed-games");

  return {
    ready: missingRequired.length === 0,
    checks,
    missingRequired,
    degradedFeatures
  };
}
