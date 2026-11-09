// The `test` every spec imports: Vitest's test with our fixtures (ADR-0005, conventions S2 and R1).
// Step 2 of the test-framework plan: only the connection. World tiers, reset levels and arenas come in steps 3–5.
import { test as base } from "vitest";
import { checkProtocol } from "./check-protocol.ts";
import { DEFAULT_PORT, GameClient } from "./client.ts";

/** Port of the running game; `npm run e2e` sets it. */
export function e2ePort(): number {
  return Number(process.env.LK_E2E_PORT ?? DEFAULT_PORT);
}

export const test = base.extend("mc", { scope: "file" }, async ({}, { onCleanup }) => {
  const mc = await GameClient.connect(e2ePort());
  onCleanup(() => mc.close());
  checkProtocol(await mc.call("hello", {}));
  return mc;
});

export { describe, expect } from "vitest";
