// Crafting in the 3 × 3 grid of a crafting table: open it with a right click, lay out the recipe, take the result.
import { act, arrange, assertThat } from "../../harness/aaa.ts";
import { give, setBlock } from "../../harness/helpers/commands.ts";
import { clickSlot, closeContainer, inventoryCount, readContainer, waitForContainer } from "../../harness/helpers/inventory.ts";
import { useItemOn } from "../../harness/helpers/player.ts";
import { waitFor } from "../../harness/helpers/wait.ts";
import { expect, test } from "../../harness/test.ts";

test.override({ world: "flat", reset: "arena" });

/** Crafting table menu slots: 0 result, 1–9 grid row by row, 37–45 hotbar. */
const RESULT = 0;
const GRID = { topLeft: 1, topMiddle: 2, topRight: 3, centre: 5, bottomMiddle: 8 };
const HOTBAR_1 = 37;
const HOTBAR_2 = 38;
const RIGHT = 1;

test("crafting table crafts a wooden pickaxe @quick", async ({ mc, arena }) => {
  const table = arena.at(8, 0, 10);
  await arrange(async () => {
    await setBlock(mc, table, "minecraft:crafting_table");
    await give(mc, "minecraft:oak_planks", 3);
    await give(mc, "minecraft:stick", 2);
  });

  await act(async () => {
    await useItemOn(mc, table, "north");
    await waitForContainer(mc, "minecraft:crafting");
    // Pick up the stack, then one right click per grid slot puts down one item.
    await clickSlot(mc, HOTBAR_1);
    for (const slot of [GRID.topLeft, GRID.topMiddle, GRID.topRight]) await clickSlot(mc, slot, { button: RIGHT });
    await clickSlot(mc, HOTBAR_2);
    for (const slot of [GRID.centre, GRID.bottomMiddle]) await clickSlot(mc, slot, { button: RIGHT });
    await waitFor(mc, async () => (await readContainer(mc)).slots[RESULT].item === "minecraft:wooden_pickaxe", { ticks: 10, what: "a pickaxe in the result slot" });
    await clickSlot(mc, RESULT, { type: "quick_move" });
    await closeContainer(mc);
  });

  await assertThat(async () => {
    await waitFor(mc, async () => (await inventoryCount(mc, "minecraft:wooden_pickaxe")) === 1, { ticks: 10, what: "a wooden pickaxe in the inventory" });
    expect(await inventoryCount(mc, "minecraft:oak_planks")).toBe(0);
    expect(await inventoryCount(mc, "minecraft:stick")).toBe(0);
  });
});
