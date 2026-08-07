import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { proxyBackendMutation } from "../../../../src/lib/backend";
import { readDevSessionFromCookieStore } from "../../../../src/lib/dev-session";

function pathFor(username: string, tenantId: string) {
  return `/v1/users/${encodeURIComponent(username)}/block?tenantId=${encodeURIComponent(tenantId)}`;
}

async function mutate(method: "PUT" | "DELETE", username: string) {
  const viewer = readDevSessionFromCookieStore(await cookies());
  if (!viewer) {
    return NextResponse.json({ error: { code: "UNAUTHENTICATED", message: "Sign in to manage blocked accounts." } }, { status: 401 });
  }
  return proxyBackendMutation(pathFor(username, viewer.tenantId), method, {}, viewer);
}

export async function PUT(_request: Request, context: { params: Promise<{ username: string }> }) {
  return mutate("PUT", (await context.params).username);
}

export async function DELETE(_request: Request, context: { params: Promise<{ username: string }> }) {
  return mutate("DELETE", (await context.params).username);
}
