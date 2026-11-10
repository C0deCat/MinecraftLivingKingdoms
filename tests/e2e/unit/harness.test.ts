// Unit tests of the harness helpers (waits, arenas, AAA blocks, commands) against a fake driver (no game needed).
import { afterEach, describe, expect, test } from "vitest";
import { act, arrange } from "../harness/aaa.ts";
import { Arena, FLOOR_Y, plotOrigin } from "../harness/arena.ts";
import { GameClient } from "../harness/client.ts";
import { run } from "../harness/helpers/commands.ts";
import { waitFor } from "../harness/helpers/wait.ts";
import { FakeBridge, FakeOpError } from "./fake-bridge.ts";

let bridge: FakeBridge | undefined;
let client: GameClient | undefined;

afterEach(async () => {
  client?.close();
  await bridge?.close();
  client = undefined;
  bridge = undefined;
});

async function start(ops: ConstructorParameters<typeof FakeBridge>[0]): Promise<GameClient> {
  bridge = new FakeBridge(ops);
  client = await GameClient.connect(await bridge.listen());
  return client;
}

describe("waitFor", () => {
  test("returns the first truthy value and waits one tick per check", async () => {
    let ticks = 0;
    const mc = await start({ "wait.ticks": (args) => ({ serverTicks: (ticks += Number(args.n)) }) });
    let checks = 0;
    const value = await waitFor(mc, () => (++checks === 3 ? "ready" : undefined), { ticks: 10, what: "ready" });
    expect(value).toBe("ready");
    expect(ticks).toBe(2);
  });

  test("fails after the tick budget with what it waited for", async () => {
    const mc = await start({ "wait.ticks": () => ({ serverTicks: 0 }) });
    await expect(waitFor(mc, () => false, { ticks: 3, what: "never" })).rejects.toThrow(/timed out after 3 ticks: never/);
  });
});

describe("arena", () => {
  test("coordinates are relative to the corner at standing height", () => {
    const arena = new Arena({ x: 8, y: FLOOR_Y + 1, z: 32 });
    expect(arena.at(1, -1, 2)).toEqual({ x: 9, y: FLOOR_Y, z: 34 });
    expect(arena.standAt(0, 0, 0)).toEqual({ x: 8.5, y: FLOOR_Y + 1, z: 32.5 });
  });

  test("a spec file's arenas rotate over the plots", () => {
    const first = plotOrigin("specs/a.spec.ts", 0);
    expect(first.y).toBe(FLOOR_Y + 1);
    expect(plotOrigin("specs/a.spec.ts", 4)).toEqual(first);
    expect(plotOrigin("specs/a.spec.ts", 1)).not.toEqual(first);
  });
});

describe("AAA blocks", () => {
  test("a failure is labelled with its block once", async () => {
    await expect(act(() => Promise.reject(new Error("boom")))).rejects.toThrow("[act] boom");
    await expect(arrange(() => act(() => Promise.reject(new Error("inner"))))).rejects.toThrow(/^\[act\] inner$/);
  });
});

describe("commands", () => {
  test("allowFail ignores a failed command and other errors still throw", async () => {
    const mc = await start({
      "command.run": (args) => {
        if (args.cmd === "kill @e[type=!minecraft:player]") throw new FakeOpError("command_failed", "No entity was found", "");
        throw new FakeOpError("no_world", "No world is open.", "");
      },
    });
    expect(await run(mc, "kill @e[type=!minecraft:player]", { allowFail: true })).toBe(0);
    await expect(run(mc, "time set noon", { allowFail: true })).rejects.toThrow(/no_world/);
  });
});
