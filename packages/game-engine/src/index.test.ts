import assert from "node:assert/strict";
import test from "node:test";
import { createChessState, createLudoState, createUnoState, moveChess, rollLudo, type GamePlayer } from "./index";

const players: GamePlayer[] = [
  { id: "a", name: "A", username: "a", seat: 0, connected: true },
  { id: "b", name: "B", username: "b", seat: 1, connected: true }
];

test("chess only permits the active seat and legal moves", () => {
  const state = createChessState();
  assert.throws(() => moveChess(state, players, "b", "e7", "e5"), /turn/iu);
  assert.equal(moveChess(state, players, "a", "e2", "e4").history[0], "e4");
});

test("ludo requires a six to leave the yard", () => {
  const state = createLudoState(2);
  assert.equal(rollLudo(state, players, "a", 2).turnSeat, 1);
  assert.equal(rollLudo(state, players, "a", 6).dice, 6);
});

test("uno deals seven private cards per player", () => {
  const state = createUnoState(4, 42);
  assert.deepEqual(state.hands.map((hand) => hand.length), [7, 7, 7, 7]);
});
