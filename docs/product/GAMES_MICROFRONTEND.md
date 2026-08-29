# Vyb Games: micro-frontends and live rooms

## Launch surface

The Games Hub is the single launch surface at `/hub/gameshub`. Its active cards route to either a server-authoritative Vyb game or a self-contained game page:

| Game | Route | Runtime |
| --- | --- | --- |
| Connect | `/hub/gameshub/connect` | Existing server-authoritative daily Connect game. It is intentionally not replaced with Connect Four. |
| Scribble | `/hub/gameshub/scribble` | Existing authenticated real-time Vyb game. |
| N-Queens | `/hub/gameshub/queens` | Existing server-authoritative daily puzzle. Its in-game **Solver** control opens the learning/visualizer mode. |
| Chess | `/hub/gameshub/chess` | First-party responsive local/online board game. |
| Ludo | `/hub/gameshub/ludo` | First-party responsive local/online board game. |
| UNO | `/hub/gameshub/uno` | First-party private online room game. |
| Colour Sort | `/hub/gameshub/color-sort` | Local static micro-frontend. |
| Word Puzzle | `/hub/gameshub/word-puzzle` | Local static micro-frontend. |

`apps/web/src/components/mini-games-controller.tsx` controls the remaining static games. It only accepts allow-listed static slugs and hosts them inside a responsive iframe. Chess, Ludo and UNO are rendered by `multiplayer-board-game.tsx`; their signed room session is obtained from a same-origin API and is never passed into a static iframe.

The allow-list and validator live in the server-neutral
`apps/web/src/lib/mini-game-slugs.ts`. The dynamic server page imports that
module directly; importing a validator from the client controller causes a
Next.js Server/Client boundary runtime error and is prohibited.

## Android runtime

The Android Games Hub exposes the same eight launch cards and intentionally
chooses the cheapest correct runtime per game:

- Connect, N-Queens and Scribble remain first-party native Compose surfaces.
- Chess local play is a native Compose board with a legal move engine, SVG
  pieces, move history, board flip, and Back/Forward/Live review.
- Chess online, Ludo and UNO use an authenticated, isolated WebView so the
  Durable Object remains the only authoritative multiplayer state machine.
  Firebase ID tokens are exchanged for a same-origin web session inside the
  container; tokens are not appended to URLs.
- Color Sort and Word Puzzle are bundled under
  `apps/mobile/app/src/main/assets/games/` and run offline. Their progress is
  device-local by design and creates no backend traffic.

The Android wrapper disables third-party cookies, pop-ups and content access.
Remote game failures are surfaced as an explicit retry/error state rather than
leaving an empty board. The web multiplayer deployment must pass its production
route smoke test before Chess online, Ludo or UNO can be marked release-ready.

## Static game paths

```text
apps/web/public/games/
  color-sort/index.html
  n-queens/index.html  # internal N-Queens Solver mode; not a second Hub card
  word-puzzle/index.html
```

Next serves these paths from `/games/<name>/index.html`. Each file is a dependency-free HTML/CSS/JavaScript application with its own touch-friendly UI and a namespaced `localStorage` key. They make no network calls, do not use paid libraries, and can work after first load without backend game traffic.

The frame uses `sandbox="allow-scripts allow-same-origin"`, has no top-navigation or pop-up permission, and is visually contained by `apps/web/app/globals.css`. `allow-same-origin` is retained only so browser local storage works as required. If games ever become third-party code, they must move to a separate origin and lose that permission.

## Scribble public and private rooms

Scribble remains a first-party real-time route rather than an iframe because it depends on authenticated WebSockets, participant state and invitation flows.

- `vyb-public` is the generic public-room alias.
- A public room has a hard 12 connected-player limit.
- Joining the alias selects the oldest public room with an available seat.
- When a room reaches 12 players, the backend immediately creates the next room (`vyb-public 2`, then `vyb-public 3`, and so on) before publishing the updated room catalog.
- Direct room IDs still join an exact room, so private-code links and public-room cards keep deterministic behavior.
- User-created rooms retain their existing private/public visibility setting and invite-code behavior.

