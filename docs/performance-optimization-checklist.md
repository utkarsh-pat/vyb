# Performance Optimization Checklist

This checklist tracks the current optimization pass and the next high-value work.

## Completed

- Isolated the local in-process backend bridge from production web builds so Next/Turbopack does not trace backend implementation modules into route bundles.
- Added backend `Server-Timing` headers and slow/error request logging.
- Added web backend-proxy timing logs for slow, failed, or explicitly sampled requests.
- Avoided expensive discovery/dashboard fetches on `/search?q=...`; query searches now fetch search results first, not feed, vibes, suggestions, and market.
- Capped marketplace dashboard read windows with configurable limits:
  - `VYB_MARKET_DASHBOARD_PRIMARY_LIMIT`
  - `VYB_MARKET_DASHBOARD_RELATION_LIMIT`
  - `VYB_MARKET_DASHBOARD_LISTING_LIMIT`
  - `VYB_MARKET_DASHBOARD_REQUEST_LIMIT`
- Added short-lived read-through caching for stable server-side backend reads: client shell, profile, viewer summary, stories, course list, suggested users, communities, and community detail.
- Dynamically loaded comment thread sheets on home, reels, and community pages.
- Added CSS `content-visibility` containment for repeated cards and rows.
- Added lazy/async image decoding for market and repost preview media.
- Added `pnpm perf:audit` for repeatable checks and production web build validation.

## Next

- Replace marketplace contact/save count scans with Data Connect aggregate/count operations or denormalized counters.
- Add targeted Data Connect queries for market media by listing/request IDs instead of tenant-wide media windows.
- Split the chat shell into route-level panes and lazy-load secure backup/device-pairing flows.
- Lazy-load marketplace composer/edit panels and media upload helpers.
- Move CPU-heavy media processing to background workers.
- Add Web Vitals reporting for TTFB, LCP, INP, CLS, and route-level payload size.
- Move production realtime fanout/presence from in-process memory to Redis, Pub/Sub, or a managed realtime service before multi-instance scaling.
