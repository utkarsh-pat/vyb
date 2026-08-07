# Vyb Games: micro-frontends and live rooms

## Launch surface

The Games Hub is the single launch surface at `/hub/gameshub`. Its active cards route to either a server-authoritative Vyb game or a self-contained game page:

| Game | Route | Runtime |
| --- | --- | --- |
| Connect | `/hub/gameshub/connect` | Existing server-authoritative daily Connect game. It is intentionally not replaced with Connect Four. |
| Scribble | `/hub/gameshub/scribble` | Existing authenticated real-time Vyb game. |
| N-Queens (daily) | `/hub/gameshub/queens` | Existing server-authoritative daily puzzle. |
| Chess | `/hub/gameshub/chess` | Local static micro-frontend. |
| Ludo | `/hub/gameshub/ludo` | Local static micro-frontend. |
| Colour Sort | `/hub/gameshub/color-sort` | Local static micro-frontend. |
| N-Queens Visualizer | `/hub/gameshub/n-queens` | Local static micro-frontend. |
| Word Puzzle | `/hub/gameshub/word-puzzle` | Local static micro-frontend. |

`apps/web/src/components/mini-games-controller.tsx` is the master controller. It only accepts allow-listed game slugs and hosts static games inside a responsive iframe. The controller deliberately does not carry a Vyb session token into the iframe.

## Static game paths

```text
apps/web/public/games/
  chess/index.html
  ludo/index.html
  color-sort/index.html
  n-queens/index.html
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
