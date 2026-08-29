"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Chess, type Square } from "chess.js";
import { ArrowsLeftRight, CaretLeft, CaretRight, ChatCircle, List, Target } from "@phosphor-icons/react";
import {
  createChessState,
  createLudoState,
  getLudoLegalTokens,
  moveChess,
  moveLudo,
  rollLudo,
  type ChessGameState,
  type GamePlayer,
  type GameState,
  type LudoGameState,
  type OnlineGameSlug,
  type UnoCard,
  type UnoGameState
} from "@vyb/game-engine";
import styles from "./multiplayer-board-game.module.css";

type RoomMessage = { id: number; userId: string; displayName: string; kind: "chat" | "emoji"; body: string; createdAt: number };
type RoomSnapshot = {
  game: OnlineGameSlug;
  roomCode: string;
  hostId: string;
  players: GamePlayer[];
  state: GameState | null;
  createdAt: number;
};
type Viewer = { userId: string; displayName: string; username: string; seat: number };
type ServerEvent =
  | { type: "snapshot"; room: RoomSnapshot; viewer: Viewer; messages: RoomMessage[] }
  | { type: "error"; message: string };

const GAME_META = {
  chess: { title: "Chess Arena", subtitle: "Ranked-ready rules, local board or a private online challenge", icon: "♞", min: 2, max: 2 },
  ludo: { title: "Ludo Club", subtitle: "Classic race for 2–4 friends with secure server dice", icon: "●", min: 2, max: 4 },
  uno: { title: "UNO Party", subtitle: "Match colours, use action cards and outplay 2–4 friends", icon: "U", min: 2, max: 4 }
} as const;

const LOCAL_CHESS_PLAYERS: GamePlayer[] = [
  { id: "local-white", name: "Player 1", username: "white", seat: 0, connected: true },
  { id: "local-black", name: "Player 2", username: "black", seat: 1, connected: true }
];
const LOCAL_LUDO_PLAYERS: GamePlayer[] = [
  { id: "local-red", name: "Red", username: "red", seat: 0, connected: true },
  { id: "local-blue", name: "Blue", username: "blue", seat: 1, connected: true },
  { id: "local-green", name: "Green", username: "green", seat: 2, connected: true },
  { id: "local-gold", name: "Gold", username: "gold", seat: 3, connected: true }
];

const LUDO_COLORS = ["#ff5264", "#46a8ff", "#35d495", "#ffc94b"];
const UNO_COLORS: Record<UnoCard["color"], string> = { red: "#ef3340", yellow: "#f8c630", green: "#1caf68", blue: "#2877db", wild: "#20273a" };

function useOnlineGame(game: OnlineGameSlug) {
  const socketRef = useRef<WebSocket | null>(null);
  const [room, setRoom] = useState<RoomSnapshot | null>(null);
  const [viewer, setViewer] = useState<Viewer | null>(null);
  const [messages, setMessages] = useState<RoomMessage[]>([]);
  const [status, setStatus] = useState<"idle" | "connecting" | "online" | "offline">("idle");
  const [error, setError] = useState("");

  const disconnect = useCallback(() => {
    socketRef.current?.close(1000, "Leaving room");
    socketRef.current = null;
    setStatus("idle");
    setRoom(null);
    setViewer(null);
    setMessages([]);
  }, []);

  useEffect(() => () => socketRef.current?.close(1000, "Page closed"), []);

  const connect = useCallback(async (roomCode = "") => {
    disconnect();
    setStatus("connecting");
    setError("");
    try {
      const response = await fetch("/api/games/multiplayer/session", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ game, roomCode })
      });
      const payload = await response.json() as { roomCode?: string; wsUrl?: string; error?: { message?: string } };
      if (!response.ok || !payload.wsUrl) throw new Error(payload.error?.message || "Could not open the room.");
      const socket = new WebSocket(payload.wsUrl);
      socketRef.current = socket;
      socket.onopen = () => setStatus("online");
      socket.onmessage = (event) => {
        const message = JSON.parse(String(event.data)) as ServerEvent;
        if (message.type === "error") {
          setError(message.message);
          return;
        }
        setRoom(message.room);
        setViewer(message.viewer);
        setMessages(message.messages);
      };
      socket.onerror = () => setError("Realtime connection failed. Check the game worker and try again.");
      socket.onclose = (event) => {
        if (event.code !== 1000) setError("Room connection closed. Rejoin with the same code.");
        setStatus("offline");
      };
    } catch (cause) {
      setStatus("offline");
      setError(cause instanceof Error ? cause.message : "Could not open the room.");
    }
  }, [disconnect, game]);

  const send = useCallback((value: Record<string, unknown>) => {
    if (socketRef.current?.readyState !== WebSocket.OPEN) {
      setError("Reconnect to the room before playing.");
      return;
    }
    socketRef.current.send(JSON.stringify(value));
  }, []);

  return { room, viewer, messages, status, error, setError, connect, disconnect, send };
}

