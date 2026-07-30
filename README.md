# Vyb

Verified, multi-tenant campus network with a responsive Next.js web/PWA, native Android app, and one modular Node backend.

## MVP Direction

The launch MVP is utility-first:

- verified university identity and official communities;
- text/image feed;
- campus Marketplace;
- one-to-one chat;
- resources;
- notifications, moderation, and admin controls;
- web/PWA and Android.

Stories are disabled for the initial public rollout. Video/Vibes, events, and games are feature-gated. Payments, wallet, escrow, anonymous posting, and group chat are out of scope.

The target is 20,000–30,000 registered users across two to three universities.

Canonical production ownership is `ceoutkarshpatel@gmail.com`, Google/Firebase
project `vybnet`, and GitHub repository `utkarsh-pat/vyb`. See
[Fresh Production Ownership](./docs/operations/FRESH_ACCOUNT_OWNERSHIP.md).

## Architecture

- `apps/web`: Next.js 16 web/PWA on Vercel;
- `apps/mobile`: Kotlin/Jetpack Compose Android app;
- `apps/backend`: modular-monolith Node backend on Cloud Run;
- Firebase Auth for identity and FCM/Crashlytics;
- Firebase SQL Connect backed by Cloud SQL PostgreSQL as the canonical transactional store;
- Cloudflare R2 for new user-uploaded media;
- PostgreSQL transactional outbox plus managed task delivery;
- strict tenant isolation in backend authorization and SQL Connect operations.

There is one production database writer and one production backend. Local JSON stores are development-only and must never be enabled on Cloud Run.

## Start

```bash
pnpm install
pnpm dev
```

Useful checks:

```bash
pnpm check
pnpm build
pnpm test:e2e
pnpm --filter @vyb/backend test:notifications
```

Android:

```powershell
Set-Location apps/mobile
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

On macOS/Linux, run the same tasks with `./gradlew`. The complete signing,
distribution, update, and rollback procedure is documented in the
[Android Release Runbook](./docs/process/ANDROID_RELEASE_RUNBOOK.md).

## Documentation

Start here:

1. [High Level Design](./docs/architecture/HLD.md)
2. [System Low Level Design](./docs/architecture/LLD.md)
3. [Software Requirements](./docs/product/SRS.md)
4. [Marketplace Module LLD](./docs/lld/phase-1/MARKETPLACE_SERVICE_LLD.md)
5. [MVP Phased Rollout](./docs/product/MVP_PHASED_ROLLOUT.md)
6. [Capacity and Cost Model](./docs/operations/CAPACITY_AND_COST_MODEL.md)
7. [MVP Launch Runbook](./docs/operations/MVP_LAUNCH_RUNBOOK.md)
8. [Production Infrastructure Specification](./docs/operations/PRODUCTION_INFRASTRUCTURE_SPEC.md)

The documentation hub is [docs/README.md](./docs/README.md).

## Production Topology

- Vercel hosts `apps/web`;
- Cloud Run hosts the only public custom backend;
- there is no Vercel backend deployment;
- secrets use provider environment/Secret Manager and service identity;
- no service-account JSON or R2 secret belongs in a client or commit.

Deployment instructions are in [CLOUD_RUN_BACKEND_DEPLOYMENT.md](./docs/process/CLOUD_RUN_BACKEND_DEPLOYMENT.md).
