// e2e specs against the running game. Started by `npm run e2e` (tools/pack/src/e2e.ts), which owns the game lifecycle.
import { defineConfig } from "vitest/config";

const runDir = process.env.LK_E2E_RUN_DIR;

export default defineConfig({
  test: {
    include: ["specs/**/*.spec.ts"],
    // One game client serves every file, so files run one at a time.
    fileParallelism: false,
    // Wall-clock hang guards only (W3); in-game waits are counted in ticks.
    testTimeout: 120_000,
    hookTimeout: 600_000,
    // F3: no retries in normal runs.
    retry: 0,
    reporters: runDir ? ["default", ["json", { outputFile: `${runDir}/report.json` }]] : ["default"],
  },
});
