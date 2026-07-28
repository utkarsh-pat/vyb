# Phase 1 LLD Index

Owner: Architecture Team
Last Updated: 2026-07-28
Change Summary: Added the Marketplace module, Chat, and Community Connect to the MVP index and aligned build order with the launch plan.

## Module LLDs

- `IDENTITY_SERVICE_LLD.md`
- `CAMPUS_SERVICE_LLD.md`
- `SOCIAL_SERVICE_LLD.md`
- `MARKETPLACE_SERVICE_LLD.md`
- `CHAT_SERVICE_LLD.md`
- `COMMUNITY_CONNECT_SURFACE_LLD.md`
- `RESOURCES_SERVICE_LLD.md`

## Build Order

1. Identity Module
2. Campus Module
3. Social Module
4. Marketplace Module
5. Chat/Marketplace Contact Integration
6. Resources Module
7. Notifications and Moderation hardening

This order is intentional because authentication and membership context are prerequisites for all downstream modules.
