// Session helpers: what the driver reports about itself and the client.
import type { GameClient } from "../client.ts";
import type { HelloResult, StateResult } from "../protocol.ts";

export function driverInfo(mc: GameClient): Promise<HelloResult> {
  return mc.call("hello", {});
}

export function clientState(mc: GameClient): Promise<StateResult> {
  return mc.call("state", {});
}
