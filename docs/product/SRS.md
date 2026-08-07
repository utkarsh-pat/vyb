# Vyb Software Requirements Specification

Owner: Product and Architecture
Last Updated: 2026-08-02
Status: Marketplace-first MVP baseline
Change Summary: Aligned requirements with native Android, Firebase SQL Connect, R2 media, Marketplace in MVP, Stories deferred, and a two-to-three-university rollout.

## 1. Product Scope

Vyb is a verified, multi-tenant campus network for students, faculty, alumni, moderators, and university administrators. The MVP combines trusted identity, official communities, useful campus content, resources, direct messaging, and a campus Marketplace.

The MVP shall serve the responsive web/PWA and native Android clients through one public modular-monolith backend.

## 2. Actors

- verified student;
- faculty/alumni member where enabled;
- community moderator;
- university tenant admin;
- central Vyb trust/admin;
- support/operator;
- unauthenticated visitor.

## 3. Functional Requirements

### 3.1 Identity and tenant onboarding

- `ID-001`: The system shall authenticate with Firebase email/password and approved Google sign-in.
- `ID-002`: Email-based users shall verify their email before authenticated tenant access.
- `ID-003`: The backend shall map Firebase `sub` to one internal user UUID.
- `ID-004`: The system shall resolve college access from an approved domain and active membership.
- `ID-005`: Unknown domains shall enter an auditable join-request flow and shall not create live tenants automatically.
- `ID-006`: Users shall complete a unique campus username and required profile fields before publishing or contacting.
- `ID-007`: Clients shall not supply trusted tenant IDs, roles, owner IDs, or verification state.
- `ID-008`: Users shall be able to log out devices and request account export, correction, or deletion.

### 3.2 Tenant and communities

- `TEN-001`: Each university shall be isolated as a tenant.
- `TEN-002`: Official communities shall support campus, branch, batch, hostel, club, and controlled custom types.
- `TEN-003`: Community reads and writes shall enforce tenant plus visibility/membership.
- `TEN-004`: Admin membership, domain, and community changes shall be audited.
- `TEN-005`: A tenant shall have independent feature flags, moderation contacts, campus handoff spots, and allowed Marketplace categories.

### 3.3 Social feed

- `SOC-001`: Verified members shall create text and image posts.
- `SOC-002`: Feed and community lists shall use cursor pagination.
- `SOC-003`: Users shall comment, reply, react, save, follow, report, and soft-delete owned content.
- `SOC-004`: Removed, deleted, unauthorized-community, blocked-user, and non-ready-media content shall not be returned.
- `SOC-005`: Feed shall be reverse chronological in MVP.
- `SOC-006`: Video/Vibes shall require a tenant feature flag, media capacity, and stricter limits.
- `SOC-007`: Stories and story music shall be disabled for the initial public launch.
- `SOC-008`: Social realtime shall be a delivery/invalidation hint; PostgreSQL shall remain durable truth.
- `SOC-009`: Followers, following, and mutual connections shall be cursor-paginated and computed by the backend without downloading a user's full graph.
- `SOC-010`: Follow, unfollow, and block shall be idempotent; blocking shall remove both-direction follow edges and be enforced across discovery, content, chat, Marketplace contact, notifications, and sharing.
- `SOC-011`: People suggestions shall exclude self, existing follows, either block direction, inactive/incomplete profiles, and cross-tenant profiles; refresh shall rotate a bounded rules-based candidate set.
- `SOC-012`: Published post audiences shall be Campus, Followers, or Community. The legacy `public` value shall mean verified Campus access and shall never grant unauthenticated internet access.
- `SOC-013`: Feed clients shall receive versioned invalidation hints and reconcile from a durable change cursor on reconnect, foreground, or sequence gap without requiring repeated manual refresh.
- `SOC-014`: While a reader is away from the top, new posts shall be offered through a non-disruptive new-post indicator instead of moving the current reading position.
- `SOC-015`: Reposts, forwards, deep links, and media resolution shall not widen the source post's audience.
- `SOC-016`: Campus and Following feeds shall remain reverse chronological in the MVP; any personalized `For You` ordering shall be a separately labeled, remotely gated lane.
- `SOC-017`: Recommendation eligibility shall enforce tenant, audience, community, either-direction block, moderation, deletion, media-readiness, and fatigue rules before scoring.
- `SOC-018`: Users shall be able to inspect a recommendation reason and provide `Show more`, `Show less`, and `Not interested` feedback without losing access to chronological feeds.
- `SOC-019`: Recommendation scoring shall use normalized quality and satisfaction signals, recency, affinity, diversity, and negative feedback rather than raw popularity alone.
- `SOC-020`: The system shall provide a recommendation reset and shall not automatically restore blocked or hidden content.