export function MultiplayerBoardGame({ game }: { game: OnlineGameSlug }) {
  const searchParams = useSearchParams();
  const initialRoom = searchParams.get("room")?.trim().toUpperCase().replace(/[^A-Z2-9]/gu, "").slice(0, 6) ?? "";
  const online = useOnlineGame(game);
  const [roomInput, setRoomInput] = useState(initialRoom);
  const [mode, setMode] = useState<"choose" | "local" | "online">("choose");
  const [localChess, setLocalChess] = useState<ChessGameState>(() => createChessState());
  const [localLudo, setLocalLudo] = useState<LudoGameState>(() => createLudoState(2));
  const [localLudoPlayers, setLocalLudoPlayers] = useState(2);
  const autoJoined = useRef(false);
  const meta = GAME_META[game];

  useEffect(() => {
    if (initialRoom && searchParams.get("join") === "1" && !autoJoined.current) {
      autoJoined.current = true;
      setMode("online");
      void online.connect(initialRoom);
    }
  }, [initialRoom, online, searchParams]);

  const resetLocal = () => {
    if (game === "chess") setLocalChess(createChessState());
    if (game === "ludo") setLocalLudo(createLudoState(localLudoPlayers));
  };

  const inviteUrl = online.room
    ? `${typeof window === "undefined" ? "" : window.location.origin}/hub/gameshub/${game}?room=${online.room.roomCode}&join=1`
    : "";

  const shareInvite = async () => {
    if (!inviteUrl || !online.room) return;
    const shareData = { title: `${meta.title} challenge`, text: `Join my ${meta.title} room ${online.room.roomCode} on Vyb`, url: inviteUrl };
    try {
      if (navigator.share) await navigator.share(shareData);
      else {
        await navigator.clipboard.writeText(inviteUrl);
        online.setError("Challenge link copied.");
      }
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") return;
      online.setError("Could not share the challenge link.");
    }
  };

  return (
    <main className={`${styles.page} ${game === "chess" ? styles.chessPage : game === "ludo" ? styles.ludoPage : styles.unoPage}`}>
      <header className={styles.header}>
        <Link href="/hub/gameshub" className={styles.back} aria-label="Back to games hub">←</Link>
        <div className={styles.heroIcon}>{meta.icon}</div>
        <div className={styles.heroCopy}>
          <p>Vyb Playground</p>
          <h1>{meta.title}</h1>
          <span>{meta.subtitle}</span>
        </div>
        {mode !== "choose" ? <button className={styles.textButton} onClick={() => { online.disconnect(); setMode("choose"); }}>Change mode</button> : null}
      </header>

      {mode === "choose" ? (
        <section className={styles.modeGrid}>
          <article className={styles.modeCard}>
            <span className={styles.livePill}>ONLINE</span>
            <h2>Play with friends</h2>
            <p>Create a private room, send a challenge link and play with live chat and emojis.</p>
            <button className={styles.primaryButton} onClick={() => { setMode("online"); void online.connect(); }}>Create room</button>
            <div className={styles.joinRow}>
              <input value={roomInput} onChange={(event) => setRoomInput(event.target.value.toUpperCase().replace(/[^A-Z2-9]/gu, "").slice(0, 6))} placeholder="ROOM CODE" aria-label="Room code" />
              <button disabled={roomInput.length !== 6} onClick={() => { setMode("online"); void online.connect(roomInput); }}>Join</button>
            </div>
          </article>
          {game !== "uno" ? (
            <article className={styles.modeCard}>
              <span className={styles.localPill}>OFFLINE</span>
              <h2>Pass & play</h2>
              <p>{game === "chess" ? "A full two-player legal chess board on this device." : "A classic 2–4 player Ludo board on one device."}</p>
              {game === "ludo" ? (
                <label className={styles.playerCount}>Players
                  <select value={localLudoPlayers} onChange={(event) => setLocalLudoPlayers(Number(event.target.value))}>
                    <option value={2}>2</option><option value={3}>3</option><option value={4}>4</option>
                  </select>
                </label>
              ) : null}
              <button className={styles.secondaryButton} onClick={() => { resetLocal(); setMode("local"); }}>Start local match</button>
            </article>
          ) : null}
        </section>
      ) : null}

      {mode === "local" ? (
        <section className={styles.gameLayout}>
          <div className={styles.boardPanel}>
            <div className={styles.panelTop}><div><b>Offline match</b><span>Pass the device after every turn</span></div><button onClick={resetLocal}>New game</button></div>
            {game === "chess" ? (
              <ChessBoard state={localChess} players={LOCAL_CHESS_PLAYERS} viewerSeat={0} local onReset={resetLocal} onMove={(from, to) => {
                const turnId = new Chess(localChess.fen).turn() === "w" ? "local-white" : "local-black";
                try { setLocalChess(moveChess(localChess, LOCAL_CHESS_PLAYERS, turnId, from, to)); } catch { /* legal highlights prevent invalid moves */ }
              }} />
            ) : (
              <LudoBoard state={localLudo} players={LOCAL_LUDO_PLAYERS.slice(0, localLudoPlayers)} viewerId={`local-${["red", "blue", "green", "gold"][localLudo.turnSeat]}`} onRoll={() => {
                const actor = LOCAL_LUDO_PLAYERS[localLudo.turnSeat];
                setLocalLudo(rollLudo(localLudo, LOCAL_LUDO_PLAYERS, actor.id, Math.floor(Math.random() * 6) + 1));
              }} onMove={(tokenIndex) => {
                const actor = LOCAL_LUDO_PLAYERS[localLudo.turnSeat];
                setLocalLudo(moveLudo(localLudo, LOCAL_LUDO_PLAYERS, actor.id, tokenIndex));
              }} />
            )}
          </div>
          <Rules game={game} />
        </section>
      ) : null}

      {mode === "online" ? (
        <section className={styles.onlineShell}>
          {online.status === "connecting" && !online.room ? <div className={styles.connecting}><span />Securing room…</div> : null}
          {online.error ? <div className={styles.notice} role="status">{online.error}<button onClick={() => online.setError("")}>×</button></div> : null}
          {online.room && online.viewer ? (
            <>
              <div className={styles.roomBar}>
                <div><span>ROOM</span><strong>{online.room.roomCode}</strong><i className={online.status === "online" ? styles.onlineDot : styles.offlineDot} /></div>
                <button onClick={() => void shareInvite()}>↗ Invite / challenge</button>
              </div>
              {!online.room.state ? (
                <Lobby room={online.room} viewer={online.viewer} onStart={() => online.send({ type: "start" })} onInvite={() => void shareInvite()} />
              ) : (
                <div className={styles.gameLayout}>
                  <div className={styles.boardPanel}>
                    {online.room.state.kind === "chess" ? <ChessBoard state={online.room.state} players={online.room.players} viewerSeat={online.viewer.seat} chatAvailable onMove={(from, to) => online.send({ type: "chess.move", from, to, promotion: "q" })} /> : null}
                    {online.room.state.kind === "ludo" ? <LudoBoard state={online.room.state} players={online.room.players} viewerId={online.viewer.userId} onRoll={() => online.send({ type: "ludo.roll" })} onMove={(tokenIndex) => online.send({ type: "ludo.move", tokenIndex })} /> : null}
                    {online.room.state.kind === "uno" ? <UnoTable state={online.room.state} players={online.room.players} viewer={online.viewer} onPlay={(cardId, color) => online.send({ type: "uno.play", cardId, color })} onDraw={() => online.send({ type: "uno.draw" })} /> : null}
                    {online.room.state.status === "finished" || online.room.state.kind === "chess" && ["checkmate", "draw"].includes(online.room.state.status) ? (
                      <button className={styles.rematch} disabled={online.room.hostId !== online.viewer.userId} onClick={() => online.send({ type: "rematch" })}>Play rematch</button>
                    ) : null}
                  </div>
                  <GameChat messages={online.messages} viewer={online.viewer} onSend={online.send} />
                </div>
              )}
            </>
          ) : null}
          {online.status === "offline" && !online.room ? <button className={styles.primaryButton} onClick={() => void online.connect(roomInput)}>Try again</button> : null}
        </section>
      ) : null}
    </main>
  );
}

