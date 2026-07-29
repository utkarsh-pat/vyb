# Fresh Stack Deployment Verification

Verified: 2026-07-29

## Deployed production resources

- Google project: `vybnet` (`850600134378`)
- Artifact image:
  `asia-south1-docker.pkg.dev/vybnet/vyb/vyb-backend@sha256:583999c960b027fcc87dd5aa4c026e4c97fea4571a76575dfb15e5efc4989e86`
- Cloud Run service/revision: `vyb-backend` / `vyb-backend-00007-gl7`
- Cloud Run URL:
  `https://vyb-backend-850600134378.asia-south1.run.app`
- Vercel team/project: `vybnet` / `vyb`
- Vercel production deployment:
  `9V5MoDzfWF2sMNL7RWiW6crRU5mk` (`989e62c`, `READY`)
- Canonical web origin: `https://www.vybnet.app`
- R2 bucket: `vyb-media-production` (private, APAC placement)

## Passed evidence

- Cloud Run returned HTTP 200 from `/health`; all ten module health entries
  reported `ok`.
- `/v1/client-shell` returned the `kiet` launch campus.
- production CORS returned the exact allowed origin for both planned web
  domains.
- Cloud Run uses the dedicated runtime service account, 1 vCPU, 512 MiB,
  concurrency 40, min 0, and launch max 3. The documented hard ceiling remains
  10 and must only be raised after measured load evidence.
- Vercel build completed all 77 Next.js routes and reached `READY`.
- Protected provider checks returned HTTP 200 for `/`,
  `/api/auth/session`, and `/api/notifications/vapid-public-key`.
- The production login page renders Google and email/password controls without
  requiring Firebase Storage.
- Firebase Auth authorizes `vyb-vybnet.vercel.app`, `vybnet.app`, and
  `www.vybnet.app`.
- Firebase web sessions are minted by Cloud Run and returned to Vercel for an
  HttpOnly cookie; no Firebase service-account key is stored in Vercel.
- Vercel accepted the `vybnet.app` ownership move from
  `utkarshpatelcse's projects` into the CEO-owned `vybnet` team.
- The apex hostname is attached to the fresh `vybnet/vyb` production
  environment. The apex currently returns a Vercel redirect to
  `www.vybnet.app`, and the `www` login page returns HTTP 200 with the Vyb
  production login shell.
- Name.com publishes both Vercel ownership TXT values at `_vercel.vybnet.app`;
  public DNS resolution and Vercel verification passed for the apex and `www`.
- Deployment `6Nyci76jppPoLwXxERVAJ7tVAC3A` was promoted after domain
  verification so the custom hostnames were assigned to production.
- `https://vybnet.app/login` returns a permanent redirect to
  `https://www.vybnet.app/login`; the `www` login and unauthenticated session
  endpoints return HTTP 200.
- Vercel is connected to the public `utkarsh-pat/vyb` repository; collaborator
  `utkarshpat` has effective `WRITE` permission and successfully pushed the
  production deploy trigger.
- R2 credentials were generated fresh under the canonical owner. Vercel stores
  all five R2 values as sensitive environment variables.
- R2 remains private. Web media reads use the same-origin
  `https://www.vybnet.app/api/media/...` proxy rather than the rate-limited
  `r2.dev` endpoint.
- Cloud Run revision `vyb-backend-00006-46r` mounted both R2 secrets through
  secret-scoped IAM bindings, became ready, and received 100% traffic.
- The Cloud Run runtime identity has `roles/firebasedataconnect.dataAdmin` for
  connector query/mutation execution. Firebase Auth remains read-only except
  for the custom `vybSessionIssuer` role, which contains only
  `firebaseauth.users.createSession`.
- Direct R2 write, read, and delete operations passed with the production
  bucket-scoped token; the smoke object was deleted after verification.
- The production media proxy returned the expected JSON 404 for a missing
  object, proving that the deployed route and R2 configuration are active.
