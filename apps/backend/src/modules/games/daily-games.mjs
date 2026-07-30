import { createHmac, timingSafeEqual } from "node:crypto";
import { getFirebaseDataConnect } from "../../../../../packages/config/src/index.mjs";

const DAY_MS = 24 * 60 * 60 * 1000;
const HINT_COOLDOWN_SECONDS = 5;
const HINT_PENALTY_SECONDS = 3;
const SESSION_PREFIX = "vyb-games.v1";
const DEFAULT_LAUNCH_DATE = "2026-05-01T00:00:00+05:30";
const connectorConfig = {
  connector: "connect",
  serviceId: "vyb",
  location: "asia-south1"
};

const GET_GAME_LEVEL_STORE_QUERY = `
  query GetBackendGameLevelStore($id: String!) {
    gamesLevel(key: { id: $id }) {
      id
      payloadJson
      totalLevels
      launchDate
      checksum
      updatedAt
    }
  }
`;

export class GamesContractError extends Error {
  constructor(status, code, message) {
    super(message);
    this.name = "GamesContractError";
    this.status = status;
    this.code = code;
  }
}

function getStoreId(game) {
  if (game === "connect") {
    return process.env.VYB_CONNECT_GAME_LEVEL_STORE_ID ?? "connect-1000-levels";
  }
  if (game === "queens") {
    return process.env.VYB_QUEENS_GAME_LEVEL_STORE_ID ?? "queens-1000-levels";
  }
  throw new GamesContractError(404, "GAME_NOT_FOUND", "This game is not available.");
}

async function loadSeedFromDataConnect(game) {
  const response = await getFirebaseDataConnect(connectorConfig).executeGraphqlRead(
    GET_GAME_LEVEL_STORE_QUERY,
    { variables: { id: getStoreId(game) } }
  );
  const row = response.data?.gamesLevel;
  if (!row?.payloadJson) {
    throw new GamesContractError(
      503,
      "GAME_LEVELS_UNAVAILABLE",
      `${game === "connect" ? "Connect" : "Queens"} levels are not deployed.`
    );
  }

  let seed;
  try {
    seed = JSON.parse(row.payloadJson);
  } catch {
    throw new GamesContractError(503, "GAME_LEVELS_INVALID", "The deployed game level payload is invalid.");
  }

  return {
    ...seed,
    launchDate: row.launchDate ?? seed.launchDate,
    totalLevels: row.totalLevels ?? seed.totalLevels
  };
}

function resolveSessionSecret() {
  const secret = process.env.VYB_GAMES_SESSION_SECRET?.trim() ?? process.env.VYB_SESSION_SECRET?.trim();
  if (secret) {
    return secret;
  }
  if (process.env.NODE_ENV !== "production") {
    return "local-vyb-games-session-secret";
  }
  throw new GamesContractError(
    503,
    "GAMES_SESSION_SECRET_MISSING",
    "Games session signing is not configured."
  );
}

function encodeSession(payload, secret) {
  const encoded = Buffer.from(JSON.stringify(payload), "utf8").toString("base64url");
  const signature = createHmac("sha256", secret).update(encoded).digest("base64url");
  return `${SESSION_PREFIX}.${encoded}.${signature}`;
}

function constantTimeEqual(left, right) {
  const leftBuffer = Buffer.from(left);
  const rightBuffer = Buffer.from(right);
  return leftBuffer.length === rightBuffer.length && timingSafeEqual(leftBuffer, rightBuffer);
}

function decodeSession(token, viewer, game, secret) {
  if (typeof token !== "string" || !token.trim()) {
    throw new GamesContractError(400, "INVALID_GAME_SESSION", "Open today's puzzle again.");
  }
  const parts = token.split(".");
  if (parts.length !== 4 || `${parts[0]}.${parts[1]}` !== SESSION_PREFIX) {
    throw new GamesContractError(400, "INVALID_GAME_SESSION", "This game session is invalid.");
  }
  const [, , encoded, signature] = parts;
  const expected = createHmac("sha256", secret).update(encoded).digest("base64url");
  if (!constantTimeEqual(signature, expected)) {
    throw new GamesContractError(400, "INVALID_GAME_SESSION", "This game session is invalid.");
  }

  let payload;
  try {
    payload = JSON.parse(Buffer.from(encoded, "base64url").toString("utf8"));
  } catch {
    throw new GamesContractError(400, "INVALID_GAME_SESSION", "This game session is invalid.");
  }

  if (
    payload?.game !== game ||
    payload?.tenantId !== viewer.tenantId ||
    payload?.userId !== viewer.userId ||
    !Number.isInteger(payload?.levelId) ||
    !Number.isInteger(payload?.dailyIndex) ||
    typeof payload?.dailyKey !== "string" ||
    !Number.isFinite(new Date(payload?.startedAt).getTime())
  ) {
    throw new GamesContractError(403, "GAME_SESSION_FORBIDDEN", "This game session belongs to another account or campus.");
  }
  return payload;
}

