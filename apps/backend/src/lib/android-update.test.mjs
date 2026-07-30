import assert from "node:assert/strict";
import test from "node:test";
import {
  buildAndroidUpdateManifest,
  isTrustedAndroidUpdateUrl
} from "./android-update.mjs";

const sha256 = "a".repeat(64);

test("update becomes available only with a newer version and verified Vyb artifact metadata", () => {
  const manifest = buildAndroidUpdateManifest({
    currentVersionCode: 5,
    env: {
      VYB_ANDROID_LATEST_VERSION_CODE: "6",
      VYB_ANDROID_LATEST_VERSION_NAME: "0.1.5",
      VYB_ANDROID_APK_URL: "https://downloads.vybnet.app/Vyb-0.1.5.apk",
      VYB_ANDROID_APK_SHA256: sha256.toUpperCase(),
      VYB_ANDROID_FORCE_UPDATE: "1",
      VYB_ANDROID_RELEASE_NOTES: "First | Second | "
    }
  });

  assert.equal(manifest.updateAvailable, true);
  assert.equal(manifest.forceUpdate, true);
  assert.equal(manifest.apkSha256, sha256);
  assert.deepEqual(manifest.releaseNotes, ["First", "Second"]);
});

test("missing checksum or an external host disables update and force-update", () => {
  for (const [apkUrl, checksum] of [
    ["https://downloads.vybnet.app/Vyb.apk", ""],
    ["https://evil.example/Vyb.apk", sha256],
    ["http://vybnet.app/Vyb.apk", sha256]
  ]) {
    const manifest = buildAndroidUpdateManifest({
      currentVersionCode: 1,
      env: {
        VYB_ANDROID_LATEST_VERSION_CODE: "2",
        VYB_ANDROID_APK_URL: apkUrl,
        VYB_ANDROID_APK_SHA256: checksum,
        VYB_ANDROID_FORCE_UPDATE: "1"
      }
    });
    assert.equal(manifest.updateAvailable, false);
    assert.equal(manifest.forceUpdate, false);
  }
});

test("trusted update URL rejects credentials and non-standard ports", () => {
  assert.equal(isTrustedAndroidUpdateUrl("https://vybnet.app/Vyb.apk"), true);
  assert.equal(isTrustedAndroidUpdateUrl("https://cdn.vybnet.app/Vyb.apk"), true);
  assert.equal(isTrustedAndroidUpdateUrl("https://user@vybnet.app/Vyb.apk"), false);
  assert.equal(isTrustedAndroidUpdateUrl("https://vybnet.app:8443/Vyb.apk"), false);
});
