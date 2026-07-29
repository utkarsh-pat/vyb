# Fresh Production Ownership Record

Last verified: 2026-07-29

## Canonical owners

| Surface | Canonical owner/account | Canonical resource |
|---|---|---|
| Google Cloud and Firebase | `ceoutkarshpatel@gmail.com` | project `vybnet`, number `850600134378` |
| Cloud Billing | `ceoutkarshpatel@gmail.com` | account `015FFD-11FC57-660C55` |
| GitHub | `utkarsh-pat` | public repository `utkarsh-pat/vyb` |
| Vercel | `ceoutkarshpatel@gmail.com` | team `vybnet`, project `vyb` |
| Product/app name | Vyb | package `social.vyb.app` |
| Public domain | new-account deployment | `vybnet.app`, `www.vybnet.app`, `api.vybnet.app` |

## Verified resources

- Firebase project attached to Google Cloud project `vybnet`.
- Firebase web app:
  - display name: `vyb`
  - app ID: `1:850600134378:web:b0736b5c5bf0386526b993`
- Firebase Android app:
  - display name: `vyb`
  - package: `social.vyb.app`
  - app ID: `1:850600134378:android:525ba14313609c8f26b993`
- Data Connect service: `vyb`, region `asia-south1`.
- Cloud SQL instance: `vyb-net`, database `vyb` (provisioned through SQL Connect).
- Initial tenant: `kiet` (`e4e861716cca437ca1dca88a78dac43c`) with verified domain `kiet.edu`.
- Initial communities: `campus`, `marketplace`, and `resources`.
- Cloud Run service `vyb-backend`, current healthy revision
  `vyb-backend-00004-26t`.
- Vercel provider URL `https://vyb-vybnet.vercel.app`.
- `vybnet.app` ownership is now in the CEO-owned `vybnet` Vercel team and the
  apex and `www` hostnames are verified on project `vyb`. The apex permanently
  redirects to `www`, which serves the promoted production deployment.
- Billing credit at verification: ₹28,320.75, valid through 2026-10-28.

## Account isolation policy

- Do not create, deploy, or bill new production resources in `utkarshpatelcse@gmail.com`, `utkarshp2003@gmail.com`, or any other shared profile.
- The old stack is read-only legacy during verification.
- Do not copy old OAuth tokens, service-account keys, provider tokens, or environment secrets into the new stack.
- Generate new credentials in the canonical owner account and rotate any value previously exposed to a shared account.
- The canonical super-admin is always `ceoutkarshpatel@gmail.com`; `VYB_SUPER_ADMIN_EMAILS` adds any future approved admins without risking an owner lockout.

## Deletion gate

Before deleting any old resource, record:

1. exact project/team/resource ID;
2. final local backup path and checksum;
3. new deployment health and tenant-isolation evidence;
4. domain and authentication verification;
5. rollback decision and approver.

Deletion is permitted only after the fresh stack has completed seven clean operating days. Deleting an old project must never be used as a way to test whether the new stack is independent.
