import { readJson, sendError, sendJson } from "../../lib/http.mjs";
import { resolveLiveContext } from "../shared/viewer-context.mjs";
import { dailyGamesService, GamesContractError } from "./daily-games.mjs";
import {
  multiplayerSessionService,
  MultiplayerSessionError
} from "./multiplayer-session.mjs";

const GAME_ROUTE = /^\/v1\/games\/(connect|queens)\/(daily|hint|submit)$/u;
const MULTIPLAYER_SESSION_ROUTE = "/v1/games/multiplayer/session";

export function getGamesModuleHealth() {
  return {
    module: "games",
    status: "ok",
    persistence: "dataconnect-levels",
    leaderboard: "disabled-until-durable-session-store"
  };
}

function sendGamesFailure(response, error) {
  if (error instanceof GamesContractError || error instanceof MultiplayerSessionError) {
    sendError(response, error.status, error.code, error.message);
    return;
  }
  console.error("[games] request failed", {
    message: error instanceof Error ? error.message : "unknown"
  });
  sendError(response, 502, "GAMES_SERVICE_FAILED", "Games are unavailable right now.");
}

export async function handleGamesRoute({
  request,
  response,
  url,
  context,
  service = dailyGamesService,
  multiplayerService = multiplayerSessionService,
  resolveViewer = resolveLiveContext
}) {
  const match = url.pathname.match(GAME_ROUTE);
  const isMultiplayerSession = url.pathname === MULTIPLAYER_SESSION_ROUTE;
  if (!match && !isMultiplayerSession) return false;

  if (!context.actor) {
    sendError(response, 401, "UNAUTHENTICATED", "Sign in with your campus account to play.");
    return true;
  }

  const resolved = await resolveViewer(context.actor);
  if (!resolved?.live?.tenant || !resolved.live.user || !resolved.live.membership) {
    sendError(response, 403, "CAMPUS_MEMBERSHIP_REQUIRED", "An active campus membership is required.");
    return true;
  }

  const viewer = {
    userId: resolved.live.user.id,
    tenantId: resolved.live.tenant.id,
    membershipId: resolved.live.membership.id,
    email: resolved.viewer.primaryEmail,
    displayName: resolved.viewer.displayName
  };

  if (isMultiplayerSession) {
    if (request.method !== "POST") {
      sendError(response, 405, "METHOD_NOT_ALLOWED", "Use POST to prepare an online game room.");
      return true;
    }
    const body = await readJson(request);
    if (body === null || !body || typeof body !== "object" || Array.isArray(body)) {
      sendError(response, 400, "INVALID_JSON", "Request body must be valid JSON.");
      return true;
    }
    try {
      sendJson(response, 200, await multiplayerService.create(viewer, body), {
        "cache-control": "private, no-store"
      });
    } catch (error) {
      sendGamesFailure(response, error);
    }
    return true;
  }

  const [, game, action] = match;

  if (action === "daily") {
    if (request.method !== "GET") {
      sendError(response, 405, "METHOD_NOT_ALLOWED", "Use GET for the daily game.");
      return true;
    }
    try {
      sendJson(response, 200, await service.start(game, viewer));
    } catch (error) {
      sendGamesFailure(response, error);
    }
    return true;
  }

  if (request.method !== "POST") {
    sendError(response, 405, "METHOD_NOT_ALLOWED", "Use POST for this game action.");
    return true;
  }
  const body = await readJson(request);
  if (body === null || !body || typeof body !== "object" || Array.isArray(body)) {
    sendError(response, 400, "INVALID_JSON", "Request body must be valid JSON.");
    return true;
  }

  try {
    const result = action === "hint"
      ? await service.hint(game, viewer, body)
      : await service.submit(game, viewer, body);
    sendJson(response, 200, result);
  } catch (error) {
    sendGamesFailure(response, error);
  }
  return true;
}
