import assert from "node:assert/strict";
import test from "node:test";
import { buildPublicRealtimeSocketUrl } from "./realtime-url.mjs";

const request = {
  headers: {
    host: "api.vybnet.app",
    "x-forwarded-host": "api.vybnet.app",
    "x-forwarded-proto": "https"
  },
  socket: { encrypted: false }
};

test("uses the request host when no realtime origin is configured", () => {
  assert.equal(
    buildPublicRealtimeSocketUrl({ request, path: "/ws/chat", token: "signed", env: {} }),
    "wss://api.vybnet.app/ws/chat?token=signed"
  );
});

test("routes websocket upgrades directly to the configured realtime origin", () => {
  assert.equal(
    buildPublicRealtimeSocketUrl({
      request,
      path: "/ws/chat",
      token: "a.b",
      env: { VYB_REALTIME_PUBLIC_ORIGIN: "https://vyb-backend.example.run.app" }
    }),
    "wss://vyb-backend.example.run.app/ws/chat?token=a.b"
  );
});

test("rejects an unsafe realtime origin protocol", () => {
  assert.throws(
    () => buildPublicRealtimeSocketUrl({
      request,
      path: "/ws/chat",
      token: "signed",
      env: { VYB_REALTIME_PUBLIC_ORIGIN: "ftp://example.com" }
    }),
    /http\(s\) or ws\(s\)/u
  );
});
