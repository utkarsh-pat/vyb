# ADR 008: Shared social-feed realtime invalidation

Status: Deployed to production; retained two-account privacy/reconnect evidence pending.

## Decision

Vyb uses a sharded Cloudflare Durable Object WebSocket hub to notify connected clients that a tenant feed changed. The hub sends a content-free `social.feed.invalidated` frame. Web and Android then fetch the authorized feed or feed-change summary from the backend.

This is an invalidation channel, not a second feed database:

- Data Connect remains the durable source of truth.
- Post writes and their seven-day feed-change event commit in one transaction.
- The existing 45-second high-water reconciliation recovers disconnects and missed frames.
- Visibility, follows, communities, mutual blocks, and moderation remain backend authorization decisions.
- No post, comment, profile, entity identifier, or media URL is broadcast by the shared hub.

## Topology

Each tenant has 16 fanout shards. A client is assigned by a stable hash of its membership ID. The backend publishes one invalidation to every shard for that tenant. This avoids a single global coordination object while keeping connection assignment deterministic.

The Worker reuses the existing `vyb-games-realtime` deployment but uses an independent Durable Object class, route, binding, and secret:

- client socket: `/ws/social`
- backend publish: `/internal/social/publish`
- binding: `SOCIAL_FEED_HUBS`
- class: `SocialFeedHub`
- Worker secret: `SOCIAL_FANOUT_SECRET`
- backend secret: `VYB_SOCIAL_REALTIME_SECRET`
- backend origin: `VYB_SOCIAL_REALTIME_PUBLIC_ORIGIN`

## Security boundaries

The authenticated backend issues five-minute HMAC session URLs. The Worker validates the signature, expiry, allowed browser origin, tenant, user, and membership claims before accepting a socket. The internal publish endpoint uses a separate constant-time bearer-secret check and accepts at most 4 KiB.

The originating membership may be excluded because its UI already applies the mutation optimistically. Other members receive only the reason and emission time.

## Failure behavior

Realtime fanout is best-effort and never rolls back a successful post mutation. If the Worker is unavailable, clients converge through the transactional high-water event and periodic reconciliation. Missing production fanout configuration is reported as `social-realtime-fanout` degradation by runtime readiness.

## Release evidence required

Completed on 2026-08-30:

1. Deployed the Data Connect `social` connector operations containing the atomic feed-change mutations; the production schema compatibility check passed.
2. Deployed Worker migration `v2` as version `3fd2837f-fc7a-4589-a42e-2d39351b0f2e` at `https://vyb-games-realtime.ceoutkarshpatel.workers.dev` in the `ceoutkarshpatel@gmail.com` account.
3. Stored the shared value independently as Cloudflare `SOCIAL_FANOUT_SECRET` and Google Secret Manager `VYB_SOCIAL_REALTIME_SECRET`; Cloud Run receives only the Secret Manager reference.
4. Built immutable backend image tag `bf94857ff80a-rtfanout-20260830-0001` and promoted revision `vyb-backend-00024-h6p` through 5%, 25%, 50%, and 100% traffic after zero-error canary checks.
5. Verified the canonical production path with an authenticated campus account: session mint succeeded, the Worker accepted the signed token, the Durable Object socket remained open, and the first frame was `social.connected`.

Still required before closing the release gate:

1. Verify two accounts on separate browser/device sessions: create, update, react/comment, block/unblock, and delete.
2. Capture socket reconnect and 45-second recovery evidence with no restricted-content leakage.
