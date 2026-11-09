// Convention F5: driver and harness must agree on the protocol before any test runs.
import { OP_DOCS, PROTOCOL_VERSION, type HelloResult } from "./protocol.ts";

/** Throws with a fix instruction when the running driver does not match protocol.ts. */
export function checkProtocol(hello: HelloResult): void {
  const problems: string[] = [];
  if (hello.protocol !== PROTOCOL_VERSION) {
    problems.push(`protocol version: driver ${hello.protocol}, harness ${PROTOCOL_VERSION}`);
  }
  const declared = new Set(Object.keys(OP_DOCS));
  const implemented = new Set(hello.ops);
  const missing = [...declared].filter((op) => !implemented.has(op));
  const undeclared = [...implemented].filter((op) => !declared.has(op));
  if (missing.length) problems.push(`declared in protocol.ts but not implemented in testkit: ${missing.join(", ")}`);
  if (undeclared.length) problems.push(`implemented in testkit but not declared in protocol.ts: ${undeclared.join(", ")}`);
  if (problems.length) {
    throw new Error(
      `The testkit driver does not match tests/e2e/harness/protocol.ts:\n  - ${problems.join("\n  - ")}\n` +
        "Fix: rebuild the testkit (`npm run e2e` rebuilds and restarts the client when its JAR changed), " +
        "or align protocol.ts with the *Ops classes in mods-src/livingkingdoms-testkit.",
    );
  }
}
