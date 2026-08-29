# Android and Web Product Parity

Last audited: 2026-08-08

This document is the release gate for the native Android app. Android is not
considered release-equivalent until every launch-user web route is either:

1. implemented with the same backend contract and user outcome;
2. intentionally excluded by product policy; or
3. blocked with an owner, reason, and target batch recorded below.

Visual parity means the same information hierarchy, design tokens, states, and
actions. It does not mean forcing desktop layout geometry onto a phone.

## Locked design contract

The production web CSS variables are the source of truth:

| Token | Web | Jetpack Compose |
| --- | --- | --- |
| Background | `#0f172a` | `VybDarkPalette.background` |
| Deep background | `#020617` | `VybDarkPalette.backgroundDeep` |
| Primary | `#6366f1` | `VybIndigo` / Material primary |
| Strong primary | `#4f46e5` | Primary gradient endpoint |
| Secondary | `#14b8a6` | `VybTeal` / Material secondary |
| Text | `#e6eefc` | `VybText` |
| Muted text | `#94a3b8` | `VybMuted` |
| Border | `#ffffff1f` | `VybBorder` |
| Card radius | `16px` | `16.dp` default glass/card radius |
| Font | Plus Jakarta Sans | Bundled Plus Jakarta Sans family |

At the measured 720 x 1600 PWA viewport, authentication uses a centered
460 px maximum-width glass panel with approximately 30 px outer radius,
indigo/teal radial ambient glows, 46-49 px controls, and pill-shaped primary
actions. Android uses the same hierarchy and tokens with density-independent
native geometry and 48 dp minimum actions.

Mobile navigation intentionally matches the responsive web bottom bar:
Home, Hub, Vibes, Market, and Profile. Chats, search, notifications, and
creation remain header/flow actions rather than a sixth bottom destination.

## Persistence rule

Account and product state must not use Android local storage as its source of
truth. Profile fields, bio, social links, follower state, chat privacy, trusted
devices, posts, and marketplace/social data are read from and written to the
owned backend contracts. Compose may retain an unsaved form draft in memory
until submit; that draft is not treated as persisted state.

The only device-scoped local values are non-account data that must remain local:
the appearance preference, cryptographic private keys in Android Keystore, an
opaque local device identifier, and the latest Firebase Installation ID used
for push registration.

## Route and capability matrix

