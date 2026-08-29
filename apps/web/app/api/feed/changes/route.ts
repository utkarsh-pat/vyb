import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { getCampusFeedChanges } from "../../../../src/lib/backend";
import { readDevSessionFromCookieStore } from "../../../../src/lib/dev-session";

export async function GET(request: Request) {
  const viewer = readDevSessionFromCookieStore(await cookies());
  if (!viewer) {
    return NextResponse.json(
      { error: { code: "UNAUTHENTICATED", message: "Sign in to refresh your campus feed." } },
      { status: 401 }
    );
  }
  const after = new URL(request.url).searchParams.get("after");
  try {
    return NextResponse.json(await getCampusFeedChanges(viewer, after));
  } catch (error) {
    console.error("[feed-changes] reconcile.failed", error);
    return NextResponse.json(
      { error: { code: "FEED_RECONCILE_FAILED", message: "Feed updates could not be checked." } },
      { status: 503 }
    );
  }
}