- PR #1 was merged into `main`. Vercel correctly blocked the
  collaborator-authored merge commit on Hobby/private-repository rules, then
  deployed the same merged source successfully from owner-authored commit
  `37b0894` without a Pro upgrade.
- On 2026-07-29 the repository visibility was intentionally changed to public
  after a high-confidence current-tree and history secret preflight. This
  removes Vercel Hobby's private-repository collaborator-author restriction;
  the earlier blocked deployment remains recorded as historical evidence.
- Collaborator `utkarshpat` then authored and pushed commit `7497130`.
  Vercel built it as production deployment `7GzALFwEm7wy9FNP8Mr4R21r51R3`
  and reached `READY`, directly verifying that the author block is removed.
- A migrated email/password account signed in successfully on the production
  domain, redirected to `/home`, and rendered the authenticated campus
  navigation, feed, and profile shell.
- The duplicate active `kiet.edu` tenant mapping was repaired transactionally.
  The canonical `kiet` tenant
  (`56734232-6095-4000-8000-000000000001`) is now the sole active mapping.
  Post-repair Data Connect verification returned 21 memberships, five
  completed profiles, eight published feed posts, and 13 published vibes.
- Campus domain resolution now queries for up to two matches and fails closed
  when more than one active mapping exists, preventing a future duplicate from
  silently selecting an empty or incorrect tenant.
- Cloud Build `cc89450c-d4f1-4ac4-860a-c5c9478d9606` deployed commit
  `989e62c` as revision `vyb-backend-00007-gl7` with 100% traffic. The
  ten-module health check returned HTTP 200.
- An authenticated migrated KIET account was reverified after the deployment:
  `/home` rendered the canonical campus feed and `/vibes` rendered migrated
  vibe cards under `KIET Group of Institutions`.
- The legacy Firebase bucket contained 160 objects (448,514,602 bytes). All
  160 were copied to `vyb-media-production`, source-MD5 verified locally, and
  destination size/checksum metadata verified with zero failures.
- A transaction rewrote all production media references: 101 canonical R2
  proxy URLs remain, zero Firebase media URLs remain, and zero referenced
  records lack a storage path. One already-removed demo record whose source
  object never existed was cleared instead of fabricating media.
- Production JPEG and MP4 proxy HEAD requests returned HTTP 200 with the
  expected content length, MIME type, and immutable cache policy.
- Active event uploads and social upload planning are now R2-only. The obsolete
  browser-to-Firebase upload branch and Firebase Admin Storage integration were
  removed. `pnpm --filter @vyb/web check` and the 77-route production build
  passed after the cutover.
- Artifact Registry cleanup removed stale digest
  `sha256:96bf69dea0a136271fe7fb64c4d42a3a9abbfaba1b243a8eb4bf030721cb52ec`.
  The current image and one verified rollback remain, and immutable tags were
  restored immediately after deletion.

## Secret-remediation evidence

- GitHub alert #1 identified the Firebase Android client API key in
  `google-services.json`.
- The Android key is now restricted to `social.vyb.app` and the registered
  debug SHA-1. SHA-1 and SHA-256 are registered on the fresh Firebase app.
- The browser key has explicit HTTP-referrer restrictions.
- `google-services.json` was removed from repository tracking in commit
  `ddb9b46` and is regenerated locally through the Firebase CLI.
- GitHub alert #1 is dismissed with the remediation recorded in its audit
  timeline.

## Open gates

1. Verify the redirected CEO Google-authenticated production page.
2. Map and smoke-test `api.vybnet.app`.
3. Smoke-test a newly uploaded event asset and its delete path after the web
   deployment.
4. Configure R2 lifecycle/orphan cleanup after application-level upload/delete
   smoke tests.
5. Run backup/restore, cross-tenant isolation, media, Marketplace, notification,
   and load-test gates.

The legacy project backup is complete. Destructive legacy-project cleanup is
performed only after the fresh deployment and new uploads pass; the current
`vybnet` project and `utkarsh-pat/vyb` repository are never cleanup targets.
