// Save → quit → reopen keeps the world (the "state survives a world reload" bar).
import { act, arrange, assertThat } from "../../harness/aaa.ts";
import { setBlock } from "../../harness/helpers/commands.ts";
import { blockAt } from "../../harness/helpers/query.ts";
import { clientState } from "../../harness/helpers/session.ts";
import { reloadWorld } from "../../harness/helpers/world.ts";
import { expect, test } from "../../harness/test.ts";

test.override({ world: "flat", reset: "arena" });

test("a block survives save and reload", async ({ mc, arena }) => {
  const pos = arena.at(8, 0, 10);
  const world = await arrange(async () => {
    await setBlock(mc, pos, "minecraft:gold_block");
    return (await clientState(mc)).world;
  });

  await act(() => reloadWorld(mc));

  await assertThat(async () => {
    const client = await clientState(mc);
    expect(client.inWorld).toBe(true);
    expect(client.world).toBe(world);
    expect(await blockAt(mc, pos)).toBe("minecraft:gold_block");
  });
});
