export type ParsedBackendErrorBody = {
  code: string;
  message: string;
  details: unknown;
};

export function parseBackendErrorBody(
  responseText: string,
  fallbackMessage: string
): ParsedBackendErrorBody {
  try {
    const payload = JSON.parse(responseText) as {
      error?: {
        code?: string;
        message?: string;
        details?: unknown;
      };
    };
    return {
      code: payload?.error?.code?.trim() || "BACKEND_REQUEST_FAILED",
      message: payload?.error?.message?.trim() || fallbackMessage,
      details: payload?.error?.details ?? null
    };
  } catch {
    return {
      code: "BACKEND_REQUEST_FAILED",
      message: responseText.trim() || fallbackMessage,
      details: null
    };
  }
}