function Lobby({ room, viewer, onStart, onInvite }: { room: RoomSnapshot; viewer: Viewer; onStart: () => void; onInvite: () => void }) {
  return (
    <section className={styles.lobby}>
      <div className={styles.lobbyVisual}><span>◌</span><i /><i /><i /></div>
      <h2>Room is ready</h2>
      <p>{room.players.length < GAME_META[room.game].min ? "Invite a friend to unlock the match." : "Players are connected. The host can start now."}</p>
      <div className={styles.seats}>
        {Array.from({ length: GAME_META[room.game].max }, (_, seat) => {
          const player = room.players[seat];
          return <div className={player ? styles.filledSeat : styles.emptySeat} key={seat}><b>{player ? player.name.slice(0, 1).toUpperCase() : "+"}</b><span>{player?.name ?? "Open seat"}</span>{player?.id === room.hostId ? <small>HOST</small> : null}</div>;
        })}
      </div>
      <div className={styles.lobbyActions}>
        <button className={styles.secondaryButton} onClick={onInvite}>Share challenge</button>
        {viewer.userId === room.hostId ? <button className={styles.primaryButton} disabled={room.players.length < GAME_META[room.game].min} onClick={onStart}>Start match</button> : <span>Waiting for host…</span>}
      </div>
    </section>
  );
}

