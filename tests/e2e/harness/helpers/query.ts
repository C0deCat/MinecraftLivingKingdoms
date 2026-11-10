// Server truth for assertThat() (convention A1).
import type { GameClient } from "../client.ts";
import type { BlockPos, EntityInfo } from "../protocol.ts";

/** Block id at a position, e.g. minecraft:stone. */
export async function blockAt(mc: GameClient, pos: BlockPos): Promise<string> {
  return (await mc.call("query.block", { pos })).block;
}

/**
 * Entities in a box, optionally of one type (e.g. minecraft:item). A killed mob stays in the world for its 20-tick
 * death animation, so dead ones are left out unless `includeDead` is set.
 */
export async function entitiesIn(
  mc: GameClient,
  from: BlockPos,
  to: BlockPos,
  type?: string,
  options: { includeDead?: boolean } = {},
): Promise<EntityInfo[]> {
  const { entities } = await mc.call("query.entities", type ? { from, to, type } : { from, to });
  return options.includeDead ? entities : entities.filter((e) => e.alive);
}
