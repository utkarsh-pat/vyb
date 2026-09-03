import assert from "node:assert/strict";
import { Readable } from "node:stream";
import { test } from "node:test";
import { handleGamesRoute } from "./index.mjs";

function requestWithJson(payload) {
  const request = Readable.from([JSON.stringify(payload)]);
  request.method = "POST";
  return request;
}

function captureResponse() {
  return {
    statusCode: null,
    headers: null,
    body: null,
    writeHead(statusCode, headers) {
      this.statusCode = statusCode;
      this.headers = headers;
    },
    end(body) {
      this.body = body;
    }
  };
}

test("multiplayer session route does not depend on the daily-game path match", async () => {
  const response = captureResponse();
  const handled = await handleGamesRoute({
    request: requestWithJson({ game: "chess" }),
    response,
    url: new URL("https://api.example.test/v1/games/multiplayer/session"),
    context: { actor: { id: "firebase-user", email: "student@college.edu" } },
    resolveViewer: async () => ({
      viewer: { primaryEmail: "student@college.edu", displayName: "Student" },
      live: {
        tenant: { id: "tenant-1" },
        user: { id: "user-1" },
        membership: { id: "membership-1" }
      }
    }),
    multiplayerService: {
      create: async (viewer, input) => ({
        roomCode: "ABC234",
        wsUrl: `wss://games.example.test/ws?game=${input.game}&user=${viewer.userId}`,
        expiresAt: 123
      })
    }
  });

  assert.equal(handled, true);
  assert.equal(response.statusCode, 200);
  assert.equal(response.headers["cache-control"], "private, no-store");
  assert.deepEqual(JSON.parse(response.body), {
    roomCode: "ABC234",
    wsUrl: "wss://games.example.test/ws?game=chess&user=user-1",
    expiresAt: 123
  });
});
