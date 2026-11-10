// Arenas (reset level `arena`): each test gets a cleared 16 × 16 plot on the flat test world and a reset player.
// Specs use only arena-relative coordinates (convention W1).
import type { GameClient } from "./client.ts";
import { blockArg, run, tp } from "./helpers/commands.ts";
import { closeContainer } from "./helpers/inventory.ts";
import { selectSlot } from "./helpers/player.ts";
import { waitTicks } from "./helpers/wait.ts";
import type { BlockPos, Vec3 } from "./protocol.ts";

/** Surface of the flat test preset: grass at y −61, the player stands at y −60 (D-22 preset layers). */
export const FLOOR_Y = -61;
export const SIZE = 16;
/** Plots sit close together so their chunks stay loaded; a reused plot is cleared anyway. */
const PLOTS: readonly [number, number][] = [[8, 8], [32, 8], [8, 32], [32, 32]];

export class Arena {
  /** Block position of the plot's corner at standing height (arena y 0 is the air layer above the floor). */
  readonly origin: BlockPos;

  constructor(origin: BlockPos) {
    this.origin = origin;
  }

  /** A block position relative to the arena corner; y 0 is standing height, y −1 the floor. */
  at(x: number, y: number, z: number): BlockPos {
    return { x: this.origin.x + x, y: this.origin.y + y, z: this.origin.z + z };
  }

  /** The centre of a block's bottom face, where a player or mob stands. */
  standAt(x: number, y: number, z: number): Vec3 {
    const pos = this.at(x, y, z);
    return { x: pos.x + 0.5, y: pos.y, z: pos.z + 0.5 };
  }

  /** Box around the plot for entity queries (a margin of two blocks, eight above standing height). */
  bounds(): { from: BlockPos; to: BlockPos } {
    return { from: this.at(-2, -2, -2), to: this.at(SIZE + 1, 8, SIZE + 1) };
  }
}

/** Plot for the n-th arena of a spec file; spreads files over the plots so neighbours rarely share one. */
export function plotOrigin(file: string, index: number): BlockPos {
  let hash = 0;
  for (const ch of file) hash = (hash * 31 + ch.charCodeAt(0)) | 0;
  const [x, z] = PLOTS[(Math.abs(hash) + index) % PLOTS.length];
  return { x, y: FLOOR_Y + 1, z };
}

/**
 * Clears the plot and resets the player (convention R1): air above the floor, a fresh grass floor, no entities but
 * the player, empty inventory with hotbar slot 0 selected, no effects, survival, full health and food, standing in
 * the middle (arena 8, 0, 8) facing south (+Z).
 */
export async function prepareArena(mc: GameClient, origin: BlockPos): Promise<Arena> {
  const arena = new Arena(origin);
  const { from, to } = arena.bounds();
  await closeContainer(mc);
  // Move the player first: standing on the previous plot, it would pick up that test's drops while being reset.
  await tp(mc, arena.standAt(SIZE / 2, 0, SIZE / 2), 0, 0);
  const dx = to.x - from.x;
  const dz = to.z - from.z;
  const box = `x=${from.x},y=${from.y},z=${from.z},dx=${dx},dy=${to.y - from.y},dz=${dz}`;
  await run(mc, `kill @e[type=!minecraft:player,${box}]`, { allowFail: true });
  // Killed mobs drop loot and experience as they die; clear those too (dead mobs vanish after 20 ticks on their own).
  await run(mc, `kill @e[type=minecraft:item,${box}]`, { allowFail: true });
  await run(mc, `kill @e[type=minecraft:experience_orb,${box}]`, { allowFail: true });
  // fill fails with "No blocks were filled" when nothing changes, i.e. on a plot that is already clean.
  await run(mc, `fill ${blockArg({ ...from, y: origin.y })} ${blockArg(to)} minecraft:air`, { allowFail: true });
  await run(mc, `fill ${blockArg({ ...from, y: FLOOR_Y })} ${blockArg({ ...to, y: FLOOR_Y })} minecraft:grass_block`, { allowFail: true });
  // The inventory is cleared last, after every drop the player could have picked up is gone.
  await run(mc, "clear @s", { allowFail: true });
  await run(mc, "effect clear @s", { allowFail: true });
  await run(mc, "gamemode survival @s", { allowFail: true });
  await run(mc, "effect give @s minecraft:instant_health 1 10 true");
  await run(mc, "effect give @s minecraft:saturation 1 20 true");
  await selectSlot(mc, 0);
  // Let the client receive the teleport, the cleared blocks and the empty inventory before the test acts.
  await waitTicks(mc, 5);
  return arena;
}
