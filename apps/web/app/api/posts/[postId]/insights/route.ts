import type { ContentInsightResponse } from "@vyb/contracts";
import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { getPostInsights } from "../../../../../src/lib/backend";
import { readDevSessionFromCookieStore } from "../../../../../src/lib/dev-session";

const ranges = new Set<ContentInsightResponse["range"]>(["24h", "7d", "30d", "lifetime"]);

export async function GET(request: Request, context: { params: Promise<{ postId: string }> }) {
  const viewer = readDevSessionFromCookieStore(await cookies());
  if (!viewer) {
    return NextResponse.json({ error: { code: "UNAUTHENTICATED", message: "Sign in to view post insights." } }, { status: 401 });
  }
  const requested = new URL(request.url).searchParams.get("range") as ContentInsightResponse["range"] | null;
  const range = requested && ranges.has(requested) ? requested : "7d";
  return NextResponse.json(await getPostInsights(viewer, (await context.params).postId, range), {
    headers: { "cache-control": "private, no-store" }
  });
}
