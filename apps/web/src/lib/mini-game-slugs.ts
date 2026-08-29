export const MINI_GAME_SLUGS = [
  "color-sort",
  "n-queens",
  "word-puzzle"
] as const;

export type MiniGameSlug = (typeof MINI_GAME_SLUGS)[number];

const MINI_GAME_SLUG_SET = new Set<string>(MINI_GAME_SLUGS);

export function isMiniGameSlug(value: string): value is MiniGameSlug {
  return MINI_GAME_SLUG_SET.has(value);
}
