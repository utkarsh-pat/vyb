# Content Measurement Release Runbook

Owner: Social Platform
Last updated: 2026-08-07
Status: M0 ingestion and rollup provisioned in production; canary and release gates pending

## Implemented contract

- A qualified view is non-unique and requires 50% visibility for one continuous
  second. An impression requires 500 ms. Video view requires 3 seconds or 30%
  progress; completion requires 95%.
- Reach is exact unique reach for the selected period. The daily
  `ContentUniqueViewerDay` claim caps recommendation contribution to one
  qualified signal per viewer/content/UTC day. The separate lifetime claim
  supports exact 7-day, 30-day, and lifetime reach.
- Viewer identifiers are tenant-scoped HMAC pseudonyms. Email, Firebase UID,
  device advertising IDs, post text, media, and arbitrary labels are never
  stored in measurement rows.
- Creator self-events, cross-tenant or unauthorized content, drafts, deleted
  posts, malformed thresholds, stale timestamps, duplicate event IDs, and
  cooldown violations are excluded server-side.
- Android and PWA collect home-feed and full-screen Vibes eligibility. Both use
  bounded queues of 200 events and batches of at most 20; Android persists in
  SharedPreferences and PWA persists in localStorage.
- The backend accepts at most 32 KB per request using actual streamed byte
  limits at both PWA proxy and Cloud Run. Server cooldowns cap impressions and
  qualified views at 30 minutes, video playback signals at 1-5 minutes, and
  carousel slide signals at five seconds per viewer/content/event type.
- Rollups recompute a complete retained UTC day before marking source events,
  making retries converge without double increments. Retention only deletes
  already-rolled raw events. Post soft deletion atomically enqueues a durable
  privacy purge; an immediate sweep removes current rows and the delayed job
  closes races with in-flight collectors across backend instances.

## Retention and cost policy

Use `VYB_ANALYTICS_RAW_RETENTION_DAYS=14` for the first 30 stable beta days.
Move to seven days after reconciliation, delayed-job, and creator-support
metrics are healthy. Three days is an incident-only floor. Daily aggregates
and lifetime exact-reach claims live with the content; daily anti-inflation
claims expire after 90 days.

Keep BigQuery export off during the pilot. The SQL rollup runs every 15 minutes
and intentionally uses the existing Cloud Run and Data Connect stack rather
than a streaming analytics service.

## Production provisioning record

Completed on 2026-08-07 in Google Cloud project `vybnet`, region
`asia-south1`:

- Data Connect schema migration and the Social connector were deployed.
- Cloud Run service `vyb-backend` is serving the analytics-ready revision with
  `analyticsMeasurement=true` and `internalJobs=true` in readiness.
- Secret Manager holds the viewer-HMAC and internal-job credentials; Cloud Run
  reads them through its service account rather than source-controlled values.
- Cloud Scheduler job `vyb-analytics-rollup` runs every 15 minutes in
  `Asia/Kolkata` and invokes the internal rollup endpoint.
- Raw event retention is configured for 14 days. BigQuery export remains off
  for the pilot.

## Required production provisioning checklist

1. [x] Generate two independent random secrets of at least 24 characters:
   `VYB_ANALYTICS_VIEWER_KEY_SECRET` and `VYB_INTERNAL_API_KEY`. Store them in
   Secret Manager and bind them to the backend. Never rotate the HMAC secret
   without a dual-key migration plan, or exact reach will split.
2. [x] Set `VYB_ANALYTICS_RAW_RETENTION_DAYS=14` and deploy the Data Connect schema
   and generated Social connector before deploying collectors.
3. [x] Create a Cloud Scheduler job in the backend region with schedule
   `*/15 * * * *`, method `POST`, target
   `/v1/internal/analytics/rollup`, and the internal credential. Restrict job
   administration to the backend operator role; do not place the key in source
   control or shell history.
4. [x] Verify `/readyz` reports `analyticsMeasurement=true` and
   `internalJobs=true`. Production refuses readiness for local, missing, or
   short secrets.
5. [ ] Run a two-account canary: one eligible viewer and the author. Confirm author
   self-use is excluded, duplicate retries do not increment, the 15-minute job
   produces one daily row, and post deletion removes all four measurement
   families plus its durable purge request after the grace sweep.

## Monitoring and rollback gates

Alert on ingestion 4xx/429/5xx rates, oldest unrolled event age, rollup duration,
events selected versus rolled, retention deletion failures, per-post 50,000
event safety-limit trips, and Data Connect request/cost growth. Collection can
be disabled at clients through a release/Remote Config gate; do not delete raw
events before their successful rollup. A missed scheduler run is recoverable
because pending events remain unrolled and the next run recomputes the day.

## Known release gates

- Android offers an author-only per-content insight action and PWA has
  measurement/insight controls. A creator-level aggregate dashboard and
  delayed/estimated labels are not complete.
- Social block persistence and Social/PWA filtering exist, but block-in-either-
  direction enforcement is not yet shared by Chat, Marketplace, Notifications,
  Events, Games, or share-card resolution. It remains a launch blocker.
- `not_interested` feedback is persisted. Rules-based `For You`, explanations,
  exposure logging, reset, shadow scoring, diversity/fatigue policy, and a
  remotely gated recommendation lane are not enabled.
- Shared Android/PWA eligibility fixtures, database-backed concurrent job lock,
  account-deletion/analytics-reset workflow, load testing, and the mandatory
  two-account production canary remain before M1 creator UI.
- Exact reach queries currently have a 50,000-viewer safety cap, which is above
  the 20,000-30,000-user pilot target. Replace it with a native distinct/count
  query before expanding beyond the pilot ceiling.
