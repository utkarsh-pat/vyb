# Android and PWA parity

## Source of truth

The PWA implementation is the product and interaction source of truth. The native Android client reproduces those flows with Jetpack Compose instead of embedding web pages.

| Surface | PWA reference | Android implementation |
| --- | --- | --- |
| Home feed, stories and post actions | `apps/web/src/components/campus-home-shell.tsx` | `apps/mobile/app/src/main/java/social/vyb/app/ui/HomeScreen.kt` |
| Post reactions, comments, share, repost and save | `apps/web/src/components/use-social-post-engagement.ts` | `apps/mobile/app/src/main/java/social/vyb/app/features/social/SocialActionsUi.kt` |
| Profile, tabs and settings | `apps/web/src/components/campus-profile-shell.tsx` | `apps/mobile/app/src/main/java/social/vyb/app/features/profile/ProfileParityOverview.kt` and `ProfileSettingsHub.kt` |
| Search | `apps/web/src/components/campus-search-shell.tsx` | `apps/mobile/app/src/main/java/social/vyb/app/features/search/SearchScreen.kt` |
| Vibes | `apps/web/src/components/campus-reels-shell.tsx` | `apps/mobile/app/src/main/java/social/vyb/app/features/stories/StoriesVibesScreens.kt` |
| Games and events | PWA campus hub surfaces | `apps/mobile/app/src/main/java/social/vyb/app/features/fun/FunHubScreen.kt` and `features/hub/CampusHubScreen.kt` |

## Responsive rules

- Compact phone: edge-to-edge feed cards, 66 dp labelled bottom navigation, compact type scale, 36–40 dp visible controls inside at least 48 dp touch targets.
- Very small phone: 58 dp icon-only bottom navigation.
- Tablet at 600 dp and above: persistent navigation rail, wider content constraints, multi-column profile media and horizontally composed profile hero.
- Text respects Android font scaling. Fixed visual controls do not use text scaling as a substitute for layout adaptation.

## Interaction parity

- Home posts support reaction selection/removal, comments, Android share sheet, repost, save and full-post view.
- Profile supports Posts, Vibes and Saved views, follower/following entry points, safe social links, edit and settings.
- Vibes support like, comment, repost, share, profile navigation and creation.
- Campus Hub supports Games/Events switching, search and filter state, saved/registered/hosting event views and event actions.
- Search uses a single-line responsive field and adaptive result layouts.

## Cost and performance guardrails

- Native screens call the existing backend APIs; no duplicate backend service is deployed for Android.
- Remote media is streamed through the shared media components and is not bundled into the APK.
- Phone and tablet layouts share the same feature code and only change presentation at window-size boundaries.
- Unit tests cover adaptive layout selection, hub filters, social post actions and social-link safety.

## Release

- Android version: `0.1.5` (`versionCode 6`)
- Artifact: `artifacts/Vyb-0.1.5-debug.apk`
- Verification: `gradlew testDebugUnitTest assembleDebug`