function normalizeCoordinate(value) {
  if (!value || typeof value !== "object") return null;
  const x = value.x;
  const y = value.y;
  return Number.isInteger(x) && Number.isInteger(y) ? { x, y } : null;
}

export function normalizeCoordinates(value, maximum = 256) {
  if (!Array.isArray(value) || value.length > maximum) return null;
  const coordinates = value.map(normalizeCoordinate);
  return coordinates.some((point) => point === null) ? null : coordinates;
}

function coordinateKey(point) {
  return `${point.x}:${point.y}`;
}

function coordinatesEqual(left, right) {
  return left.x === right.x && left.y === right.y;
}

function normalizeGameSeed(game, seed) {
  if (!seed || !Array.isArray(seed.levels) || seed.levels.length === 0) {
    throw new GamesContractError(503, "GAME_LEVELS_INVALID", "No valid game levels are deployed.");
  }
  const launchDate = seed.launchDate ?? DEFAULT_LAUNCH_DATE;
  if (!Number.isFinite(new Date(launchDate).getTime())) {
    throw new GamesContractError(503, "GAME_LAUNCH_DATE_INVALID", "The game launch date is invalid.");
  }

  const levels = seed.levels.filter((level) => {
    if (!Number.isInteger(level?.level_id) || !Number.isInteger(level?.grid_size)) return false;
    if (game === "connect") {
      return Array.isArray(level.dots) && Array.isArray(level.solution_path);
    }
    return Array.isArray(level.regions) && Array.isArray(level.solution);
  });
  if (levels.length === 0) {
    throw new GamesContractError(503, "GAME_LEVELS_INVALID", "No valid game levels are deployed.");
  }
  return { launchDate, levels };
}

function getDailyState(launchDate, levels, nowMs) {
  const launchMs = new Date(launchDate).getTime();
  const dailyIndex = Math.max(0, Math.floor((nowMs - launchMs) / DAY_MS));
  const level = levels[dailyIndex % levels.length];
  const launchDay = launchDate.match(/^(\d{4})-(\d{2})-(\d{2})/u);
  const dailyKey = launchDay
    ? new Date(Date.UTC(Number(launchDay[1]), Number(launchDay[2]) - 1, Number(launchDay[3]) + dailyIndex))
        .toISOString()
        .slice(0, 10)
    : `day-${dailyIndex}`;
  return {
    dailyIndex,
    dailyKey,
    level,
    nextResetAt: new Date(launchMs + (dailyIndex + 1) * DAY_MS).toISOString()
  };
}

function publicConnectLevel(level) {
  return {
    levelId: level.level_id,
    gridSize: level.grid_size,
    dots: level.dots.map((dot) => ({ id: dot.id, x: dot.x, y: dot.y })),
    difficulty: level.difficulty ?? "Intro"
  };
}

function publicQueensLevel(level) {
  const regionIds = new Set(level.regions.flat().filter(Number.isInteger));
  return {
    levelId: level.level_id,
    gridSize: level.grid_size,
    regionCount: regionIds.size,
    regions: level.regions,
    difficulty: level.difficulty ?? "Intro"
  };
}

function createSession(game, viewer, daily, nowIso) {
  return {
    game,
    tenantId: viewer.tenantId,
    userId: viewer.userId,
    levelId: daily.level.level_id,
    dailyIndex: daily.dailyIndex,
    dailyKey: daily.dailyKey,
    startedAt: nowIso,
    lastHintAt: null,
    hintsUsed: 0,
    errorsMade: 0,
    completedAt: null
  };
}

function requireCurrentSession(session, daily) {
  if (
    session.levelId !== daily.level.level_id ||
    session.dailyIndex !== daily.dailyIndex ||
    session.dailyKey !== daily.dailyKey
  ) {
    throw new GamesContractError(409, "GAME_SESSION_EXPIRED", "This daily session expired. Open today's puzzle again.");
  }
}

function validPrefixLength(path, solution) {
  let index = 0;
  while (index < path.length && index < solution.length && coordinatesEqual(path[index], solution[index])) {
    index += 1;
  }
  return index;
}

