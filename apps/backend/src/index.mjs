import { createServer } from "node:http";
import { loadRootEnv } from "../../../packages/config/src/index.mjs";
import { attachCorsContext, buildCorsHeaders, sendError, sendJson } from "./lib/http.mjs";
import { createRequestContext } from "./lib/request-context.mjs";
import { evaluateRuntimeReadiness } from "./lib/readiness.mjs";
import { buildAndroidUpdateManifest } from "./lib/android-update.mjs";
import { launchCollege } from "./modules/identity/college-access.mjs";
import { getCampusModuleHealth, handleCampusRoute } from "./modules/campus/index.mjs";
import { canOpenChatRealtimeConnection, getChatModuleHealth, handleChatRoute } from "./modules/chat/index.mjs";
import { attachChatWebSocketServer } from "./modules/chat/realtime-hub.mjs";
import { getIdentityModuleHealth, handleIdentityRoute } from "./modules/identity/index.mjs";
import { getMarketModuleHealth, handleMarketRoute } from "./modules/market/index.mjs";
import { getModerationModuleHealth, handleModerationRoute } from "./modules/moderation/index.mjs";
import { getResourcesModuleHealth, handleResourcesRoute } from "./modules/resources/index.mjs";
import { getEventsModuleHealth, handleEventsRoute } from "./modules/events/index.mjs";
import { getNotificationsModuleHealth, handleNotificationsRoute } from "./modules/notifications/index.mjs";
import { canOpenSocialRealtimeConnection, getSocialModuleHealth, handleSocialRoute } from "./modules/social/index.mjs";
import { attachSocialWebSocketServer } from "./modules/social/realtime-hub.mjs";
import {
  attachScribbleWebSocketServer,
  getScribbleModuleHealth,
  handleScribblePublicRoomsRoute,
  handleScribbleSocketTokenRoute
} from "./modules/games/scribble-realtime-hub.mjs";
import { getGamesModuleHealth, handleGamesRoute } from "./modules/games/index.mjs";
import { getAnalyticsModuleHealth, handleAnalyticsRoute } from "./modules/analytics/index.mjs";

loadRootEnv();

const port = Number(process.env.PORT ?? 4000);
const routeHandlers = [handleAnalyticsRoute, handleIdentityRoute, handleCampusRoute, handleSocialRoute, handleChatRoute, handleResourcesRoute, handleMarketRoute, handleEventsRoute, handleNotificationsRoute, handleModerationRoute, handleGamesRoute];

