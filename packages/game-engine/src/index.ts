import { Chess } from "chess.js";

export const ONLINE_GAME_SLUGS = ["chess", "ludo", "uno"] as const;
export type OnlineGameSlug = (typeof ONLINE_GAME_SLUGS)[number];

export type GamePlayer = {
  id: string;
  name: string;
  username: string;
  seat: number;
  connected: boolean;
};

export type ChessGameState = {
  kind: "chess";
  fen: string;
  history: string[];
  status: "waiting" | "playing" | "checkmate" | "draw";
  winnerId: string | null;
};

export type LudoGameState = {
  kind: "ludo";
  status: "waiting" | "playing" | "finished";
  turnSeat: number;
  dice: number | null;
  tokens: number[][];
  winnerId: string | null;
  lastEvent: string;
};

export type UnoCard = {
  id: string;
  color: "red" | "yellow" | "green" | "blue" | "wild";
  value: string;
};

export type UnoGameState = {
  kind: "uno";
  status: "waiting" | "playing" | "finished";
  turnSeat: number;
  direction: 1 | -1;
  drawPile: UnoCard[];
  discardPile: UnoCard[];
  hands: UnoCard[][];
  activeColor: UnoCard["color"];
  winnerId: string | null;
  lastEvent: string;
};

export type GameState = ChessGameState | LudoGameState | UnoGameState;

export function isOnlineGameSlug(value: string): value is OnlineGameSlug {
  return ONLINE_GAME_SLUGS.includes(value as OnlineGameSlug);
}

export function createChessState(): ChessGameState {
  const chess = new Chess();
  return { kind: "chess", fen: chess.fen(), history: [], status: "playing", winnerId: null };
}

export function moveChess(
  state: ChessGameState,
  players: GamePlayer[],
  actorId: string,
  from: string,
  to: string,
  promotion = "q"
): ChessGameState {
  if (state.status !== "playing") throw new Error("This match is not active.");
  const chess = new Chess(state.fen);
  const expectedSeat = chess.turn() === "w" ? 0 : 1;
  if (players.find((player) => player.id === actorId)?.seat !== expectedSeat) {
    throw new Error("Wait for your turn.");
  }
  const move = chess.move({ from, to, promotion });
  if (!move) throw new Error("That move is not legal.");
  const status = chess.isCheckmate() ? "checkmate" : chess.isDraw() ? "draw" : "playing";
  return {
    ...state,
    fen: chess.fen(),
    history: [...state.history, move.san],
    status,
    winnerId: status === "checkmate" ? actorId : null
  };
}

const LUDO_TRACK_LENGTH = 52;
const LUDO_FINISH = 58;
const LUDO_STARTS = [0, 13, 26, 39];
const LUDO_SAFE_GLOBAL = new Set([0, 8, 13, 21, 26, 34, 39, 47]);

export function createLudoState(playerCount: number): LudoGameState {
  if (playerCount < 2 || playerCount > 4) throw new Error("Ludo needs 2 to 4 players.");
  return {
    kind: "ludo",
    status: "playing",
    turnSeat: 0,
    dice: null,
    tokens: Array.from({ length: playerCount }, () => [-1, -1, -1, -1]),
    winnerId: null,
    lastEvent: "Red starts the match."
  };
}

export function getLudoLegalTokens(state: LudoGameState) {
  if (state.dice === null) return [];
  const dice = state.dice;
  return state.tokens[state.turnSeat]
    .map((position, index) => ({ position, index }))
    .filter(({ position }) => (position === -1 ? dice === 6 : position < LUDO_FINISH && position + dice <= LUDO_FINISH))
    .map(({ index }) => index);
}

export function rollLudo(state: LudoGameState, players: GamePlayer[], actorId: string, dice: number): LudoGameState {
  if (state.status !== "playing" || state.dice !== null) throw new Error("Move the selected token first.");
  if (players.find((player) => player.id === actorId)?.seat !== state.turnSeat) throw new Error("Wait for your turn.");
  if (!Number.isInteger(dice) || dice < 1 || dice > 6) throw new Error("Invalid dice result.");
  const rolled = { ...state, dice, lastEvent: `${players[state.turnSeat]?.name ?? "Player"} rolled ${dice}.` };
  if (getLudoLegalTokens(rolled).length > 0) return rolled;
  return {
    ...rolled,
    dice: null,
    turnSeat: (state.turnSeat + 1) % state.tokens.length,
    lastEvent: `${rolled.lastEvent} No token can move.`
  };
}

