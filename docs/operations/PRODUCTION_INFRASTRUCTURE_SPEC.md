# Production Infrastructure Specification

Last updated: 2026-07-29

## Resource naming

| Resource | Name |
|---|---|
| Google/Firebase project | `vybnet` |
| Data Connect service | `vyb` |
| Cloud SQL instance | `vyb-net` |
| PostgreSQL database | `vyb` |
| Cloud Run service | `vyb-backend` |
| Cloud Run service account | `vyb-backend@vybnet.iam.gserviceaccount.com` |
| Artifact Registry repository | `asia-south1-docker.pkg.dev/vybnet/vyb` |
| Vercel project | `vyb` |
| R2 bucket | `vyb-media-production` |

All Google resources use `asia-south1` when supported. Media delivery is global through the R2 public/custom domain.

## Cloud Run

Initial limits:

```text
cpu: 1
memory: 512Mi
min-instances: 0
max-instances: 10
concurrency: 80
timeout: 3600s
port: 8080
ingress: all (application auth enforced)
```

Current production URL:
`https://vyb-backend-850600134378.asia-south1.run.app`. The custom
`api.vybnet.app` mapping is a cutover step, not a second backend.

Use a dedicated runtime service account with only Data Connect client, Firebase Auth/FCM access, Secret Manager accessor for named secrets, Cloud Tasks enqueue, and necessary logging/monitoring permissions. Do not grant Owner or Editor.

## Cloud SQL and Data Connect

- PostgreSQL through Firebase Data Connect.
- 10 GB initial SSD, single zone, automated daily backup before external beta.
- Pool maximum 5 connections per Cloud Run instance.
- No public direct database access for application clients.
- Schema migration is executed from reviewed repository state.
- PITR is enabled before payments or broad university operational dependency.

## Vercel

- Import `utkarsh-pat/vyb`.
- Root directory: `apps/web`.
- Framework: Next.js.
- Production branch: `main`.
- Frontend only; no backend adapter or duplicate API.
- Production environment points to `https://api.vybnet.app`.
- Attach `vybnet.app` and `www.vybnet.app` after provider URL passes smoke tests.
- Current verified provider alias: `https://vyb-vybnet.vercel.app`.
- Until `api.vybnet.app` is switched, both public and server API base variables
  point to the verified Cloud Run URL.
- Firebase session cookies are minted by the trusted Cloud Run runtime. Vercel
  receives no service-account JSON or long-lived Google private key.

## Client-key controls

- Firebase Android config is local/CI-generated and excluded from Git.
- Android API key is restricted to package `social.vyb.app` and registered
  signing certificate SHA-1.
- Browser API key is restricted to the stable Vercel alias, planned production
  domains, Firebase auth-handler domains, and local development origins.
- Play App Signing SHA-1/SHA-256 must be added before public Android release.

## R2

- Bucket `vyb-media-production`.
- Bucket-scoped read/write token for backend only.
- Public R2 access is disabled. Browser reads use the same-origin
  `https://www.vybnet.app/api/media/...` server route, so bucket CORS is not
  required for MVP.
- Social images and video, event media, Marketplace media, resources, and
  encrypted chat objects use R2. Application-specific upload limits are
  enforced before persistence.
- `R2_PUBLIC_BASE_URL=https://www.vybnet.app/api/media`.
- Access key ID and secret access key live in Secret Manager for Cloud Run and
  sensitive Vercel variables; no credential is stored in Git or client code.
- Abort incomplete multipart uploads and delete orphaned objects through scheduled cleanup.

## Cost controls

- Billing budget ₹2,000/month with 50/80/100% alerts.
- Cloud Run min 0 and hard max 10.
- Cost-sensitive media features can be disabled by product flags; existing
  migrated story/video assets remain readable from R2.
- Artifact retention: current image plus one verified rollback image. Delete
  older exact digests after a successful deployment and restore immutable tags.
- Health-check logs excluded; routine logs retained 14–30 days.
- No Supabase runtime, no Vercel backend, no second SQL database, no always-on Redis/Kafka/Kubernetes.

## Required pre-production evidence

- `pnpm check`, `pnpm build`, `pnpm dc:compile`.
- Auth and tenant-isolation tests.
- Marketplace and media smoke tests.
- Cloud SQL backup and restore record.
- Cloud Run staged traffic rollback.
- Vercel rollback.
- Domain/TLS and Firebase authorized-domain verification.
- Load test and cost forecast at the expected campus cohort.
