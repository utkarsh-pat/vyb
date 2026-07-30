# Vyb Release Candidate QA — 2026-07-30

## Release identity

- Branch: `codex/android-web-parity`
- Draft pull request: [#3](https://github.com/utkarsh-pat/vyb/pull/3)
- Base RC commit: `5da6ecae08494050ebcb1e1b9016f1fe95423e36`
- No-traffic deployment guardrail commit: `725e30762884f9fbca53772dfa9bdb9973c006df`
- Legacy R2 path compatibility commit: `6a89187`
- Android version: `0.1.4` (`versionCode` 5)
- APK: `artifacts/Vyb-0.1.4-debug.apk`
- APK SHA-256: `D7382F44140F63EE3122BFAD413E13E37AE0793BE2493D6B8C139779ED79639D`

No password, ID token, API key, service-account credential, or R2 credential is stored in this report or in the release artifacts.

## Automated verification

| Check | Result |
|---|---|
| Backend tests | Pass, 23/23 |
| Web TypeScript | Pass |
| Android unit tests | Pass |
| Android lint | Pass |
| Android debug APK assembly | Pass |
| Repository secret scan | Pass |
| GitHub Backend CI | Pass |
| GitHub Android CI | Pass |
| Vercel preview checks | Pass, including the final media-fix preview |

The Android GitHub workflow initially reported a missing
`GOOGLE_SERVICES_JSON_BASE64` repository secret. The secret was populated from
the ignored, locally verified Firebase configuration. No Firebase
configuration or secret was committed.

## Staged backend deployment

- Cloud Build: `629aa766-c1b0-4785-b7be-3c9c3b6684db`
- Container digest:
  `sha256:85a9d7d943cdd5be81e86f4af80eac121382044d0b676d25289a4bd388b66e8b`
- No-traffic revision: `vyb-backend-00011-bsb`
- RC tag: `rc-014`
- Live revision retained at 100% traffic: `vyb-backend-00018-wep`

The RC revision passed:

- `GET /health`: `200`, all 11 modules healthy
- `GET /ready`: `200`
- `GET /v1/client-shell`: `200`
- `GET /v1/app-updates/android`: `200`
- Production-origin CORS check
- Authenticated session bootstrap for two distinct users
- Authenticated feed, vibes, market, chats, and notifications reads for both users
- Cloud Run error-log scan with no RC revision errors

Production backend traffic was not shifted.

## R2 migration verification

The old Firebase Storage source was temporarily restored inside its recovery
window for read-only migration verification and immediately returned to
`DELETE_REQUESTED`.

| Metric | Result |
|---|---:|
| Source objects | 160 |
| Source bytes | 448,514,602 |
| R2 objects before verification | 160 |
| Objects copied | 0 |
| Objects already matching | 160 |
| Verified objects | 160 |
| Verification failures | 0 |

The migration was already complete, so no duplicate transfer was performed.
R2 object metadata and source generation/checksum metadata were used as the
authoritative verification signal.

### Legacy path defect found during device QA

Android device QA exposed one legacy profile post as `Media unavailable`.
Application logs identified URLs shaped like:

```text
/api/media/firebase-migration/social/<tenant>/posts/<placement>/<user>/<asset>
```

The verified R2 key is the same path without the historical
`firebase-migration/` prefix. The web media proxy now resolves this known
legacy prefix to the canonical R2 key and retains the original key as a safe
fallback. This avoids database rewrites, duplicate objects, and additional R2
storage cost. The protected Vercel preview returned the legacy-prefixed test
asset as a complete `image/webp` response with its expected 384×384 intrinsic
dimensions.

## Two-profile web QA

Two independent Chrome profiles were used, one per test account.

| Surface | Account A | Account B | Result |
|---|---|---|---|
| Authentication | Correct Utkarsh identity | Correct Ashwani identity | Pass |
| Home | 8 articles; media rendered | 8 articles; media rendered | Pass |
| Hub | Connect, Scribble, N-Queens rendered | Connect, Scribble, N-Queens rendered | Pass |
| Vibes | 13 articles; media rendered | 13 articles; media rendered | Pass |
| Market | Loaded without alert | Loaded without alert | Pass |
| Messages | Community-first empty state | Community-first empty state | Pass |
| Dashboard | Correct profile and media | Correct profile and media | Pass |

No framework error overlay, application alert, or `Media unavailable` state was
observed in the final web pass.

## Two-emulator Android QA

Two Pixel 7 API 35 emulator instances ran concurrently on separate ADB serials
with isolated read-only runtime overlays. The same release-candidate APK was
installed on both, and both accounts completed a clean email/password login.
Notification permission was granted through the Android permission dialog.

| Surface | Account A | Account B | Result |
|---|---|---|---|
| Authentication | `UTKARSH PATEL` | `ASHWANI BAGHEL` | Pass |
| Home | Correct greeting and campus feed | Correct greeting and campus feed | Pass |
| Hub | Campus tabs and empty event state | Campus tabs and empty event state | Pass |
| Vibes | Native vibe rendered with actions | Native vibe rendered with actions | Pass |
| Market | Correct account identity and empty state | Correct account identity and empty state | Pass |
| Profile | Correct profile and stats; one legacy media 404 found | Correct profile and stats | Fix verified in preview; production retest required |
| Chats | Stable empty state | Stable empty state | Pass |
| Notifications | 0-unread empty state | 0-unread empty state | Pass |
| Settings | Correct account email and security controls | Correct account email and security controls | Pass |

Evidence screenshots are stored locally under `artifacts/qa/android/` and are
intentionally ignored by Git.

No `FATAL EXCEPTION`, application ANR, or Vyb process crash was observed.
Hardware-emulator color-format warnings were non-fatal. A cold-boot Android
system `Messages` process ANR appeared once before app QA, was dismissed, and
did not involve the Vyb package.

## Release decision

The RC is suitable for continued staged rollout. Keep the backend revision at
no traffic until an explicit production rollout decision. Recommended rollout
order:

1. Promote the reviewed web build.
2. Repeat the two-account profile/media smoke against production.
3. Promote the Cloud Run RC revision with a small canary only after explicit approval.
4. Publish the versioned APK artifact or Play internal-test build.
5. Monitor authentication failures, media `404`s, backend `5xx`s, and crash-free users.
