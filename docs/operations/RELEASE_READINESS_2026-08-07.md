# Release Readiness — 2026-08-07

## What is deployed

- Google Cloud project: `vybnet`; backend region: `asia-south1`.
- Cloud Run service: `vyb-backend`, revision `vyb-backend-00018-bvd`, 100% of
  traffic. Minimum instances remain zero, concurrency remains 80, and CPU
  throttling remains enabled to preserve pilot cost efficiency.
- Firebase Data Connect: Social schema and connector include the M0 measurement
  tables, block records, recommendation-feedback persistence, and durable
  tenant-scoped feed-change records.
- Cloud Scheduler: `vyb-analytics-rollup`, `*/15 * * * *` in `Asia/Kolkata`,
  invokes the protected internal rollup. The deployment-day manual trigger
  returned HTTP 200.
- Secrets: Cloud Run reads the current internal-job and analytics-viewer keys
  from Secret Manager. Earlier internal-job key versions and an obsolete tagged
  Cloud Run QA route were revoked/removed on this date.

## Product state included in this change set

- Author-only content insights and content-measurement preferences are exposed
  through backend, PWA, and Android client contracts.
- PWA and Android send bounded, idempotent content-measurement batches.
- Android social overflow supports author insights and non-author
  `Not interested`; legacy `public` audience is rendered as **Campus**.
- Social graph storage includes blocks and mutual relationship lookups; Social
  surfaces and the primary Chat read/create/send/realtime paths reject blocked
  pairs.
- The backend exposes a bounded, cursor-based, audience- and block-filtered
  `GET /v1/feed/changes` reconciliation endpoint. No Android/PWA poller or
  cross-instance fanout consumes it yet.

## Verification performed

- Backend test suite: `35/35` passing.
- Web TypeScript check: passing.
- Data Connect generated-client compile: passing.
- Cloud Run `/ready`: analytics measurement and internal jobs both ready.
- Scheduler invocation: HTTP 200 from Cloud Run.
- Android Kotlin compile: passing offline.

## Must close before launch

1. Run and retain evidence for the two-account, two-tenant production canary:
   audience permissions, duplicate events, author exclusion, one rollup row,
   delete/privacy purge, and block matrix.
2. Extend the shared block policy beyond Social and the primary Chat paths to
   Marketplace, Notifications, Events, Games, search, and share-card
   resolution. It is not yet a complete cross-product privacy boundary.
3. Wire Android/PWA to the durable feed-change reconciliation endpoint, add a
   transactional outbox plus managed/shared realtime fanout, and verify cache
   purges. Process-local WebSocket hints do not recover safely across
   instances.
4. Finish universal encrypted entity cards and canonical deep links for post,
   vibe, event, game, listing, and profile sharing.
5. Add shared Android/PWA measurement fixtures, creator aggregate UI, an
   account-deletion/analytics-reset test, load tests, alerting, and a rollback
   drill.
6. Keep `For You` disabled until score shadowing, diversity/trust guardrails,
   explanations, reset, and remote gate ownership are complete.

## Cost posture

The design intentionally avoids a streaming analytics platform and BigQuery
export for the pilot. Raw events retain for 14 days, roll up every 15 minutes,
and use the existing Cloud Run/Data Connect stack. Revisit the retention window,
Cloud Run instance settings, and query plans after the canary/load-test metrics
exist; do not optimize by weakening authorization or event deduplication.

## Rollback note

If a code regression is discovered, shift Cloud Run traffic to the last known
good revision using the normal reviewed deployment procedure. Do not restore
destroyed secret versions; create and bind a fresh secret version instead.