export function moveLudo(state: LudoGameState, players: GamePlayer[], actorId: string, tokenIndex: number): LudoGameState {
  const actor = players.find((player) => player.id === actorId);
  if (actor?.seat !== state.turnSeat) throw new Error("Wait for your turn.");
  if (!getLudoLegalTokens(state).includes(tokenIndex)) throw new Error("That token cannot move.");
  const dice = state.dice as number;
  const tokens = state.tokens.map((row) => [...row]);
  const current = tokens[state.turnSeat][tokenIndex];
  const next = current === -1 ? 0 : current + dice;
  tokens[state.turnSeat][tokenIndex] = next;

  let captured = false;
  if (next >= 0 && next < LUDO_TRACK_LENGTH) {
    const global = (LUDO_STARTS[state.turnSeat] + next) % LUDO_TRACK_LENGTH;
    if (!LUDO_SAFE_GLOBAL.has(global)) {
      tokens.forEach((opponentTokens, opponentSeat) => {
        if (opponentSeat === state.turnSeat) return;
        opponentTokens.forEach((position, opponentIndex) => {
          const opponentGlobal = position >= 0 && position < LUDO_TRACK_LENGTH
            ? (LUDO_STARTS[opponentSeat] + position) % LUDO_TRACK_LENGTH
            : -1;
          if (opponentGlobal === global) {
            opponentTokens[opponentIndex] = -1;
            captured = true;
          }
        });
      });
    }
  }

  const finished = tokens[state.turnSeat].every((position) => position === LUDO_FINISH);
  const extraTurn = dice === 6 || captured;
  return {
    ...state,
    tokens,
    dice: null,
    status: finished ? "finished" : state.status,
    winnerId: finished ? actorId : null,
    turnSeat: finished || extraTurn ? state.turnSeat : (state.turnSeat + 1) % state.tokens.length,
    lastEvent: finished
      ? `${actor?.name ?? "Player"} won the match!`
      : captured
        ? `${actor?.name ?? "Player"} captured a token and plays again.`
        : `${actor?.name ?? "Player"} moved token ${tokenIndex + 1}.`
  };
}

function seededShuffle<T>(items: T[], seed: number) {
  const copy = [...items];
  let value = seed >>> 0;
  for (let index = copy.length - 1; index > 0; index -= 1) {
    value = (value * 1664525 + 1013904223) >>> 0;
    const target = value % (index + 1);
    [copy[index], copy[target]] = [copy[target], copy[index]];
  }
  return copy;
}

function createUnoDeck(seed: number) {
  const cards: UnoCard[] = [];
  const colors = ["red", "yellow", "green", "blue"] as const;
  let id = 0;
  colors.forEach((color) => {
    cards.push({ id: `${color}-${id++}`, color, value: "0" });
    for (let number = 1; number <= 9; number += 1) {
      cards.push({ id: `${color}-${id++}`, color, value: String(number) }, { id: `${color}-${id++}`, color, value: String(number) });
    }
    ["skip", "reverse", "draw2"].forEach((value) => {
      cards.push({ id: `${color}-${id++}`, color, value }, { id: `${color}-${id++}`, color, value });
    });
  });
  for (let index = 0; index < 4; index += 1) {
    cards.push({ id: `wild-${id++}`, color: "wild", value: "wild" });
    cards.push({ id: `wild4-${id++}`, color: "wild", value: "wild4" });
  }
  return seededShuffle(cards, seed);
}

export function createUnoState(playerCount: number, seed: number): UnoGameState {
  if (playerCount < 2 || playerCount > 4) throw new Error("UNO needs 2 to 4 players.");
  const deck = createUnoDeck(seed);
  const hands = Array.from({ length: playerCount }, () => [] as UnoCard[]);
  for (let round = 0; round < 7; round += 1) hands.forEach((hand) => hand.push(deck.pop() as UnoCard));
  let first = deck.pop() as UnoCard;
  while (first.color === "wild" || ["skip", "reverse", "draw2"].includes(first.value)) {
    deck.unshift(first);
    first = deck.pop() as UnoCard;
  }
  return {
    kind: "uno",
    status: "playing",
    turnSeat: 0,
    direction: 1,
    drawPile: deck,
    discardPile: [first],
    hands,
    activeColor: first.color,
    winnerId: null,
    lastEvent: "UNO match started."
  };
}

