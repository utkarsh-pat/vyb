import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";
import { MiniGamesController } from "../../../../src/components/mini-games-controller";
import { MultiplayerBoardGame } from "../../../../src/components/multiplayer-board-game";
import { readDevSessionFromCookieStore } from "../../../../src/lib/dev-session";
import { isMiniGameSlug } from "../../../../src/lib/mini-game-slugs";
import { isOnlineGameSlug } from "@vyb/game-engine";

type MiniGamePageProps = { params: Promise<{ slug: string }> };

export default async function MiniGamePage({ params }: MiniGamePageProps) {
  const viewer = readDevSessionFromCookieStore(await cookies());
  if (!viewer) redirect("/login");

  const { slug } = await params;
  if (isOnlineGameSlug(slug)) return <MultiplayerBoardGame game={slug} />;
  if (!isMiniGameSlug(slug)) notFound();
  return <MiniGamesController game={slug} />;
}
