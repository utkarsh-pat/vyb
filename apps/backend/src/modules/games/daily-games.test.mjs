import assert from "node:assert/strict";
import { test } from "node:test";
import {
  createDailyGamesService,
  GamesContractError,
  normalizeCoordinates
} from "./daily-games.mjs";

const NOW = Date.parse("2026-05-01T06:00:00.000Z");
const SECRET = "test-games-session-secret-with-sufficient-entropy";
const viewer = {
  userId: "user-1",
  tenantId: "tenant-1",
  displayName: "Test Student"
};
const connectSolution = [
  { x: 0, y: 0 },
  { x: 1, y: 0 },
  { x: 1, y: 1 },
  { x: 0, y: 1 }
];
const queensSolution = [
  { x: 0, y: 1 },
  { x: 1, y: 3 },
  { x: 2, y: 0 },
  { x: 3, y: 2 }
];

const seeds = {
  connect: {
    launchDate: "2026-05-01T00:00:00.000Z",
    levels: [
      {
        level_id: 1,
        grid_size: 2,
        dots: [
          { id: 1, x: 0, y: 0 },
          { id: 2, x: 0, y: 1 }
        ],
        solution_path: connectSolution,
        difficulty: "Intro"
      }
    ]
  },
  queens: {
    launchDate: "2026-05-01T00:00:00.000Z",
    levels: [
      {
        level_id: 1,
        grid_size: 4,
        regions: [
          [1, 1, 1, 1],
          [2, 2, 2, 2],
          [3, 3, 3, 3],
          [4, 4, 4, 4]
        ],
        solution: queensSolution,
        difficulty: "Intro"
      }
    ]
  }
};

function createFixtureService() {
  let nowMs = NOW;
  return {
    service: createDailyGamesService({
      loadSeed: async (game) => structuredClone(seeds[game]),
      now: () => nowMs,
      getSecret: () => SECRET
    }),
    advance(milliseconds) {
      nowMs += milliseconds;
    }
  };
}

test("coordinate normalization rejects malformed and oversized payloads", () => {
  assert.equal(normalizeCoordinates("not-an-array"), null);
  assert.equal(normalizeCoordinates([{ x: 1, y: "2" }]), null);
  assert.equal(normalizeCoordinates(Array.from({ length: 3 }, () => ({ x: 0, y: 0 })), 2), null);
  assert.deepEqual(normalizeCoordinates([{ x: 1, y: 2 }]), [{ x: 1, y: 2 }]);
});

test("daily responses expose public levels without solutions or leaderboards", async () => {
  const { service } = createFixtureService();
  const connect = await service.start("connect", viewer);
  const queens = await service.start("queens", viewer);

  assert.equal(connect.game, "connect");
  assert.equal(connect.level.levelId, 1);
  assert.equal("solution_path" in connect.level, false);
  assert.equal(connect.leaderboardOptIn, false);
  assert.deepEqual(connect.leaderboard, []);

  assert.equal(queens.game, "queens");
  assert.equal(queens.level.regionCount, 4);
  assert.equal("solution" in queens.level, false);
  assert.equal(queens.leaderboardOptIn, false);
});

test("signed sessions are bound to the Firebase actor tenant and user", async () => {
  const { service } = createFixtureService();
  const daily = await service.start("connect", viewer);

  await assert.rejects(
    service.hint(
      "connect",
      { ...viewer, tenantId: "tenant-2" },
      { sessionId: daily.sessionId, path: [] }
    ),
    (error) =>
      error instanceof GamesContractError &&
      error.status === 403 &&
      error.code === "GAME_SESSION_FORBIDDEN"
  );
});

test("Connect hint rotates the signed session and exact server solution submits", async () => {
  const fixture = createFixtureService();
  const daily = await fixture.service.start("connect", viewer);
  const hint = await fixture.service.hint("connect", viewer, {
    sessionId: daily.sessionId,
    path: [connectSolution[0]]
  });

  assert.notEqual(hint.sessionId, daily.sessionId);
  assert.deepEqual(hint.nextMove, connectSolution[1]);
  assert.equal(hint.hintsUsed, 1);

  fixture.advance(10_000);
  const rejected = await fixture.service.submit("connect", viewer, {
    sessionId: hint.sessionId,
    path: connectSolution.slice(0, -1)
  });
  assert.equal(rejected.solved, false);

  const solved = await fixture.service.submit("connect", viewer, {
    sessionId: hint.sessionId,
    path: connectSolution
  });
  assert.equal(solved.solved, true);
  assert.equal(solved.hintsUsed, 1);
  assert.equal(solved.adjustedTimeSeconds >= 3, true);
});

test("Queens highlights conflicts and accepts only the server-owned solution", async () => {
  const { service } = createFixtureService();
  const daily = await service.start("queens", viewer);
  const hint = await service.hint("queens", viewer, {
    sessionId: daily.sessionId,
    queens: [
      { x: 0, y: 0 },
      { x: 0, y: 3 }
    ],
    marks: []
  });

  assert.equal(hint.stage, "conflict");
  assert.equal(hint.errorCells.length, 2);
  assert.equal(hint.hintsUsed, 1);

  const invalid = await service.submit("queens", viewer, {
    sessionId: hint.sessionId,
    queens: [
      { x: 0, y: 0 },
      { x: 0, y: 3 },
      { x: 2, y: 1 },
      { x: 3, y: 2 }
    ]
  });
  assert.equal(invalid.solved, false);
  assert.equal(invalid.errorsMade, 1);

  const solved = await service.submit("queens", viewer, {
    sessionId: hint.sessionId,
    queens: queensSolution
  });
  assert.equal(solved.solved, true);
  assert.equal(solved.hintsUsed, 1);
});
