# Android/PWA design QA

## Reference

The implementation was compared against the checked-in PWA code, especially:

- `apps/web/src/components/campus-home-shell.tsx`
- `apps/web/src/components/campus-profile-shell.tsx`
- `apps/web/src/components/campus-search-shell.tsx`
- `apps/web/src/components/campus-reels-shell.tsx`
- `apps/web/src/components/campus-events-shell.tsx`
- `apps/web/src/components/campus-messages-shell.tsx`
- `apps/web/app/feed-enhancements.css`
- `apps/web/app/styles/campus-shell.css`

The user-provided phone captures were used as visual state references for Home, Profile, Vibes, Market, Games and Events.

## Implementation captures

- Phone Home: `apps/mobile/qa-parity-home-density-final.png`
- Phone Profile: `apps/mobile/qa-parity-profile-density-final.png`
- Tablet Profile: `apps/mobile/qa-parity-tablet-profile-final.png`
- Home/card detail: `artifacts/qa-parity-home3.png`, `artifacts/qa-parity-post-detail.png`
- Games/Events: `artifacts/qa-parity-games-final.png`, `artifacts/qa-parity-selector-blend-final2.png`
- Vibes/Profile/Community: `artifacts/qa-parity-vibes-final.png`, `artifacts/qa-parity-profile-final3.png`, `artifacts/qa-parity-community.png`

## Viewports and states

| Target | Rendered viewport | Density | State |
| --- | --- | --- | --- |
| Compact Android phone | 720 × 1600 px | 320 dpi / 360 × 800 dp | Authenticated Home and Profile |
| Android tablet | 1600 × 2560 px | 320 dpi / 800 × 1280 dp | Authenticated Home and Profile |

## Comparison history

1. Initial phone render showed oversized top actions, story control, profile avatar, profile tabs and bottom navigation.
2. The Compose typography scale was aligned with the PWA rem sizes.
3. Visible phone controls were reduced while retaining accessible 48 dp interaction targets.
4. Profile avatar changed from 72 dp to 60 dp, compact tab padding from 9 dp to 6 dp, and the labelled phone navigation from 76 dp to 66 dp.
5. Final phone verification confirmed a compact visual hierarchy without clipped labels or overlapping controls.
6. Tablet verification confirmed the compact changes do not affect the wide profile hero, three-column media grid or persistent navigation rail.
7. Post metadata now uses PWA-style compact ages; card taps open the full-post state while nested actions retain their own behavior.
8. Post and Vibes rails share a subdued action treatment and the circular sync-style repost icon.
9. Vibes media, `See likes`, reaction-member dialog and expandable-caption behavior were exercised on the authenticated emulator.
10. Profile Posts now includes posts and vibes, uses oval reaction badges, and preserves live remote media.
11. Games/Events now use the connected S-curve selector. The selected half is transparent so it blends with the exact page gradient, while only the inactive half draws the dark connected curve.
12. Leaderboard/streak, Connect/Scribble/N-Queens, Upcoming/Saved/Ended and Host Event were compared at the same compact viewport.
13. Splash/launcher artwork uses safe-zone insets and no longer clips the Vyb mark; the in-app loading mark no longer draws a rectangular shadow canvas.

## Result

No open P0/P1/P2 issue was found in the verified compact parity pass. Unit tests, Android lint and the debug APK build pass.

final result: passed

---

# Chat bubble and voice-note QA — 2026-08-16

## Reference and implementation

- Reference: `C:/Users/utkar/AppData/Local/Temp/codex-clipboard-9d08b237-87fc-412a-992c-eeff6a4cbd68.png`
- Compact implementation: `.tmp/chat-final-qa/emulator-5554-thread2.png`
- Large implementation: `.tmp/chat-final-qa/emulator-5556-thread2.png`
- States: authenticated two-account secure thread, long text and 27-second
  voice note visible.

## Comparison result

1. Voice notes expose a recognizable play control, waveform seek surface,
   elapsed/total time and playback-speed control without a redundant “Voice
   note” caption.
2. Message bubbles size to content and keep an opposite-side gutter so sender
   direction remains clear on compact and large screens.
3. Timestamp and receipt state are anchored to the bottom-right of every
   message bubble, including long text and voice messages.
