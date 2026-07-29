# Vyb MVP Capacity and Cost Model

Last updated: 2026-07-29
Currency planning rate: use the live provider invoice; USD/INR conversions below are indicative.

## Verified starting credit

The new billing account `015FFD-11FC57-660C55` is linked to project `vybnet`.

- Free Trial credit available: **₹28,320.75**
- Remaining at verification: **100%**
- Start: **2026-07-28**
- Expiry: **2026-10-28**

This is time-limited credit, not a permanent monthly subsidy. The design must remain affordable after expiry.

## Planning load

- 20,000–30,000 registered users across two to three universities.
- 3,000–6,000 DAU.
- 300–800 peak concurrent sessions.
- 20–60 API requests per DAU per day after caching.
- Stories and long video disabled; images compressed before upload.

## Cost-first baseline

| Component | Initial configuration | Planning range/month |
|---|---|---:|
| Cloud SQL/Data Connect | smallest compatible shared instance, 10 GB SSD, single zone | ₹800–₹1,500 |
| Cloud Run API | 1 vCPU/512 MiB, min 0, launch max 3, concurrency 40 | ₹0–₹800 |
| Firebase Auth/FCM/Analytics/Crashlytics/Remote Config | free allowances, usage dependent | ₹0–₹300 |
| Cloud Tasks, Secret Manager, logs, Artifact Registry | low-volume and retention-controlled | ₹0–₹500 |
| Cloudflare R2 | image-first, free allowances then usage-based | ₹0–₹500 |
| Vercel web | Hobby only if usage is eligible; otherwise current paid plan | ₹0 or plan price |
| Domain/email/monitoring | provider dependent | ₹100–₹1,500 |

Expected Google infrastructure usage:

- controlled pilot: roughly ₹800–₹2,000/month before trial credit;
- 20k–30k registered and 3k–6k DAU: roughly ₹1,500–₹4,500/month before Vercel/domain;
- video-heavy use can exceed this quickly and remains disabled.

The verified trial credit should cover the expected initial Google infrastructure during its validity if guardrails are maintained. It does not prevent charges after expiry or usage beyond credit.

## Guardrails

- Set a billing budget at ₹2,000/month initially with alerts at 50%, 80%, and 100%.
- Add a second forecast alert at 120%; budgets notify but do not hard-stop services.
- Cloud Run min 0, launch max 3, concurrency 40. The approved hard ceiling is
  10; raise toward it only after measured saturation.
- Database pool maximum 5 connections per API instance.
- Keep video/Stories off and enforce per-user media quotas.
- Retain the active container image plus the previous three; delete untagged images older than 14 days.
- Exclude health checks from normal request logs and keep routine log retention at 14–30 days.
- Use single-zone SQL for pilot; HA is an explicit business/SLO decision because it approximately doubles compute cost.

## Upgrade triggers

Upgrade database memory before scaling API instances when any two persist for 15 minutes:

- CPU above 70%;
- memory pressure or restarts;
- p95 SQL query latency above 150 ms;
- connections above 70%;
- lock waits affecting user-facing p95.

Add PITR before paid transactions or broad university dependence. Add HA only when downtime cost justifies the fixed increase.
