# Marketplace-first MVP Phased Rollout

Last updated: 2026-07-29

## Launch scope

Included: verified university login, profile, campus communities, text/image feed, Marketplace browse/create/edit/save/contact/sold/report, one-to-one chat, resources, notifications, moderation, analytics, remote kill switches, web/PWA, and Android.

Excluded: Stories, long video, payments/escrow, shipping, cross-campus public trading, live streaming, recommendation ML, anonymous posting, group chat, and complex gamification.

Production starts with an empty canonical database. Legacy test data is not a launch dependency.

## Rollout sequence

| Phase | Audience | Typical duration | Exit gate |
|---|---:|---:|---|
| 0. Foundation | engineering only | 2–4 days | fresh project, schema, backups, secrets, media, alerts, and direct URLs verified |
| 1. Staff dogfood | 20–50 | 3–5 days | no P0/P1; auth, tenant, listing, chat, report, and rollback flows pass |
| 2. Closed campus beta | 200–500 at university 1 | 7 days | crash-free above 99.5%, API errors below 1%, moderation under 24 hours |
| 3. First campus launch | 2,000–5,000 | 7–14 days | read p95 below 500 ms, DB headroom above 30%, abuse manageable |
| 4. University 2 | 8,000–15,000 cumulative | 14 days | cross-tenant security suite and restore drill pass |
| 5. University 3 | 20,000–30,000 cumulative | ongoing | SLO, retention, support, and cost per DAU remain within budget |

## Technical launch order

1. Verify ownership, billing, credits, project IDs, GitHub repository, and local backup record.
2. Create Firebase web and Android apps; configure Auth providers and authorized domains.
3. Provision Data Connect/Cloud SQL in `asia-south1`; compile and deploy schema/connectors.
4. Seed only required tenants, verified domains, official communities, categories, and admin account.
5. Create R2 bucket, scoped token, CORS, public media domain, lifecycle, and upload/read/delete smoke test.
6. Create Secret Manager values and deploy Cloud Run with min 0, max 10,
   concurrency 80, and no initial traffic.
7. Run health, auth, two-tenant isolation, Marketplace, chat, notification, and media tests.
8. Deploy `apps/web` from `utkarsh-pat/vyb` on the new Vercel ownership.
9. Attach `vybnet.app` and `www.vybnet.app`; attach `api.vybnet.app` to Cloud Run.
10. Enable staff accounts, then 5% / 25% / 50% / 100% cohorts through Remote Config.
11. Publish Android through internal, closed, then production Play tracks.
12. After seven clean days and verified backups, delete superseded old-account resources.

## Marketplace acceptance gates

- Tenant-scoped browse/search/detail/create/edit/delete/save/contact/sold/report.
- Server-side authorization for every mutation.
- No private contact leak in list/search/log/analytics responses.
- R2 upload intent, object read, deletion, quota, and orphan cleanup are observable.
- Abuse limits, block/report, prohibited categories, and moderation tools are enabled.
- No payment collection; in-person exchange safety guidance is shown.

## Rollback

- Application: route Cloud Run traffic to the previous healthy revision.
- Feature: disable Marketplace writes or media using Remote Config.
- Frontend: promote the previous Vercel deployment.
- Database: restore the verified Cloud SQL backup only when forward recovery is unsafe.
- Domain: retain provider deployment URLs until custom-domain verification is complete.
