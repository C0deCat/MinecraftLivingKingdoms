---
status: stable
last_reviewed: 2026-11-09
---

# ADR-0005: Test framework: GameTest plus our e2e driver

**Status:** Accepted · **Date:** 2026-11-09

## Context

Tests will run often, both as regression checks and for new features, and agents will write most of them. Until now we have `npm run smoke` (launch to the title screen), the test server with `/lkworld` commands and `validate-world`. None of them can drive a player: enter a world created with given settings, take an item, use it, check the result.

What we need:
- mechanics checks that run in seconds;
- player journeys through the real client: screens, input, save and reload;
- a format that agents write well and that reads like an ordinary test suite.

Facts checked on 2026-11-09 (documentation review; nothing installed yet):
- **Vanilla GameTest** ships with Minecraft and NeoForge 1.21.1. Tests are annotated Java methods (`@GameTest`, `@GameTestHolder`) that run on a logical server in a void test world. There is no client, so no screens and no real input. In 1.21.5 GameTest changed to data-driven test instances, so examples for newer versions do not apply.
- **Fabric Client GameTest** has the right design (a client test context, world builder, input, screenshots), but it is Fabric-only and targets newer Minecraft versions.
- **MineClient Bridge** (an HTTP control API for the NeoForge 1.21.1 client) is young and unproven, and adding it would mean a third-party mod in our test setup.
- **Minescript** (Python inside the client) is a scripting tool, not a test framework. **Mineflayer** bots cannot join modded servers.
- Game-testing frameworks elsewhere (Unity Test Framework, AltTester, Unreal Automation Spec, GdUnit4, Bedrock GameTest, Playwright) share a few conventions:
  - a split between logic tests and tests with a running game;
  - dedicated small test scenes;
  - condition waits with timeouts instead of sleeps;
  - stable locators instead of coordinates;
  - action-level input;
  - screen objects;
  - automatic failure artifacts.

## Decision

Two layers, chosen per test by the lowest layer that can catch the bug:

1. **GameTest** for server-side mechanics, in our dev-only mod `livingkingdoms_testkit`. Run on the test server with the pack's server mods (`npm run gametest`).
2. **e2e tests through our own driver**:
   - the same testkit mod runs a localhost socket in the client and executes protocol ops (world, input, player, containers, UI, commands, queries, screenshots);
   - tests are TypeScript in the Playwright style, run by **Vitest** (exact pin, dev dependency of `tests/e2e/` only);
   - `npm run e2e` owns the game lifecycle: start or attach, world cache.

The conventions, the command set and the speed tactics are in the [test framework plan](../exec-plans/active/2026-11-09-test-framework.md), until the `e2e-test` skill and `tests/e2e/AGENTS.md` exist. A test-only flat world preset and the exemption of test worlds from full pre-generation are D-22.

## Options considered

- **JSON scenarios executed by the mod.** Cheapest to start with. But loops, shared steps, parameters and timed waits turn it into a small home-made language, and every run would relaunch the game. Rejected in favour of TypeScript; JSON stays as the wire format.
- **TypeScript tests on `node:test`.** No dependencies and the same native TypeScript as `tools/`. Fixtures, polling waits and readable failures would have to be written and maintained by us, and global setup and test tags are still experimental in Node 24. Rejected: Vitest gives Playwright-style fixtures (`test.extend` with file scope), richer assertions and reporters, and its cost is one pinned dev dependency outside the root tooling.
- **MineClient Bridge.** Would save writing the bridge, but it is a young third-party mod that needs a code review and approval, and its API is not shaped for tests. Rejected for now; it can be reconsidered with new facts.
- **Fabric Client GameTest.** Not available on NeoForge 1.21.1. Its design informs ours.
- **Only GameTest.** Cannot test screens, real input or world reload, which are the riskiest parts of a modpack.

## Consequences

- A new mod project `mods-src/livingkingdoms-testkit/`. It is never part of `modpack/`; it is installed only into the e2e Prism instance and the test server.
- A second Prism instance `living-kingdoms-e2e` with test settings, so the play instance and the user's saves stay untouched.
- The repository gets its first npm dependency (Vitest), confined to `tests/e2e/` with its own lock file.
- The protocol op list has one source (`tests/e2e/harness/protocol.ts`). The mod checks it at `hello`, and a generated reference note is checked by `docs:check`.
- e2e runs need the Windows host with a signed-in Prism account. A cloud session can build and type-check, but cannot reach runtime passed.
