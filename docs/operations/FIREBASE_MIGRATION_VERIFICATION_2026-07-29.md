# Firebase Migration Verification

Verified: 2026-07-29

## Scope

Source was the legacy Firebase project `vybnet-e2242`. Target is the fresh
Firebase project `vybnet`, owned by `ceoutkarshpatel@gmail.com`. Supabase was
not used as a migration source and is not part of the production runtime.

## Target foundation

- Firebase project number: `850600134378`.
- Web app: `1:850600134378:web:b0736b5c5bf0386526b993`.
- Android app: `1:850600134378:android:525ba14313609c8f26b993`.
- Android package: `social.vyb.app`.
- SQL Connect service: `vyb` in `asia-south1`.
- Cloud SQL instance/database: `vyb-net` / `vyb`.
- Connectors: identity, campus, social, chat, resources, moderation,
  marketplace, and connect.
- Authentication providers: Email/Password and Google.

The local web environment, `.firebaserc`, Android `google-services.json`, and
generated SQL Connect SDKs point to the target project.

## SQL migration evidence

The source and target SQL schemas each contained 464 inspected application
columns and had zero schema differences. Before import, the target's five
bootstrap rows were exported as a rollback artifact.

All 39 application tables were exported from the legacy SQL Connect database
and imported into the target while preserving primary keys and foreign keys.
The migration moved 2,465 rows:

| Domain | Migrated rows |
| --- | ---: |
| Tenancy and identity | 53 |
| Communities | 27 |
| Games and connect | 26 |
| Social posts, stories and interactions | 418 |
| Chat | 244 |
| Marketplace | 31 |
| Moderation | 2 |
| Audit and activity | 1,664 |
| Total | 2,465 |

Post-import verification canonicalized every source and target row as JSON.
Result: 2,465 verified rows and zero table mismatches.

## Firebase Authentication evidence

- Six legacy users were exported and imported into the target project.
- Source and target UID sets match.
- Source and target provider sets match.
- Three password users retained their imported Firebase Scrypt records.
- The legacy Firebase Scrypt signer key, salt separator, rounds, and memory
  cost were used during the final import.
- Firebase CLI intentionally omits password hashes from a subsequent export
  when the imported account has a non-zero password-hash version. Import
  success, UID/provider parity, and configured provider state are therefore
  the retained verification evidence; secrets are not committed.

## Protected local evidence

Raw exports, per-table import variables, pre/post migration snapshots, and
password-hash parameters are stored only under `.local-backups/`. That
directory is git-ignored and must never be uploaded, committed, or copied into
deployment artifacts.

## Deletion gate

Do not delete `vybnet-e2242` until:

1. Cloud Run, R2, Vercel and all three production domains pass smoke tests;
2. at least one migrated password user and one migrated Google user complete a
   real sign-in test on the target;
3. a target SQL backup and restore rehearsal are recorded;
4. seven clean operating days complete;
5. the exact legacy project and billing impact are re-verified immediately
   before deletion.