### 3.3.1 Content measurement and creator insights

- `ANA-001`: Android and web/PWA shall emit the same versioned definitions for impression, qualified view, video play/view, reach, dwell/watch time, completion, engagement, and negative feedback.
- `ANA-002`: A video view shall require at least two continuous playback seconds while at least 50% of the player is visible and the client is foregrounded.
- `ANA-003`: Reach shall be a unique eligible-viewer aggregate and shall exclude author self-use, test/admin traffic, crawlers, blocked/unauthorized access, and duplicate retries.
- `ANA-004`: Content events shall be batched, bounded, idempotent, authenticated, tenant-validated, and free of post text, media bytes, private messages, and contact information.
- `ANA-005`: Raw content events shall have bounded retention and shall be reduced into hourly/daily aggregates through idempotent backend-owned jobs.
- `ANA-006`: Authors shall receive private content and creator insights for views, reach, interactions, profile/follow attribution, video retention, and carousel slide drop-off.
- `ANA-007`: Insight APIs shall never expose an individual viewer list and shall suppress cohort breakdowns below 100 unique eligible accounts.
- `ANA-008`: Public raw view counts shall remain disabled for MVP; private insights shall clearly identify delayed or estimated metrics.
- `ANA-009`: Audience response shall separate positive actions, consumption, fast skips/hides, and safety feedback; optional language sentiment shall never be a sole moderation or ranking decision.
- `ANA-010`: Firebase Analytics may measure product funnels, but creator insight and recommendation truth shall use the canonical server-validated content-event pipeline.

### 3.4 Marketplace

- `MKT-001`: Marketplace shall be part of the MVP.
- `MKT-002`: Verified users shall publish sale, wanted, and lend/borrow posts inside their tenant.
- `MKT-003`: Listings/requests shall support category, condition where applicable, integer minor-unit price/budget, campus spot, and up to four ready images.
- `MKT-004`: Users shall browse with cursor pagination and bounded filters/search.
- `MKT-005`: Users shall save/unsave eligible listings idempotently.
- `MKT-006`: Users shall contact the owner through an idempotent one-to-one chat flow without exposing private phone/email.
- `MKT-007`: Owners shall edit, reserve, mark sold/fulfilled, expire, and soft-delete inventory.
- `MKT-008`: Users shall report inventory or sellers and block contact.
- `MKT-009`: The system shall enforce prohibited categories, publish/contact limits, and moderation states.
- `MKT-010`: Vyb shall not process payments, hold funds, provide escrow, arrange delivery, or guarantee transactions in MVP.
- `MKT-011`: Marketplace safety and liability disclaimers shall be visible at publish, detail, and contact entry points.

### 3.5 Chat

- `CHAT-001`: MVP messaging shall be one-to-one inside the same tenant.
- `CHAT-002`: Clients shall encrypt supported message payloads before persistence.
- `CHAT-003`: Backend-owned data stores and logs shall not persist plaintext E2EE message bodies.
- `CHAT-004`: Messages shall be idempotent, ordered by server cursor, and durable before realtime notification.
- `CHAT-005`: Reconnect shall reconcile from durable history.
- `CHAT-006`: Marketplace contact shall create or reuse a direct conversation through the Chat module.
- `CHAT-007`: Block, report, read state, reaction, typing, and presence controls shall be supported within approved privacy limits.
- `CHAT-008`: Group chat and unreviewed multi-device key recovery are deferred.
- `CHAT-009`: Internal entity sharing shall use an encrypted versioned reference and a permission-aware resolver rather than an irrevocable copied content snapshot.
- `CHAT-010`: Chat shall render actionable Post, Vibe, Event, Game, Marketplace, and Profile cards and fail closed when the entity becomes deleted, blocked, moderated, expired, sold, or unauthorized.

