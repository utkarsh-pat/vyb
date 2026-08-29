# Release Readiness — 2026-08-07

## What is deployed

- Google Cloud project: `vybnet`; backend region: `asia-south1`.
- Cloud Run service: `vyb-backend`, revision `vyb-backend-00024-h6p`, 100% of
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
- On 2026-08-30, the shared social-feed fanout shipped at
  `https://vyb-games-realtime.ceoutkarshpatel.workers.dev`; Cloud Run reads its
  matching HMAC value from Secret Manager and reports `socialRealtimeFanout`
  ready.

## Product state included in this change set

- On 2026-08-26, Android and PWA began consuming the durable feed high-water
  stream across foreground and reconnect recovery. Both preserve reading
  position behind a New posts affordance and converge by reloading the
  authorized feed, including cache purge after deletion, block, moderation, or
  audience loss. The repository now commits post create/update/delete and its
  content-free feed-change record in one Data Connect transaction (or one local
  fallback snapshot). Shared content-free fanout is implemented for the Worker,
  backend, PWA, and Android. The connector, Worker migration, secrets, and
  backend revision are deployed; live two-account reconnect/privacy evidence
  remains a release gate.

- Author-only content insights and content-measurement preferences are exposed
  through backend, PWA, and Android client contracts.
- PWA and Android send bounded, idempotent content-measurement batches.
- Android social overflow supports author insights and non-author
  `Not interested`; legacy `public` audience is rendered as **Campus**.
- Social graph storage includes blocks and mutual relationship lookups; Social
  surfaces and the primary Chat read/create/send/realtime paths reject blocked
  pairs.
- The backend exposes a bounded, cursor-based, audience- and block-filtered
  `GET /v1/feed/changes` reconciliation endpoint. PWA receives safe live
  public-card updates and content-free restricted-feed invalidations; Android
  and PWA now consume its content-free high-water summary. Cross-instance
  invalidation fanout is implemented in code and awaits production deployment.
- On 2026-08-25, the shared two-direction relationship policy was extended to
  Marketplace, Events, backend/native notifications, PWA notifications,
  Scribble rooms, and Chess/Ludo/UNO Durable Object admission. Blocked actors
  are removed from visible lists and aggregate counts; direct interactions and
  online-room joins fail closed.

## Verification performed

- Backend test suite: `50/50` passing on the 2026-08-30 fanout deployment pass,
  including `10/10` focused cross-domain relationship/notification tests.
- Games realtime Durable Object suite: `5/5` passing, including blocked-pair
  room concealment and social session/publish boundaries.
- Web TypeScript check: passing.
- Data Connect generated-client compile: passing.
- Cloud Run `/ready`: analytics measurement, internal jobs, and shared social
  fanout ready on revision `vyb-backend-00024-h6p`.
- Production rollout passed 5%, 25%, 50%, and 100% traffic stages with zero
  revision error logs. The final authenticated smoke test returned an open
  Durable Object socket and `social.connected` from the canonical backend path.
- Scheduler invocation: HTTP 200 from Cloud Run.
- Android Kotlin compile: passing offline.
- On 2026-08-09, direct messaging passed a local two-account/two-emulator
  authenticated round trip. Encrypted messages propagated live in both
  directions without manual refresh, inbox preview/unread recovery passed after
  process restart, and the post-delivery idle trace showed no recursive API
  traffic. The tenant-scoped conversation key, duplicate inbox rows, optional
  R2 hidden-state dependency and read/delivery refresh loop found during this
  run were corrected.
- The follow-up low-latency pass added immediate Android local echo, cached the
  active secure conversation session, consumed committed encrypted socket
  envelopes directly, moved inbox preview/analytics after backend fanout, and
  made read-receipt persistence non-blocking. A fresh two-emulator run rendered
  the message on the recipient without a conversation-history refetch and
  reached `Read` on the sender. This proves the local single-backend text path;
  it does not close the shared multi-instance fanout launch gate. See
  [ADR-007](../architecture/ADR_007_CHAT_REALTIME_DELIVERY.md).

## Must close before launch

1. Run and retain evidence for the two-account, two-tenant production canary:
   audience permissions, duplicate events, author exclusion, one rollup row,
   delete/privacy purge, and block matrix.
2. Deploy and run the retained two-account/two-tenant block matrix for the new
   Marketplace, Notifications, Events and Games boundaries. Finish the
   remaining secondary Chat-action audit and permission-aware share-card
   resolver; those are the remaining cross-product privacy gates.
3. Retain two-account offline/reconnect cache-purge evidence for the deployed
   atomic feed-change operations and Durable Object fanout. Android/PWA shared
   fanout and periodic reconciliation are wired; cross-account privacy evidence
   is still required.
4. Finish universal encrypted entity cards and canonical deep links for post,
   vibe, event, game, listing, and profile sharing.
5. Add shared Android/PWA measurement fixtures, creator aggregate UI, an
   account-deletion/analytics-reset test, load tests, alerting, and a rollback
   drill.
6. Keep `For You` disabled until score shadowing, diversity/trust guardrails,
   explanations, reset, and remote gate ownership are complete.
7. Complete message attachment, community-realtime, offline/retry,
   process-recreation and block/privacy matrix QA before calling Chat launch
   complete; the direct text-message path is verified, not the whole messaging
   product.

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
