# Social Module LLD

Owner: Social Platform
Last Updated: 2026-08-02
Change Summary: Added social graph, block, audience, feed-delta, universal sharing, content measurement, creator insights, and gated discovery contracts while retaining Stories and video launch gates.

> Launch status: text and image feed capabilities are MVP. Stories, story music, and their endpoints are implemented/deferred and remain disabled with `stories_enabled=false` for the initial public rollout. Vibes/video require `video_beta=true` for an allowlisted tenant/cohort. The canonical system behavior and rollout gates are defined in `docs/architecture/LLD.md` and `docs/product/MVP_PHASED_ROLLOUT.md`.

## 1. Metadata

- Feature name: Social Module Phase 1
- Owner: Social Platform
- Runtime: `apps/backend`
- Phase: Phase 1
- Date: 2026-04-22
- Status: Active with launch-gated subsections
- Linked SRS section: 2.4 Campus Square Feed and 2.6 Moderation
- Linked HLD section: Phase 1 Module Map, Media Architecture, Observability
- Linked ADRs: `ADR_002_STORY_MUSIC_SEARCH_AND_CLIENT_EXPORT.md`, `ADR_005_SOCIAL_GRAPH_FEED_FRESHNESS_AND_SHARE_CARDS.md`, `ADR_006_CONTENT_MEASUREMENT_AND_RECOMMENDATION.md`

## 2. Problem Statement

We need a trustworthy campus social layer where verified members can create posts, short-form vibes, and time-limited stories, search other users by campus user ID, follow profiles, repost campus content, inspect likers, participate in threaded comments, report unsafe content, and consume immersive story and vibe playback across mobile and desktop. Phase 1 also includes optional royalty-free music composition for one story asset at publish time. The module must remain tenant-safe, community-aware, and ready for future ranking without forcing that complexity into Phase 1.

## 3. Scope

In scope:

- text and image posts
- video vibes
- time-limited stories as deferred implementation, not initial launch scope
- immersive story viewer behaviors, including segmented progress, own-story add affordance, seen-state rings, and viewer audio playback
- royalty-free story music search plus single-asset client-side story music composition retained behind the disabled Stories flag
- public profile discovery by campus user ID
- follow and unfollow graph
- followers, following, mutual connections, rotating suggestions, and server-enforced user blocking
- Campus, Followers, and Community audience selection with legacy `public` compatibility
- durable feed change cursors and non-disruptive new-post delivery
- shared content-event definitions, private creator insights, and bounded rollups
- explicit recommendation feedback and a remotely gated rules-based `For You` lane
- permission-aware universal share references for social and non-social entities
- feed reads by tenant and community
- comments
- reactions
- threaded replies and comment likes
- post likers list
- repost and quote repost
- story reactions and seen state
- author edit and soft delete for posts and vibes
- responsive desktop and mobile social interaction surfaces
- immersive vibes playback with default sound-on intent, tap pause or resume, and press-and-hold speed-up
- tenant-scoped WebSocket fanout for post, comment, and reaction changes on active feed or vibe clients
- extraction-ready domain boundaries

Out of scope:

- polls
- anonymous posting
- recommendation ML or unexplainable ranking
- direct messaging
- third-party GIF search provider integration
- multi-asset story music export in one batch publish
- backend media transcoding or waveform generation for story music
- premium licensed music catalog ingestion beyond the selected royalty-free provider
- dedicated transcoding fleet for social video

## 4. Owning Module

- Primary owner: `social`
- Runtime boundary: `apps/backend/src/modules/social`
- Secondary dependencies: `campus`, future `media`, future `moderation`

## 5. User Flows

