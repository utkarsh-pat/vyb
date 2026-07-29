# Fresh Stack Deployment Verification

Verified: 2026-07-29

## Deployed production resources

- Google project: `vybnet` (`850600134378`)
- Artifact image:
  `asia-south1-docker.pkg.dev/vybnet/vyb/vyb-backend@sha256:9f368e0530ad838a2c68c96385937c69d1f05a08ee1083db94345ae7840b5cc5`
- Cloud Run service/revision: `vyb-backend` / `vyb-backend-00004-26t`
- Cloud Run URL:
  `https://vyb-backend-850600134378.asia-south1.run.app`
- Vercel team/project: `vybnet` / `vyb`
- Vercel production alias: `https://vyb-vybnet.vercel.app`

## Passed evidence

- Cloud Run returned HTTP 200 from `/health`; all ten module health entries
  reported `ok`.
- `/v1/client-shell` returned the `kiet` launch campus.
- production CORS returned the exact allowed origin for both planned web
  domains.
- Cloud Run uses the dedicated runtime service account, 1 vCPU, 512 MiB,
  concurrency 40, min 0, and max 10.
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

1. Sign in to the Name.com registrar account and add the exact `_vercel` TXT
   ownership record currently shown by the Vercel project. Remove the temporary
   TXT only after Vercel reports the domain as verified.
2. Attach/verify `www.vybnet.app` on the fresh project and prove that both
   public hostnames resolve to the fresh deployment rather than a legacy
   project association.
3. Select the CEO Google account once in the open production auth chooser and
   verify the redirected authenticated page.
4. Activate R2 with owner-provided billing details, then create bucket, token,
   CORS, lifecycle, and media-domain configuration.
5. Map and smoke-test `api.vybnet.app`.
6. Run backup/restore, cross-tenant isolation, media, Marketplace, notification,
   and load-test gates.

Legacy resources remain intact until every gate passes and the fresh stack has
seven clean operating days.
