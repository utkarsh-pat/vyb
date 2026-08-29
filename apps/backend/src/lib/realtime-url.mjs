function firstForwardedValue(value) {
  return String(value ?? "").split(",")[0].trim();
}

function normalizeRealtimeOrigin(value) {
  const raw = String(value ?? "").trim();
  if (!raw) return null;

  const origin = new URL(raw);
  if (!["http:", "https:", "ws:", "wss:"].includes(origin.protocol)) {
    throw new Error("Realtime public origin must use http(s) or ws(s).");
  }
  origin.pathname = "/";
  origin.search = "";
  origin.hash = "";
  return origin;
}

export function buildPublicRealtimeSocketUrl({ request, path, token, env = process.env }) {
  const configuredOrigin = normalizeRealtimeOrigin(env.VYB_REALTIME_PUBLIC_ORIGIN);
  const forwardedProto = firstForwardedValue(request.headers["x-forwarded-proto"]);
  const forwardedHost = firstForwardedValue(request.headers["x-forwarded-host"]);
  const secure = forwardedProto ? forwardedProto === "https" : Boolean(request.socket?.encrypted);
  const fallbackHost = forwardedHost || request.headers.host;
  const origin = configuredOrigin ?? new URL(`${secure ? "https" : "http"}://${fallbackHost}`);
  const socketUrl = new URL(path, origin);

  socketUrl.protocol = socketUrl.protocol === "https:" || socketUrl.protocol === "wss:" ? "wss:" : "ws:";
  socketUrl.searchParams.set("token", token);
  return socketUrl.toString();
}
