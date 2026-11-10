// Command helpers for arrange() (convention A1): cheats through /commands on the server thread.
import { OpFailure, type GameClient } from "../client.ts";
import type { BlockPos, Vec3 } from "../protocol.ts";
import { readContainer } from "./inventory.ts";
import { waitFor, waitTicks } from "./wait.ts";

/** Runs a command as the player with operator rights. `allowFail` ignores a failed command (e.g. kill with no target). */
export async function run(mc: GameClient, cmd: string, options: { allowFail?: boolean; as?: "player" | "server" } = {}): Promise<number> {
  try {
    return (await mc.call("command.run", { cmd, as: options.as ?? "player" })).result;
  } catch (error) {
    if (options.allowFail && error instanceof OpFailure && error.code === "command_failed") return 0;
    throw error;
  }
}

export function blockArg(pos: BlockPos): string {
  return `${pos.x} ${pos.y} ${pos.z}`;
}

/**
 * Gives items and waits until the client has them too: acting before the client sees the item (breaking with a
 * pickaxe it does not hold yet, clicking an empty slot) makes the server reject the action.
 */
export async function give(mc: GameClient, item: string, count = 1): Promise<number> {
  const menu = await readContainer(mc);
  const before = clientCount(menu.slots, item);
  const result = await run(mc, `give @s ${item} ${count}`);
  if (menu.type === "minecraft:inventory") {
    await waitFor(mc, async () => clientCount((await readContainer(mc)).slots, item) >= before + count, {
      ticks: 20,
      what: `the client to receive ${count} ${item}`,
    });
  }
  // The client can see the item before the server player has ticked with it; the held item's attribute modifiers
  // (attack damage, mining) only apply on that tick. Without it a sword hits like a fist and breaking is rejected.
  await waitTicks(mc, 1);
  return result;
}

function clientCount(slots: { item: string; count: number }[], item: string): number {
  return slots.filter((s) => s.item === item).reduce((sum, s) => sum + s.count, 0);
}

export function setBlock(mc: GameClient, pos: BlockPos, block: string): Promise<number> {
  return run(mc, `setblock ${blockArg(pos)} ${block}`);
}

export function fill(mc: GameClient, from: BlockPos, to: BlockPos, block: string): Promise<number> {
  return run(mc, `fill ${blockArg(from)} ${blockArg(to)} ${block}`);
}

/** Summons an entity at the centre of a block; `nbt` is the SNBT compound, e.g. {NoAI:1b}. */
export function summon(mc: GameClient, entity: string, pos: BlockPos, nbt = ""): Promise<number> {
  return run(mc, `summon ${entity} ${pos.x + 0.5} ${pos.y} ${pos.z + 0.5} ${nbt}`.trim());
}

/** Teleports the player; yaw 0 faces south (+Z). */
export function tp(mc: GameClient, to: Vec3, yaw = 0, pitch = 0): Promise<number> {
  return run(mc, `tp @s ${to.x} ${to.y} ${to.z} ${yaw} ${pitch}`);
}
