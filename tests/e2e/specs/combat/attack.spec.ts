// Melee: one hit with a sword kills a mob with one health point.
import { act, arrange, assertThat } from "../../harness/aaa.ts";
import { give, summon } from "../../harness/helpers/commands.ts";
import { attack } from "../../harness/helpers/player.ts";
import { entitiesIn } from "../../harness/helpers/query.ts";
import { waitFor } from "../../harness/helpers/wait.ts";
import { expect, test } from "../../harness/test.ts";

test.override({ world: "flat", reset: "arena" });

// A husk, not a zombie: the arena is in daylight and a zombie would burn to death on its own.
const MOB = "minecraft:husk";

test("player kills a husk with a sword @quick", async ({ mc, arena }) => {
  const { from, to } = arena.bounds();
  const target = await arrange(async () => {
    await give(mc, "minecraft:iron_sword");
    await summon(mc, MOB, arena.at(8, 0, 10), "{NoAI:1b,Health:1f,PersistenceRequired:1b}");
    const mobs = await waitFor(mc, async () => {
      const found = await entitiesIn(mc, from, to, MOB);
      return found.length === 1 ? found : undefined;
    }, { ticks: 10, what: "the husk to appear" });
    return mobs[0];
  });
  expect(target.alive).toBe(true);

  await act(() => attack(mc, target.id));

  await assertThat(() =>
    waitFor(mc, async () => (await entitiesIn(mc, from, to, MOB)).length === 0, { ticks: 30, what: "the husk to die" }),
  );
});
