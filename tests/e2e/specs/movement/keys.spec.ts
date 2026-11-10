// Key path (convention A3): holding key.forward moves the player the way the camera faces.
import { act, arrange, assertThat } from "../../harness/aaa.ts";
import { tp } from "../../harness/helpers/commands.ts";
import { holdKey, look, playerState } from "../../harness/helpers/player.ts";
import { waitFor, waitTicks } from "../../harness/helpers/wait.ts";
import { expect, test } from "../../harness/test.ts";

test.override({ world: "flat", reset: "arena" });

test("forward key moves the player forward @quick", async ({ mc, arena }) => {
  const start = arena.standAt(8, 0, 2);
  await arrange(async () => {
    await tp(mc, start, 0, 0);
    await look(mc, 0, 0);
    await waitTicks(mc, 3);
  });

  // Yaw 0 faces south (+Z); 20 ticks of walking cover about four blocks.
  await act(() => holdKey(mc, "key.forward", 20));

  await assertThat(async () => {
    const player = await waitFor(mc, async () => {
      const p = await playerState(mc);
      return p.pos.z - start.z > 2 ? p : undefined;
    }, { ticks: 20, what: "the player to move more than two blocks south" });
    expect(Math.abs(player.pos.x - start.x)).toBeLessThan(0.5);
  });
});
