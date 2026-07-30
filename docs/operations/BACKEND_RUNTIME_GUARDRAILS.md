# Backend runtime and cost guardrails

## Cloud Run baseline

The backend deploy keeps `min-instances=0` so an idle MVP does not pay for an
always-on instance. The declared baseline is one vCPU, 512 MiB memory, 80
concurrent requests, a maximum of ten instances, CPU throttling, and the
existing 300-second request timeout. These values make configuration drift
visible; change them only after observing Cloud Run CPU, memory, request
latency, instance count, and WebSocket duration.

Media transcoding is CPU and memory intensive. If production traces show
multiple FFmpeg jobs overlapping on one instance, move transcoding to a queue
or separately limited worker before raising general API memory. Raising
`min-instances` should remain a measured latency decision, not a default.

## Build cache

Cloud Build best-effort pulls and pushes the mutable `build-cache` image.
Either cache operation is deliberately allowed to fail, so a first deployment,
a deleted cache tag, or an immutable-tag registry policy still performs a clean
build and deploys the immutable commit image. The commit image remains the
deployment and rollback artifact. When accepted, the cache tag points at the
same image layers, so Artifact Registry deduplicates their bytes.

Cache effectiveness depends on the Dockerfile order: lockfiles and workspace
package manifests are copied before application source. Source-only changes
can therefore reuse the production dependency layer, including the large
FFmpeg dependency.

## Health contracts

- `GET /health` is liveness. It performs no network or paid-service calls.
- `GET /ready` is non-networked startup/configuration readiness. Missing
  Firebase project configuration returns `503`.
- R2 media and signed daily-game configuration are reported as degraded
  features without making the whole API unavailable. This allows identity and
  campus recovery while clearly exposing incomplete optional configuration.

Neither endpoint validates remote Firebase, Data Connect, or R2 availability.
Remote dependency health belongs in synthetic monitoring at a deliberately
low frequency to avoid traffic and billing amplification.

## CI

Backend CI is path-filtered and cancels superseded runs. It installs only the
backend dependency closure, performs entry-point syntax checks, and runs all
Node test-runner tests. This catches configuration/readiness and module
regressions without paying for Android or web builds when only backend code
changes.

## Remaining production work

- The current FCM outbox uses a local JSON store and a 15-second worker in each
  backend process. It is suitable for local validation, not a multi-instance
  Cloud Run deployment. Move the outbox to durable storage and trigger one
  bounded worker through Cloud Tasks or an equivalent queue before relying on
  push delivery at scale.
- Video transcoding currently runs FFmpeg inside the request-serving runtime.
  Load-test concurrent video publishing at 512 MiB; use a bounded asynchronous
  worker if jobs overlap or cause memory pressure.
- Commit-tagged Artifact Registry images need a project-level cleanup policy
  that preserves the active revision and one verified rollback image.
- The declared maximum is 800 in-flight requests (`10 x 80`), not 800
  simultaneous CPU-heavy jobs. Capacity testing must model media and
  WebSocket traffic separately from ordinary API requests.