export async function handleBackendRequest(request, response) {
  const startedAt = Date.now();
  const url = new URL(request.url ?? "/", `http://${request.headers.host ?? "localhost"}`);
  attachCorsContext(response, request);
  const context = await createRequestContext(request);
  response.__vybRequestStartedAt = startedAt;
  const actorLabel = context.actor ? `${context.actor.id}:${context.actor.email}` : "anonymous";
  const slowRequestThresholdMs = Number(process.env.VYB_BACKEND_SLOW_REQUEST_MS ?? 1000);

  response.on("finish", () => {
    const durationMs = Date.now() - startedAt;
    const isSlow =
      Number.isFinite(slowRequestThresholdMs) &&
      slowRequestThresholdMs > 0 &&
      durationMs >= slowRequestThresholdMs;
    const log = isSlow || response.statusCode >= 500 ? console.warn : console.log;

    log(
      `[backend] ${context.requestId} ${request.method} ${url.pathname} ${response.statusCode} ${durationMs}ms actor=${actorLabel}${isSlow ? " slow=true" : ""}`
    );
  });

  try {
    if (request.method === "OPTIONS") {
      response.writeHead(204, {
        ...buildCorsHeaders(response.__vybCorsAllowOrigin ?? null),
        "server-timing": `app;dur=${Math.max(0, Date.now() - startedAt)}`,
        "x-request-id": context.requestId
      });
      response.end();
      return;
    }

    if (request.method === "GET" && url.pathname === "/health") {
      sendJson(
        response,
        200,
        {
          service: "backend",
          runtime: "modular-monolith",
          status: "ok",
          timestamp: new Date().toISOString(),
          modules: [
            getIdentityModuleHealth(),
            getCampusModuleHealth(),
            getSocialModuleHealth(),
            getChatModuleHealth(),
            getResourcesModuleHealth(),
            getMarketModuleHealth(),
            getEventsModuleHealth(),
            getNotificationsModuleHealth(),
            getModerationModuleHealth(),
            getGamesModuleHealth(),
            getScribbleModuleHealth(),
            getAnalyticsModuleHealth()
          ]
        },
        {
          "x-request-id": context.requestId
        }
      );
      return;
    }

    if (request.method === "GET" && url.pathname === "/v1/client-shell") {
      sendJson(
        response,
        200,
        {
          shell: "pwa-first",
          mobileInstallable: true,
          desktopResponsive: true,
          nativeReadyContracts: true,
          backendRuntime: "modular-monolith",
          launchCampus: launchCollege,
          hero: {
            eyebrow: "Verified Campus Network",
            title: "One trusted home for college identity, community, and utility.",
            summary: "Vyb is a multi-tenant platform for verified campus life, built to onboard one trusted campus at a time."
          },
          pillars: [
            {
              title: "Trusted Identity",
              description: "Access begins with a verified college email so every interaction stays inside the right campus boundary."
            },
            {
              title: "Useful Community",
              description: "Students should land inside relevant college, branch, batch, and hostel spaces instead of scattered chat groups."
            },
            {
              title: "Daily Utility",
              description: "Notes, resources, and the social layer should work together so the product earns repeat use."
            }
          ],
          phaseOne: [
            "College-scoped authentication and onboarding",
            "Verified communities and membership routing",
            "Campus feed for text and image posts",
            "Academic resource vault",
            "Moderation-aware backend foundation"
          ],
          trustPoints: [
            "Single backend runtime for simpler Phase 1 delivery",
            "Responsive web now, native-ready contracts later",
            "Strict tenant boundaries across every authenticated flow"
          ]
        },
        {
          "x-request-id": context.requestId
        }
      );
      return;
    }

    if (request.method === "GET" && url.pathname === "/ready") {
      const readiness = evaluateRuntimeReadiness();
      sendJson(
        response,
        readiness.ready ? 200 : 503,
        {
          service: "backend",
          status: readiness.ready ? "ready" : "not-ready",
          ...readiness,
          revision: process.env.K_REVISION ?? null
        },
        {
          "cache-control": "no-store",
          "x-request-id": context.requestId
        }
      );
      return;
    }

    if (request.method === "GET" && url.pathname === "/v1/app-updates/android") {
      const currentVersionCode = Number(url.searchParams.get("versionCode") ?? 0);
      const manifest = buildAndroidUpdateManifest({ currentVersionCode });
      sendJson(
        response,
        200,
        manifest,
        {
          "cache-control": "public, max-age=60",
          "x-request-id": context.requestId
        }
      );
      return;
    }

    if (await handleScribbleSocketTokenRoute({ request, response, url, context })) {
      return;
    }

    if (await handleScribblePublicRoomsRoute({ request, response, url, context })) {
      return;
    }

    for (const handler of routeHandlers) {
      // Each module decides whether it owns the route and writes the response directly.
      if (await handler({ request, response, url, context })) {
        return;
      }
    }

    sendError(response, 404, "ROUTE_NOT_FOUND", `Unknown route ${url.pathname}`, null, {
      "x-request-id": context.requestId
    });
  } catch (error) {
    console.error(`[backend] ${context.requestId} unhandled-request-error`, {
      method: request.method,
      path: url.pathname,
      actor: actorLabel,
      message: error instanceof Error ? error.message : "unknown",
      stack: error instanceof Error ? error.stack : null
    });

    if (!response.headersSent) {
      sendError(response, 500, "INTERNAL_ERROR", "We could not process this request right now.", null, {
        "x-request-id": context.requestId
      });
      return;
    }

    response.end();
  }
}

// Vercel imports this module as a request handler. It cannot keep a listening
// socket or accept HTTP upgrades, so the standalone server is only created
// outside that runtime.
if (process.env.VYB_SERVERLESS_RUNTIME !== "vercel" && process.env.VERCEL !== "1") {
  const server = createServer(handleBackendRequest);

  attachChatWebSocketServer(server, {
    authorizeConnection: canOpenChatRealtimeConnection
  });

  attachSocialWebSocketServer(server, {
    authorizeConnection: canOpenSocialRealtimeConnection
  });

  attachScribbleWebSocketServer(server);

  server.listen(port, () => {
    console.log(`[backend] listening on http://localhost:${port}`);
  });
}
