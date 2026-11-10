# AGENTS.md — livingkingdoms-testkit

Dev-only NeoForge 1.21.1 mod `livingkingdoms_testkit`: the e2e driver for `tests/e2e/`, the flat test preset (D-23) and our GameTests. Design: [ADR-0005](../../docs/decisions/ADR-0005-test-framework.md), plan: [test framework](../../docs/exec-plans/active/2026-11-09-test-framework.md). Rules from the root [AGENTS.md](../../AGENTS.md) and [mods-src/AGENTS.md](../AGENTS.md) apply.

## Rules

- Never add this mod to `modpack/`. `npm run e2e` installs it into the `living-kingdoms-e2e` Prism instance only; the play instance and the test server do not get it.
- The driver is client-only (`livingkingdoms.testkit.client`, registered with `Dist.CLIENT`). Common code (GameTests, the flat preset data) must stay server-safe.
- The socket listens on 127.0.0.1 only (default port 25598, `-Dlivingkingdoms.testkit.port`). Socket threads only parse and write; each op moves its work to the client thread (`OnClient`) or the integrated server thread.
- Every op is declared in `tests/e2e/harness/protocol.ts` and registered in an `*Ops` class here. `hello` reports the registered names; the harness refuses to run when the two lists differ. Bump `PROTOCOL_VERSION` in both places when the wire format changes.
- Errors the test author can fix are `OpError(code, message, hint)`. The hint says what to change.
- Threads: client state through `OnClient`, world state through `OnServer` (the integrated server thread); an op that spans ticks uses `TickTasks` (client ticks) or `TickTasks.serverTicks`.
- Never open a world while `state.loading` is true (the startup reload overlay): `Minecraft.doWorldLoad` then never returns. World ops refuse with `not_ready`.
- Test worlds are named `e2e-…`; `world.delete` accepts nothing else.