function ChessBoard({ state, players, viewerSeat, local = false, chatAvailable = false, onReset, onMove }: { state: ChessGameState; players: GamePlayer[]; viewerSeat: number; local?: boolean; chatAvailable?: boolean; onReset?: () => void; onMove: (from: string, to: string) => void }) {
  const [selected, setSelected] = useState("");
  const [viewPly, setViewPly] = useState<number | null>(null);
  const [flipped, setFlipped] = useState(false);
  const [optionsOpen, setOptionsOpen] = useState(false);
  const liveChess = useMemo(() => new Chess(state.fen), [state.fen]);
  const chess = useMemo(() => {
    if (viewPly === null || viewPly >= state.history.length) return liveChess;
    const replay = new Chess();
    for (const move of state.history.slice(0, viewPly)) replay.move(move);
    return replay;
  }, [liveChess, state.history, viewPly]);
  const orientationBlack = local ? flipped : viewerSeat === 1;
  const ranks = orientationBlack ? [1, 2, 3, 4, 5, 6, 7, 8] : [8, 7, 6, 5, 4, 3, 2, 1];
  const files = orientationBlack ? ["h", "g", "f", "e", "d", "c", "b", "a"] : ["a", "b", "c", "d", "e", "f", "g", "h"];
  const isLive = viewPly === null || viewPly >= state.history.length;
  const legal: Square[] = selected && isLive ? liveChess.moves({ square: selected as Square, verbose: true }).map((move) => move.to) : [];
  const activeSeat = liveChess.turn() === "w" ? 0 : 1;
  const currentPly = viewPly ?? state.history.length;

  useEffect(() => {
    setSelected("");
    setViewPly(null);
  }, [state.fen]);

  const tap = (square: Square) => {
    if (selected && legal.includes(square)) {
      onMove(selected, square);
      setSelected("");
      return;
    }
    const piece = liveChess.get(square);
    if (piece && piece.color === liveChess.turn() && (local || viewerSeat === activeSeat) && isLive) setSelected(square);
    else setSelected("");
  };

  const moveBack = () => {
    if (!state.history.length) return;
    setSelected("");
    setViewPly(Math.max(0, currentPly - 1));
  };
  const moveForward = () => {
    if (currentPly >= state.history.length) return;
    setSelected("");
    const next = currentPly + 1;
    setViewPly(next >= state.history.length ? null : next);
  };

  return (
    <div className={styles.chessWrap}>
      <div className={styles.moveRail} aria-label="Move history">
        {state.history.length ? state.history.map((move, index) => (
          <button key={`${move}-${index}`} className={currentPly === index + 1 ? styles.activeMove : ""} onClick={() => { setSelected(""); setViewPly(index + 1 >= state.history.length ? null : index + 1); }}>
            <small>{index % 2 === 0 ? `${Math.floor(index / 2) + 1}.` : ""}</small>{move}
          </button>
        )) : <span>No moves yet</span>}
      </div>
      <PlayerStrip
        player={players[orientationBlack ? 0 : 1]}
        active={activeSeat === (orientationBlack ? 0 : 1)}
        label={`${orientationBlack ? "White" : "Black"}${activeSeat === (orientationBlack ? 0 : 1) ? " to move" : " waiting"}${chess.inCheck() && activeSeat === (orientationBlack ? 0 : 1) ? " · Check" : ""}`}
      />
      <div className={styles.chessBoard} role="grid" aria-label="Chess board">
        {ranks.flatMap((rank) => files.map((file) => {
          const square = `${file}${rank}` as Square;
          const piece = chess.get(square);
          const dark = (files.indexOf(file) + ranks.indexOf(rank)) % 2 === 1;
          return <button key={square} role="gridcell" aria-label={square} className={`${styles.chessSquare} ${dark ? styles.darkSquare : styles.lightSquare} ${selected === square ? styles.selectedSquare : ""} ${legal.includes(square) ? styles.legalSquare : ""}`} onClick={() => tap(square)}>
            {piece ? <svg className={styles.chessPiece} viewBox="0 0 40 40" aria-hidden="true"><use href={`/games/chess/pieces.svg#${piece.color}${piece.type}`} /></svg> : null}
            <small>{file === files[0] ? rank : ""}{rank === ranks[ranks.length - 1] ? file : ""}</small>
          </button>;
        }))}
      </div>
      <PlayerStrip
        player={players[orientationBlack ? 1 : 0]}
        active={activeSeat === (orientationBlack ? 1 : 0)}
        label={`${orientationBlack ? "Black" : "White"}${activeSeat === (orientationBlack ? 1 : 0) ? " to move" : " waiting"}${chess.inCheck() && activeSeat === (orientationBlack ? 1 : 0) ? " · Check" : ""}`}
      />
      <div className={styles.chessFooter}><span>{isLive ? state.history.slice(-4).join(" · ") || "Opening position" : `Viewing move ${currentPly} of ${state.history.length}`}</span><b>{state.status === "checkmate" ? "Checkmate" : state.status === "draw" ? "Draw" : isLive ? "Live position" : "History"}</b></div>
      <div className={styles.chessActions}>
        <button aria-expanded={optionsOpen} onClick={() => setOptionsOpen((value) => !value)}><List aria-hidden="true" /><span>Options</span></button>
        {chatAvailable ? <a href="#game-chat"><ChatCircle aria-hidden="true" /><span>Chat</span></a> : <button disabled><ArrowsLeftRight aria-hidden="true" /><span>Pass & play</span></button>}
        <button className={styles.liveAction} disabled={isLive} onClick={() => { setSelected(""); setViewPly(null); }}><Target aria-hidden="true" /><span>Live</span></button>
        <button disabled={!state.history.length || currentPly <= 0} onClick={moveBack}><CaretLeft aria-hidden="true" /><span>Back</span></button>
        <button disabled={currentPly >= state.history.length} onClick={moveForward}><CaretRight aria-hidden="true" /><span>Forward</span></button>
      </div>
      {optionsOpen ? <div className={styles.chessOptions}>
        <button onClick={() => { setFlipped((value) => !value); setSelected(""); }}>Flip board</button>
        {onReset ? <button onClick={() => { onReset(); setOptionsOpen(false); }}>New game</button> : null}
      </div> : null}
    </div>
  );
}

