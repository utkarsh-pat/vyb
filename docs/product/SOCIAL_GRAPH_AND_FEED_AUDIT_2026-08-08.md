# Social graph, feed liveliness, and games audit

Date: 2026-08-08

## Outcome

This pass fixes the game-route crash, closes the concrete Social privacy and
relationship defects found in the current implementation, and makes realtime
feed arrival behave like a production social feed. It does not remove product
UI or silently replace intentional features.

## Games

- Static game slugs are defined in the server-safe
  `apps/web/src/lib/mini-game-slugs.ts` module. Server routes must never import a
  validator from a `"use client"` component.
- The allow-listed iframe games are Chess, Ludo, Colour Sort, N-Queens Solver,
  and Word Puzzle. Existing Vyb Connect, daily N-Queens, and Scribble keep their
  first-party routes and are not replaced by Connect Four.
- Static games remain sandboxed, dependency-free client games. Connect and
  daily N-Queens are backend-authoritative; Scribble is the realtime multiplayer
  game. A static game's local progress is not represented as server multiplayer.
- An unused generic realtime token/verification stub was removed. It referenced
  a Worker with no repository implementation and had no game client consumer;
  retaining it would falsely imply that static Chess/Ludo/puzzles were online
  multiplayer.

## Social graph invariants

1. Follow, unfollow, and block are backend-authoritative and tenant-scoped.
2. A soft-deleted follow edge is reactivated on re-follow; the unique
   `followKey` is never inserted a second time.
3. Blocking removes both directional follow edges. Unblocking never restores
   those relationships automatically.
4. Either block direction hides the profile, search/suggestion result,
   follower/following/mutual lists, feed content, stories, and primary direct
   chat access.
5. Suggested users exclude existing follows and blocked accounts, then rank
   mutual connections, same stream/course context, and bounded campus
   popularity. This is a deterministic cold-start policy, not opaque ML.
6. Audience decisions are server-owned: `public`, `followers`, or `community`.
   Client-side filtering is never the authorization boundary.

## Feed liveliness and privacy

- Public posts may be delivered as full realtime feed cards because every
  signed-in tenant member is eligible to discover them.
- Followers/community payloads are never broadcast on the tenant-wide socket.
  Instead, the socket sends a content-free `social.feed.invalidated` signal.
  The authorized page shows **Feed updates available** and refreshes through
  the normal server audience checks when the user accepts it.
- Public realtime posts are buffered behind a **new posts** pill instead of
  shifting the user's scroll position. Duplicate socket events are rejected by
  post ID.
- Update/delete/reaction/comment frames for restricted posts are not exposed to
  unauthorized socket clients.
- The current primary feed remains cursor-stable chronological delivery. The
  durable `not_interested` signal is enforced now; a scored For You lane must
  use a versioned rank cursor before it replaces chronological pagination.

## Content measurement

- Qualified views remain non-unique events while reach is unique per eligible
  viewer.
- Video play/view/replay/completion and carousel progress remain separate
  signals.
- Server cooldowns, self-view exclusion, authorization checks, and the existing
  14-day raw-event retention prevent inexpensive replay inflation while keeping
  enough data for short-term recommendation features.

## Verification performed

- `pnpm --filter @vyb/backend test`: 36/36 passed, including restricted-post
  realtime eligibility and analytics uniqueness/cooldown coverage.
- `pnpm --filter @vyb/backend check`: passed.
- `pnpm --filter @vyb/web check`: passed.
- `pnpm --filter @vyb/web build`: passed; all 80 route-generation entries and
  the dynamic `/hub/gameshub/[slug]` route compiled successfully.
- Firebase Data Connect compiled the updated Social operations and regenerated
  the Social admin SDK. The new operation must be deployed before production
  re-follow behavior depends on it.

## Remaining launch gates

1. Deploy the Social Data Connect operation containing `ActivateFollow`, then
   run the two-account follow -> unfollow -> follow and block -> unblock matrix
   against production-like data.
2. Extend the shared block policy to Marketplace contact, notifications,
   events/games invitations, and every entity share-card resolver. Social and
   the primary direct-chat paths are protected today; universal enforcement is
   still a launch gate.
3. Add automated two-tenant fixtures for audience changes while clients are
   backgrounded, reconnecting, and offline.
4. Build the For You lane as a separate candidate/rank system with stable rank
   cursors, diversity/fatigue caps, transparent reasons, and a control group.
5. Move Scribble room presence/fan-out from process memory to a durable shared
   coordinator before running more than one backend instance.

These gates are intentionally explicit. Passing a compile or unit test is not
evidence that a live cloud schema was deployed or that a two-device policy
matrix was exercised.