### 3.6 Resources and events

- `RES-001`: Verified users shall browse and upload tenant/community-scoped academic resources.
- `RES-002`: Resource files shall use direct upload and metadata registration.
- `RES-003`: Resource lists shall be cursor-paginated and index-backed.
- `EVT-001`: Events may launch behind a tenant flag after relational persistence is complete.
- `EVT-002`: Events shall support interest, registration, or application modes without platform payment collection.
- `EVT-003`: Host-only registration review/export shall be authorized and audited.
- `SHR-001`: Shareable entities shall expose one canonical authenticated card model and HTTPS deep link across Android and PWA.
- `SHR-002`: External shares of campus-scoped content shall expose only generic branded metadata before authentication and authorization.
- `SHR-003`: Card actions shall be entity-aware: open/react/comment, register/save, play, contact seller, or follow only when currently allowed.

### 3.7 Media

- `MED-001`: File bytes shall not be stored in PostgreSQL.
- `MED-002`: Clients shall upload through short-lived signed intents directly to object storage.
- `MED-003`: The backend shall verify object existence, size, MIME signature, checksum, tenant/user path, and quota before publication.
- `MED-004`: Only `ready` media shall be publishable.
- `MED-005`: Expired/orphan uploads shall be deleted by a scheduled job.
- `MED-006`: Provider-specific storage code shall remain behind an adapter.

### 3.8 Notifications

- `NOT-001`: Business writes shall not wait for FCM.
- `NOT-002`: Notification intents shall be committed through a transactional outbox.
- `NOT-003`: Delivery shall be retry-safe, idempotent, preference-aware, and dead-lettered after bounded attempts.
- `NOT-004`: Invalid FCM devices shall be deactivated.
- `NOT-005`: Notification payloads shall not reveal private message content on lock screens by default.

### 3.9 Moderation and administration

- `MOD-001`: Users shall report posts, comments, media, Marketplace content/users, resources, events, and chat users.
- `MOD-002`: Moderators shall triage, investigate, action, dismiss, and close cases.
- `MOD-003`: High-risk actions shall record actor, reason, target, before/after state, request ID, and timestamp.
- `MOD-004`: Each launch tenant shall have a staffed escalation roster.
- `MOD-005`: Kill switches shall support per-feature, per-tenant read-only/disabled state.
- `MOD-006`: Admin interfaces shall never rely on client-provided role claims.

## 4. Non-Functional Requirements

### 4.1 Capacity

- `NFR-CAP-001`: The system shall support 30,000 registered users, 6,000 expected DAU, and 250 peak RPS.
- `NFR-CAP-002`: It shall pass a 500 RPS burst test without cross-tenant data, connection exhaustion, or unbounded backlog.
- `NFR-CAP-003`: Growing lists shall use keyset cursors with page limits.
- `NFR-CAP-004`: Media delivery cost shall be budgeted separately from API/database cost.

### 4.2 Performance

- `NFR-PERF-001`: Authenticated reads shall target p95 below 400 ms.
- `NFR-PERF-002`: Authenticated writes shall target p95 below 700 ms excluding file transfer.
- `NFR-PERF-003`: Feed first page shall target p95 below 800 ms on supported Indian mobile networks.
- `NFR-PERF-004`: Hot queries shall be index-backed and reviewed with launch-like data.

### 4.3 Availability and recovery

- `NFR-REL-001`: Monthly API availability target shall be 99.9%.
- `NFR-REL-002`: User-visible success shall only be returned after durable commit.
- `NFR-REL-003`: Async jobs shall be idempotent and retryable.
- `NFR-REL-004`: MVP RPO shall be 24 hours, improving to 15 minutes when PITR is enabled by the 20,000-user gate.
- `NFR-REL-005`: Regional application RTO shall be four hours.
- `NFR-REL-006`: Backup restoration shall be tested before public launch and quarterly.

