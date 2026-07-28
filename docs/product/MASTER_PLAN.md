# Vyb Master Plan

Owner: Product and Engineering
Last Updated: 2026-07-28
Status: Marketplace-first MVP execution plan

## 1. Product Thesis

Vyb wins by becoming the trusted utility layer for campus life, not by copying every social-media feature. The first retention loop is:

```text
verified identity -> useful campus/community content -> Marketplace/resources/chat action -> return
```

Stories are not required to validate this loop. Marketplace is.

## 2. Current Reality

Already present in the repository:

- Next.js web/PWA and native Kotlin/Compose Android clients;
- modular Node backend with identity, campus, social, market, chat, resources, events, notifications, moderation, and games modules;
- Firebase Auth/FCM/Storage integrations;
- Firebase Data Connect schemas and generated SDKs;
- fresh Firebase/Data Connect production configuration; legacy migration code is not part of the launch path;
- feed, Vibes, Stories, chat, Marketplace, resources, events, games, notifications, and profile surfaces;
- Cloud Run and Vercel deployment assets.

Current risk:

- implemented screens exceed production readiness;
- new infrastructure still requires full deployment and verification;
- local development fallbacks must remain disabled in production;
- the old Vercel backend must not be connected to the new frontend;
- media and Marketplace hot paths need launch hardening;
- older documents describe Android as future, Marketplace as Phase 3, and Stories as required Phase 1.

## 3. Locked MVP Scope

Must ship:

- identity/profile/tenant;
- official communities;
- text/image feed;
- Marketplace;
- direct chat;
- resources;
- notifications;
- moderation/admin/audit;
- web/PWA and Android.

Gated:

- events;
- Vibes/video;
- games.

Deferred:

- Stories/story music;
- payments/wallet/escrow;
- anonymous Nook;
- group chat;
- AI/ranking/competitions.

## 4. Execution Workstreams

### A. Data convergence

- keep Firebase SQL Connect schema and connector operations compiled;
- verify Firebase issuer/audience and server-side membership resolution;
- start the canonical production database empty and seed only approved launch data;
- store all new user media in R2;
- remove production dual-write and JSON fallback.

### B. Platform hardening

- production Cloud Run backend;
- pooled/bounded database connections;
- direct upload intent;
- outbox plus managed delivery;
- global rate limiting and idempotency;
- backup, PITR gate, restore drill;
- structured dashboards, alerts, budgets, and kill switches.

### C. Marketplace completion

- cursor/detail endpoints;
- UUID/amount/state cleanup;
- unique saves and idempotent contact;
- Chat-owned conversation creation;
- report/block/prohibited category policy;
- moderation/admin tools;
- safety messaging and analytics.

### D. Client launch

- align web and Android contracts;
- disable Stories;
- gate video/events/games;
- Play internal/closed/production tracks;
- crash and performance instrumentation;
- accessibility, low-network, update, and rollback QA.

### E. Campus operations

- domain/admin/official community bootstrap;
- content/resource/Marketplace seed plan;
- ambassador training;
- moderation and support roster;
- university escalation and incident communications.

## 5. Build Order

1. architecture/SRS/LLD approval;
2. fresh Data Connect/Cloud SQL provisioning and seed tooling;
3. auth/tenant/RLS hardening;
4. direct media upload and storage adapter;
5. Marketplace V2 application layer and schema;
6. chat/contact integration;
7. outbox, notifications, rate limits, idempotency;
8. moderation/admin;
9. web/Android compatibility and feature flags;
10. backup/load/security/incident tests;
11. first-campus rollout;
12. second-campus tenant isolation;
13. 20,000–30,000-user gate.

## 6. Milestones

### M0 — Architecture complete

- canonical docs updated;
- scope and launch gates approved;
- cost budget approved.

### M1 — Production foundation

- one writer per domain;
- Cloud Run production topology;
- RLS and direct uploads;
- no ephemeral fallback;
- backups and outbox.

### M2 — Marketplace-safe MVP

- target API/schema complete;
- moderation and Chat integration;
- prohibited content and safety policy;
- web/Android contract tests.

### M3 — Closed beta

- 50–150 trusted testers;
- seven stable days;
- operational metrics and support ready.

### M4 — First university

- 500–1,500 closed beta, then 3,000–7,000 broad release;
- seeded utility;
- phase gates met.

### M5 — Multi-university

- second and third tenant;
- 20,000–30,000 users;
- PITR and media cost path enabled;
- 30-day SLO compliance.

## 7. Decision Log

- modular monolith remains correct for the initial target;
- Cloud Run is the only production backend; no Vercel backend adapter remains;
- Firebase Auth remains; SQL Connect/Cloud SQL PostgreSQL is canonical;
- dual-write is forbidden;
- R2 is the user-media store and egress budget is a first-class metric;
- Marketplace moves into MVP;
- Stories move out of initial launch;
- broad video waits for capacity and egress evidence;
- Marketplace does not move money;
- tenant isolation and moderation gates are product requirements, not later hardening.

## 8. Top Risks

| Risk | Control |
|---|---|
| database divergence | one production writer and no runtime fallback |
| cross-tenant data leak | backend authorization + RLS + two-tenant tests |
| media bill spike | direct upload, video flags, storage adapter, budget alerts |
| unsafe Marketplace | category policy, rate limits, report/block, moderation, no payments |
| empty launch | seeded communities/resources/market and ambassadors |
| realtime loss after horizontal scale | managed fanout and durable reconciliation |
| notification loss | transactional outbox and managed retries |
| Android release lock-in | staged Play tracks, feature flags, previous-version compatibility |
| scope creep | Stories/video/payments remain explicitly gated |
| weak operations | launch commander, runbook, dashboards, restore/incident drills |

## 9. Documents That Govern Execution

- [HLD](../architecture/HLD.md)
- [System LLD](../architecture/LLD.md)
- [SRS](./SRS.md)
- [MVP Phased Rollout](./MVP_PHASED_ROLLOUT.md)
- [Marketplace LLD](../lld/phase-1/MARKETPLACE_SERVICE_LLD.md)
- [Capacity and Cost Model](../operations/CAPACITY_AND_COST_MODEL.md)
- [Launch Runbook](../operations/MVP_LAUNCH_RUNBOOK.md)
