function isLocalDevelopmentOrigin(origin) {
  try {
    const parsed = new URL(origin);
    return ["localhost", "127.0.0.1", "::1", "[::1]"].includes(parsed.hostname);
  } catch {
    return false;
  }
}

function getConfiguredCorsOrigins() {
  return (process.env.VYB_CORS_ALLOWED_ORIGINS ?? process.env.VYB_WEB_ORIGIN ?? "")
    .split(",")
    .map((origin) => origin.trim())
    .filter(Boolean);
}

export function resolveCorsAllowOrigin(origin) {
  if (typeof origin !== "string" || !origin.trim()) {
    return null;
  }

  const normalized = origin.trim();
  const allowedOrigins = getConfiguredCorsOrigins();

  if (allowedOrigins.includes(normalized)) {
    return normalized;
  }

  if (process.env.NODE_ENV !== "production" && isLocalDevelopmentOrigin(normalized)) {
    return normalized;
  }

  return null;
}

export function attachCorsContext(response, request) {
  response.__vybCorsAllowOrigin = resolveCorsAllowOrigin(request.headers.origin);
}

export function buildCorsHeaders(allowOrigin = null) {
  const headers = {
    "access-control-allow-methods": "GET,POST,PUT,PATCH,DELETE,OPTIONS",
    "access-control-allow-headers":
      "authorization,content-type,x-demo-user-id,x-demo-email,x-demo-display-name,x-request-id,x-vyb-internal-key"
  };

  if (allowOrigin) {
    headers["access-control-allow-origin"] = allowOrigin;
    headers.vary = "Origin";
  }

  return headers;
}

function buildTimingHeaders(response) {
  const startedAt = Number(response.__vybRequestStartedAt ?? 0);

  if (!Number.isFinite(startedAt) || startedAt <= 0) {
    return {};
  }

  return {
    "server-timing": `app;dur=${Math.max(0, Date.now() - startedAt)}`
  };
}

export function sendJson(response, statusCode, payload, extraHeaders = {}) {
  response.writeHead(statusCode, {
    "content-type": "application/json; charset=utf-8",
    ...buildCorsHeaders(response.__vybCorsAllowOrigin ?? null),
    ...buildTimingHeaders(response),
    ...extraHeaders
  });
  response.end(JSON.stringify(payload));
}

export function sendError(response, statusCode, code, message, details = null, extraHeaders = {}) {
  sendJson(
    response,
    statusCode,
    {
      error: {
        code,
        message,
        details
      }
    },
    extraHeaders
  );
}

export class RequestBodyTooLargeError extends Error {
  constructor(maxBytes) {
    super(`Request body exceeds the ${maxBytes} byte limit.`);
    this.name = "RequestBodyTooLargeError";
    this.maxBytes = maxBytes;
  }
}

export async function readTextBody(request, { maxBytes = Number.POSITIVE_INFINITY } = {}) {
  const chunks = [];
  let totalBytes = 0;
  for await (const chunk of request) {
    const buffer = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
    totalBytes += buffer.byteLength;
    if (totalBytes > maxBytes) {
      throw new RequestBodyTooLargeError(maxBytes);
    }
    chunks.push(buffer);
  }

  if (chunks.length === 0) {
    return "";
  }

  return Buffer.concat(chunks).toString("utf8");
}

export async function readJson(request, options) {
  const body = await readTextBody(request, options);
  if (!body) {
    return {};
  }

  try {
    return JSON.parse(body);
  } catch {
    return null;
  }
}