### 4.4 Security and privacy

- `NFR-SEC-001`: Firebase tokens shall be verified at the backend.
- `NFR-SEC-002`: Tenant/object authorization shall be enforced in backend and RLS.
- `NFR-SEC-003`: All Data Connect operations shall declare explicit authorization and tenant scope.
- `NFR-SEC-004`: Secret/service-role credentials shall never be exposed to clients.
- `NFR-SEC-005`: Rate limits shall apply by IP, user, tenant, endpoint, and target as appropriate.
- `NFR-SEC-006`: Uploads shall be size-limited, MIME-sniffed, checksum-verified, and scanned.
- `NFR-SEC-007`: Tokens, cookies, private contact data, and message plaintext shall be redacted from logs.
- `NFR-SEC-008`: Privacy notice, retention, export/correction/deletion, and incident handling shall be reviewed for applicable Indian data-protection obligations.

### 4.5 Maintainability

- `NFR-MNT-001`: The MVP backend shall remain one modular deployable.
- `NFR-MNT-002`: Each domain shall have one production writer.
- `NFR-MNT-003`: No production JSON/temporary-disk fallback shall exist.
- `NFR-MNT-004`: New external services require an ADR.
- `NFR-MNT-005`: API changes shall support the current and previous mobile release.
- `NFR-MNT-006`: Migrations shall use expand/deploy/backfill/switch/contract.

### 4.6 Observability

- `NFR-OBS-001`: Every request shall have request/trace IDs and structured logs.
- `NFR-OBS-002`: Dashboards shall cover API, database, async, media, auth, Marketplace, chat, crashes, moderation, and cost.
- `NFR-OBS-003`: Alerts shall avoid high-cardinality user content.
- `NFR-OBS-004`: Release SHA and feature-flag state shall be recoverable for incidents.

## 5. External Systems

- Firebase Auth;
- Firebase Cloud Messaging, Crashlytics, and optional managed realtime;
- Firebase Data Connect backed by Cloud SQL PostgreSQL;
- object storage/CDN through the storage adapter;
- Google Cloud Run, Cloud Tasks/Scheduler, Secret Manager, and Logging;
- Vercel for the web/PWA;
- Google Play for Android distribution.

Openverse and story composition dependencies are not launch-critical while Stories are disabled.

## 6. MVP Acceptance Criteria

- a verified user joins only the correct university tenant;
- two test users in different tenants cannot discover, fetch, contact, or infer each other’s tenant content;
- onboarding and profile work on web and Android;
- feed, comments, reactions, resources, chat, notifications, and reports work against the canonical database;
- followers, following, mutuals, suggestions, block enforcement, post audiences, and live feed reconciliation pass two-account and two-tenant tests;
- Post, Vibe, Event, Game, Marketplace, and Profile cards share internally and externally without widening access;
- content measurement passes foreground, visibility, duration, deduplication, self-view, authorization, deletion, and retention fixtures on Android and PWA;
- creator insights are private, privacy-thresholded, and reconcile to durable aggregates;
- chronological Campus/Following feeds remain available while any `For You` experiment passes trust, diversity, cost, and latency guardrails;
- Marketplace browse/create/save/contact/sold/report works with moderation and no private-contact leak;
- direct media upload works without proxying file bytes through the normal API;
- Stories are disabled and video is controlled by tenant allowlist;
- production runs on Vercel web plus one Cloud Run backend; no Vercel backend adapter exists;
- no production domain has two writers or an ephemeral fallback;
- load, backup restore, incident, and rollback gates pass;
- pricing alerts and module kill switches are configured.

## 7. Explicit Non-Goals

- microservice fleet;
- Kafka or Kubernetes;
- sharding;
- full-text search service;
- recommendation ML (explainable rules-based discovery is allowed);
- money movement;
- open cross-campus network;
- public anonymous content;
- feature parity with Instagram/Facebook.