- Flow 1: verified member creates a text, image, or vibe post in a tenant or community scope.
- Flow 2: user opens the feed, sees the latest published posts from allowed scopes, and opens media in a full-screen viewer.
- Flow 3: user searches another member by campus user ID and follows them.
- Flow 4: user publishes a story and followed profiles see it in their story lane while it is active.
- Flow 5: user comments, replies, reacts, or attaches a supported GIF or sticker in a comment thread after tenant and community access are verified for the target post.
- Flow 6: user inspects likers, direct reposts, or quote reposts an existing post or vibe after the target post scope is verified.
- Flow 7: an author edits or soft-deletes their own post or vibe.
- Flow 8: a viewer opens the story viewer, progresses through stories, marks them seen, and optionally likes a story.
- Flow 9: user reports unsafe social content when moderation support is enabled.
- Flow 10: user selects one story asset, browses the royalty-free music library, previews a 15, 30, 45, or 60 second clip, positions the music sticker, exports the final MP4 in the browser, and then publishes it as a normal story item.
- Flow 11: viewer opens `/vibes`, the active item attempts sound-on playback, a single tap pauses or resumes, and a press-and-hold temporarily boosts playback speed.
- Flow 12: backend persists a social mutation, emits a small `/ws/social` event, and active clients patch local feed, comment, and reaction state without polling.
- Flow 13: a user refreshes people suggestions and receives a new eligible, rules-ranked page with mutual context and no followed/blocked/cross-tenant accounts.
- Flow 14: a user blocks another account; both-direction follows are removed and every product surface immediately loses access without revealing block direction.
- Flow 15: an open feed receives a versioned invalidation, fetches a bounded delta, and either prepends at the top or shows a new-post pill without moving the reader.
- Flow 16: a user shares a Post, Vibe, Event, Game, Marketplace listing, or Profile as an encrypted reference; the recipient resolves the current authorized actionable card.
- Flow 17: an eligible foreground impression/view is buffered locally, sent in an idempotent batch, validated by the backend, and reduced into private creator aggregates.
- Flow 18: an author opens post/vibe insights and sees delayed/estimated reach, engagement, watch/retention, carousel drop-off, and audience response without viewer identities.
- Flow 19: a viewer requests `Why this post?`, selects `Show more`, `Show less`, or `Not interested`, or resets recommendations; the canonical feedback updates future discovery without changing chronological lanes.

## 6. API Design

### `POST /v1/posts`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: tenant id, optional community id, body, media references, visibility
- response schema: live published post payload
- error schema: unauthorized scope, invalid media, validation failure
- rate limit policy: moderate per user, tighter burst protection
- Community Connect rule: if `communityId` is present, the social module must verify the viewer has an active membership in that community before writing the post.
- Community identity rule: community-scoped posts force verified identity and `allowAnonymousComments: false` in V1.

### `PATCH /v1/posts/{postId}`

- caller: web/PWA or Android client
- auth requirement: verified membership required and author-only
- request schema: optional `title`, `body`, `location`
- response schema: updated published post payload
- error schema: post not found, unauthorized author, validation failure
- rate limit policy: moderate per user
- Community Connect rule: if the existing post has `communityId`, the author must still have active membership in that community before editing.

### `DELETE /v1/posts/{postId}`

- caller: web/PWA or Android client
- auth requirement: verified membership required and author-only
- request schema: none
- response schema: `postId`, `deleted`
- error schema: post not found, unauthorized author
- rate limit policy: moderate per user
- Community Connect rule: if the existing post has `communityId`, the author must still have active membership in that community before deleting.

### `GET /v1/feed`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: `mode=campus|following|for_you`, optional community id, opaque cursor, bounded limit
- response schema: paginated published posts
- error schema: invalid tenant, unauthorized community, bad cursor
- rate limit policy: moderate per user
- Community Connect rule: community-scoped feed reads are member-only in the current V1 implementation.
- Ordering rule: `campus` and `following` are reverse chronological; `for_you` is available only to an allowlisted Remote Config cohort and returns an explanation code per item.

### `GET /v1/vibes`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: tenant id, cursor, limit
- response schema: paginated vibe posts
- error schema: invalid tenant, bad cursor
- rate limit policy: moderate per user

### `GET /v1/posts/{postId}/likes`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: optional `limit`
- response schema: member list for the active post reactions
- error schema: post not found, invalid limit, unauthorized community scope
- rate limit policy: moderate per user
- Community Connect rule: liker lists for community-scoped posts are member-only.

### `POST /v1/stories`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: tenant id, media type, media payload, optional caption, and optional client-exported music-backed video reference when story music composition is used
- response schema: created story payload with expiry
- error schema: invalid media, unauthorized scope, validation failure
- rate limit policy: moderate per user

### `GET /v1/stories`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: tenant id
- response schema: active stories visible to the current viewer
- error schema: invalid tenant
- rate limit policy: moderate per user

### `GET /api/story-music`

