# ADR-005: Social graph, live feed freshness, and universal share cards

Date: 2026-08-02
Status: accepted target design
Owners: Social Platform, Chat, Identity, and Product

## Implementation status (2026-08-07)

The Social data model now has persisted block records and recommendation
feedback, while the backend exposes follow/following/mutual lookups and
block-aware Social feed/profile paths. Android and PWA have the corresponding
per-content feedback and block-control foundations.

This ADR is deliberately not complete: durable feed change/outbox records,
cross-instance fanout, bounded delta reconciliation, rule-based rotating
suggestions, universal block enforcement, and the shared encrypted entity-card
resolver remain the target design and are release gates. No client should
claim that a Social-only block filter protects chat, market, or sharing.

## Context

Vyb already has tenant-scoped follow/unfollow, follower/following lists, basic
recent-profile suggestions, three post visibility choices, a social WebSocket,
and post/vibe sharing to direct chat. Those pieces are not yet a complete
social contract:

- mutual connections and user-to-user blocking are not persisted;
- suggestions are recent profiles rather than a rotating, filtered relevance list;
- the client label `public` means verified campus access, while the database default is `tenant`;
- the current WebSocket fanout is process-local and cannot recover missed events after reconnect or Cloud Run instance changes;
- chat sharing copies a post/vibe snapshot and other entity types do not share one permission-aware card contract.

This ADR defines the feature-completion target before reliability,
optimization, and release hardening begin.

## Decision

Keep Firebase SQL Connect/Cloud SQL PostgreSQL as durable truth and the Cloud
Run modular monolith as the authorization boundary. Clients shall not query
social tables directly. Realtime is an invalidation and reconciliation aid,
never a second source of truth.

The target consists of five linked contracts:

1. an idempotent social graph with explicit block edges;
2. server-enforced campus, followers, and community audiences;
3. cursor feeds with durable delta reconciliation and lightweight realtime hints;
4. deterministic, rotating people suggestions with mutual context;
5. one permission-aware share-reference format for posts, vibes, events, games, Marketplace listings, and profiles.

## 1. Social graph contract

### Relationships

- `followers`: active inbound follow edges where `following_user_id` is the profile owner;
- `following`: active outbound follow edges where `follower_user_id` is the profile owner;
- `isFollowing`: the viewer actively follows the subject;
- `followsViewer`: the subject actively follows the viewer;
- `mutuals`: accounts the viewer follows that also follow the subject. This is `(viewer following) INTERSECT (subject followers)`, not a client-side intersection of downloaded lists.

MVP campus profiles accept follows immediately; private-account follow
requests are deferred. Follow and unfollow mutations are idempotent. Re-follow
reactivates the existing soft-deleted unique edge instead of inserting a second
`follow_key`. The API returns the new relationship state plus authoritative
counters, and clients optimistically update then reconcile with that response.

### Required persistence

Retain `follows` with its unique `follow_key` and soft-delete semantics. Add:

- `user_blocks(id, tenant_id, blocker_user_id, blocked_user_id, block_key, created_at, updated_at, deleted_at)`;
- a unique active identity through `block_key`;
- indexes for active inbound/outbound follows and both directions of blocks;
- a bounded `feed_events(sequence, tenant_id, target_user_id, event_type, entity_type, entity_id, entity_version, created_at, expires_at)` reconciliation log or the equivalent transactional outbox projection.

Follow/block writes must be backend-owned SQL Connect operations with explicit
`NO_ACCESS` connector authorization, because the backend resolves Firebase
identity, tenant membership, and policy. Multi-row block changes are one transaction.

### Suggestions

`GET /v1/users/suggestions` returns a cursor-paginated candidate set and a
stable `refreshToken`. It must exclude self, existing follows, either direction
of an active block, suspended/deactivated profiles, profiles outside the
viewer's tenant, and incomplete or undiscoverable profiles.

The initial score is rules-based, not ML: mutual count, same campus course or
cohort, shared active communities, profile completeness, and recent healthy
activity. It must not inspect private chat content. Candidates inside the same
score band are deterministically rotated using viewer ID plus a daily seed and
the refresh token; do not use unbounded `ORDER BY random()`.

