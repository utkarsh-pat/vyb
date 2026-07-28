# apps/backend

Phase 1 backend runtime for Vyb.

Responsibilities:

- modular monolith HTTP server
- auth boundary, request context, and public API handling
- domain modules for identity, campus, social, and resources
- SQL Connect access with development-only local stores
- future extraction-ready module boundaries without multiple deployables today
- production hosting on Google Cloud Run through the repo root container build
- optional continuous deployment through the root `cloudbuild.backend.yaml` trigger config

## Native Android API additions

These routes use the normal Firebase bearer authentication context. Tenant and
user IDs are resolved server-side rather than accepted from the client.

- `PUT /v1/posts/:postId/save`
- `GET /v1/events`
- `GET /v1/events/:eventId`
- `POST /v1/events`
- `PUT /v1/events/:eventId/save`
- `PUT /v1/events/:eventId/interest`
- `POST /v1/events/:eventId/register`
- `GET /v1/events/:eventId/registrations` (event host only)
- `POST /v1/events/:eventId/cancel` (event host only)
- `GET /v1/notifications`
- `PUT /v1/notifications/:notificationId/read`
- `PUT /v1/notifications/read-all`
- `POST /v1/notifications/register-device`
- `GET /v1/chats/socket-token?conversationId=...`

Post saves use Data Connect `PostSave` operations, with the local social store
as the development fallback. Events reuse the `CampusEventStore` JSON contract
and local store used by the web app.

Android device registration persists the FCM registration token in both the
endpoint and provider-specific subscription fields. The backend FCM outbox:

- queues push-enabled, privacy-safe notifications for registered Android devices
- sends notification plus deep-link data through Firebase Admin Messaging
- uses the existing pending/sent/failed delivery lifecycle and exponential retry
- removes devices when FCM reports an invalid or unregistered token
- leaves existing browser Web Push subscriptions and delivery handling unchanged

On Cloud Run, Firebase Admin uses Application Default Credentials from the
service identity. Local development may use the repository's existing Firebase
Admin environment credential options. No service-account JSON is embedded in
the backend.

The worker runs once after startup and every 15 seconds while the backend
instance has CPU. Set `VYB_FCM_WORKER_INTERVAL_MS` (minimum 5000) to tune the
interval, or `VYB_FCM_WORKER_DISABLED=1` to disable it. Cloud Run can suspend
background CPU or scale to zero, so production should also invoke the backend
on a schedule (or enable instance-based CPU) when delivery latency must be
independent of normal traffic.

Run the focused outbox tests with:

```bash
pnpm --filter @vyb/backend test:notifications
```

## Production runtime

Cloud Run is the only backend deployment target. Vercel hosts only `apps/web`.
Cloud Run uses workload identity, SQL Connect, FCM and R2 server credentials.
WebSocket routes and background workers therefore share one runtime contract.
