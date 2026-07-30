# Cloud Run Backend Deployment

Production backend: Cloud Run in `asia-south1`. Vercel is frontend-only.

## Required configuration

- Runtime service account with minimum Firebase/Data Connect, Cloud Tasks and Secret Manager access.
- Secret bindings for `VYB_SESSION_SECRET` and all R2 credentials.
- Plain configuration for project/service/location, CORS origins and public R2 base URL.
- `min-instances=0`, `max-instances=10`, `concurrency=80`, initial memory 512 MiB.
- Cloud SQL/Data Connect connection pool maximum 5 per instance.

Never ship service-account JSON, `.env`, Supabase credentials or R2 secrets in the image.
Before every remote build, run `gcloud meta list-files-for-upload` and confirm
that `.local-backups`, `.env`, `.vercel`, build outputs, and local dependencies
are excluded by `.gcloudignore` and `.dockerignore`.

## Release

1. Run `pnpm install --frozen-lockfile`.
2. Run `pnpm dc:compile`, backend syntax/tests and web type-check.
3. Build an immutable image tagged with the Git SHA.
4. Deploy a no-traffic revision. `cloudbuild.backend.yaml` enforces
   `--no-traffic`; never remove that guardrail from the automated build.
5. Verify `/health`, authenticated identity, tenant isolation, marketplace and R2 upload/read.
6. Shift 5%, 25%, 50%, then 100% traffic while watching error rate, p95 and DB connections.
7. Retain the active and previous three images; cleanup untagged images older than 14 days.

## Rollback

Route traffic to the previous healthy revision. Database schema changes must be backward-compatible for at least one release. Do not restore Supabase dual writes.
