const SHA256_PATTERN = /^[a-f0-9]{64}$/u;

export function isTrustedAndroidUpdateUrl(value) {
  try {
    const url = new URL(String(value ?? "").trim());
    const hostname = url.hostname.toLowerCase();
    return (
      url.protocol === "https:" &&
      !url.username &&
      !url.password &&
      (url.port === "" || url.port === "443") &&
      (hostname === "vybnet.app" || hostname.endsWith(".vybnet.app"))
    );
  } catch {
    return false;
  }
}

export function buildAndroidUpdateManifest({
  env = process.env,
  currentVersionCode = 0
} = {}) {
  const latestVersionCode = Number(env.VYB_ANDROID_LATEST_VERSION_CODE ?? 5);
  const latestVersionName = String(env.VYB_ANDROID_LATEST_VERSION_NAME ?? "0.1.4").trim();
  const minimumSupportedVersionCode = Number(env.VYB_ANDROID_MIN_SUPPORTED_VERSION_CODE ?? 1);
  const apkUrl = String(env.VYB_ANDROID_APK_URL ?? "").trim();
  const apkSha256 = String(env.VYB_ANDROID_APK_SHA256 ?? "").trim().toLowerCase();
  const distributionReady =
    isTrustedAndroidUpdateUrl(apkUrl) &&
    SHA256_PATTERN.test(apkSha256);
  const hasNewerVersion =
    Number.isInteger(currentVersionCode) &&
    currentVersionCode > 0 &&
    Number.isInteger(latestVersionCode) &&
    latestVersionCode > currentVersionCode;

  return {
    platform: "android",
    latestVersionCode,
    latestVersionName,
    minimumSupportedVersionCode,
    forceUpdate: distributionReady && env.VYB_ANDROID_FORCE_UPDATE === "1",
    apkUrl,
    apkSha256,
    releaseNotes: String(
      env.VYB_ANDROID_RELEASE_NOTES ??
        "Custom APK updates, theme toggle, app logo, and smoother publishing."
    )
      .split("|")
      .map((item) => item.trim())
      .filter(Boolean),
    updateAvailable: distributionReady && hasNewerVersion,
    updatedAt: env.VYB_ANDROID_UPDATED_AT ?? "2026-07-30T00:00:00.000Z"
  };
}