Refresh returns a new page/seed so the same small list is not repeated, while
server-side exclusions prevent already-followed or blocked users from
reappearing. The response includes up to three mutual previews and `mutualCount`.

## 2. Block contract

Block is a privacy boundary, not a client filter. `PUT` and `DELETE
/v1/users/{username}/block` are idempotent and server-enforced across every
read and mutation.

Creating a block must atomically:

- activate the directed block edge;
- soft-delete active follow edges in both directions;
- reject new follows, mentions, direct-chat creation, Marketplace contact, and share-to-chat attempts between the pair;
- remove both users from each other's search, suggestions, followers, following, mutuals, notifications, reaction/liker lists, profile, feed, Vibes, stories, and Marketplace discovery;
- stop new direct-message delivery and hide the conversation for the blocker.

Existing chat/content records are retained for abuse investigation, account
export, and legal retention; normal clients do not receive them. Shared
community membership does not override a block. Counts and empty/error
responses must not reveal which party blocked the other. Moderation access is
an audited server-only exception.

Unblocking does not restore follows, notifications, or conversations
automatically. Users must explicitly follow or start an allowed conversation again.

## 3. Post audience contract

| Canonical scope | Who may read | Publish rule |
|---|---|---|
| `campus` | active verified members of the post tenant | default |
| `followers` | author plus active followers in the same tenant | evaluated at read time |
| `community` | author plus active members of the selected community | `communityId` required |

The current wire value `public` is a legacy alias for `campus`; it never means
unauthenticated internet access. New clients send `campus`. The backend accepts
`public` for the current and previous mobile release, normalizes new writes to
`campus`, and reads old `public`/`tenant` rows as `campus` until a backfill is verified.

Audience checks run for feed/detail reads, comments, reactions, likes, saves,
reposts, notifications, internal shares, external deep links, and media
resolution. A repost or forwarded card cannot widen the source audience.
Follower and community access is dynamic: losing the relationship immediately
removes access. The author retains access unless the content is deleted or moderated.

The composer shows a concise explanation, defaults to Campus, lists only
joined communities, and may remember the last safe choice on that device.
Anonymous presentation is not an audience and remains launch-disabled.

## 4. Feed freshness contract

Users do not need to repeatedly pull-to-refresh. The standard flow is:

1. Fetch a reverse-chronological page using an opaque `(publishedAt, id)` cursor.
2. Keep one normalized local entity cache per signed-in account.
3. Receive small versioned invalidation hints for publish, edit, delete, reaction, comment, follow, unfollow, and block changes.
4. Fetch a bounded delta from `GET /v1/feed/changes?after=<sequence>` and apply it idempotently by `entityId + entityVersion`.
5. On reconnect, app foreground, login restore, or sequence gap, reconcile from the durable cursor before declaring the feed live.

When the user is at the top and idle, a new eligible post can be prepended after
a short debounce. While the user is reading lower in the feed, content must not
jump; show a `New posts`/`See new posts` pill that fetches and returns to the top
when tapped. Do not show an exact count by default: ranking, moderation, block,
and audience changes can make it unstable and computing a live total adds work
without improving the primary action. A bounded count may be added later only
when it is already exact in the fetched delta; never run a separate count query
for the pill.
Edits/reactions patch in place, while deletes, blocks, and revoked scope remove
items immediately. Pull-to-refresh and active-tab reselect remain explicit
recovery/re-ranking actions.

The existing `/ws/social` channel remains a hint transport but must not remain
process-local for multi-instance production. Publish its events through the
transactional outbox and a managed/shared fanout, or route all active sockets
through a deliberately single realtime owner until managed fanout is enabled.
Each event carries sequence, tenant, optional target user, entity type/id,
version, and no private body/media payload.

Do not attach a database listener to every feed row or poll every client every
few seconds. SQL Connect list refreshes must be narrowly scoped if used; the
backend delta endpoint is the canonical cross-platform contract and bounds Cloud SQL work.

