// Session: the driver answers and runs inside the pinned game. Needs no world, so it declares no world tier.
import { expect, test } from "../../harness/test.ts";
import { clientState, driverInfo } from "../../harness/helpers/session.ts";

test("driver reports the pinned Minecraft and NeoForge versions @quick", async ({ mc }) => {
  const info = await driverInfo(mc);
  expect(info.minecraft).toBe("1.21.1");
  expect(info.neoforge).toBe("21.1.265");
  expect(Object.keys(info.ownJars).some((jar) => jar.startsWith("livingkingdoms_testkit-"))).toBe(true);
});

test("client state is readable on any screen @quick", async ({ mc }) => {
  const state = await clientState(mc);
  expect(typeof state.inWorld).toBe("boolean");
  expect(state.paused).toBe(false);
});
