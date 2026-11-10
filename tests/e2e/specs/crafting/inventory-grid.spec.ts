// Crafting in the 2 × 2 grid of the player's inventory screen, opened with the inventory key.
import { act, arrange, assertThat } from "../../harness/aaa.ts";
import { give } from "../../harness/helpers/commands.ts";
import { clickSlot, closeContainer, inventoryCount, readContainer } from "../../harness/helpers/inventory.ts";
import { pressKey } from "../../harness/helpers/player.ts";
import { waitFor } from "../../harness/helpers/wait.ts";
import { expect, test } from "../../harness/test.ts";

test.override({ world: "flat", reset: "arena" });

/** Player inventory menu slots. */
const RESULT = 0;
const GRID_TOP_LEFT = 1;
const HOTBAR_1 = 36;

// The arena fixture is requested for its reset (empty inventory, hotbar slot 0 free) even though no coordinate is used.
test("oak log crafts into four planks in the inventory grid @quick", async ({ mc, arena: _arena }) => {
  await arrange(() => give(mc, "minecraft:oak_log", 1));

  await act(async () => {
    await pressKey(mc, "key.inventory");
    await waitFor(mc, async () => (await readContainer(mc)).screen?.endsWith("InventoryScreen"), { ticks: 10, what: "the inventory screen" });
    await clickSlot(mc, HOTBAR_1);
    await clickSlot(mc, GRID_TOP_LEFT);
    await waitFor(mc, async () => (await readContainer(mc)).slots[RESULT].item === "minecraft:oak_planks", { ticks: 10, what: "planks in the result slot" });
    await clickSlot(mc, RESULT, { type: "quick_move" });
    await closeContainer(mc);
  });

  await assertThat(async () => {
    await waitFor(mc, async () => (await inventoryCount(mc, "minecraft:oak_planks")) === 4, { ticks: 10, what: "four planks in the inventory" });
    expect(await inventoryCount(mc, "minecraft:oak_log")).toBe(0);
  });
});
