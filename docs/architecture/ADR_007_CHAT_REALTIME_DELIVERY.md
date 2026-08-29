# ADR-007: Low-latency, reliable chat delivery

Status: Accepted for staged implementation
Date: 2026-08-09

## Decision

Vyb chat uses a local-first client experience and separates realtime delivery
from durable persistence work:

1. The sender renders an encrypted optimistic envelope immediately and assigns
   it a client idempotency key.
2. The authenticated realtime channel carries committed encrypted envelopes,
   typing, presence, delivery receipts, read receipts, and retry hints.
3. The primary database remains the durable source of truth for messages,
   membership, block policy, retention, and audit state.
4. Inbox preview generation, analytics, push notifications, and other derived
   work do not delay realtime fanout.
5. A recipient renders and decrypts the realtime envelope before persisting its
   read receipt. Receipt failure is retried and must not hide the message.

The current Android and PWA clients implement the local-first path. Android no
longer reloads the conversation before each send or after every socket message.
The backend now emits a committed message before sender-only preview and
analytics work.

## Service-level targets

These are pilot objectives, not claims about an unmeasured production system:

- local echo: p95 below 50 ms;
- sender acknowledgement on a warm regional connection: p95 below 500 ms;
- online recipient render in the same region: p95 below 750 ms;
- reconnect convergence: p95 below 3 seconds;
- no accepted message loss, duplicate display, or cross-tenant delivery.

Record client enqueue, server accept, durable commit, fanout, recipient render,
delivered, and read timestamps under one trace id. Track p50/p95/p99 separately
for text, media, cold starts, and reconnects.

The Android debug build emits content-free `VybChatLatency` logcat events for
send-to-accept, send-to-delivered, send-to-read, peer socket arrival, socket
connect/reconnect, and presence-heartbeat RTT. Run
`scripts/measure-chat-latency.ps1` with both QA emulators to capture a bounded
test window and produce p50/p95/p99 plus a raw CSV. Production telemetry must
hash or rotate message correlation identifiers and must never include message
plaintext, attachment names, email addresses, or encryption material.

## Production fanout topology

### Pilot transport routing

Firebase Hosting remains the public REST origin at `api.vybnet.app`, but it is
not used as a WebSocket proxy. Chat and Scribble socket tokens are issued with
the explicit `VYB_REALTIME_PUBLIC_ORIGIN` Cloud Run origin and clients upgrade
directly to that service. Cloud Run request timeout is 3600 seconds so a healthy
socket is not terminated by the former five-minute HTTP timeout. Keep REST and
realtime origins independently configurable; never infer production WebSocket
support from a successful Hosting rewrite response.

Production revision `vyb-backend-00022-bz2` was the first revision verified
with this routing. Both Android QA devices connected to the direct socket and
received messages in both directions without a manual refresh.

The process-local WebSocket map is suitable only for local development and a
single backend instance. It is not a production fanout boundary: two users may
connect to different Cloud Run instances.

Before multi-instance launch, route conversation sockets through a shared
regional realtime coordinator. The preferred cost-aware option is a Cloudflare
Durable Object per conversation shard using hibernatable WebSockets. The object
authenticates a short-lived, conversation-scoped token, fans out encrypted
envelopes, maintains ephemeral presence, and sleeps while sockets remain
connected. A transactional message outbox in the authoritative backend supplies
retry and reconciliation after partial failures.

Firebase Realtime Database is a technically valid alternative for presence and
offline synchronization, but it should not be introduced as a second message
source of truth beside Data Connect. Doing so would add dual-write ordering,
authorization, retention, deletion, and billing complexity. If selected later,
it must be the explicitly owned realtime transport with an outbox bridge rather
than an ad-hoc duplicate store.

## Delivery and offline state machine

`pending -> accepted -> sent -> delivered -> read`

- Pending is client-local and retryable.
- Accepted means the server validated identity, tenant, block policy, payload,
  TTL, and idempotency key.
- Sent means the durable write committed.
- Delivered means an authenticated recipient device acknowledged the envelope.
- Read means the user opened the conversation and policy allows exposing read
  receipts.

Clients retain a bounded encrypted outbox and retry with the same idempotency
key after reconnect. Reconciliation uses a cursor, never a full-history reload.
Ordering is server-assigned per conversation. Duplicate envelopes are ignored
by server message id and client idempotency key.

## Presence and privacy

Presence is soft state with a short lease and heartbeat. Show `online` only
while an unexpired device lease exists; otherwise show a privacy-controlled
last-seen value. Typing expires automatically and is never persisted. Block,
tenant, membership, and conversation authorization are checked before socket
subscription and again before every accepted write.

The current pilot heartbeat is foreground-only and updates process-local soft
state; it does not write Data Connect or Firestore. Consequently its direct
database cost is zero, but HTTP request/compute cost grows with active chat
concurrency and process-local state cannot provide correct presence across
multiple Cloud Run instances. Firebase Realtime Database would improve crash
and disconnect detection through `/.info/connected` and server-owned
`onDisconnect`, but it would not reduce message-delivery latency and would add
a second realtime subsystem. Keep the pilot heartbeat until the shared
realtime coordinator is deployed; then derive presence from authenticated
socket leases. RTDB remains an acceptable presence-only fallback if that
coordinator is deferred.

Typing is rendered as an ephemeral incoming bubble on both web and Android.
The three-dot wave uses the same peer bubble geometry, and the first incoming
message event clears typing before inserting the real bubble. No artificial
delay is added to create the visual transition.

## Encryption boundary

The existing P-256 ECDH/AES-GCM envelopes protect message and attachment
content from normal server-side plaintext access, but they are not equivalent
to WhatsApp's Signal protocol. They do not yet provide the audited Double
Ratchet, pre-key sessions, forward secrecy, post-compromise security, safety
number verification, or complete multi-device session management.

Do not market the current scheme as WhatsApp-grade end-to-end encryption. For
that claim, adopt a maintained, audited Signal Protocol implementation, define
multi-device key lifecycle and recovery, and commission an external security
review. Never implement a custom ratchet.

## Media and voice

Encrypt media locally, upload directly to R2 with short-lived scoped upload
authorization, then send only encrypted attachment metadata through chat.
Generate encrypted thumbnails/waveforms on-device where practical. Text and
thumbnail delivery must not wait for full media download. Voice calling is a
separate WebRTC product requiring TURN fallback, call signaling, abuse controls,
and call-specific privacy UX; voice notes remain encrypted chat attachments.

## Release gates

- shared multi-instance fanout and transactional outbox;
- deterministic idempotency and per-conversation ordering tests;
- two-account tests for foreground, background, killed app, airplane mode,
  reconnect, block during send, and key rotation;
- load test at target concurrent sockets and burst message rate;
- p95/p99 latency dashboards, reconnect/error alarms, and cost alarms;
- attachment, voice-note, TTL, deletion, and receipt privacy matrices;
- external review before any WhatsApp-grade encryption claim.