function isExactPath(path, solution) {
  return path.length === solution.length && path.every((point, index) => coordinatesEqual(point, solution[index]));
}

function queensConflictCells(level, queens) {
  const conflicts = new Set();
  const inside = (point) =>
    point.x >= 0 && point.y >= 0 && point.x < level.grid_size && point.y < level.grid_size;
  queens.forEach((point) => {
    if (!inside(point)) conflicts.add(coordinateKey(point));
  });

  for (let leftIndex = 0; leftIndex < queens.length; leftIndex += 1) {
    for (let rightIndex = leftIndex + 1; rightIndex < queens.length; rightIndex += 1) {
      const left = queens[leftIndex];
      const right = queens[rightIndex];
      const sameRegion =
        inside(left) &&
        inside(right) &&
        level.regions[left.y]?.[left.x] === level.regions[right.y]?.[right.x];
      if (
        left.x === right.x ||
        left.y === right.y ||
        (Math.abs(left.x - right.x) <= 1 && Math.abs(left.y - right.y) <= 1) ||
        sameRegion
      ) {
        conflicts.add(coordinateKey(left));
        conflicts.add(coordinateKey(right));
      }
    }
  }
  return queens.filter((point) => conflicts.has(coordinateKey(point)));
}

function isQueensSolution(level, queens) {
  if (queens.length !== level.grid_size || queensConflictCells(level, queens).length > 0) return false;
  const expected = new Set(level.solution.map(coordinateKey));
  return queens.every((point) => expected.has(coordinateKey(point)));
}

function elapsedSeconds(session, nowMs) {
  return Math.max(0, Number(((nowMs - new Date(session.startedAt).getTime()) / 1000).toFixed(2)));
}

