# Vyb MVP Launch Runbook

Last updated: 2026-07-29

## Ownership gate

- [x] Google/Firebase project ID is `vybnet`.
- [x] Project owner is `ceoutkarshpatel@gmail.com`.
- [x] Billing account `015FFD-11FC57-660C55` is linked.
- [x] GitHub repository is private `utkarsh-pat/vyb`; `utkarshpat` has verified
  `WRITE`/push access.
- [x] New Vercel team `vybnet` and project `vyb` ownership verified under the CEO account.
- [x] `vybnet.app` ownership moved from the legacy Vercel scope to the CEO-owned `vybnet` team.
- [x] R2 ownership, bucket, token scope, and recovery owner are recorded.
- [x] No new production resource is created in an old shared account.

## Foundation

- [x] Firebase web and Android apps created; config files contain project `vybnet`.
- [x] Email/password and Google sign-in enabled; Firebase Auth authorizes the stable Vercel alias and all planned production web domains.
- [x] Data Connect service `vyb`, instance `vyb-net`, and database `vyb` provisioned in `asia-south1`.
- [x] `pnpm dc:compile`, schema compatibility check, migration, and connector deployment pass.
- [x] Legacy SQL data verified: 2,465 rows across 39 tables with zero table mismatches.
- [x] Legacy Auth imported: six matching UIDs/providers with Firebase Scrypt parameters.
- [ ] Automated database backup enabled and restore test recorded.
- [x] Private R2 bucket and bucket-scoped object read/write token configured.
- [x] Same-origin private-R2 media proxy configured; public `r2.dev` access is
  intentionally disabled.
- [ ] R2 lifecycle/orphan cleanup and media upload/read/delete smoke test recorded.
- [x] Cloud Run secrets, launch max 3 (approved hard ceiling 10), concurrency
  40, public health endpoint, and budget guardrail configured.
- [x] ₹2,000 monthly budget alerts at 50%, 80%, and 100%.
- [ ] Stories, video, payments, events, and games flags off unless explicitly approved.

## Verification

- [ ] Auth issuer/audience, migrated-password sign-in, migrated-Google sign-in, and revoked/expired token tests pass.
- [ ] Two users in separate tenants cannot enumerate, fetch, contact, or infer each other's data.
- [ ] Marketplace create/edit/browse/save/contact/sold/report passes on web and Android.
- [ ] Upload intent/read/delete/quota/orphan-cleanup passes.
- [ ] Chat, FCM, notification retry, moderation, and audit flows pass.
- [ ] Load test demonstrates 300–800 concurrent sessions with database headroom above 30%.
- [x] Failed R2 configuration revision received 0% traffic and the previous
  healthy Cloud Run revision remained at 100%.
- [x] Secret-scoped IAM bindings applied; Cloud Run revision
  `vyb-backend-00006-46r` is healthy at 100% traffic.
- [x] R2 production token passed write/read/delete; the smoke object was deleted.
- [x] Vercel production deployment `9UQa2cvY4vYARvBXfcosaFvSUtjX` is ready and
  `/api/media/...` reaches private R2.
- [x] GitHub secret-scanning alert #1 remediated and dismissed; Android config is no longer tracked.

## Deployment

1. Deploy Data Connect schema and connectors.
2. Deploy Cloud Run revision with no traffic.
3. Run health, auth, isolation, Marketplace, chat, notification, and R2 smoke tests.
4. Route 5%, then 25%, 50%, and 100% when gates remain green.
5. Deploy web and verify provider URL before custom-domain attachment.
6. Switch `api.vybnet.app`, then `vybnet.app` and `www.vybnet.app`.
7. Publish Android internal track using `https://api.vybnet.app/`.

## Incident thresholds

- P0: data leak, auth bypass, secret exposure, or irreversible corruption — disable writes and rollback immediately.
- P1: login, listing, contact, or chat unavailable for more than 10 minutes — rollback revision.
- Cost incident: forecast exceeds budget by 25% — disable heavy media, hold rollout, reduce maximum scaling, and inspect egress/logging.

## Legacy cleanup

Delete old Firebase/Google Cloud/Vercel resources only after:

- [x] new domains serve verified production;
- [ ] new auth and data paths pass;
- [ ] backup and rollback evidence exists;
- [ ] seven clean operating days complete;
- [ ] exact old targets are re-verified immediately before deletion.

Current cutover blockers:

- Complete the migrated Google sign-in and authenticated-page smoke test.
- Complete application-level media upload/delete, tenant-isolation,
  backup/restore, and load-test gates before legacy deletion.
