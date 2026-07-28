# Google Cloud Cost and Credit Audit — Fresh Account

Audit refreshed: 2026-07-29

## Verified account state

- Owner: `ceoutkarshpatel@gmail.com`.
- Google Cloud/Firebase project: `vybnet`.
- Project number: `850600134378`.
- Billing account: `015FFD-11FC57-660C55`.
- The billing account's linked-projects table contains `vybnet`.
- Available Free Trial credit at verification: ₹28,320.75 (100% remaining).
- Credit validity: 2026-07-28 through 2026-10-28.
- Firebase Data Connect service: `vyb`, region `asia-south1`.
- Cloud SQL instance: `vyb-net`, created through SQL Connect's no-cost-trial provisioning path.

## Interpretation

The verified credit is time-limited. It should cover the initial Google infrastructure pilot under the documented limits, but it is not a permanent subsidy and does not create a hard spending cap. Budgets only alert.

Google One consumer storage and AI Pro benefits are not application object storage. Only credits visible on the linked Cloud Billing account are included in the production forecast.

## Cost decision

- Keep Data Connect/Cloud SQL as the only canonical PostgreSQL database.
- Run one Cloud Run backend with min instances 0 and max instances 10.
- Keep Vercel frontend-only.
- Use R2 for new user media.
- Keep Stories, long video, payments, and always-on auxiliary services disabled.
- A ₹2,000 monthly Google Cloud budget named `vybnet-monthly-guardrail`
  is active with 50%, 80%, and 100% alerts.

## Legacy note

The previous project `vybnet-e2242` and its billing are not part of the new production cost model. They remain deletion candidates only after fresh-stack verification. No new workload may be created there.

The verified SQL and Auth migration is documented in
[Firebase Migration Verification](./FIREBASE_MIGRATION_VERIFICATION_2026-07-29.md).
The old project remains online only as a rollback source until every deletion
gate in that document passes.