- caller: web story composer only
- auth requirement: same-origin client access; no extra app auth gate in Phase 1
- request schema: search mode accepts optional `q` and `limit`; stream mode accepts `mode=stream` and `trackId`
- response schema: search returns royalty-free track summaries; stream returns proxied audio bytes for the selected track
- error schema: upstream provider unavailable, track missing, invalid query
- rate limit policy: low to moderate per client to protect upstream usage

### `GET /v1/users/search`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: tenant id, query, limit
- response schema: matched user summaries plus follow-state and stats
- error schema: invalid tenant, invalid limit
- rate limit policy: moderate per user

### `GET /v1/users/{username}`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: tenant id
- response schema: public campus profile, follow stats, and recent posts
- error schema: user not found, invalid tenant
- rate limit policy: moderate per user
- Community Connect rule: recent posts must exclude community-scoped posts from communities the viewer cannot access.

### `PUT/DELETE /v1/users/{username}/follow`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: tenant id
- response schema: updated follow state and stats snapshot
- error schema: user not found, invalid tenant
- rate limit policy: moderate per user

### `GET /v1/users/{username}/mutuals`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: opaque cursor and bounded limit
- response schema: mutual profiles, `mutualCount`, and `nextCursor`
- rule: computed as accounts the viewer follows that also follow the subject; block and tenant filters are mandatory

### `GET /v1/users/suggestions`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: opaque cursor, refresh token, and bounded limit
- response schema: rotating eligible profiles with mutual previews and relationship state
- rule: no self, active follow, either block direction, incomplete/inactive, or cross-tenant candidate

### `PUT/DELETE /v1/users/{username}/block`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- response schema: authoritative blocked state
- rule: idempotent; create is transactional with both-direction follow removal; unblock restores nothing

### `GET /v1/feed/changes`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: durable `after` sequence and bounded limit
- response schema: ordered entity IDs, versions, event types, latest sequence, and gap/reload indication
- rule: response contains no post body or media payload and re-evaluates audience/block access

### `POST /v1/analytics/events:batch`

- caller: web/PWA or Android event collector
- auth requirement: verified membership required
- request schema: contract version plus 1-50 bounded events with unique event IDs, content reference, source, visibility/watch/progress values, client/session timestamps, and no arbitrary content payload
- response schema: accepted, duplicate, dropped, and invalid counts plus server watermark
- rule: backend resolves user/tenant/author/scope, ignores self/test/background/unauthorized measurement as defined by ADR-006, and bulk inserts through backend-only Data Connect operations

### `GET /v1/posts/{postId}/insights`

- caller: post/vibe author or authorized trust/tenant operator
- auth requirement: verified membership plus server-resolved authorship/role
- request schema: `range=24h|7d|30d|lifetime`
- response schema: delayed/estimated view, reach, interaction, attribution, watch/retention, carousel, and audience-response aggregates with processing watermark
- rule: never returns a viewer list and suppresses a cohort with fewer than 100 unique eligible accounts

### `GET /v1/creators/me/insights`

- caller: authenticated creator
- auth requirement: verified membership; identity is always `auth.uid`
- request schema: bounded date range and optional content type
- response schema: creator-owned aggregate summary and top content
- rule: a caller cannot pass an owner ID

### `PUT /v1/preferences/content/{contentId}`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: `show_more`, `show_less`, `not_interested`, or item reset
- response schema: authoritative preference and explanation impact
- rule: idempotent, tenant/scope checked, and distinct from report/block moderation

### `POST /v1/recommendations/reset`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- response schema: reset version and effective time
- rule: clears derived interest signals but does not delete follows, blocks, saves, posts, or moderation history

### `GET /v1/share-cards/{type}/{id}`

- caller: authenticated internal share/deep-link renderer
- auth requirement: verified membership required
- response schema: current normalized card plus permitted actions, or neutral unavailable state
- rule: enforce tenant, block, audience, membership, moderation, lifecycle, and source-scope rules at view time

### `POST /v1/posts/{postId}/comments`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: body, optional parent comment id, optional `mediaUrl`, optional `mediaType`
- response schema: created comment
- error schema: post not found, unauthorized scope, validation failure
- rate limit policy: moderate per user
- Community Connect rule: comments and replies on community-scoped posts are member-only.
- Community identity rule: anonymous comments are not accepted for community-scoped posts in V1.

### `PUT /v1/comments/{commentId}/reactions`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: comment reaction type
- response schema: current comment-like state and aggregate count snapshot
- error schema: comment not found, unauthorized scope
- rate limit policy: moderate per user
- Community Connect rule: the comment's parent post must pass tenant and community access before the reaction is accepted.

