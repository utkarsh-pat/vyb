"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import type { MiniGameSlug } from "../lib/mini-game-slugs";

const GAMES = {
  "color-sort": { title: "Color Sort", description: "Sort every colour into its own tube with undo and saved progress.", icon: "C" },
  "n-queens": { title: "N-Queens Solver", description: "Explore the daily puzzle's backtracking solution step by step.", icon: "Q" },
  "word-puzzle": { title: "Word Puzzle", description: "A local daily-style five-letter challenge.", icon: "W" }
} as const;

export function MiniGamesController({ game }: { game: MiniGameSlug }) {
  const [frameReady, setFrameReady] = useState(false);
  const meta = GAMES[game];
  const src = useMemo(() => `/games/${game}/index.html`, [game]);

  useEffect(() => {
    setFrameReady(false);
  }, [src]);

  return (
    <main className="vyb-mini-game-page">
      <header className="vyb-mini-game-header">
        <Link href="/hub/gameshub" className="vyb-mini-game-back">← Games hub</Link>
        <div>
          <p className="vyb-mini-game-kicker">Vyb Playground</p>
          <h1>{meta.icon} {meta.title}</h1>
          <p>{meta.description}</p>
        </div>
      </header>
      <section className="vyb-mini-game-frame-shell" aria-busy={!frameReady}>
        {!frameReady ? <div className="vyb-mini-game-loading">Loading {meta.title}…</div> : null}
        <iframe
          src={src}
          title={`Vyb ${meta.title}`}
          sandbox="allow-scripts allow-same-origin"
          loading="eager"
          onLoad={() => setFrameReady(true)}
          className="vyb-mini-game-frame"
        />
      </section>
      <p className="vyb-mini-game-note">Runs entirely on this device. Progress and preferences stay in local storage.</p>
    </main>
  );
}
