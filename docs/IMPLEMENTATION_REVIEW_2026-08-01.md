# Implementation review — 1 August 2026

## Scope

This review covers the current uncommitted Android, PWA, media, marketplace,
search, profile, chat, app-update, and design-token changes. The open-ended
Task Set 2 feature/optimization hunt was intentionally excluded.

## Productionized in this pass

- Android post cards now follow the PWA interaction contract: the card itself
  does not hijack taps, while post media opens the immersive post viewer.
- The Android immersive viewer now owns working reaction, comments, save,
  share, repost, edit, delete, report, profile, video playback, and dismissal
  actions.
- Comment anonymity, GIF selection, local image attachment, media-only
  comments, edit, delete, report, reaction, reply, and swipe-to-reply are wired
  to the social APIs.
- Android share targets now come from real encrypted chat conversations and a
  shared post is sent as an encrypted `vibe_card`; fake people and fake success
  toasts are no longer used.
- Add-to-story opens the real story composer. Reaction member rows open real
  profiles and the visible Follow control calls the follow API.
- Search now renders People, Posts, Vibes, and Marketplace result lanes and
  preserves the selected post/vibe ID when navigating to its viewer.
- Public-profile tiles navigate to their post/vibe. The profile Post action
  opens the real post composer.
- Marketplace create/contact surfaces remain open when a mutation fails and
  close only after backend success.
- PWA media remains muted for autoplay, video taps reach the media lightbox,
  the lightbox has consistent image/video UI toggling, and its visible Follow
  button calls the backend.
- Inactive Android Vibes videos are paused, full-screen videos autoplay, and
  the story scrubber supports tap, drag, and accessibility progress actions.
- Invalid update-manifest checks are logged instead of being silently lost.
- Android post and avatar uploads now stream content URIs directly to the media
  endpoint instead of Base64-encoding full files in memory.
- Reaction-member responses include exact viewer/follow state, so both clients
  render truthful Follow actions without per-row profile requests.
- Notifications and marketplace search preserve entity IDs. Posts outside the
  first feed page are fetched by ID with the normal visibility checks.
- The Android Saved profile tab now queries real saved posts and distinguishes
  loading, error, empty, and populated states.
- The message inbox refreshes on resume. Active community conversations use a
  foreground-only, bounded 15-second reconciliation loop while direct chats
  continue to use realtime socket events.
- Android and PWA dark surfaces now share the same semantic navy surface family.
- Post media limits are consistently enforced at 8 across Android, PWA, and the
  backend. Multi-image database rows remain independent carousel assets, while
  adaptive Vibe encodes remain grouped as video variants.
- Android post lightbox media now has a visible pager counter and position
  indicators, filters invalid URLs, and renders a clear no-media fallback.
- System Back dismisses the active post lightbox and returns to the feed instead
  of finishing the activity. The feed now has a working Material pull gesture,
  branded Vyb refresh mark, and a scroll-aware create-post FAB.
- The post studio is a single bottom sheet for text and media posts. The old
  nested content layer that intercepted touches after dismissal was removed.
- Post media selection is additive up to eight items. Every selected item has
  explicit edit/remove actions; horizontal swipes reorder the publish order,
  and the shared editor provides crop/fill, transform, text, sticker, draw,
  rotate, undo, redo, and preview controls.
- Composer state is debounced into a device-local draft, including selected
  media URIs, order, caption, location, and edits. Accidental Back restores the
  draft on the next open, while successful publication clears it. Manual Save
  draft and scheduled-publish controls are visible in the same sheet.
- Native upload first uses the constant-memory binary endpoint and performs one
  bounded legacy JSON retry when an older deployed backend returns the rollout
  mismatch `INVALID_JSON`; uploaded comment images store R2 URLs, not data URLs.
- Vibe right-to-left profile navigation now follows the documented direction,
  excludes the scrubber lane, and shows a progressive profile hint. The actual
  Vibe player owns interactive seek, timestamp tooltip, 2x hold playback, audio
  track detection, a compact in-frame mute affordance, and device-persistent
  mute preference.