const LUDO_PATH: Array<[number, number]> = [
  [6,0],[6,1],[6,2],[6,3],[6,4],[6,5],[5,6],[4,6],[3,6],[2,6],[1,6],[0,6],[0,7],[0,8],[1,8],[2,8],[3,8],[4,8],[5,8],[6,9],[6,10],[6,11],[6,12],[6,13],[6,14],[7,14],[8,14],[8,13],[8,12],[8,11],[8,10],[8,9],[9,8],[10,8],[11,8],[12,8],[13,8],[14,8],[14,7],[14,6],[13,6],[12,6],[11,6],[10,6],[9,6],[8,5],[8,4],[8,3],[8,2],[8,1],[8,0],[7,0]
];
const LUDO_HOME: Array<Array<[number, number]>> = [
  [[7,1],[7,2],[7,3],[7,4],[7,5],[7,6]],
  [[1,7],[2,7],[3,7],[4,7],[5,7],[6,7]],
  [[7,13],[7,12],[7,11],[7,10],[7,9],[7,8]],
  [[13,7],[12,7],[11,7],[10,7],[9,7],[8,7]]
];
const LUDO_STARTS = [0, 13, 26, 39];

function LudoBoard({ state, players, viewerId, onRoll, onMove }: { state: LudoGameState; players: GamePlayer[]; viewerId: string; onRoll: () => void; onMove: (tokenIndex: number) => void }) {
  const legal = getLudoLegalTokens(state);
  const mySeat = players.find((player) => player.id === viewerId)?.seat ?? state.turnSeat;
  const canAct = mySeat === state.turnSeat && state.status === "playing";
  const tokenCell = (seat: number, position: number, index: number): [number, number] => {
    if (position < 0) {
      const origins: Array<Array<[number, number]>> = [[[2,2],[2,4],[4,2],[4,4]],[[2,10],[2,12],[4,10],[4,12]],[[10,10],[10,12],[12,10],[12,12]],[[10,2],[10,4],[12,2],[12,4]]];
      return origins[seat][index];
    }
    if (position >= 52) return LUDO_HOME[seat][Math.min(5, position - 52)];
    return LUDO_PATH[(LUDO_STARTS[seat] + position) % 52];
  };
  return (
    <div className={styles.ludoWrap}>
      <div className={styles.turnBanner}><span style={{ background: LUDO_COLORS[state.turnSeat] }} /> <b>{players[state.turnSeat]?.name ?? `Player ${state.turnSeat + 1}`}</b> to play <small>{state.lastEvent}</small></div>
      <div className={styles.ludoBoard}>
        <div className={`${styles.ludoHome} ${styles.homeRed}`} /><div className={`${styles.ludoHome} ${styles.homeBlue}`} /><div className={`${styles.ludoHome} ${styles.homeGold}`} /><div className={`${styles.ludoHome} ${styles.homeGreen}`} />
        {LUDO_PATH.map(([row, column], index) => {
          const startSeat = LUDO_STARTS.indexOf(index);
          const isSafe = [0, 8, 13, 21, 26, 34, 39, 47].includes(index);
          return <i key={`path-${index}`} className={`${styles.ludoCell} ${isSafe ? styles.safeCell : ""}`} style={{ gridRow: row + 1, gridColumn: column + 1, background: startSeat >= 0 ? `${LUDO_COLORS[startSeat]}bb` : undefined }} />;
        })}
        {LUDO_HOME.flatMap((lane, seat) => lane.map(([row, column], index) => <i key={`home-${seat}-${index}`} className={styles.ludoCell} style={{ gridRow: row + 1, gridColumn: column + 1, background: `${LUDO_COLORS[seat]}aa` }} />))}
        <div className={styles.ludoCenter}>VYB</div>
        {state.tokens.flatMap((tokens, seat) => tokens.map((position, tokenIndex) => {
          const [row, column] = tokenCell(seat, position, tokenIndex);
          const actionable = seat === state.turnSeat && legal.includes(tokenIndex) && canAct;
          return <button key={`${seat}-${tokenIndex}`} className={`${styles.ludoToken} ${actionable ? styles.actionableToken : ""}`} style={{ gridRow: row + 1, gridColumn: column + 1, background: LUDO_COLORS[seat] }} disabled={!actionable} onClick={() => onMove(tokenIndex)} aria-label={`${players[seat]?.name ?? "Player"} token ${tokenIndex + 1}`}><span>{tokenIndex + 1}</span></button>;
        }))}
      </div>
      <button className={styles.diceButton} disabled={!canAct || state.dice !== null} onClick={onRoll}><span>{state.dice ?? "⚄"}</span>{state.dice === null ? "Roll dice" : "Choose a token"}</button>
    </div>
  );
}

