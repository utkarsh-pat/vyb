# Vyb Documentation Hub

Owner: Architecture and Product
Last Updated: 2026-08-02
Status: Marketplace-first MVP documentation set

## Canonical Review Order

1. [High Level Design](./architecture/HLD.md)
2. [System Low Level Design](./architecture/LLD.md)
3. [Software Requirements Specification](./product/SRS.md)
4. [Master Plan](./product/MASTER_PLAN.md)
5. [MVP Phased Rollout](./product/MVP_PHASED_ROLLOUT.md)
6. [Capacity and Cost Model](./operations/CAPACITY_AND_COST_MODEL.md)
7. [Google Cloud Cost Audit](./operations/GOOGLE_CLOUD_COST_AUDIT_2026-07-28.md)
8. [MVP Launch Runbook](./operations/MVP_LAUNCH_RUNBOOK.md)
9. [Firebase Migration Verification](./operations/FIREBASE_MIGRATION_VERIFICATION_2026-07-29.md)
10. [Fresh Production Ownership](./operations/FRESH_ACCOUNT_OWNERSHIP.md)
11. [Production Infrastructure Specification](./operations/PRODUCTION_INFRASTRUCTURE_SPEC.md)
12. [Fresh Stack Deployment Verification](./operations/FRESH_STACK_DEPLOYMENT_VERIFICATION_2026-07-29.md)
13. [Backend Runtime and Cost Guardrails](./operations/BACKEND_RUNTIME_GUARDRAILS.md)
14. [Android/Web Quality and Parity Audit](./qa/QUALITY_PARITY_AUDIT_2026-07-30.md)
15. [Android Release Runbook](./process/ANDROID_RELEASE_RUNBOOK.md)
16. [Client Platform Strategy](./architecture/CLIENT_PLATFORM_STRATEGY.md)
17. relevant module LLD under `lld/phase-1`;
18. matching API contract, query review, and ADR;
19. [Complete System Test Cases](./qa/SYSTEM_TEST_CASES.md).
20. [Android and PWA Parity Release Review](./qa/ANDROID_PWA_PARITY_RELEASE_2026-08-02.md).

## Architecture Decisions

- [ADR-001: Hosting Topology](./architecture/ADR_001_PHASE1_HOSTING_TOPOLOGY.md)
- [ADR-002: Story Music](./architecture/ADR_002_STORY_MUSIC_SEARCH_AND_CLIENT_EXPORT.md) — implementation retained, launch deferred
- [ADR-003: Chat Realtime and E2EE](./architecture/ADR_003_PHASE1_CHAT_REALTIME_AND_E2EE.md)
- [ADR-004: MVP Data and Media Topology](./architecture/ADR_004_MVP_DATA_AND_MEDIA_TOPOLOGY.md)

## Phase 1 LLDs

- [Identity](./lld/phase-1/IDENTITY_SERVICE_LLD.md)
- [Campus](./lld/phase-1/CAMPUS_SERVICE_LLD.md)
- [Community Connect](./lld/phase-1/COMMUNITY_CONNECT_SURFACE_LLD.md)
- [Social](./lld/phase-1/SOCIAL_SERVICE_LLD.md)
- [Marketplace](./lld/phase-1/MARKETPLACE_SERVICE_LLD.md)
- [Chat](./lld/phase-1/CHAT_SERVICE_LLD.md)
- [Resources](./lld/phase-1/RESOURCES_SERVICE_LLD.md)

## Documentation Rules

- requirement changes update the SRS;
- architecture changes update the HLD and require an ADR when an external service or major boundary changes;
- implementation design updates the system/module LLD;
- public API and hot query changes update their contract/review;
- phase, scope, or progress changes update the Master Plan and rollout document;
- production operating changes update the launch/deployment runbooks;
- current code and target design must be labeled separately;
- if documents conflict, the most recently updated canonical document in the review order wins until the conflict is resolved.

## Launch Scope Note

Marketplace is MVP. Stories/story music are implemented but disabled for initial public launch. Vibes/video, events, and games are gated. Wallet, payments, escrow, anonymous posting, and group chat are deferred.
