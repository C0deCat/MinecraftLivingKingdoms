// Waits in game ticks (convention W2): never sleep or setTimeout in specs.
import type { GameClient } from "../client.ts";

/** Waits n integrated server ticks. */
export async function waitTicks(mc: GameClient, n: number): Promise<void> {
  await mc.call("wait.ticks", { n });
}

/**
 * Checks `cond` once per `step` server ticks until it returns a truthy value, and returns that value.
 * Fails after `ticks` ticks with `what` in the message.
 */
export async function waitFor<T>(
  mc: GameClient,
  cond: () => Promise<T | undefined | null | false> | T | undefined | null | false,
  options: { ticks: number; what: string; step?: number },
): Promise<T> {
  const step = Math.max(1, options.step ?? 1);
  let last: unknown;
  for (let waited = 0; ; waited += step) {
    const value = await cond();
    if (value) return value;
    last = value;
    if (waited >= options.ticks) break;
    await waitTicks(mc, step);
  }
  throw new Error(`waitFor timed out after ${options.ticks} ticks: ${options.what} (last value: ${JSON.stringify(last)})`);
}
