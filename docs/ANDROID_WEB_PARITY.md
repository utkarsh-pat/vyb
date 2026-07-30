# Android and PWA parity

## Source of truth

The PWA implementation is the product and interaction source of truth. The native Android client reproduces those flows with Jetpack Compose instead of embedding web pages.

| Surface | PWA reference | Android implementation |
| --- | --- | --- |
| Home feed, stories and post actions | `apps/web/src/components/campus-home-shell.tsx` | `apps/mobile/app/src/main/java/social/vyb/app/ui/HomeScreen.kt` |
| Post reactions, comments, share, repost and save | `apps/web/src/components/use-social-post-engagement.ts` | `apps/mobile/app/src/main/java/social/vyb/app/features/social/SocialActionsUi.kt` |
| Profile, tabs and settings | `apps/web/src/components/campus-profile-shell.tsx` | `apps/mobile/app/src/main/java/social/vyb/app/features/profile/PwaProfileSurface.kt` and `ProfileSettingsHub.kt` |
| Search | `apps/web/src/components/campus-search-shell.tsx` | `apps/mobile/app/src/main/java/social/vyb/app/features/search/SearchScreen.kt` |
| Vibes | `apps/web/src/components/campus-reels-shell.tsx` | `apps/mobile/app/src/main/java/social/vyb/app/features/stories/StoriesVibesScreens.kt` |
| Games and events | PWA campus hub surfaces | `apps/mobile/app/src/main/java/social/vyb/app/features/fun/FunHubScreen.kt` and `features/hub/CampusHubScreen.kt` |

## Responsive rules

- Compact phone: edge-to-edge feed cards, 66 dp labelled bottom navigation, compact type scale, 36–40 dp visible controls inside at least 48 dp touch targets.
- Very small phone: 58 dp icon-only bottom navigation.
- Tablet at 600 dp and above: persistent navigation rail, wider content constraints, multi-column profile media and horizontally composed profile hero.
- Text respects Android font scaling. Fixed visual controls do not use text scaling as a substitute for layout adaptation.

## Interaction parity

- Home posts support card-tap full-post view, reaction selection/removal, comments, Android share sheet, circular repost and save. Nested actions consume their own tap and long-press still opens reaction choices.
- Profile supports a mixed Posts grid (posts and vibes), dedicated Vibes and Saved views, oval reaction badges, follower/following entry points, safe social links, edit and settings.
- Vibes support single-tap pause/resume, double-tap like, press-hold 2x playback, like-member discovery, expandable captions, comment, circular repost, share, profile navigation and creation.
- Campus Hub uses the connected PWA Games/Events selector. Games expose leaderboard/streak plus Connect, Scribble and N-Queens; Events expose search, notification state, Upcoming/Saved/Ended scopes and event hosting.
- Messages exposes chat/community switching and an official-campus community summary before the available circles.
- Search uses a single-line responsive field and adaptive result layouts.

## Cost and performance guardrails

- Native screens call the existing backend APIs; no duplicate backend service is deployed for Android.
- Remote media is streamed through the shared media components and is not bundled into the APK.
- Phone and tablet layouts share the same feature code and only change presentation at window-size boundaries.
- Unit tests cover adaptive layout selection, hub filters, social post actions and social-link safety.

## Release

- Android version: `0.1.7` (`versionCode 8`)
- Artifact: `artifacts/Vyb-0.1.7-debug.apk`
- Verification: `gradlew testDebugUnitTest lintDebug assembleDebug` plus authenticated API 35 emulator interaction and media checks.