4. Composer camera and attachment actions remain inside the thumb zone; native
   system navigation insets do not cover the app navigation bar.
5. Fullscreen audio/media viewers use an explicit close affordance and do not
   expose carousel count/dot chrome.

## Result

No open P0/P1/P2 visual issue was found in this two-device chat pass. The
Google Play Services/System UI ANR observed on one concurrent headless emulator
did not involve the Vyb process and recovered after choosing **Wait**.

final result: passed

---

# Two-device production chat and media QA — 2026-08-09

## Matrix

- Devices: API 35 emulators at 1080×2400 and 720×1600.
- Identities: two distinct authenticated campus accounts in one direct chat.
- Backend: production revision `vyb-backend-00022-bz2`.
- Evidence: `.tmp/two-device-chat/typing-bubble.png` and
  `.tmp/two-device-chat/voice-player-final.png`.

## Verified behavior

1. Both clients connected to the direct Cloud Run WebSocket transport; Firebase
   Hosting remains REST-only.
2. The waving typing placeholder appeared on the peer and was atomically
   replaced by the delivered message.
3. Text passed in both directions. The measured reverse message reached API
   acceptance in 1066 ms and read state in 1556 ms; peer socket fanout was
   immediate after server creation.
4. Encrypted image and voice-note uploads reached R2 and rendered on the other
   device. Voice preparation is asynchronous and play state appeared within one
   second without an ANR or UI-thread network stall.
5. Voice controls include play/pause, seek, elapsed time, and total duration.
   Generated `Photo`/`Voice note` labels are suppressed instead of appearing as
   user captions.
6. View-once media delivered with the correct marker. Its composer control is
   hidden at rest and appears only after an eligible image/video is selected.

## Remaining scale gate

Process-local socket maps are not safe across multiple Cloud Run instances.
Before higher-concurrency release, add the shared realtime coordinator and
transactional outbox described in ADR-007. Direct presigned R2 uploads are the
next latency/cost optimization for media.

## Result

Functional two-device text, typing, image, voice, and view-once QA passed.
Multi-instance fanout remains a release-scale architecture gate.

final result: passed for the current single-instance pilot topology

---

# Realtime chat typing transition QA — 2026-08-09

## Scope

- Web/PWA: open-conversation timeline and incoming realtime events.
- Native Android: Compose conversation timeline and realtime ViewModel state.
- Intended behavior: the peer's typing state occupies the next incoming-message
  position as a compact three-dot bubble, then disappears atomically when the
  actual message event arrives. No artificial delivery delay is introduced.

## Verification

1. Both clients render an animated, incoming-side three-dot bubble instead of
   a detached status label.
2. Incoming message handling clears the matching peer's typing state before
   inserting/upserting the message, preventing a stale indicator or a duplicate
   row.
3. The thread follows the indicator only when the user is already at the bottom,
   so reading older messages is not interrupted.
