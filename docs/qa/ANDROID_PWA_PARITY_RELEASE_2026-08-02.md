# Android and PWA Parity Release Review

Owner: Product Engineering
Date: 2026-08-02
Release: Android `0.1.21 (22)` and current PWA main release
Status: Ready for main after automated validation

## Outcome

The native Android app and responsive PWA now share the same user-facing
contracts for the launch surfaces. The implementation is intentionally native
where platform behavior differs, but navigation destinations, post actions,
media ordering, composer limits, draft lifecycle, scheduling controls, profile
content links, theme semantics, and empty/error states are aligned.

This review covers the seven parity commits on `codex/android-web-parity` plus
the final working-tree fixes completed before main integration.

## Parity Matrix

| Area | Android | PWA | Result |
| --- | --- | --- | --- |
| Home/feed chrome | Responsive header, scroll-aware glass FAB, reselect-to-top | Same navigation contract and FAB direction behavior | Aligned |
| Post media | Up to 8 items, reorder, full-width carousel, active-page video autoplay | Up to 8 items, drag reorder, swipe carousel, active/visible muted autoplay | Aligned |
| Post actions | Reaction, circular comment, internal share, repost, save | Same actions and internal share sheet | Aligned |
| Post detail | Tap opens entity detail; swipe carousel; image zoom; Back returns to feed | Lightbox with swipe carousel, pinch zoom and history-aware close | Aligned |
| Vibes | Fill-measured stage, persistent mute, 2x hold, seek, transient play feedback | Cover stage, persistent mute, hold speed, seek and transient feedback | Aligned |
| Hub | Connected Games/Events selector, leaderboard/streak and filtered events | Same information architecture and destinations | Aligned |
| Messages | Connected Chats/Community selector and entity-aware navigation | Same selector and community/chat navigation | Aligned |
| Search | Responsive people and discovery results with entity navigation | Same result types and destinations | Aligned |
| Marketplace | Items/Requests/Lend controls, filters, sort, semantic empty states | Same controls and marketplace actions | Aligned |
| Profile | Posts/Vibes/Saved shelves; every tile opens its entity | Every grid tile links to post/vibe detail | Aligned |
| Composer | Single studio, 8-item limit, edit/reorder, exact schedule picker | Same studio contract, edit/reorder and `datetime-local` picker | Aligned |
| Drafts | Multiple device-local drafts, badge, explicit load/discard, no auto-load | IndexedDB-backed multi-drafts including media, badge, explicit load/discard, no auto-load | Aligned |
| Exit/publish lifecycle | Back saves draft with toast; publish collapses sheet and drives FAB ring | Close/back saves draft with home toast; queued publish returns home and drives FAB ring | Aligned |
| Theme | Semantic light/dark surfaces and system navigation insets | Semantic light/dark surfaces and safe-area-aware mobile navigation | Aligned |

## Final PWA Gaps Closed

1. Replaced the legacy single `localStorage` draft that auto-loaded on every
   composer open with an explicit multi-draft shelf.
2. Draft media is stored in IndexedDB using structured clone, so local image,
   story, and vibe files can be restored instead of retaining text only.
3. Added draft count badge, load, discard, active-draft update, and removal of
   only the published draft.
4. Header close, backdrop close, browser Back, and page exit perform a
   best-effort local save and show `saved as draft` on Home. `Cancel creation`
   remains the explicit destructive action.
5. Connected the existing background publish queue to the mobile Home FAB, so
   the progress ring follows the real queued task rather than a timer.
6. Converted newly added schedule and draft surfaces to semantic theme tokens
   to prevent dark hard-coded panels in light mode.
7. Changed composer blob-URL disposal to unmount-only cleanup. Reordering or
   adding media no longer revokes URLs that are still active in the editor.

## Platform-Specific Exceptions

- Android system-bar insets, APK update installation, keyboard behavior,
  WorkManager, and `content://` URI handling have no browser equivalent.
- PWA IndexedDB/background publish recovery and browser safe-area CSS have no
  direct native equivalent.
- These are implementation differences, not product behavior differences.

## Validation

- `pnpm check`
- `pnpm --filter @vyb/backend test` (23/23 passing)
- `pnpm --filter @vyb/web check`
- `pnpm --filter @vyb/web build`
- `pnpm dc:compile`
- backend Node syntax and automated test suites
- `gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
- `git diff --check`
- repository secret-pattern scan before staging
- Android emulator QA for Vibes fill, explicit draft load/discard, exact date
  picker, profile tile navigation, carousel behavior, and publish lifecycle
- Local PWA boot/login boundary QA; authenticated localhost interaction is
  intentionally blocked by the production Firebase HTTP-referrer policy

The first remote Android lint run identified an API-33-only `URLDecoder`
overload in notification deep-link parsing. It was replaced with the
API-26-compatible charset-name overload and the exact CI Gradle command then
passed locally before the follow-up push.

## Next Work, in Priority Order

### P0 - Durable server-owned scheduling

Current clients persist scheduled work locally and resume while the app/PWA is
available. Move scheduling authority to the backend with an idempotency key,
`scheduledAt`, owner/tenant authorization, retry state, cancellation, and a
small dispatcher. This is the most important correctness gap before public use.

Recommendation: store schedule metadata with the post command, dispatch due
jobs through the existing low-cost Google backend boundary, and keep R2 upload
references immutable. Do not keep a phone or browser process responsible for a
business-level schedule.

### P0 - Release observability and safe rollback

Add Android Crashlytics, web/backend structured error correlation, publish
queue failure metrics, and a release dashboard covering login, feed load,
upload, schedule, chat, and marketplace mutations. Add Remote Config kill
switches for Vibes, games, events, and new composer paths.

### P0 - Separate development authentication boundary

Local browser QA is blocked by the production Firebase API-key referrer policy,
which is correct for production. Create a separate Firebase development project
or Auth Emulator workflow for localhost. Do not weaken the production API-key
restriction.

### P1 - Automated cross-platform contract and visual regression

Add phone/tablet PWA screenshots and Compose screenshot tests for Home, Hub,
Vibes, Market, Search, Messages, Profile, composer, light theme, and both Android
navigation modes. Keep API contract fixtures shared so Android and web cannot
silently interpret counts or media arrays differently.

### P1 - Marketplace trust and moderation

Before scaling beyond the initial campuses, prioritize listing moderation,
report/block flows, seller response SLAs, duplicate/spam controls, and an admin
review queue. Payments and escrow should remain out of MVP until trust metrics
show healthy campus liquidity.

### P2 - Media cost and performance tuning

Generate standard R2 variants and thumbnails asynchronously, measure cache-hit
ratio, and enforce playback selection by viewport/network. Keep originals only
where editing or moderation requires them, with documented retention.

## Product Recommendation

The next area should be **release reliability**, not another large visible
feature. Durable scheduling, observability, a safe dev-auth environment, and
automated parity regression will protect the work already completed and reduce
the cost of every later Marketplace, Chat, and campus expansion.
