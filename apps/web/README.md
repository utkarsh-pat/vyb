# apps/web

Phase 1 shipping client.

Responsibilities:

- Next.js App Router
- responsive mobile and desktop web experience
- PWA support
- install prompt and service worker registration
- backend-backed shell reads with graceful fallback mode
- Firebase Auth login UI with secure server-side session bootstrap
- cookie-backed viewer session for SSR, route handlers, and backend-backed auth-aware testing
- authenticated `/home` feed landing surface plus a secondary `/dashboard` profile route
- route handlers for post and resource creation through the backend
- SSR and client rendering where appropriate
- no privileged backend business logic
- recommended production host is Vercel, with the backend API base URL pointing at the separately deployed backend

## Vercel deployment

Create a dedicated Vercel project for this app from the monorepo with these settings:

| Setting | Value |
| --- | --- |
| Root Directory | `apps/web` |
| Include source files outside Root Directory | Enabled |
| Framework Preset | Next.js |
| Install Command | `pnpm install --frozen-lockfile` |
| Build Command | `pnpm build` |
| Output Directory | Leave unset (Next.js default) |
| Node.js | 20.x or newer |

The repository root lockfile and `packageManager` field pin pnpm. The outside-root
source option is required because this app imports the workspace packages under
`packages/`.

Set the following variables for Production and Preview. Do not commit their values.

### Required for the app and login

| Variable | Purpose |
| --- | --- |
| `NEXT_PUBLIC_API_BASE_URL` | `https://api.vybnet.app`, with no trailing slash |
| `VYB_API_BASE_URL` | Same backend URL for server-side route handlers |
| `VYB_SESSION_SECRET` | Long random secret used to sign the web session |
| `VYB_INTERNAL_API_KEY` | Long random shared secret; its value must match the backend |
| `NEXT_PUBLIC_FIREBASE_API_KEY` | Firebase web application API key |
| `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN` | Firebase Auth domain |
| `NEXT_PUBLIC_FIREBASE_PROJECT_ID` | Firebase project ID |
| `NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID` | Firebase web application sender ID |
| `NEXT_PUBLIC_FIREBASE_APP_ID` | Firebase web application ID |
| `FIREBASE_PROJECT_ID` | `vybnet`; server-side Admin SDK project |

Do not deploy a service-account JSON to Vercel or the browser. Privileged data
and media operations belong to the Cloud Run backend.

### Feature-dependent variables

| Variable | Purpose |
| --- | --- |
| `NEXT_PUBLIC_FIREBASE_DATABASE_URL` | Firebase Realtime Database features such as live presence |
| `NEXT_PUBLIC_FIREBASE_MEASUREMENT_ID` | Firebase Analytics |
| `VYB_VAPID_PUBLIC_KEY` / `NEXT_PUBLIC_VYB_VAPID_PUBLIC_KEY` | Web push public key |
| `VYB_VAPID_PRIVATE_KEY` | Web push private key |
| `VYB_VAPID_SUBJECT` | Web push contact, for example `mailto:support@example.com` |
| `VYB_SUPER_ADMIN_EMAILS` | Comma-separated super-admin allowlist |
| `VYB_CONNECT_SESSION_SECRET` | Dedicated Connect game-session signing secret |

After both projects have stable domains, allow the frontend origin in the
backend's CORS configuration and add the frontend domain to Firebase
Authentication's authorized domains.
