import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";
import { MiniGamesController, isMiniGameSlug } from "../../../../src/components/mini-games-controller";
import { readDevSessionFromCookieStore } from "../../../../src/lib/dev-session";

type MiniGamePageProps = { params: Promise<{ slug: string }> };

export default async function MiniGamePage({ params }: MiniGamePageProps) {
  const viewer = readDevSessionFromCookieStore(await cookies());
  if (!viewer) redirect("/login");

  const { slug } = await params;
  if (!isMiniGameSlug(slug)) notFound();
  return <MiniGamesController game={slug} />;
}