### `PUT /v1/posts/{postId}/reactions`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: reaction type
- response schema: current reaction state and aggregate count snapshot
- error schema: post not found, unauthorized scope
- rate limit policy: moderate per user
- Community Connect rule: reactions on community-scoped posts are member-only.

### `POST /v1/posts/{postId}/repost`

- caller: web/PWA or Android client
- auth requirement: verified membership required and completed profile
- request schema: optional `quote`, optional `placement`
- response schema: created repost item in feed or vibe placement
- error schema: post not found, incomplete profile, unauthorized scope
- rate limit policy: moderate per user
- Community Connect rule: reposts preserve the source post's `communityId` and require access to that source community.

### `PUT /v1/stories/{storyId}/reactions`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: story reaction type
- response schema: current story-like state and aggregate count snapshot
- error schema: story not found, unauthorized scope
- rate limit policy: moderate per user

### `PUT /v1/stories/{storyId}/seen`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: none
- response schema: story seen-state acknowledgement
- error schema: story not found, unauthorized scope
- rate limit policy: moderate per user

### `POST /v1/reports`

- caller: web/PWA or Android client
- auth requirement: verified membership required
- request schema: `targetType`, `targetId`, `reason`
- response schema: created report summary
- error schema: invalid payload, unauthorized scope
- rate limit policy: moderate per user

## 7. Module Interactions

- calling layer: backend edge
- target module: `social`
- reason: public feed, post, comment, and reaction APIs
- interaction type: direct in-process invocation
- failure handling: return safe API errors

- calling module: `social`
- target module: `campus`
- reason: validate membership permission for feed reads and writes
- interaction type: direct in-process domain call
- failure handling: fail closed for writes, safe fallback for local dev reads where explicitly allowed

- calling module: `social`
- target module: `identity`
- reason: resolve campus user IDs and public profile data for feed authors, search, and follow views
- interaction type: direct in-process repository call
- failure handling: fail closed for writes and return safe empty states for discovery reads when profile data is unavailable

- calling layer: backend edge
- target module: `moderation`
- reason: social surfaces submit content reports through the shared moderation module
- interaction type: direct in-process invocation
- failure handling: return visible report errors without mutating social content state

- calling layer: web story composer
- target module: `apps/web /api/story-music`
- reason: query the approved royalty-free music provider and proxy the selected track back to the browser for export
- interaction type: same-origin web helper request
- failure handling: show visible music-library or stream-fetch errors and keep normal story publishing available

- calling layer: web story composer
- target module: browser-side `ffmpeg.wasm`
- reason: merge one selected visual asset, the selected music clip, and the draggable music sticker into the final MP4 before upload
- interaction type: client-side media composition
- failure handling: abort export, surface safe client error text, and allow publish retry without server mutation

## 8. Data Model Changes

- tables touched: `posts`, `post_media`, `stories`, `story_reactions`, `story_views`, `follows`, `user_blocks`, `feed_events`/outbox projection, `comments`, `comment_reactions`, `reactions`, `user_activity`, `content_events`, `content_unique_viewers`, `content_insights_hourly`, `content_insights_daily`, `user_interest_signals`, `content_features`, `recommendation_exposures`
- columns added: canonical audience and event version/sequence fields as defined by ADR-005
- indexes added: `posts (tenant_id, created_at desc)`, `posts (community_id, created_at desc)`, `comments (post_id, created_at asc)`, `reactions (post_id)`
- unique constraints: `reactions (post_id, membership_id)` for one active reaction per member per post
- social graph constraints: unique active follow key and block key; block creation soft-deletes both-direction follows in one transaction
- soft delete impact: posts and comments use soft delete with status change support
- backfill required: online normalization of legacy `public`/`tenant` audience values to `campus` after the compatibility reader is deployed and verified
- measurement constraints: unique event ID; unique content/viewer-key/day reach claim; additive rollup keys by tenant/content/date/source; raw event retention no longer than 14 days after a durable watermark
- Data Connect access: measurement ingestion, rollup, interest-signal, and recommendation exposure operations use `@auth(level: NO_ACCESS)` and generated Admin SDK calls from Cloud Run; clients never call generated operations directly

## 9. Query Plan