export function createDailyGamesService({
  loadSeed = loadSeedFromDataConnect,
  now = () => Date.now(),
  getSecret = resolveSessionSecret
} = {}) {
  async function loadDaily(game) {
    const normalized = normalizeGameSeed(game, await loadSeed(game));
    return {
      ...normalized,
      daily: getDailyState(normalized.launchDate, normalized.levels, now())
    };
  }

  return {
    async start(game, viewer) {
      const { launchDate, daily } = await loadDaily(game);
      const startedAt = new Date(now()).toISOString();
      const session = createSession(game, viewer, daily, startedAt);
      const sessionId = encodeSession(session, getSecret());
      const common = {
        game,
        sessionId,
        dailyIndex: daily.dailyIndex,
        dailyKey: daily.dailyKey,
        launchDate,
        nextResetAt: daily.nextResetAt,
        serverStartedAt: startedAt,
        hintsUsed: 0,
        leaderboardOptIn: false,
        sessionCompletedAt: null,
        elapsedSeconds: null,
        adjustedTimeSeconds: null,
        leaderboard: [],
        viewerBest: null
      };
      if (game === "connect") {
        return { ...common, level: publicConnectLevel(daily.level) };
      }
      return {
        ...common,
        errorsMade: 0,
        streakBonusPoints: 0,
        level: publicQueensLevel(daily.level),
        viewerStreak: 0,
        canReplay: true
      };
    },

    async hint(game, viewer, body) {
      const coordinates = normalizeCoordinates(game === "connect" ? body?.path : body?.queens);
      const marks = game === "queens" ? normalizeCoordinates(body?.marks ?? []) : [];
      if (!coordinates || !marks) {
        throw new GamesContractError(400, "INVALID_HINT_BODY", "Send valid game coordinates.");
      }
      const { daily } = await loadDaily(game);
      const session = decodeSession(body?.sessionId, viewer, game, getSecret());
      requireCurrentSession(session, daily);

      const nowMs = now();
      const lastHintMs = session.lastHintAt ? new Date(session.lastHintAt).getTime() : 0;
      const cooldownSeconds = lastHintMs
        ? Math.max(0, HINT_COOLDOWN_SECONDS - Math.floor((nowMs - lastHintMs) / 1000))
        : 0;
      if (cooldownSeconds > 0) {
        if (game === "connect") {
          return {
            sessionId: body.sessionId,
            message: `Hint cooling down: ${cooldownSeconds}s.`,
            reason: "Hint cooldown protects game integrity.",
            nextMove: null,
            from: null,
            validPrefixLength: validPrefixLength(coordinates, daily.level.solution_path),
            hintsUsed: session.hintsUsed,
            cooldownSeconds,
            ghostExpiresAt: null
          };
        }
        return {
          sessionId: body.sessionId,
          stage: "complete",
          message: `Hint cooling down: ${cooldownSeconds}s.`,
          reason: "Hint cooldown protects game integrity.",
          errorCells: [],
          autoMarkCells: [],
          nextQueen: null,
          regionId: null,
          hintsUsed: session.hintsUsed,
          errorsMade: session.errorsMade,
          cooldownSeconds,
          hintExpiresAt: null
        };
      }

      session.hintsUsed += 1;
      session.lastHintAt = new Date(nowMs).toISOString();
      const sessionId = encodeSession(session, getSecret());
      if (game === "connect") {
        const prefixLength = validPrefixLength(coordinates, daily.level.solution_path);
        return {
          sessionId,
          message: "The next safe cell is highlighted.",
          reason: "The hint follows the server-owned solution from your valid prefix.",
          nextMove: daily.level.solution_path[prefixLength] ?? null,
          from: prefixLength > 0 ? daily.level.solution_path[prefixLength - 1] : null,
          validPrefixLength: prefixLength,
          hintsUsed: session.hintsUsed,
          cooldownSeconds: HINT_COOLDOWN_SECONDS,
          ghostExpiresAt: new Date(nowMs + 3000).toISOString()
        };
      }

      const conflicts = queensConflictCells(daily.level, coordinates);
      const queenKeys = new Set(coordinates.map(coordinateKey));
      const nextQueen = conflicts.length === 0
        ? daily.level.solution.find((point) => !queenKeys.has(coordinateKey(point))) ?? null
        : null;
      return {
        sessionId,
        stage: conflicts.length > 0 ? "conflict" : nextQueen ? "reveal" : "complete",
        message: conflicts.length > 0
          ? "Conflicting queens are highlighted."
          : nextQueen
            ? "The next safe queen is highlighted."
            : "The board is complete.",
        reason: conflicts.length > 0
          ? "Queens cannot share a row, column, region, or touching cell."
          : "The hint uses the server-owned daily solution.",
        errorCells: conflicts,
        autoMarkCells: [],
        nextQueen,
        regionId: nextQueen ? daily.level.regions[nextQueen.y]?.[nextQueen.x] ?? null : null,
        hintsUsed: session.hintsUsed,
        errorsMade: session.errorsMade,
        cooldownSeconds: HINT_COOLDOWN_SECONDS,
        hintExpiresAt: new Date(nowMs + 3000).toISOString()
      };
    },

    async submit(game, viewer, body) {
      const coordinates = normalizeCoordinates(game === "connect" ? body?.path : body?.queens);
      if (!coordinates) {
        throw new GamesContractError(400, "INVALID_SUBMIT_BODY", "Send valid game coordinates.");
      }
      const { daily } = await loadDaily(game);
      const session = decodeSession(body?.sessionId, viewer, game, getSecret());
      requireCurrentSession(session, daily);

      const nowMs = now();
      const solved = game === "connect"
        ? isExactPath(coordinates, daily.level.solution_path)
        : isQueensSolution(daily.level, coordinates);
      if (!solved && game === "queens" && coordinates.length === daily.level.grid_size) {
        session.errorsMade += 1;
      }
      const elapsed = solved ? elapsedSeconds(session, nowMs) : null;
      if (solved) session.completedAt = new Date(nowMs).toISOString();
      const adjusted = elapsed === null ? null : Number((elapsed + session.hintsUsed * HINT_PENALTY_SECONDS).toFixed(2));
      const sessionId = encodeSession(session, getSecret());

      if (game === "connect") {
        return {
          solved,
          message: solved
            ? "Solved. This verified run is complete."
            : "That route does not match today's puzzle yet.",
          sessionId,
          elapsedSeconds: elapsed,
          hintsUsed: session.hintsUsed,
          adjustedTimeSeconds: adjusted,
          leaderboard: [],
          viewerBest: null
        };
      }

      const errorCells = solved ? [] : queensConflictCells(daily.level, coordinates);
      return {
        solved,
        message: solved
          ? "Solved. This verified run is complete."
          : `Place exactly ${daily.level.grid_size} valid queens before submitting.`,
        sessionId,
        errorCells,
        errorReason: errorCells.length > 0
          ? "Queens cannot share a row, column, region, or touching cell."
          : null,
        elapsedSeconds: elapsed,
        hintsUsed: session.hintsUsed,
        errorsMade: session.errorsMade,
        adjustedTimeSeconds: adjusted,
        streakBonusPoints: 0,
        leaderboard: [],
        viewerBest: null
      };
    }
  };
}

export const dailyGamesService = createDailyGamesService();
