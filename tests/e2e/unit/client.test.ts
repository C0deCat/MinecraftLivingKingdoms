// Unit tests of the harness connection and protocol check against a fake driver (no game needed).
import { afterEach, describe, expect, test } from "vitest";
import { checkProtocol } from "../harness/check-protocol.ts";
import { GameClient, OpFailure } from "../harness/client.ts";
import { FakeBridge, FakeOpError, fakeHello } from "./fake-bridge.ts";

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

describe("connection", () => {
  test("a typed call returns the driver's result", async () => {
    const mc = await start({ hello: () => fakeHello() });
    expect((await mc.call("hello", {})).minecraft).toBe("1.21.1");
  });

  test("concurrent calls are matched to their responses by id", async () => {
    const mc = await start({
      state: async () => {
        await new Promise((r) => setTimeout(r, 30));
        return { screen: null, inWorld: false, tick: null, dimension: null, paused: false };
      },
      ping: () => ({}),
    });
    const [state, ping] = await Promise.all([mc.call("state", {}), mc.call("ping", {})]);
    expect(state.inWorld).toBe(false);
    expect(ping).toEqual({});
  });

  test("an ok:false answer becomes an OpFailure with the hint", async () => {
    const mc = await start({ state: () => { throw new FakeOpError("not_in_world", "No world is loaded.", "Declare a world tier."); } });
    const error = await mc.call("state", {}).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(OpFailure);
    expect((error as OpFailure).code).toBe("not_in_world");
    expect((error as OpFailure).message).toContain("Hint: Declare a world tier.");
  });

  test("an op without an answer fails after its timeout", async () => {
    const mc = await start({ ping: () => new Promise(() => {}) });
    await expect(mc.call("ping", {}, { timeoutMs: 50 })).rejects.toThrow(/no answer within 50 ms/);
  });

  test("connecting to a closed port explains how to start the game", async () => {
    bridge = new FakeBridge({});
    const port = await bridge.listen();
    await bridge.close();
    bridge = undefined;
    await expect(GameClient.connect(port)).rejects.toThrow(/npm run e2e/);
  });
});

describe("protocol check", () => {
  test("a driver with the same ops and version passes", () => {
    expect(() => checkProtocol(fakeHello())).not.toThrow();
  });

  test("missing and undeclared ops are both listed", () => {
    const hello = fakeHello({ ops: ["hello", "ping", "state", "teleport"] });
    expect(() => checkProtocol(hello)).toThrow(/not implemented in testkit: quit[\s\S]*not declared in protocol.ts: teleport/);
  });

  test("a different protocol version fails", () => {
    expect(() => checkProtocol(fakeHello({ protocol: 99 }))).toThrow(/protocol version: driver 99/);
  });
});