- query name: tenant feed query
- filter fields: `tenant_id`, `status = published`, `deleted_at is null`
- sort order: `created_at desc`
- expected scale: highest read volume in Phase 1
- supporting index: `posts (tenant_id, created_at desc)`
- why this is safe: tenant filter plus cursor pagination

- query name: community feed query
- filter fields: `community_id`, `status = published`, `deleted_at is null`
- sort order: `created_at desc`
- expected scale: medium-high
- supporting index: `posts (community_id, created_at desc)`
- why this is safe: bounded scope and cursor pagination

- query name: comment thread query
- filter fields: `post_id`, `deleted_at is null`
- sort order: `created_at asc`
- expected scale: every post detail view
- supporting index: `comments (post_id, created_at asc)`
- why this is safe: per-post scoped

- query name: people suggestions query
- filter fields: `tenant_id`, active/discoverable profile, follow exclusion, both-direction block exclusion
- sort order: bounded rules score followed by deterministic refresh rotation
- expected scale: medium read volume on search/profile refresh
- supporting index: active follow/block direction indexes plus tenant profile discovery index
- why this is safe: bounded tenant candidate pool; no random full-table ordering

- query name: feed change reconciliation
- filter fields: `tenant_id`, optional target user, `sequence > high_water`
- sort order: `sequence asc`
- expected scale: frequent small foreground/reconnect reads
- supporting index: `(tenant_id, sequence)` and optional `(target_user_id, sequence)`
- why this is safe: bounded delta pages with expiry and full-feed fallback on a gap

- query name: content event batch insert
- filter fields: trusted tenant/content eligibility resolved before write; unique event IDs
- sort order: append-only server receive time
- expected scale: 200,000-500,000 accepted events/day at pilot maturity
- supporting index: unique event ID plus `(tenant_id, received_at)` retention index
- why this is safe: 20-50 row bulk inserts, strict payload limits, backend-only operation, no user-facing transaction dependency

- query name: content insight rollup
- filter fields: server receive watermark and tenant shard/range
- sort order: receive time ascending
- expected scale: 5-15 minute bounded job
- supporting index: raw watermark index; unique aggregate keys for idempotent upsert
- why this is safe: retries converge, raw rows expire only after the durable watermark, and rollup lag is observable

- query name: `For You` candidates
- filter fields: tenant, audience, community, either-direction block, moderation, ready media, age, seen/fatigue caps
- sort order: rules score followed by deterministic tie-break and opaque cursor
- expected scale: disabled at launch, then bounded allowlisted cohorts
- supporting index: existing eligible feed indexes plus bounded affinity/exposure keys
- why this is safe: authorization is an eligibility stage before scoring; no score can restore an ineligible entity

## 10. Validation and Security

- auth checks: membership must be verified for posting, story creation, following, commenting, reacting, reposting, and reporting
- community guard: every interaction that accepts a post id or comment id must resolve the owning post and reject the request when the viewer lacks active membership in that post's community
- block guard: every identity/content/chat/Marketplace/notification/share read and mutation must reject or suppress either direction of an active block on the server
- audience guard: `campus`, `followers`, and `community` are evaluated at read/action time; legacy `public` is only a Campus compatibility alias
- burst limits: comment writes, reaction writes, reposts, thread reads, and content-management mutations use per-member short-window limits at the backend boundary
- tenant checks: reads and writes validated through campus-owned context
- input validation: body length, media count, allowed MIME types, campus user-ID resolution, reaction enums, repost quote length, author-only edit/delete gates, story music clip lengths limited to 15 or 30 or 45 or 60 seconds, and story music publish limited to one selected asset at a time
- abuse prevention: post, comment, reaction, repost, thread-read, and report rate limits plus content status workflow
- audit logging: report creation, moderator removals, and privileged edits
- measurement privacy: no raw viewer list, no sensitive traits, HMAC-derived viewer key, no text/media/chat payload, 100-unique-account cohort threshold, and tested reset/deletion/retention behavior
- recommendation integrity: raw popularity and optional sentiment cannot independently rank, moderate, or remove content; explicit block/report immediately removes eligibility
- client behavior notes: vibe playback should attempt sound-on startup with safe muted fallback if the browser blocks autoplay; story viewer audio must remain user-toggleable; own-story bubbles must support separate open-story and add-story interactions

## 11. Observability