function UnoTable({ state, players, viewer, onPlay, onDraw }: { state: UnoGameState; players: GamePlayer[]; viewer: Viewer; onPlay: (cardId: string, color?: UnoCard["color"]) => void; onDraw: () => void }) {
  const [wildCard, setWildCard] = useState<string | null>(null);
  const hand = state.hands[viewer.seat] ?? [];
  const canAct = state.turnSeat === viewer.seat && state.status === "playing";
  const top = state.discardPile[state.discardPile.length - 1];
  const play = (card: UnoCard) => card.color === "wild" ? setWildCard(card.id) : onPlay(card.id);
  const enterLandscape = async () => {
    try {
      if (!document.fullscreenElement) await document.documentElement.requestFullscreen?.();
      const orientation = screen.orientation as ScreenOrientation & { lock?: (value: "landscape") => Promise<void> };
      await orientation.lock?.("landscape");
    } catch {
      // Browsers that block orientation locking still keep the rotate guidance visible.
    }
  };
  return (
    <div className={styles.unoTable}>
      <div className={styles.rotatePrompt}>
        <span>↻</span><b>Rotate for UNO table</b><small>Landscape gives every player and card more room.</small>
        <button onClick={() => void enterLandscape()}>Open landscape</button>
      </div>
      <div className={styles.unoOpponents}>{players.filter((player) => player.seat !== viewer.seat).map((player) => <div className={state.turnSeat === player.seat ? styles.activeOpponent : ""} key={player.id}><b>{player.name.slice(0, 1)}</b><span>{player.name}</span><small>{state.hands[player.seat]?.length ?? 0} cards</small></div>)}</div>
      <div className={styles.unoCenter}>
        <button className={styles.unoDeck} disabled={!canAct} onClick={onDraw}><b>VYB</b><span>Draw {state.drawPile.length || ""}</span></button>
        <div className={styles.unoCard} style={{ background: UNO_COLORS[top.color] }}><span>{top.value.replace("draw", "+").replace("reverse", "↻").replace("skip", "⊘").toUpperCase()}</span><b>{top.value}</b></div>
      </div>
      <p className={styles.unoEvent}><i style={{ background: UNO_COLORS[state.activeColor] }} />{state.lastEvent}</p>
      <div className={styles.unoHand} aria-label="Your cards">{hand.map((card, index) => <button key={card.id} disabled={!canAct} onClick={() => play(card)} className={styles.unoCard} style={{ background: UNO_COLORS[card.color], zIndex: index }}><span>{card.value.replace("draw", "+").replace("reverse", "↻").replace("skip", "⊘").toUpperCase()}</span><b>{card.value}</b></button>)}</div>
      {wildCard ? <div className={styles.colorPicker}><b>Choose the next colour</b><div>{(["red", "yellow", "green", "blue"] as const).map((color) => <button key={color} style={{ background: UNO_COLORS[color] }} onClick={() => { onPlay(wildCard, color); setWildCard(null); }}>{color}</button>)}</div><button onClick={() => setWildCard(null)}>Cancel</button></div> : null}
    </div>
  );
}