| Web surface | Android surface | Functionality status | Visual status | Required work |
| --- | --- | --- | --- | --- |
| `/login` | `LoginScreen` | Email sign-in, account creation, reset, and Google sign-in implemented; Google credential validation and authenticated session bootstrap hardened | Close; locked tokens applied | Re-run email and Google sign-in on a clean emulator and verify all error states |
| `/onboarding`, `/complete-profile` | `OnboardingScreen` | Live profile read/upsert, debounced tenant-scoped username availability, course/branch catalog pickers with fallback, required-field validation, R2 avatar upload, and HTTPS social-link validation implemented | Close | Clean-account device QA and trusted-device pairing |
| `/home` | `HomeScreen` | Live feed, swipeable multi-asset image/video cards, stories, create post, like/save/share/repost/report, threaded comments, owner edit/delete, and duplicate-load guards implemented | Close | Authenticated offline/retry, process-recreation, and social realtime QA |
| `/create` | `CreatePostComposer`, `MediaComposerScreen`, `StoryBuilderScreen` | Post/story/vibe composition and media upload implemented; device-local drafts/scheduling intentionally removed | Partial | Add a backend-owned scheduling contract before exposing scheduled publishing |
| `/search` | `SearchScreen` | Debounced People, Posts, Vibes and Market discovery with UID-scoped bounded cache, partial-failure isolation, media cards, follow state and safe destination callbacks | Close | Production dataset and destination-selection device QA |
| `/notifications` | `NotificationScreen` | Direct backend inbox, per-recipient read state, FID registration, foreground/data-only rendering, filters/read actions, allowlisted native deep links, and cold/warm activity handoff implemented | Close | Verify real FCM taps from background/terminated states and every allowlisted destination |
| `/messages`, `/messages/[id]` | `MessagesFeatureScreen` | Inbox, public-profile direct-message entry, encrypted conversation, realtime typing/delivery/read updates, canonical conversation de-duplication and unread previews implemented; PWA-parity intrinsic bubbles, inline time/receipt metadata, mirrored bottom-corner direction, and persistent Instant/1h/24h/7d/30d/90d auto-destruct settings sheet are included; API 31+ uses Keystore ECDH and API 26-30 wraps the EC private key with Keystore AES-GCM | Close | Attachments, voice-note transport, process-recreation and the complete privacy-policy matrix |
| `/messages/community/[slug]` | `MessagesFeatureScreen` community mode | Membership-filtered discovery, chronological conversation, refresh, text send and deep navigation implemented against web-compatible backend contracts | Partial | Multi-account/device QA; add native realtime after a Firebase-Bearer social socket-token endpoint exists |
| `/vibes`, `/reels` | `NativeVibesScreen` | Live feed, playback, like/save/share/repost/report, threaded comments/replies/reactions and owner controls implemented through shared social-actions state; loading is lifecycle-aware and duplicate guarded | Close | Paging, realtime state, and process-recreation QA |
| `/market` | `MarketFeatureScreen` | Browse, create, save, contact, and mark-sold implemented; client discovery now includes search, category and price/recent sorting, saved filtering, and listing/request image-video display | Close | Verify production data, filters and media on device; creation-time image attachments remain a gap |
| `/dashboard` | `ProfileFeatureScreen` | Live profile, R2 avatar selection/upload, remote post media, stats, posts/vibes tabs, edit/save, followers/following, HTTPS social links, appearance, recovery, and sign-out implemented | Close | Production avatar/social-link device QA |
| `/u/[username]` | `SearchScreen` profile detail | Public profile, stats, posts, and follow controls implemented | Partial | Dedicated deep-link route and full media grid |
| `/profile/settings/chat-privacy` | `ProfileFeatureScreen` privacy panel | Last-seen visibility, read receipts, and typing settings persist through the live chat API | Close | Authenticated multi-account policy QA |
| `/settings/security` | `ProfileFeatureScreen` security panel | Password recovery plus trusted-device list/revoke with destructive confirmation implemented | Partial | Device registration/pairing, key backup recovery, and active-device claim |
| `/hub` | `CampusHubScreen`, `FunHubScreen` | Events, resources, community details and Bearer-authenticated Connect/Queens contracts implemented | Partial | Align remaining hub detail sheets and deploy/device-test the new Games backend |
| `/events/host` | `CampusHubScreen` host mode | Core create/edit and registration review with approve/waitlist/reject implemented; existing advanced config/media is preserved on edit | Partial | Native R2 upload, dynamic form/team builder, attachments, CSV export and cancellation |
| `/hub/gameshub/connect` | `FunHubScreen` | Daily, hint and submit aligned to tenant/user-bound Bearer backend contracts | Partial | Deploy/device-test; add durable leaderboard/run persistence |
| `/hub/gameshub/queens` | `FunHubScreen` | Daily, hint and submit aligned to tenant/user-bound Bearer backend contracts with server-side validation | Partial | Deploy/device-test; add Queens-aware durable leaderboard/run schema |
| `/hub/gameshub/scribble`, `/join/scribble` | `ScribbleScreen` in `FunHubScreen` | Bearer socket-token exchange, public/private lobby, create/join, reconnect/rejoin, round state, word selection, guess, timer, canvas stroke batching, clear/skip/retry/leave implemented | Partial | Deploy backend token route; device/multi-account QA; settings, invites, reactions, eraser, share/result posting |
| `/hub/gameshub/chess` | `ChessGameScreen` | Native legal local chess with castling, en passant, promotion, check/checkmate, move history, board flip and history navigation; Online opens the shared authenticated multiplayer runtime | Close | Deploy the current web game route and Durable Object runtime; run two-device room/chat/invite QA |
| `/hub/gameshub/ludo`, `/hub/gameshub/uno` | `AuthenticatedWebGameScreen` | Authenticated isolated WebView reuses the authoritative web room, invite, chat and game protocol without duplicating multiplayer state in the APK | Partial | Production web game deployment currently returns a Server Component error; deploy and run two-device room QA |
| `/hub/gameshub/color-sort`, `/hub/gameshub/word-puzzle` | `LocalHtmlGameScreen` | Self-contained offline HTML games bundled in APK assets with local device persistence and no backend cost | Close | Release-device smoke test |
| `/admin` | None | Intentionally web-only | N/A | Keep restricted to the administrative web surface |

