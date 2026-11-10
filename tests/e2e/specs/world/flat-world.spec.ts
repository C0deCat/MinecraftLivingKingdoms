// The run's flat test world (D-23) opens and the arena puts the player on its floor.
import { assertThat } from "../../harness/aaa.ts";
import { blockAt } from "../../harness/helpers/query.ts";
import { playerState } from "../../harness/helpers/player.ts";
import { clientState } from "../../harness/helpers/session.ts";
import { expect, test } from "../../harness/test.ts";

test.override({ world: "flat", reset: "arena" });

test("flat test world opens with the player on the ground in survival @quick", async ({ mc, arena }) => {
  await assertThat(async () => {
    const client = await clientState(mc);
    expect(client.inWorld).toBe(true);
    expect(client.screen).toBeNull();
    expect(client.world).toMatch(/^e2e-flat-/);
    const player = await playerState(mc);
    expect(player.dimension).toBe("minecraft:overworld");
    expect(player.gamemode).toBe("survival");
    expect(player.onGround).toBe(true);
    expect(player.pos.y).toBe(arena.origin.y);
    expect(await blockAt(mc, arena.at(8, -1, 8))).toBe("minecraft:grass_block");
  });
});