function PlayerStrip({ player, active, label }: { player?: GamePlayer; active?: boolean; label: string }) {
  return <div className={`${styles.playerStrip} ${active ? styles.activeStrip : ""}`}><b>{player?.name.slice(0, 1).toUpperCase() ?? "?"}</b><span>{player?.name ?? "Waiting player"}<small>{label}</small></span><i>{player?.connected === false ? "OFFLINE" : active ? "TURN" : "ONLINE"}</i></div>;
}

function GameChat({ messages, viewer, onSend }: { messages: RoomMessage[]; viewer: Viewer; onSend: (value: Record<string, unknown>) => void }) {
  const [text, setText] = useState("");
  const send = () => { if (!text.trim()) return; onSend({ type: "chat", text }); setText(""); };
  return (
    <details id="game-chat" className={styles.chat} open>
      <summary className={styles.chatToggle}>◯ <span>Game chat</span><b>{messages.length}</b></summary>
      <div className={styles.emojiRow}>{["🔥","👏","😂","😮","🤝","🏆"].map((emoji) => <button key={emoji} onClick={() => onSend({ type: "emoji", emoji })}>{emoji}</button>)}</div>
      <div className={styles.messageList}>{messages.length ? messages.map((message) => <div key={message.id} className={message.userId === viewer.userId ? styles.ownMessage : ""}><b>{message.displayName}</b><p>{message.body}</p></div>) : <p className={styles.emptyChat}>Say hello before the first move.</p>}</div>
      <div className={styles.chatInput}><input value={text} maxLength={280} onChange={(event) => setText(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter") send(); }} placeholder="Message your opponent" /><button onClick={send}>Send</button></div>
    </details>
  );
}

function Rules({ game }: { game: OnlineGameSlug }) {
  return <aside className={styles.rules}><h3>Quick rules</h3><p>{game === "chess" ? "Select a piece, then a highlighted square. Checks, castling, promotion, en passant, draws and checkmate follow standard chess rules." : "Roll a six to leave home. Capture on non-safe squares, complete one full lap and move every token into its home lane."}</p><small>Local progress stays on this device. Online moves are validated by the room server.</small></aside>;
}
