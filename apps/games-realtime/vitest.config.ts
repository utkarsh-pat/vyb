import { cloudflareTest } from "@cloudflare/vitest-pool-workers";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [
    cloudflareTest({
      wrangler: { configPath: "./wrangler.jsonc" },
      miniflare: {
        bindings: {
          GAME_SESSION_SECRET: "test-game-session-secret",
          SOCIAL_FANOUT_SECRET: "test-social-fanout-secret"
        }
      }
    })
  ]
});