The room allocator lives in `apps/backend/src/modules/games/scribble-realtime-hub.mjs`. It is currently in-process, like the existing Scribble WebSocket hub. Before a multi-instance production rollout, move room membership and fan-out to a durable shared realtime coordinator; otherwise a Cloud Run restart or multiple instances can split room state.

## Verification checklist

1. Open every active Games Hub card on desktop and mobile.
2. Reload each local game and confirm its progress remains on that device only.
3. Join Scribble as twelve distinct authenticated clients through `vyb-public`; confirm the next client lands in the newly-created public room.
4. Create a private Scribble room, share its invite code, and confirm it never appears in the public catalog.
5. Exercise Connect and the daily N-Queens route to confirm the existing implementations were not replaced.
6. Run a production Next build so the dynamic game route's Server/Client
   boundary is validated in addition to interactive browser QA.
# Online board games (Chess, Ludo and UNO)

Chess, Ludo and UNO are first-party React experiences rather than static game iframes. They share the `@vyb/game-engine` package so local play and the authoritative online room use the same rules. Color Sort, N-Queens Visualizer and Word Puzzle remain sandboxed static micro-frontends.

## Runtime architecture

- `apps/web/src/components/multiplayer-board-game.tsx` owns responsive lobby, board, invite, challenge, chat and emoji UX.
- `apps/web/app/api/games/multiplayer/session/route.ts` reads the signed Vyb session and issues a five-minute, room-scoped HMAC socket token. Browser clients never receive the signing secret.
- `apps/games-realtime` is a Cloudflare Worker. It maps each `tenant + game + room code` to one SQLite-backed Durable Object.
- The Durable Object validates every move, persists room state before broadcasting, redacts other players' UNO hands, and uses hibernating WebSockets so idle rooms do not keep compute active.
- Chat is bounded to the most recent 50 entries per room. A reconnecting player recovers their seat, game snapshot and chat without resetting the match.

## Supported modes

| Game | Local | Online | Players | Online actions |
| --- | --- | --- | --- | --- |
| Chess | Two-player pass-and-play | Private room | 2 | legal moves, challenge link, chat, emoji, rematch |
| Ludo | 2–4 player pass-and-play | Private room | 2–4 | server dice, token moves/captures, challenge link, chat, emoji, rematch |
| UNO | — | Private room | 2–4 | landscape-first responsive table, private hands, draw/play/wild colour, challenge link, chat, emoji, rematch |

## Local development

1. Keep `VYB_GAME_SESSION_SECRET` in the web `.env` equal to `GAME_SESSION_SECRET` in `apps/games-realtime/.dev.vars`.
2. Set `VYB_GAMES_WS_URL=ws://localhost:8787` for the web app.
3. Run `pnpm --filter @vyb/games-realtime dev` and the normal web/backend development command.
4. Open two authenticated browser profiles, create a room in one profile and join the six-character room code in the other.

## Production activation

The online cards should only be promoted after the Worker is deployed and the web production environment points at it.

```powershell
pnpm --filter @vyb/games-realtime exec wrangler secret put GAME_SESSION_SECRET
pnpm --filter @vyb/games-realtime exec wrangler deploy
```

Set the same random value as `VYB_GAME_SESSION_SECRET` in the web runtime and set `VYB_GAMES_WS_URL` to the deployed `wss://` Worker origin. Never put the secret in `wrangler.jsonc`, client JavaScript or Git. The allowlist includes `vybnet.app`, `www.vybnet.app`, the verified Vercel provider alias and local development; update it when the canonical web domain changes.

## Security and reliability constraints

- Room tokens expire after five minutes and are scoped to one user, tenant, game and room.
- Room codes exclude ambiguous characters and do not grant identity by themselves.
- Every game action is checked against the current server turn and state; clients cannot choose dice values or view another UNO hand.
- A room cannot accept new seats after a match begins. Existing seats may reconnect.
- Durable Object SQLite is the source of truth; in-memory state is never required to survive hibernation.