- PWA parity now includes the same persistent Vibe mute preference and an
  accessible interactive video scrubber with drag thumb and timestamp tooltip.
  The mobile web feed also exposes the branded pull-to-refresh interaction while
  preserving its scroll-aware create-post control.
  Post/story/vibe composer text, audience, anonymity, comment policy, community,
  and schedule state auto-save after edits so accidental browser Back does not
  discard the recoverable portion of a draft; manual Save draft remains available.
- Profile, Games/Events Hub, and Chats/Community selectors support horizontal
  swipes in addition to taps.
- Older no-update manifests may omit `apkSha256`; they now deserialize safely,
  while update downloads still require a valid 64-character checksum.
- Phone layouts no longer duplicate Create Post in the top app bar. Android and
  PWA now reveal a translucent brand-tinted glass FAB only after the feed has
  moved away from its initial scroll position; wide layouts retain the header
  action.
- The shared media workflow supports live in-gesture drawing, pinch-resizable
  text, crop/fill framing, an expanded Emojis tray, an explicit preview exit,
  and media-specific export copy. Android exposes a direct drag handle plus
  accessible move controls, while PWA thumbnails use pointer/touch reordering.
- Android and PWA Vibe players expose matching play, pause, and 2x feedback.
  Native hold-to-speed consumes the following tap so releasing a hold resumes
  normal playback instead of pausing it.
- Native post lightbox images support bounded pinch zoom and pan. PWA lightbox
  zoom now applies a visible image transform for tap, pinch, and wheel input.
- PWA post media receives the same per-item edit entry point as native, with
  edited output replacing the selected item without changing its carousel slot.
- Android and PWA composer previews now use a real multi-media carousel with an
  active position counter, previous/next or swipe navigation, per-item edit and
  remove controls, and order-preserving thumbnail reordering. Native emulator QA
  moved item one to slot three and confirmed both thumbnail and pager order.
- The feed create-post FAB is direction-aware: hidden at the initial position,
  revealed while moving deeper into the feed, and hidden while moving back
  toward the top. This behavior is shared by Android and the phone PWA.
- Post editing is publish-intent aware. Post images keep an original, square,
  4:5, or 16:9 frame instead of being forced into a story canvas; story/vibe
  output remains 9:16. Drawing is live, transforms are pinch/drag based, and
  edited media replaces its original carousel slot.
- Vibe play/pause feedback now renders above the native video surface and fades
  after 700 ms. The paused frame remains unobstructed for screenshots. PWA uses
  the equivalent transient feedback animation.
- Primary navigation follows platform conventions on Android and PWA: selecting
  another icon switches tabs; reselecting the active icon returns that section
  to its root/top and refreshes its data.
- Native light theme surfaces now use the shared semantic palette instead of
  dark-only backgrounds across Marketplace controls, Search discovery, Profile
  and its grids, the post composer and settings, and the comments sheet.
  Immersive media viewers intentionally retain black media-safe chrome.

## Verified

- `pnpm check`
- `pnpm --filter @vyb/web check`
- `pnpm --filter @vyb/web build`
- `gradlew :app:compileDebugKotlin`
- `gradlew :app:testDebugUnitTest :app:assembleDebug`
- Emulator: single-sheet open/close/reopen remained touch-responsive; local
  draft restored; post lightbox Back returned to Home; pull refresh exposed the
  branded loading semantic; mute preference survived Home/Vibes navigation.
- Emulator (0.1.11): phone Home started without a duplicate top Post action;
  scrolling revealed the glass FAB; the composer reopened with its local draft;
  Vibe long-hold showed live 2x feedback and resumed normal playback on release;
  single tap showed Play feedback; AndroidRuntime reported no crash.
- Emulator (0.1.12): Home FAB passed top/down/up visibility checks; a three-image
  post exposed a `1/3` carousel; dragging the first thumbnail to the third slot
  changed the order from `66,64,63` to `64,63,66` and synced the pager to `3/3`;
  post Crop exposed Original, Fill, 1:1, 4:5, and 16:9 framing; active Home
  reselect returned to the header and triggered refresh; Vibe feedback was
  visible immediately after pause, absent after the fade interval, and the
  video frame remained unchanged.
