import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { proxyBackendMutation } from "../../../../../src/lib/backend";
import { readDevSessionFromCookieStore } from "../../../../../src/lib/dev-session";

async function mutate(request: Request | null, postId: string, method: "PUT" | "DELETE") {
  const viewer = readDevSessionFromCookieStore(await cookies());
  if (!viewer) {
    return NextResponse.json({ error: { code: "UNAUTHENTICATED", message: "Sign in to tune recommendations." } }, { status: 401 });
  }
  const payload = request ? await request.json().catch(() => null) : {};
  return proxyBackendMutation(`/v1/posts/${encodeURIComponent(postId)}/recommendation`, method, payload ?? {}, viewer);
}

export async function PUT(request: Request, context: { params: Promise<{ postId: string }> }) {
  return mutate(request, (await context.params).postId, "PUT");
}

export async function DELETE(_request: Request, context: { params: Promise<{ postId: string }> }) {
  return mutate(null, (await context.params).postId, "DELETE");
}
