// Player actions for act() (convention A3): the controller path by default, the key path for key tests.
import type { GameClient } from "../client.ts";
import type { BlockPos, Face, PlayerState, Vec3 } from "../protocol.ts";

/** The server-side player. */
export function playerState(mc: GameClient): Promise<PlayerState> {
  return mc.call("player.state", {});
}

/** Walks in a straight line to a point (no pathfinding). */
export function walkTo(mc: GameClient, target: Vec3, options: { tolerance?: number; ticks?: number } = {}): Promise<Vec3> {
  return mc.call("player.walkTo", { target, ...options });
}

export async function breakBlock(mc: GameClient, pos: BlockPos, ticks = 200): Promise<void> {
  await mc.call("player.breakBlock", { pos, ticks });
}

/** Right-clicks a face of a block with the held item. */
export async function useItemOn(mc: GameClient, pos: BlockPos, face: Face = "up"): Promise<string> {
  return (await mc.call("player.useItemOn", { pos, face })).result;
}

export async function attack(mc: GameClient, entityId: number): Promise<void> {
  await mc.call("player.attack", { entity: entityId });
}

export async function selectSlot(mc: GameClient, slot: number): Promise<void> {
  await mc.call("player.selectSlot", { slot });
}

/** Holds a key mapping (e.g. key.forward) for a number of client ticks. */
export async function holdKey(mc: GameClient, mapping: string, ticks: number): Promise<void> {
  await mc.call("input.hold", { mapping, ticks });
}

/** Clicks a key mapping once (e.g. key.inventory). */
export async function pressKey(mc: GameClient, mapping: string): Promise<void> {
  await mc.call("input.press", { mapping });
}

/** Turns the camera: yaw 0 south (+Z), 90 west, 180 north, 270 east (+X). */
export async function look(mc: GameClient, yaw: number, pitch = 0): Promise<void> {
  await mc.call("input.look", { yaw, pitch });
}
