// The `test` every spec imports: Vitest's test with our fixtures (ADR-0005, conventions S2 and R1).
// World tiers so far: `flat` only; reset levels: `arena` only. `cached` / `fresh` tiers and the `world` / `fresh`
// reset levels come with the world cache (test framework plan, step 3).
import { test as base } from "vitest";
import { Arena, plotOrigin, prepareArena } from "./arena.ts";
import { checkProtocol } from "./check-protocol.ts";
import { DEFAULT_PORT, GameClient } from "./client.ts";
import { ensureRunWorld } from "./helpers/world.ts";

export type WorldTier = "flat";
export type ResetLevel = "arena";

/** Port of the running game; `npm run e2e` sets it. */
export function e2ePort(): number {
  return Number(process.env.LK_E2E_PORT ?? DEFAULT_PORT);
}

/** Arenas handed out in this spec file so far; picks the plot of the next one. */
let arenaCount = 0;

export const test = base
  .extend("mc", { scope: "file" }, async ({}, { onCleanup }) => {
    const mc = await GameClient.connect(e2ePort());
    onCleanup(() => mc.close());
    checkProtocol(await mc.call("hello", {}));
    return mc;
  })
  // Convention S2: declared at the top of each spec file with test.override({ world, reset }).
  .extend("world", "flat" as WorldTier)
  .extend("reset", "arena" as ResetLevel)
  /** A cleared plot on this run's test world, with the player reset and standing in its middle. */
  .extend("arena", async ({ mc, world, reset, task }): Promise<Arena> => {
    if (world !== "flat" || reset !== "arena") throw new Error(`world tier '${world}' / reset '${reset}' is not implemented yet; use flat + arena.`);
    await ensureRunWorld(mc);
    return prepareArena(mc, plotOrigin(task.file.name, arenaCount++));
  });

export { describe, expect } from "vitest";