function nextUnoSeat(state: UnoGameState, steps = 1) {
  const count = state.hands.length;
  return (state.turnSeat + state.direction * steps + count * steps) % count;
}

function refillUnoDrawPile(state: UnoGameState) {
  if (state.drawPile.length > 0 || state.discardPile.length <= 1) return state;
  const top = state.discardPile[state.discardPile.length - 1];
  return { ...state, drawPile: seededShuffle(state.discardPile.slice(0, -1), Date.now()), discardPile: [top] };
}

export function playUnoCard(
  state: UnoGameState,
  players: GamePlayer[],
  actorId: string,
  cardId: string,
  chosenColor?: UnoCard["color"]
): UnoGameState {
  const actor = players.find((player) => player.id === actorId);
  if (state.status !== "playing" || actor?.seat !== state.turnSeat) throw new Error("Wait for your turn.");
  const hand = state.hands[state.turnSeat];
  const card = hand.find((candidate) => candidate.id === cardId);
  if (!card) throw new Error("That card is not in your hand.");
  const top = state.discardPile[state.discardPile.length - 1];
  if (card.color !== "wild" && card.color !== state.activeColor && card.value !== top.value) {
    throw new Error("Match the current colour or value.");
  }
  if (card.color === "wild" && !["red", "yellow", "green", "blue"].includes(chosenColor ?? "")) {
    throw new Error("Choose a colour for the wild card.");
  }

  const hands = state.hands.map((row) => [...row]);
  hands[state.turnSeat] = hands[state.turnSeat].filter((candidate) => candidate.id !== cardId);
  let direction = state.direction;
  let steps = 1;
  let drawPile = [...state.drawPile];
  if (card.value === "reverse") {
    direction = state.hands.length === 2 ? state.direction : (state.direction * -1) as 1 | -1;
    steps = state.hands.length === 2 ? 2 : 1;
  } else if (card.value === "skip") {
    steps = 2;
  } else if (card.value === "draw2" || card.value === "wild4") {
    const amount = card.value === "draw2" ? 2 : 4;
    const target = (state.turnSeat + direction + hands.length) % hands.length;
    for (let index = 0; index < amount; index += 1) {
      if (!drawPile.length) break;
      hands[target].push(drawPile.pop() as UnoCard);
    }
    steps = 2;
  }
  const winner = hands[state.turnSeat].length === 0;
  const interim = { ...state, direction, hands };
  return {
    ...state,
    hands,
    drawPile,
    discardPile: [...state.discardPile, card],
    activeColor: card.color === "wild" ? (chosenColor as UnoCard["color"]) : card.color,
    direction,
    turnSeat: winner ? state.turnSeat : nextUnoSeat(interim, steps),
    status: winner ? "finished" : "playing",
    winnerId: winner ? actorId : null,
    lastEvent: winner ? `${actor?.name ?? "Player"} called UNO and won!` : `${actor?.name ?? "Player"} played ${card.value}.`
  };
}

export function drawUnoCard(state: UnoGameState, players: GamePlayer[], actorId: string): UnoGameState {
  const actor = players.find((player) => player.id === actorId);
  if (state.status !== "playing" || actor?.seat !== state.turnSeat) throw new Error("Wait for your turn.");
  let next = refillUnoDrawPile(state);
  if (!next.drawPile.length) throw new Error("No cards remain to draw.");
  const hands = next.hands.map((row) => [...row]);
  const drawPile = [...next.drawPile];
  hands[next.turnSeat].push(drawPile.pop() as UnoCard);
  next = { ...next, hands, drawPile };
  return {
    ...next,
    turnSeat: nextUnoSeat(next),
    lastEvent: `${actor?.name ?? "Player"} drew a card.`
  };
}

export function redactGameState(state: GameState | null, viewerSeat: number) {
  if (!state || state.kind !== "uno") return state;
  return {
    ...state,
    drawPile: [],
    hands: state.hands.map((hand, seat) => seat === viewerSeat ? hand : hand.map((_, index) => ({ id: `hidden-${seat}-${index}`, color: "wild" as const, value: "hidden" })))
  };
}