## Implementation batches

### Batch 0 — foundation and hygiene

- [x] Verify debug assembly from the actual mobile Gradle root.
- [x] Lock Compose dark tokens to the production web theme.
- [x] Consolidate Retrofit, JSON, logging, timeout, and token handling.
- [x] Remove unused mock repositories and duplicate placeholder screens.
- [x] Wire the Home notification action to a real destination.
- [x] Clear all blocking Android Lint findings.
- [x] Add a non-zero unit-test source set for onboarding contract validation.
- [x] Use lifecycle-aware state collection on launch-critical feature screens.
- [x] Prevent duplicate authenticated bootstrap/story/vibe refresh work.
- [x] Expose a retry state when campus/session bootstrap fails.
- [ ] Add Compose UI tests for launch-critical screens.

### Batch 1 — identity, discovery, and profiles

- [x] Native onboarding/profile completion.
- [x] Search and suggested-user surface.
- [x] Public profile and follow/unfollow.
- [x] Own-profile tabs, edit/save, and follower/following lists.
- [x] Persisted chat-privacy settings.
- [x] Password recovery and trusted-device list/revoke.
- [x] Tenant-scoped username availability, catalog pickers, R2 avatar upload,
  and HTTPS-host allowlisting for profile links.
- [ ] Trusted-device registration/pairing and encrypted key-backup recovery.

### Batch 2 — social creation and consumption

- [x] Feed image/video rendering and preservation of backend viewer engagement state.
- [x] Swipeable multi-asset image/video carousel with page count and indicators.
- [x] Shared Home/Vibes like, save, share, repost, report, threaded reply, and
  owner edit/delete actions.
- [x] Comment reactions with server-authoritative counts across Home and Vibes.
- [ ] Dedicated repost-card rendering.
- [x] Dedicated notifications with read filters/actions and safe native deep links.
- [x] Route notification hrefs received by cold-start and warm activity intents.
- [x] Render foreground/data-only FCM notifications and register Android
  installations through the current FID API.
- [ ] Verify real FCM push taps while backgrounded and terminated.
- [x] Remove device-local post/media drafts, staged media, and WorkManager scheduling.
- [ ] Add backend-owned scheduled publishing, then restore scheduling UI with server status/retry.
- [ ] Vibes paging/playback/action verification.

### Batch 3 — campus hub and events

- [x] Native event host/edit core and registration review management.
- [ ] Registration/application form builder, team editor and attachment configuration.
- [ ] Resource/community detail parity.
- [ ] Event/R2 create, replace, and delete smoke tests.

### Batch 4 — chat, marketplace, and games

- [x] Direct-message entry from public profiles with tenant-safe conversation creation.
- [x] Two-account Android chat round trip: encrypted ID1-to-ID2 and ID2-to-ID1
  messages render live without manual refresh; cold inbox reload shows the
  decrypted preview and unread badge.
- [x] Chat realtime feedback-loop guard: delivery/read acknowledgements no
  longer trigger recursive conversation reloads, and overlapping message
  events are serialized into one pending refresh.
- [x] Canonical conversation/participant de-duplication prevents duplicate
  Compose keys and cross-tenant direct-conversation reuse.
