# Vyb Marketplace MVP — System Low-Level Design

Status: implementation contract
Last updated: 2026-07-30

## 1. Request lifecycle

1. Client obtains a Firebase ID token.
2. Cloud Run verifies issuer, audience, signature, expiry, and revocation policy.
3. Request context resolves `{firebaseUid, userId, tenantId, membershipId, roles}` from Data Connect.
4. Route validates body, query, pagination cursor, authorization, and idempotency key.
5. Domain service executes a generated connector operation.
6. Transaction writes the domain row and outbox event together.
7. Worker delivers side effects through Cloud Tasks and records success or retry state.
8. Response and structured logs share a request ID; tokens, credentials, and private contact values are redacted.

## 2. Module ownership

| Module | Connector | Owns |
|---|---|---|
| Identity | `identity` | user identity and profile |
| Campus | `campus` | tenant, verified domain, membership, community |
| Social | `social` | text/image posts, comments, reactions; Stories schema gated |
| Marketplace | `marketplace` | listings, requests, saves, contact intent, sold state, reports, media metadata |
| Chat | `chat` | conversation, participant, message, delivery state |
| Resources | `resources` | notes/resources and moderation state |
| Moderation | `moderation` | report, action, reason, evidence, audit trail |
| Connect | `connect` | optional game/session state; server-only connector |

Cross-module writes go through service functions or outbox events. Clients never call Admin SDK operations.

## 3. Marketplace transaction rules

- A listing belongs to exactly one tenant and seller membership.
- Browse/search always filters tenant, active status, moderation state, and cursor.
- Create/update/delete/sold mutations require seller ownership or an authorized moderator.
- Save is unique on `(listingId, membershipId)`.
- Contact intent is idempotent and creates or resolves a Chat-owned conversation.
- Private contact data is returned only after an authorized contact action and is never indexed or logged.
- Money movement is not implemented in MVP; users receive exchange and safety guidance.
- Every report creates an immutable audit event.

## 4. Media contract

Object key:

```text
{module}/{tenantId}/{assetType}/{placement}/{userId}/{uuid}.{extension}
```

SQL metadata:

```text
storagePath, publicUrl, mimeType, sizeBytes, width, height, durationMs,
ownerUserId, tenantId, checksum, status, createdAt
```

Rules:

- Upload intent expires in 10 minutes and is bound to owner, tenant, MIME, size, and object key.
- Images are limited to 4 MB after client compression.
- Video upload is disabled at launch.
- Orphan cleanup deletes unreferenced objects after a 24-hour grace period.
- Versioned/content-addressed objects use immutable cache headers.

## 5. Database rules

- UUID primary keys; explicit tenant foreign keys on tenant-owned rows.
- Composite indexes start with `tenantId`; hot lists include status and creation/order columns.
- Cursor pagination only; default page 20, hard maximum 50.
- No unbounded scans, offset pagination, or wildcard client filters.
- Soft delete user-generated content; hard delete only through retention jobs.
- Idempotency required for create listing/post/message/contact and all retryable writes.
- `pnpm dc:compile` must pass before migration or connector deployment.
- Production never falls back to JSON, Firestore, Supabase, or another SQL writer.

## 6. Runtime configuration

Cloud Run:

```text
FIREBASE_PROJECT_ID=vybnet
FIREBASE_DATACONNECT_SERVICE_ID=vyb
FIREBASE_DATACONNECT_LOCATION=asia-south1
VYB_CORS_ALLOWED_ORIGINS=https://vybnet.app,https://www.vybnet.app
R2_ACCOUNT_ID
R2_ACCESS_KEY_ID
R2_SECRET_ACCESS_KEY
R2_BUCKET
R2_PUBLIC_BASE_URL
VYB_INTERNAL_API_KEY
VYB_SESSION_SECRET
VYB_SUPER_ADMIN_EMAILS=ceoutkarshpatel@gmail.com
VYB_ANDROID_APK_URL
VYB_ANDROID_APK_SHA256
```

Web public Firebase values are safe client configuration, but all private keys
and Admin credentials remain server-side. Cloud Run uses Application Default
Credentials. `R2_ACCESS_KEY_ID` and `R2_SECRET_ACCESS_KEY` are Secret Manager
references in Cloud Run and sensitive variables in Vercel; they are never
committed or exposed to browser code.

The R2 bucket is private. `R2_PUBLIC_BASE_URL` is the canonical same-origin
`https://www.vybnet.app/api/media` route. That route reads R2 server-side and
returns immutable cached responses, so production does not depend on the
rate-limited public `r2.dev` endpoint.

Android push registration uses the Firebase Installation ID callback from FCM.
The client stores the latest FID as device-scoped state, uploads it after an
authenticated session exists, and refreshes the server timestamp whenever FCM
re-registers the installation. The notification outbox targets `fid`; legacy
`token` targets remain read-only migration compatibility until existing
installations have refreshed.

The Android direct-update manifest is valid only when the advertised version is
newer and both `VYB_ANDROID_APK_URL` and `VYB_ANDROID_APK_SHA256` pass server
validation. Android repeats the trusted-host and checksum validation, downloads
to app-scoped storage, deletes an empty or mismatched APK, and invokes the
installer only after verification.

## 7. Deployment contract

- Data Connect schema first, backward-compatible backend second, frontend/Android last.
- New Cloud Run revision receives no traffic until liveness, readiness, auth,
  tenant-isolation, Marketplace, chat, media, notification, and update-manifest
  smoke tests pass.
- Traffic sequence: 5%, 25%, 50%, 100%.
- Domain cutover occurs only after direct deployment URLs are healthy.
- Rollback routes traffic to the previous revision and disables risky features through Remote Config.

## 8. Alerts

- Error rate above 3% for 5 minutes.
- Read p95 above 500 ms or write p95 above 800 ms for 10 minutes.
- Database connections or CPU above 70%.
- Cloud Run max-instance saturation.
- Failed outbox/task backlog age above 5 minutes.
- Monthly budget thresholds at 50%, 80%, and 100%.
