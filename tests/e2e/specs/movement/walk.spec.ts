// Walking through the controller path: the player turns and holds forward until it reaches a point.
import { act, arrange, assertThat } from "../../harness/aaa.ts";
import { tp } from "../../harness/helpers/commands.ts";
import { playerState, walkTo } from "../../harness/helpers/player.ts";
import { waitFor, waitTicks } from "../../harness/helpers/wait.ts";
import { expect, test } from "../../harness/test.ts";

test.override({ world: "flat", reset: "arena" });

test("player walks ten blocks east @quick", async ({ mc, arena }) => {
  const start = arena.standAt(3, 0, 8);
  const target = arena.standAt(13, 0, 8);
  await arrange(async () => {
    await tp(mc, start);
    await waitTicks(mc, 3);
  });

  await act(() => walkTo(mc, target, { ticks: 200 }));

  await assertThat(async () => {
    const player = await waitFor(mc, async () => {
      const p = await playerState(mc);
      return Math.hypot(p.pos.x - target.x, p.pos.z - target.z) < 1 ? p : undefined;
    }, { ticks: 20, what: "the server player to stand within a block of the target" });
    expect(player.pos.x - start.x).toBeGreaterThan(9);
    expect(player.pos.y).toBe(arena.origin.y);
  });
});
