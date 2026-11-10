// Placing a block by right-clicking the top face of the floor with a block in hand.
import { act, arrange, assertThat } from "../../harness/aaa.ts";
import { give } from "../../harness/helpers/commands.ts";
import { inventoryCount } from "../../harness/helpers/inventory.ts";
import { useItemOn } from "../../harness/helpers/player.ts";
import { blockAt } from "../../harness/helpers/query.ts";
import { waitFor } from "../../harness/helpers/wait.ts";
import { expect, test } from "../../harness/test.ts";

test.override({ world: "flat", reset: "arena" });

test("player places a dirt block on the ground @quick", async ({ mc, arena }) => {
  await arrange(() => give(mc, "minecraft:dirt", 4));

  await act(() => useItemOn(mc, arena.at(8, -1, 10), "up"));

  await assertThat(async () => {
    await waitFor(mc, async () => (await blockAt(mc, arena.at(8, 0, 10))) === "minecraft:dirt", { ticks: 10, what: "dirt on the floor" });
    expect(await inventoryCount(mc, "minecraft:dirt")).toBe(3);
  });
});