- Emulator (0.1.13): Home FAB is visible on landing, hides while scrolling
  deeper into the feed, and returns on the first reverse scroll. Published
  post media now owns its horizontal pager gesture instead of the outer
  open-post tap target; Android and PWA show slide position chrome, and the
  feed mapping test preserves mixed image/video items in backend order.
- The post composer now uses a shorter caption surface, an explicit media
  count, a full-width picker, clearer add-more guidance, direct Draft/Schedule
  actions, and a single primary Share action without removing editing or
  reordering controls.
- Minified release APK/R8 mapping were generated by the existing release build.
- Emulator (0.1.17): light-mode Marketplace, Profile, Settings, post composer,
  and comments sheet passed visual contrast QA; dark-mode Profile remained
  visually correct after the semantic-token migration.
- Emulator (0.1.18): the same running Home activity passed gesture to
  three-button to gesture navigation-mode switching. The app bottom navigation
  remeasured above the 126 px three-button system inset and returned to the
  gesture inset without relaunching. The implementation uses Android-reported
  navigation, tappable-element, and mandatory-gesture insets rather than a
  device-specific bottom padding.
- Emulator (0.1.19): the Home brand mark increased from 36 dp to 42 dp and its
  search/chat glyphs from 20 dp to 24 dp while retaining accessible 48 dp tap
  targets. The animated loading mark keeps its formation timing but no longer
  draws duplicate translucent halos or arm shadows around the solid mark.
- Emulator (0.1.20): post metrics now identify the backend value as saves, not
  shares. Feed and lightbox share actions open the same internal secure sharing
  sheet as Vibes. A save remained selected across a Home/Profile/Home round
  trip, appeared in Saved, and an unsave removed it immediately; the test shelf
  changed from three items back to two and the original account state was
  restored. A unit regression covers stale feed snapshots after save mutation.
- Emulator (0.1.21): Vibes use a fill-measured native video surface, removing
  the extra letterbox bands while retaining center-crop behavior. Mixed-media
  feed videos are muted and autoplay only when both their carousel page is
  selected and the post is mostly visible. The composer now supports multiple
  local drafts, explicit load/discard, a draft-count badge, Back-to-draft with a
  small confirmation toast, exact native date/time selection, immediate sheet
  collapse on publish, and a progress ring around the Home publish FAB. Draft
  loading also synchronizes the outer Post/Story/Vibe selector instead of
  leaving two conflicting publish intents. Profile Post, Vibe, and Saved tiles
  now deep-link into the corresponding interactive detail surface.
- Web parity (0.1.21): the scheduling dialog accepts an exact local date and
  time and converts it to the existing ISO scheduling contract. Existing web
  carousel visibility autoplay, media-cover behavior, and profile entity links
  were revalidated against the Android implementation.
- Final PWA parity (0.1.21): the legacy auto-loaded single text draft is
  replaced by an IndexedDB multi-draft shelf that restores media, requires an
  explicit load choice, supports load/discard badges, removes only the
  published draft, and surfaces the real background-publish progress on the
  Home FAB. Draft and scheduling panels now use semantic light/dark tokens.
  Composer blob URLs are retained across add/reorder state changes and revoked
  only when removed, replaced, or when the composer unmounts.
- Verification (0.1.21): `pnpm --filter @vyb/web check` and
  `gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug`
  passed. Emulator QA confirmed migrated draft count, blank composer reopen,
  explicit draft load/discard, synchronized Story intent, the Post schedule
  menu and native calendar picker, profile-tile detail navigation, and Vibe
  fill rendering without an AndroidRuntime crash.

## Recommendation status

All six recommendations from the initial review are implemented. Native R2
uploads use streaming request bodies (the existing JSON path remains only for
web compatibility), social navigation is entity-aware, Saved is backed by its
own query, community chat reconciliation is lifecycle bounded, and dark surface
colors are centralized through shared semantic tokens.

## QA environment note

Android emulator QA uses the available `Vyb_API_35` target. APK artifacts are
versioned under `artifacts/` with a matching SHA-256 checksum.