- [x] Community chat discovery, membership-safe conversation, refresh, text send, and navigation.
- [ ] Community chat realtime after a Firebase-Bearer socket-token endpoint is available.
- [x] Encrypted image/voice-note attachments and the view-once delivery
  lifecycle passed the fresh two-device matrix. R2 upload/download, recipient
  socket fanout, asynchronous voice preparation, play/pause, seeking, elapsed
  time, and media-only view-once composer visibility were verified. Android
  14+ screenshot notices remain a best-effort platform signal, not a prevention
  guarantee.
- [ ] Trusted-device/privacy controls.
- [x] Marketplace discovery controls and existing listing/request media display.
- [ ] Marketplace creation-time image attachments and production device QA.
- [x] Native Scribble lobby, room, canvas, word/guess flow, and reconnect.
- [ ] Scribble settings/invites/reactions/eraser/share/result-posting and
  multi-account device QA.
- [x] Add Bearer-authenticated Connect/Queens backend daily/hint/submit contracts.
- [ ] Deploy and device-test Games; add durable leaderboard/run persistence.

## Verification gate

Each batch must pass:

- `apps/mobile/gradlew.bat :app:assembleDebug :app:testDebugUnitTest --no-daemon`
- `apps/mobile/gradlew.bat :app:lintDebug --no-daemon`
- authenticated API smoke tests against `https://api.vybnet.app`
- compact-phone and large-phone screenshot comparison
- loading, empty, error, offline, retry, and signed-out state checks
- TalkBack labels and 48 dp minimum interactive targets

Current verification status (2026-08-09):

- The earlier `0.1.3` baseline passed `:app:assembleDebug`,
  `:app:testDebugUnitTest`, and `:app:lintDebug` with 13 unit tests.
- The final `0.1.4` worktree passed `:app:testDebugUnitTest`,
  `:app:lintDebug`, and `:app:assembleDebug` after a clean rebuild. All 52 JVM
  tests passed; Android Lint reported zero fatal and zero error findings. The
  remaining 22 warnings are intentional toolchain/dependency version notices,
  not application-code findings.
- The internal debug APK was upgrade-installed on the API 35 emulator as
  version `0.1.4 (5)`. An existing authenticated KIET session loaded the home
  feed through the production API without a fatal exception or emulator-local
  API fallback.
- Notification push-tap, marketplace discovery/media, story/vibe interactions,
  and community chat still require their complete authenticated device matrix.
- Direct chat passed an API 35 two-emulator authenticated matrix at 1080x2400
  and 720x1600: both peers opened the same tenant-scoped conversation, sent and
  decrypted messages in both directions in real time, and the backgrounded
  recipient recovered the latest preview with unread count `1` after a cold
  reload. A five-second idle trace after delivery produced zero additional
  backend requests, confirming the former read/delivery refresh storm is closed.
- Android now sends a lifecycle-aware 30-second presence heartbeat while the
  Messages surface is visible. Debug builds emit content-free timing events for
  API acceptance, recipient socket arrival, delivered/read receipts, socket
  reconnect, and presence RTT. `scripts/measure-chat-latency.ps1` aggregates a
  bounded two-emulator run into p50/p95/p99 and raw CSV evidence.
- Web and Android now show the peer typing state as a waving three-dot incoming
  bubble. An incoming realtime message atomically clears that placeholder and
  inserts the message bubble, preserving the perceived position without
  delaying delivery.
- Local R2 S3 authentication was verified by listing
  `vyb-media-production`. The production Cloud Run `/ready` contract reports
  `r2Media: true`; its access key and secret remain Secret Manager references,
  and the bucket's public `r2.dev` endpoint remains disabled.
- Production realtime revision `vyb-backend-00022-bz2` now issues direct Cloud
  Run WebSocket URLs instead of routing upgrades through Firebase Hosting.
  Both emulators connected successfully. A reverse-direction text message was
  accepted in 1066 ms, read in 1556 ms, and appeared at the peer from the
  realtime event without polling. Image and 27-second voice-note delivery also
  passed. The remaining media performance improvement is direct presigned R2
  upload; the current encrypted base64 JSON upload works but adds avoidable
  request size and backend transit.