4. Reduced-motion web clients receive a static, accessible indicator.
5. Android `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, and
   `:app:assembleDebug` pass. Web TypeScript validation passes.

## Result

No open P0/P1/P2 implementation issue was found in this typing-transition pass.
Fresh two-device perceived-latency sampling remains a release QA measurement,
not a code blocker.

final result: passed

---

# Chat settings and message-bubble parity — 2026-08-09

## Reference and implementation

- Selected reference: `C:/Users/utkar/AppData/Local/Temp/codex-clipboard-858e8ee8-ddbf-44a5-a42f-1b924905aabf.png`
- PWA source of truth: `apps/web/src/components/campus-messages-shell.tsx` and `apps/web/app/styles/messages.css`
- Android conversation: `.tmp/chat-design-audit/android-chat-final-corner.png`
- Android settings sheet: `.tmp/chat-design-audit/android-chat-settings-final.png`
- Side-by-side comparison: `.tmp/chat-design-audit/chat-settings-comparison-final.png`
- Verified viewport: 720 × 1600 px authenticated Android emulator

## Comparison history

1. Replaced the fixed 76% message width with intrinsic content width and a 480 dp maximum for long messages.
2. Matched the PWA inline metadata reservation so time and delivery/read state stay on the final text line without covering message content.
3. Kept receipt state compact as the PWA colored dot; tapping the receipt expands its readable label.
4. Mirrored the PWA corner geometry: incoming messages tighten at bottom-left and sender messages at bottom-right.
5. Replaced the compact-header TTL pill/dropdown with the selected mobile bottom-sheet pattern.
6. Added Instant, 1h, 24h, 7d, 30d and 90d options. The selected value persists locally per authenticated user and conversation, matching the PWA storage behavior.
7. Verified Instant remained selected after closing and reopening the conversation and that the backend contract accepts `instant` as a supported duration key.
8. Verified the settings sheet, close control, refresh action, composer, receipt toggle and compact/wrapped bubble states on the authenticated emulator.

## Result

No open P0/P1/P2 visual or interaction issue was found in this chat parity pass. Android compilation, unit tests and the debug APK build pass.

final result: passed

---

# Native Android games QA — 2026-08-08

## Reference and implementation

- Chess reference: `C:/Users/utkar/AppData/Local/Temp/codex-clipboard-16d71953-9520-49a1-b536-2010831fee0a.jpg`
- Android implementation capture: `artifacts/qa/android/chess-mobile-final.png`
- Color Sort capture: `artifacts/qa/android/color-sort-mobile.png`
- Viewport: 1080 × 2400 px, 420 dpi emulator
- State: authenticated Games Hub; Chess local opening and `1. d4` history states

## Comparison and interaction findings

1. The Android Chess composition now follows the reference hierarchy: move
   rail, opponent strip, fixed square board, self strip, and bottom match
   actions. It uses original open-source Staunton SVG assets rather than text
   glyphs.
2. The board remained 8 × 8 while a legal `d2-d4` move changed turn to Black.
   Back then exposed `Viewing move 0 of 1`, verifying non-destructive history.
3. Player labels were changed to explicit high-contrast colors because the
   fixed dark game surface cannot safely inherit light-theme text tokens.
4. Color Sort launched from bundled APK assets. A flex sizing defect that made
   balls invisible in Android WebView was fixed in both web and APK copies.
5. Word Puzzle's `100dvh` compact layout collapsed to 20 CSS pixels in Android
   WebView. It now uses a scroll-safe viewport fallback and a denser board and
   keyboard so the puzzle remains usable above Android navigation insets.
6. Production Ludo loaded its authenticated container, but the deployed web
   route emitted a Next.js Server Component runtime error. Android now shows a
   clear unavailable state; release remains blocked until the current web game
   build is deployed and two-device room QA passes.

## Result

Native Chess and offline game surfaces pass compact Android QA. Online
Chess/Ludo/UNO are implemented through the shared authoritative runtime but
remain production-QA blocked by the stale web deployment.

final result: partially blocked by production deployment

---

# Chess mobile image-to-code QA — 2026-08-08

## Reference and implementation

- Reference: `C:/Users/utkar/AppData/Local/Temp/codex-clipboard-16d71953-9520-49a1-b536-2010831fee0a.jpg`
- Verified implementation: `.tmp/chess-mobile-qa/02-after-d4.png`
- Viewport: 390 × 844 CSS px
- State: authenticated Chess Arena, local match after `1. d4`

## Comparison history

1. Replaced text glyph pieces with a real Staunton SVG piece set and retained the green/cream board language from the reference.
2. Added dedicated opponent and self strips. Online play renders the real room display names; local pass-and-play renders Player 1 and Player 2 with their White/Black turn state.
3. Added a horizontally scrollable SAN move-history rail and working move selection.
4. Added functional Back, Forward and Live controls. History mode reconstructs the board from the recorded SAN moves without mutating the active match.
5. Added local New game and Flip board actions plus a direct Chat action for online rooms.
6. Locked the board to an 8 × 8 minmax grid with square containment. Browser measurements before and after history navigation remained 370.4 × 370.4 px.
7. Verified `1. d4`, history Back, history Forward and return-to-live behavior in the authenticated Chrome profile.

## Deliberate scope

- A chess clock was not faked. It should be added only when the multiplayer room owns an authoritative server clock so reconnects and background tabs cannot desynchronize it.
- The screen follows the reference composition and interaction hierarchy while retaining Vyb branding and original assets.

## Result

No open P0/P1/P2 issue was found in the verified mobile chess pass. Web type-check, production build and game-engine tests pass.

final result: passed
