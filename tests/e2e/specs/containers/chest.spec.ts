// Taking items out of a chest: open it with a right click and shift-click the stack into the inventory.
import { act, arrange, assertThat } from "../../harness/aaa.ts";
import { blockArg, run } from "../../harness/helpers/commands.ts";
import { clickSlot, closeContainer, inventoryCount, waitForContainer } from "../../harness/helpers/inventory.ts";
import { useItemOn } from "../../harness/helpers/player.ts";
import { waitFor } from "../../harness/helpers/wait.ts";
import { test } from "../../harness/test.ts";

test.override({ world: "flat", reset: "arena" });

/** Single chest menu slots: 0–26 chest. */
const CHEST_FIRST = 0;

test("player takes diamonds out of a chest @quick", async ({ mc, arena }) => {
  const chest = arena.at(8, 0, 10);
  await arrange(() => run(mc, `setblock ${blockArg(chest)} minecraft:chest[facing=north]{Items:[{Slot:0b,id:"minecraft:diamond",count:3}]}`));

  await act(async () => {
    await useItemOn(mc, chest, "north");
    await waitForContainer(mc, "minecraft:generic_9x3");
    await clickSlot(mc, CHEST_FIRST, { type: "quick_move" });
    await closeContainer(mc);
  });

  await assertThat(() =>
    waitFor(mc, async () => (await inventoryCount(mc, "minecraft:diamond")) === 3, { ticks: 10, what: "three diamonds in the inventory" }),
  );
});
