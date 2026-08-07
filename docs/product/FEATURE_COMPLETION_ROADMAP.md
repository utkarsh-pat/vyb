# Feature Completion Roadmap

Owner: Product and Engineering
Last Updated: 2026-08-07
Status: feature-completion in progress; M0 measurement is provisioned in production

## Governing order

Vyb work proceeds in this order:

1. complete the intended product features and backend ownership;
2. finish Android/PWA behavior and contract parity;
3. pass two-account, two-tenant, real-device end-to-end QA;
4. harden backend reliability and observability;
5. optimize cost and performance using measured data;
6. perform release hardening and staged rollout.

Optimization must not be used to defer missing correctness. Release work must
not hide incomplete server-owned features behind client-only behavior.

## P0 feature-completion batches

### FC-0 - Contract freeze and compatibility

- Adopt [ADR-005](../architecture/ADR_005_SOCIAL_GRAPH_FEED_FRESHNESS_AND_SHARE_CARDS.md).
- Rename user-facing `Public` reach to `Campus`.
- Add a canonical `campus` wire value while accepting legacy `public` for the current and previous mobile release.
- Freeze shared Android/PWA response fixtures for relationship state, feed cursors, change events, and share cards.

Exit: old clients continue to publish safely and new clients interpret the
same audience identically.

Implementation checkpoint (2026-08-07): Android now renders the legacy
`public` wire value as **Campus**. The server continues to accept the legacy
wire value until the current app release is retired. The canonical server wire
value and cross-client fixtures are still release gates.

### FC-1 - Social graph, mutuals, block, and discovery

- Add block persistence and backend-owned authorized operations.
- Make follow/unfollow/block mutations idempotent and transactional where they touch multiple edges; re-follow must reactivate the unique soft-deleted edge.
- Add cursor-based followers, following, and mutuals APIs.
- Return `isFollowing`, `followsViewer`, `mutualCount`, and mutual previews.
- Replace recent-profile suggestions with bounded, filtered, rotating, rules-based suggestions.
- Enforce block across Social, Identity/Search, Chat, Marketplace, Notifications, Events, Games, and internal sharing.

Exit: the two-account block matrix passes with no alternate-route or count
leak, and suggestion refresh never returns ineligible users.

Implementation checkpoint (2026-08-07): persisted block records,
follow/following/mutuals lookups, social-feed filtering, PWA block controls,
and the backend mutuals API are implemented. Primary Chat inbox/read,
direct-conversation creation, send, and realtime admission now reject blocked
pairs. Block enforcement is not yet universal: Marketplace, Notifications,
Events, Games, search, share-card resolution, and remaining secondary Chat
actions must use the same policy before launch.

### FC-2 - Audience enforcement and live feed delta

- Normalize Campus/Followers/Community scope on publish and every downstream action.
- Add opaque feed cursors, a high-water sequence, and a bounded delta endpoint.
- Persist change/outbox records with business writes.
- Make Android and PWA auto-prepend only while the user is at the top and idle; otherwise show an uncounted `New posts`/`See new posts` pill while preserving the reading position.
- Reconcile on foreground, reconnect, login restore, and sequence gaps.
- Purge blocked, deleted, moderated, or newly unauthorized entities immediately from local caches.

Exit: a second account publishes while the first account stays open; the first
account updates without manual refresh and converges after an offline/reconnect test.

Implementation checkpoint (2026-08-07): post create/update/delete writes now
record a durable 24-hour `FeedChangeEvent`; `GET /v1/feed/changes` provides a
bounded opaque cursor and rechecks current tenant, audience, follow, community,
and block eligibility before returning an entity id. Clients do not yet consume
this endpoint, and the record is not a transactional outbox or shared realtime
fanout; those remain required before this batch can exit.

### FC-3 - Universal actionable sharing

- Replace copied post/vibe snapshots with encrypted `entity_card` references.
- Implement the permission-aware share-card resolver.
- Add Post, Vibe, Event, Game, Marketplace, and Profile cards to both chat clients.
- Add per-entity CTAs and unavailable/revoked states.
- Add canonical `/s/{type}/{id}` links, Android App Links, PWA routes, Android Sharesheet, Web Share API, and copy fallback.
- Keep unauthenticated previews generic for verified-campus content.

Exit: every entity type shares internally and externally, opens the correct
detail/action, and fails closed after deletion, block, or scope loss.

### FC-4 - Content measurement and private creator insights

