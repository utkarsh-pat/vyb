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
