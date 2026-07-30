# Android Release Runbook

Owner: Android and Release Engineering
Last updated: 2026-07-30
Status: Required release procedure

This runbook covers the two supported Android distribution channels:

- **Play channel:** signed Android App Bundle distributed through Google Play.
- **Direct channel:** stable release-signed APK distributed from a Vyb-owned
  HTTPS endpoint for approved internal or pre-Play testing.

A debug APK is a disposable QA artifact. It must never become the installation
baseline for production users because debug signing keys are machine-scoped and
cannot provide a durable update chain.

## 1. Release ownership and prerequisites

Before preparing a release, verify:

- the Firebase and Google Cloud project is `vybnet`;
- the Android application ID is `social.vyb.app`;
- the release is built from the intended protected Git commit and clean worktree;
- the stable upload/release keystore is available only to its approved custodians;
- keystore, alias, and passwords are supplied through the approved secret store,
  never committed to Git or written into documentation;
- an encrypted keystore recovery copy and recovery procedure have been tested;
- Play App Signing SHA-1 and SHA-256 fingerprints are registered in Firebase;
- `apps/mobile/app/google-services.json` targets project ID `vybnet`;
- production API base URL is `https://api.vybnet.app/`;
- backend supports the current and previous Android releases.

If any signing or ownership check is uncertain, stop. Do not create a
replacement key or publish from a personal account.

## 2. Version contract

Every release must have:

- a product semantic version in the root `VERSION` file;
- the same Android `versionName`;
- a strictly increasing Android `versionCode`;
- matching releasable workspace package versions;
- a matching backend update-manifest default/configuration;
- a release note at `docs/releases/<version>.md`;
- an immutable Git tag `v<version>`.

The release note records the Git commit, tag, build timestamp, minimum and
target SDK, API endpoint, feature flags, test evidence, artifact URLs, SHA-256
digests, signing certificate SHA-256 digest, rollout plan, rollback plan, and
known issues.

## 3. Pre-release verification

Wait until other Android builds have stopped, then run from `apps/mobile`:

```bash
./gradlew --stop
./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

On Windows PowerShell use `gradlew.bat`:

```powershell
.\gradlew.bat --stop
.\gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

The release owner must also complete:

- authenticated email/password and Google sign-in smoke tests;
- onboarding, home, hub, vibes, market, messages, notifications, and profile
  navigation smoke tests;
- loading, empty, error, offline, retry, and signed-out state checks;
- media upload/read/delete checks against private R2;
- compact-phone and large-phone visual checks;
- notification permission, FCM tap, and supported deep-link checks;
- Firebase Installation ID registration timestamp and foreground/data-only
  notification checks; legacy token registrations are migration-only;
- accessibility labels, focus order, and 48 dp touch-target checks;
- update tests from the current supported release to the candidate;
- tenant-isolation and API compatibility checks.

No release proceeds with a failing CI gate or undocumented manual blocker.

## 4. Play channel

The Play artifact is a release-signed App Bundle:

```powershell
.\gradlew.bat :app:bundleRelease `
  -PvybReleaseStoreFile=C:\secure\vyb-upload.jks `
  -PvybReleaseStorePassword=REDACTED `
  -PvybReleaseKeyAlias=vyb-upload `
  -PvybReleaseKeyPassword=REDACTED
```

Before upload:

- confirm the bundle is release-signed with the expected upload certificate;
- confirm package, version name, version code, SDK levels, and production API;
- retain the bundle and verification output as CI/release artifacts;
- upload first to Play Internal Testing;
- install from Play and verify Firebase Auth, API, media, FCM, and update flow.

Play-distributed builds must use Google Play's supported in-app update path.
They must not depend on unknown-app-source permission or the direct APK
installer. The Play/direct build separation remains a release blocker until
the application implements and verifies it.

## 5. Direct channel

The direct artifact must be a **release** APK signed by the same stable
certificate as every previous direct production build. Never publish
`app-debug.apk`.

Before publication:

1. Verify the APK signature and record its signer certificate SHA-256 digest.
2. Verify package `social.vyb.app`, version name, and increasing version code.
3. Calculate and record the APK SHA-256 digest.
4. Install over the previous direct release without uninstalling it.
5. Complete launch, login, API, R2 media, notification, and deep-link smoke tests.
6. Upload the immutable APK to the Vyb-owned release location.
7. Verify the final HTTPS download and its digest.
8. Set `VYB_ANDROID_APK_URL` to the immutable Vyb-owned HTTPS URL and
   `VYB_ANDROID_APK_SHA256` to the verified 64-character digest.
9. Update the backend version/minimum-version values only after the artifact is
   reachable and the manifest endpoint reports `updateAvailable=true`.

The direct update manifest must never advertise a debug-signed, unsigned,
missing, mutable, non-HTTPS, or checksum-mismatched artifact.

The Android client independently allowlists `vybnet.app` and its subdomains,
rejects credentials/non-standard ports, hashes the complete downloaded file,
and deletes it on an empty or mismatched result. Server validation does not
replace client verification.

## 6. Update publication order

Use this order to avoid broken update prompts:

1. deploy backward-compatible backend changes;
2. verify current and previous apps against the backend;
3. publish the immutable signed artifact;
4. verify download URL, SHA-256, package, version, and signer;
5. publish optional update metadata to internal users;
6. observe installation and crash/auth/API health;
7. expand rollout gradually;
8. raise the minimum supported version only after the approved adoption gate.

A forced update requires explicit product and incident-owner approval. Login
and recovery paths must remain usable, and the advertised artifact must already
be verified from a clean device and as an upgrade.

## 7. Staged rollout

Recommended gates:

1. release engineering devices;
2. internal testers;
3. closed university pilot;
4. 5% production;
5. 25%;
6. 50%;
7. 100%.

At every gate check crash-free sessions, ANRs, login success, API error rate,
media failures, update completion, and support reports. Pause expansion when a
release gate fails; do not solve a client incident by silently changing the
published binary at the same URL.

## 8. Rollback

Android binaries already installed cannot be remotely replaced with a lower
version code. Rollback therefore means:

- stop the Play staged rollout or remove the direct update advertisement;
- keep backend contracts compatible with the previous supported app;
- disable the faulty feature with a server-owned feature flag when available;
- publish a fixed build with a higher version code;
- restore the last safe backend revision when the failure is server-side;
- preserve logs, release metadata, and artifact digests for incident review.

Do not raise `minimumSupportedVersionCode` during rollback. Do not delete a
published artifact until no update manifest or release record references it.

## 9. Post-release record

Attach to the GitHub release:

- signed AAB for the Play channel, when applicable;
- stable release-signed APK for the direct channel, when applicable;
- SHA-256 files;
- signing and package verification output;
- unit-test and lint reports;
- release note and manual QA sign-off.

After full rollout, record final health, rollout timestamps, known issues, and
the oldest supported version. Retain the prior signed release and rollback
evidence according to the project retention policy.
