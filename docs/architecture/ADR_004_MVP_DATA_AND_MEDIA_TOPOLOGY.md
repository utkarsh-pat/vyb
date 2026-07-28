# ADR-004: Google-first transactional platform with R2 media

Date: 2026-07-29
Status: accepted

## Decision

Use Firebase Data Connect backed by Cloud SQL PostgreSQL as the only canonical application database, Cloud Run as the only backend runtime, Firebase for authentication/notifications/analytics/configuration, and Cloudflare R2 for user media. Keep Vercel only for the web frontend.

The new production database starts empty; legacy Supabase data is not imported. Firebase Storage is not used for new user media. Firestore may be used only for explicitly ephemeral presence when justified and must never mirror core SQL entities.

## Why

- The new billing account has verified time-limited trial credit of ₹28,320.75 through 2026-10-28.
- The current Cloud SQL shared configuration is the smallest practical SQL Connect instance.
- PostgreSQL fits tenant-scoped marketplace/social/chat relationships and avoids Firestore read-amplification.
- R2 provides low-cost object storage and avoids coupling media growth to database/runtime spend.
- One database and one backend eliminate dual-write drift and incident ambiguity.

## Consequences

- The SQL instance is a small fixed cost after trial allowances/credits and must be monitored with a ₹2,000 monthly budget.
- Stories and heavy video remain disabled until retention justifies their storage and moderation cost.
- R2 requires a Cloudflare account, bucket, custom/public domain and bucket-scoped API token.
- No legacy database credential or runtime fallback is copied to the fresh stack.

## Rejected

- Supabase as canonical DB: adds a second provider and duplicates the selected Google stack.
- Firestore as canonical marketplace/social DB: weaker fit for relational queries and potentially higher read cost.
- Firebase Storage for all media: simpler integration but less attractive media economics.
- Vercel backend plus Cloud Run: two deploy surfaces and inconsistent realtime/runtime behavior.