## 5. Universal share-card contract

All shareable entities use an encrypted `ShareReference`, not a copied content snapshot:

```json
{
  "version": 1,
  "type": "post|vibe|event|game|market_listing|profile",
  "id": "opaque-entity-id",
  "action": "open",
  "context": null
}
```

For internal chat, the reference is encrypted in the existing E2EE message
payload using message kind `entity_card`. After decryption, the recipient calls
`GET /v1/share-cards/{type}/{id}`. That resolver rechecks block, tenant,
audience, membership, deletion, moderation, and entity state at view time and
returns the current card model:

- common: title, subtitle, thumbnail, badges, canonical path, and availability;
- post/vibe: Open, react, or comment when allowed;
- event: View, save, register/apply, or cancel according to state;
- game: Play or open the specific challenge/result;
- Marketplace: View item, save, or contact seller when available;
- profile: View profile or follow when permitted.

Forwarding reuses the reference and cannot broaden access. Deleted, sold,
expired, moderated, or newly blocked entities render a neutral `Unavailable`
card without leaking why.

External sharing uses one canonical HTTPS route, `/s/{type}/{id}`, Android App
Links, the equivalent PWA route, Android Sharesheet, and Web Share API with a
copy-link fallback. Because MVP content is verified-campus scoped,
unauthenticated Open Graph metadata is branded and generic; captions,
identities, prices, and media are returned only after authentication and
authorization. Public profile unfurls may be added later as an explicit user opt-in.

Share-created, share-opened, and card-action analytics record entity type and
coarse outcome, not encrypted chat text or recipient private data.

## API additions and changes

- `GET /v1/users/{username}/followers?cursor=`
- `GET /v1/users/{username}/following?cursor=`
- `GET /v1/users/{username}/mutuals?cursor=`
- `GET /v1/users/suggestions?cursor=&refreshToken=`
- `PUT|DELETE /v1/users/{username}/block`
- `GET /v1/feed?cursor=` returning `nextCursor`, `highWaterSequence`
- `GET /v1/feed/changes?after=` returning ordered, bounded changes
- `GET /v1/share-cards/{type}/{id}`
- `/s/{type}/{id}` canonical web/deep-link route

List APIs use cursor pagination; the current fixed-limit follower/following
responses remain a one-release compatibility path.

## Acceptance tests

Two-account and two-tenant tests must prove:

- follow/unfollow idempotency, authoritative counts, `isFollowing`, `followsViewer`, and mutual intersection;
- refreshed suggestions rotate and never include self, followed, blocked, suspended, incomplete, or cross-tenant profiles;
- block removes both-direction follows and suppresses every listed surface and write path; unblock restores nothing implicitly;
- Campus, Followers, and Community posts are readable and actionable only by their intended dynamic audience;
- a new post appears automatically at top or behind the new-post pill without disturbing scroll position;
- reconnect and missed-event recovery yield the same feed as a fresh query;
- each entity type renders and acts correctly in Android chat, PWA chat, copied links, and external share sheets;
- expired/deleted/blocked cards become unavailable with no data leak;
- Android and PWA produce equivalent behavior and accessibility semantics.

## Consequences

- Follow and block enforcement becomes a cross-module policy used by Social,
  Chat, Marketplace, Search, Notifications, Events, Games, and sharing.
- One durable change cursor adds small write/storage cost but prevents broad
  polling and makes realtime recovery testable.
- Rule-based suggestions are sufficient for 20,000-30,000 launch users and do
  not require an ML service.
- Generic external previews are less viral than public unfurls but preserve the
  verified-campus privacy promise.

## Rejected alternatives

- Client-only block filters: leak data through alternate endpoints and counts.
- Full feed polling: unnecessary Cloud SQL read load and poor mobile battery use.
- Inserting new posts into a scrolled feed immediately: disruptive UX.
- Copying complete entity snapshots into chat: stale, hard to revoke, and unsafe when audience membership changes.
- Treating `public` as internet-public: conflicts with the tenant-isolated MVP.