- Adopt [ADR-006](../architecture/ADR_006_CONTENT_MEASUREMENT_AND_RECOMMENDATION.md).
- Freeze shared Android/PWA event eligibility fixtures for impressions, qualified views, video views, reach, watch/dwell, completion, carousel slides, and interactions.
- Add bounded offline batching and an idempotent backend ingestion endpoint.
- Add backend-only raw event, unique reach, and hourly/daily rollup operations with retention jobs.
- Ship author-only post/vibe and creator insights with explicit delayed/estimated labels and privacy thresholds.
- Keep Firebase Analytics limited to product funnels rather than creator or recommendation truth.

Exit: sampled sessions reconcile to rollups; retries do not double count; self,
background, blocked, cross-tenant, and unauthorized activity is excluded; the
expected pilot volume stays within connection, latency, storage, and budget limits.

Implementation checkpoint (2026-08-07): event ingestion, Android/PWA bounded
collectors, feed and Vibes qualification, exact reach ledgers, daily rollups,
retention, deletion purge, abuse cooldowns, author-only per-content insights,
Android overflow actions, PWA insight/measurement controls, the production
Data Connect schema, and the 15-minute production rollup job are deployed.
Shared cross-platform eligibility fixtures, creator aggregate insight UI,
two-account canary evidence, and universal block-aware eligibility remain
release gates; see the [measurement runbook](../operations/CONTENT_MEASUREMENT_RUNBOOK.md).

### FC-5 - Recommendation controls and gated discovery

- Keep Campus and Following reverse chronological.
- Add explicit `Why this post?`, `Show more`, `Show less`, and `Not interested` controls plus recommendation reset.
- Build a rules-based candidate/score pipeline with authorization-first filtering, normalized quality signals, diversity, fatigue caps, and new-creator exploration.
- Run the score in shadow mode before exposing a remotely gated `For You` lane.
- Treat sentiment as an optional aggregate aid; keep safety classifiers and moderation decisions separate.

Exit: blocked/unauthorized content never enters candidates, explanations match
the actual reason, shadow evaluation meets diversity/trust/cost gates, and
chronological ordering is unchanged.

Implementation checkpoint (2026-08-07): per-content recommendation feedback
(`not_interested`) is persisted and exposed to Android/PWA. It is a control,
not a ranking launch: `For You`, explanations, reset, shadow scoring,
diversity/fatigue policy, and remote gating remain intentionally incomplete.

### FC-6 - Remaining server-owned feature gaps

- Move scheduled publishing from device/browser persistence to an idempotent backend dispatcher with cancellation, retry, and audit state.
- Complete any remaining visible UI controls that still use local/fake state.
- Confirm profile tiles, notifications, search results, and deep links resolve the canonical entity instead of a copied view.

Exit: a closed client cannot prevent scheduled work, and no launch-visible
control claims success without durable backend state.

## P0 parity and end-to-end QA

- Run Android phone/tablet and PWA phone/desktop fixtures for every feature-completion batch.
- Use two accounts in one tenant plus two accounts across different tenants.
- Cover follow/unfollow/mutuals, suggestion rotation, block/unblock, each post audience, background/foreground feed convergence, offline recovery, and all share-card actions.
- Add accessibility, low-network, three-button/gesture navigation, and old mobile-release compatibility coverage.

Exit: no P0/P1 functional or cross-platform parity defect remains.

## Work after feature completion

### Reliability

- managed/shared realtime fanout;
- transactional outbox delivery and retry/dead-letter metrics;
- Crashlytics and structured trace correlation;
- backup/restore, failover, rollback, and Remote Config kill-switch drills;
- separate safe development authentication environment.

### Measured optimization

- query plans and indexes for actual hot paths;
- feed delta batch sizing and event retention;
- R2 thumbnails/variants, cache hit ratio, and viewport/network media policy;
- Cloud Run concurrency and SQL pool tuning from load tests;
- budget alarms and cost per DAU/active tenant.

### Release

- automated visual and contract regression;
- Marketplace trust/moderation readiness;
- staged Play and web cohorts;
- staff dogfood, closed campus beta, then university expansion through the gates in [MVP Phased Rollout](./MVP_PHASED_ROLLOUT.md).

## Product recommendations

- Keep the launch network campus-scoped; do not add unauthenticated public feeds merely to make external previews richer.
- Prefer explainable rules-based discovery until retention, safety, diversity,
  and satisfaction data justifies a separately reviewed ranking-ML decision.
- Do not optimize for raw views; use qualified consumption, saves/sends,
  meaningful interactions, follows, explicit feedback, and trust guardrails.
- Never restore follows automatically after unblock.
- Do not make realtime insertion move a user's reading position.
- Use one share reference and resolver across domains instead of six copied card implementations.
- Defer private-profile follow requests until the immediate-follow campus model is stable and moderation requirements are known.