- Generated attachment labels (`Photo`, `Video`, `Voice note`) are no longer
  rendered as fake captions. Native voice bubbles now expose compact
  play/pause, seek, elapsed and total-time controls without blocking Compose's
  main thread. The view-once control appears only after eligible media is
  selected.
- Authenticated Vibes QA found a production media-route `404` for one migrated
  Vibe. Native loading/error fallback is verified; media backup recovery or
  record repair remains an operations/data task.
- The complete backend test gate passes (23/23), including Games,
  notifications/FID delivery, updater, readiness, membership isolation,
  identity username and upload-intent contracts. Deployment and authenticated
  device verification remain required for new endpoints.
- Compose UI coverage remains a release blocker.

Media loading uses Coil `3.4.0` because it is compatible with the current
compileSdk 35 toolchain. Coil `3.5.0` requires compileSdk 36, so that
upgrade is deferred until the Android build toolchain is upgraded as one tested
change rather than being mixed into a feature batch.

Kotlin `2.3.0` requires AGP `8.13.2` or newer. The project is locked to AGP
`8.13.2` and Gradle `8.13`; the former AGP `8.7.3` combination caused the
Compose lint detector to crash because its Kotlin analysis API was incompatible.

Scheduled publishing must be server-owned. The removed implementation staged
media in the app's files directory and depended on WorkManager/device uptime.
That model could lose or delay product state after uninstall, logout, battery
restrictions, or device changes and therefore violated the persistence rule.

No parity row may be marked complete solely because a composable exists. The
backing API action, persistence, navigation, error handling, and cleanup path
must all be verified.

## Chat media parity follow-up — 2026-08-16

- Android and PWA now keep message metadata in the bottom-right corner of each
  bubble. Android uses a compact translucent metadata pill so timestamps and
  delivered/read state remain legible over text, media and voice notes.
- Android message rows preserve a visible opposite-side gutter, use a thin
  media surround, and size text bubbles to their content up to the responsive
  maximum width.
- Android voice notes expose play/pause, a draggable waveform seek surface,
  elapsed/total time and `1x`/`1.5x`/`2x` playback speed. Hold-to-record and
  slide-to-cancel remain composer gestures rather than persistent labels.
- Android and PWA support staged multi-media selection, per-image editing,
  direct camera capture, explicit view-once close/consumption, and fullscreen
  media viewing. Normal media uses a swipeable fullscreen carousel; images are
  zoomable.
- PWA chat copy and the newly touched controls were checked against the current
  Vercel Web Interface Guidelines. New icon-only controls have accessible
  labels, async recording state is announced, the fullscreen viewer contains
  overscroll, and loading/typing copy uses the ellipsis character.
- `DESIGN.md` is the repository-level Vercel-inspired UI reference. Installed
  mobile and Android skills are development guidance only; they add no runtime
  dependency or production bundle cost.

Verification:

- `pnpm --filter @vyb/web check` — passed.
- `:app:testDebugUnitTest :app:assembleDebug` — passed on version `0.1.24 (24)`.
- The final APK was upgrade-installed and launched on API 35 emulators
  `emulator-5554` and `emulator-5556`; both retained their distinct authenticated
  accounts and opened the same secure thread.
- Both devices rendered the waveform player, content-sized incoming/outgoing
  bubbles and bottom-right message metadata. No `social.vyb.app` crash appeared
  in the crash buffer. One headless emulator experienced a Google Play
  Services/System UI resource ANR while both AVDs ran concurrently; choosing
  **Wait** recovered the system UI and Vyb remained stable.
- Artifact: `artifacts/Vyb-0.1.24-debug.apk`, SHA-256
  `2294614B4C9E61977E977971D1C3F175BD7E9249BAC2658ED12170DDD17A91BF`.
