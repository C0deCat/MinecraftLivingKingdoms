// World helpers: the test world of this run and save → quit → reopen.
import type { GameClient } from "../client.ts";
import { run } from "./commands.ts";

/** World loading can take far longer than one op's default guard. */
const WORLD_OP_TIMEOUT_MS = 300_000;

/** One flat test world per `npm run e2e` run; the launcher sets LK_E2E_RUN_ID. */
export function runWorldName(): string {
  return `e2e-flat-${process.env.LK_E2E_RUN_ID ?? "manual"}`;
}

/**
 * Makes sure this run's flat test world is open: reuses it when it already is, reopens it when it exists,
 * otherwise deletes older e2e- worlds and creates it (world tier `flat`, D-23).
 */
export async function ensureRunWorld(mc: GameClient): Promise<string> {
  const name = runWorldName();
  const state = await mc.call("state", {});
  if (state.inWorld && state.world === name) return name;
  if (state.inWorld) await mc.call("world.leave", {}, { timeoutMs: WORLD_OP_TIMEOUT_MS });
  const { names } = await mc.call("world.list", {});
  if (names.includes(name)) {
    await mc.call("world.open", { name }, { timeoutMs: WORLD_OP_TIMEOUT_MS });
    return name;
  }
  for (const old of names.filter((n) => n.startsWith("e2e-"))) await mc.call("world.delete", { name: old });
  await mc.call("world.create", { name, gamemode: "survival", difficulty: "normal" }, { timeoutMs: WORLD_OP_TIMEOUT_MS });
  await run(mc, "time set noon");
  await run(mc, "weather clear");
  return name;
}

/** Saves and quits to the title screen, then opens the same save again (the "survives a reload" bar). */
export async function reloadWorld(mc: GameClient): Promise<void> {
  const state = await mc.call("state", {});
  if (!state.world) throw new Error("reloadWorld: no world is open.");
  await mc.call("world.leave", {}, { timeoutMs: WORLD_OP_TIMEOUT_MS });
  await mc.call("world.open", { name: state.world }, { timeoutMs: WORLD_OP_TIMEOUT_MS });
}
