// Breaking a block in survival through the client game mode, with the right tool.
import { act, arrange, assertThat } from "../../harness/aaa.ts";
import { give, setBlock } from "../../harness/helpers/commands.ts";
import { inventoryCount } from "../../harness/helpers/inventory.ts";
import { breakBlock } from "../../harness/helpers/player.ts";
import { blockAt, entitiesIn } from "../../harness/helpers/query.ts";
import { waitFor } from "../../harness/helpers/wait.ts";
import { test } from "../../harness/test.ts";

test.override({ world: "flat", reset: "arena" });

test("player breaks stone with a pickaxe and it drops cobblestone @quick", async ({ mc, arena }) => {
  const stone = arena.at(8, 0, 10);
  await arrange(async () => {
    await setBlock(mc, stone, "minecraft:stone");
    await give(mc, "minecraft:iron_pickaxe");
  });

  await act(() => breakBlock(mc, stone));

  await assertThat(async () => {
    // breakBlock returns when the client sees air; the server confirms a tick or two later.
    await waitFor(mc, async () => (await blockAt(mc, stone)) === "minecraft:air", { ticks: 10, what: "air where the stone was" });
    const { from, to } = arena.bounds();
    // The drop either lies on the ground or has already been picked up.
    await waitFor(mc, async () => {
      if ((await inventoryCount(mc, "minecraft:cobblestone")) > 0) return true;
      const items = await entitiesIn(mc, from, to, "minecraft:item");
      return items.some((e) => e.item?.item === "minecraft:cobblestone");
    }, { ticks: 20, what: "a cobblestone drop" });
  });
});
