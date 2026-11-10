// M4-A prototype (ADR-0030): an MCA Reborn villager made a resident harvests wheat for its order, delivers it to
// a chest, and goes on doing so after save → quit → reopen. The order lives in our save data, the link in an
// attachment on the entity; both must survive the reload.
import { act, arrange, assertThat } from "../../harness/aaa.ts";
import type { GameClient } from "../../harness/client.ts";
import { blockArg, run, setBlock } from "../../harness/helpers/commands.ts";
import { waitFor } from "../../harness/helpers/wait.ts";
import { reloadWorld } from "../../harness/helpers/world.ts";
import type { BlockPos } from "../../harness/protocol.ts";
import { expect, test } from "../../harness/test.ts";

test.override({ world: "flat", reset: "arena" });

const TAG = "lk_e2e_farmer";
/** One trip per crop plus the delivery, walking at villager speed; the flat world stays at noon, inside labor hours. */
const WORK_TICKS = 1200;

/** Wheat in the chest, read on the server: `data get` returns the stack count, and fails while there is no wheat. */
function wheatIn(mc: GameClient, chest: BlockPos): Promise<number> {
  return run(mc, `data get block ${blockArg(chest)} Items[{id:"minecraft:wheat"}].count`, { allowFail: true });
}

async function ripen(mc: GameClient, field: BlockPos[]): Promise<void> {
  for (const pos of field) await setBlock(mc, pos, "minecraft:wheat[age=7]");
}

test("resident keeps harvesting for its order after save and reload @slow", { timeout: 300_000 }, async ({ mc, arena }) => {
  const field = [arena.at(4, 0, 4), arena.at(5, 0, 4), arena.at(6, 0, 4)];
  const chest = arena.at(10, 0, 10);

  await arrange(async () => {
    for (const pos of field) await setBlock(mc, { ...pos, y: pos.y - 1 }, "minecraft:farmland");
    await ripen(mc, field);
    await setBlock(mc, chest, "minecraft:chest");
    await run(mc, `lksettle spawn ${blockArg(arena.at(12, 0, 4))}`);
    await run(mc, `tag @e[type=!minecraft:player,limit=1,sort=nearest] add ${TAG}`);
    await run(mc, `lksettle assign @e[tag=${TAG},limit=1] harvest ${blockArg(field[1])} ${blockArg(chest)} 3`);
  });

  await act(async () => {
    await waitFor(mc, async () => (await wheatIn(mc, chest)) === field.length, {
      ticks: WORK_TICKS,
      step: 20,
      what: "the first harvest in the chest before the reload",
    });
    await reloadWorld(mc);
    await ripen(mc, field);
  });

  await assertThat(async () => {
    await waitFor(mc, async () => (await wheatIn(mc, chest)) === 2 * field.length, {
      ticks: WORK_TICKS,
      step: 20,
      what: "the second harvest in the chest after the reload",
    });
    // `lksettle status` returns the number of resident records.
    expect(await run(mc, "lksettle status")).toBe(1);
  });
});