- logs: post create, vibe create, story create, feed read, user search, follow state change, comment create, comment reaction upsert, repost create, story seen, story reaction, report create, and post soft delete
- metrics: feed latency, post creation success rate, story publish rate, vibe publish rate, follow conversion, comment volume, repost volume, accepted/dropped/duplicate content events, rollup lag, raw-table growth, insight freshness, `For You` hide/report/block rate, diversity, new-creator exposure, and story/video completion
- alerts: error spikes on post publish, feed retrieval, or social interaction writes; rollup lag above 15 minutes; unbounded raw-event growth; recommendation guardrail regression; measurement cost forecast breach
- trace IDs: required at the backend boundary and through module calls

## 12. Failure Modes

- campus access resolution fails: writes fail closed, reads may fall back only in explicit starter mode
- media registration missing: publish fails closed with a visible client error
- reaction race: last write wins under unique constraint and upsert behavior
- follow race: duplicate follow writes collapse to one relationship
- block/follow race: the transaction and block precondition make block win; no follow may remain or be recreated while either block direction is active
- realtime gap or Cloud Run instance change: clients reconcile from the durable sequence and perform a full cursor refresh when retention has expired
- revoked share: resolver returns a neutral unavailable card and never serves the copied historical snapshot
- story expiry race: story reads always filter by active expiry window
- royalty-free music provider unavailable: story music search and streaming fail without blocking plain story publish
- `ffmpeg.wasm` assets fail to load or client export exceeds device capacity: music-backed story export fails locally and the composer should offer retry or publish without music
- browser autoplay policy blocks sound-on startup: vibes or story audio may require an explicit user unmute interaction even when the product defaults to sound-on intent
- event batch retry/duplicate: unique event IDs make retries idempotent and the response separates duplicate from invalid events
- rollup job failure: creator insights retain the last processing watermark, display delayed state, and retry without affecting feed reads/writes
- recommendation service/flag failure: return the chronological Campus/Following lane; never fall back to unauthorized or stale ranked candidates

## 13. Rollout Plan

- feature flags: optional post create flag plus independent measurement, creator insights, explicit feedback, shadow scoring, and `For You` cohort controls
- migration order: create social tables, publish read endpoint, then post write path
- measurement rollout: shared fixtures, private dogfood insights, feedback plus shadow scoring, then one-campus 5% `For You` cohort before 25/50/100 expansion
- rollback plan: independently disable non-essential measurement/insights/feedback/`For You`; disable post creation while keeping read-only chronological feed if core instability appears

## 14. Test Plan

- unit tests: post validation, audience normalization, mutual intersection, suggestion exclusions/rotation, block precedence, feed-event deduplication, share-card authorization, metric visibility/duration thresholds, event dedupe, self/test/background exclusion, unique reach, rollup idempotency, recommendation eligibility/diversity/fatigue, story visibility, follow graph, reaction upsert, comment-thread building, repost validation, story music input validation, and soft-delete guards
- integration tests: post create with every audience, post update/delete, story create, story seen, user search, follow/block update, followers/following/mutual pagination, suggestion refresh, feed pagination/delta recovery, event batch retry/retention, author-only insights/privacy threshold, explicit feedback/reset, chronological lane stability, shadow scorer safety, every share entity resolver, report create, and story music search helper responses
- client verification: immersive story viewer playback, story audio toggle behavior, own-story add affordance, vibe tap pause or resume, and press-and-hold speed boost on supported browsers
- contract tests: post create, feed lanes, vibe read, event batch/insights/preferences/reset, story create/read/react/seen, user search, public profile, follow update, comment create, comment reaction update, post likes, repost create, and post update/delete
- manual QA: use two same-tenant and two cross-tenant accounts to create audience-scoped posts, verify measurement thresholds and insights, follow/unfollow, inspect mutuals, rotate suggestions, block/unblock, inspect recommendation reasons/feedback, verify automatic feed freshness and reconnect recovery, share every entity internally/externally, then complete the existing post/story/vibe interaction suite

## 15. Documentation Updates Required

- HLD: if feed scope or moderation flow changes
- SRS: if social scope expands
- Master Plan: when feed ships to first tenant
- API docs: all public social endpoints
- Runbook: feed outage and moderation handling

## 16. Open Questions

- when should the curated GIF and sticker tray move to provider-backed search without slowing comment compose time
- do we need a denormalized feed read model before broader campus launch
