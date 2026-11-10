---
status: active
last_reviewed: 2026-11-09
---

# Test framework: GameTest + our e2e driver

**Status:** Active

## Goal

Agents and the user can write and run automated tests in two layers ([ADR-0005](../../decisions/ADR-0005-test-framework.md)):
- `npm run gametest` runs our GameTests on the test server with the pack's server mods;
- `npm run e2e` drives the real client: it creates or opens a world of a chosen tier, controls the player, runs commands and asserts the world state.

Done means the two seed specs and one sample GameTest reach runtime passed on the host PC. The conventions are in the `e2e-test` skill, the op catalog is generated and checked by `docs:check`, and the measured timings are in [quality.md](../../quality.md).

## Context

- Decisions: [ADR-0005](../../decisions/ADR-0005-test-framework.md), D-22 (flat test preset as an exception to D-18; test worlds exempt from D-20).
- Taken with the user on 2026-11-09:
  - TypeScript tests in the Playwright style, not JSON scenarios;
  - Vitest pinned exactly as a dev dependency of `tests/e2e/` only;
  - the conventions below, including the strict F4;
  - work on the branch `main-e2iix5`.
- Not a milestone step: it serves every milestone from M1-C on. The quality bars it will carry are in [quality.md](../../quality.md#end-to-end-test-bars).
- Code to reuse:
  - `tools/pack/src/smoke.ts`: Prism launch, log markers, `stopGame()`;
  - `test-server.ts`: test server, `--cmd`, `--until`;
  - `instance.ts`: `buildOwnMods`, `syncMods`, `writeIni`, Prism paths;
  - `lib.ts`;
  - `mods-src/gradle/neoforge-mod.gradle` (`gameTestServer` run);
  - `livingkingdoms/world/client/CreateWorldHooks.java` (size selection in `WorldCreationUiState`).

### Architecture

```
tests/e2e/specs/<area>/<feature>.spec.ts ──Vitest──► harness: fixtures, screens/, helpers/, protocol.ts
                                                       │ newline-delimited JSON over TCP 127.0.0.1:25598
npm run e2e (tools/pack/src/e2e.ts)                    ▼
  build, sync the e2e instance, world cache,        livingkingdoms_testkit (dev-only mod)
  start or attach to the client                     ├─ bridge: socket thread → client thread / integrated server thread
                                                    ├─ ops: session, world, input, player, container, ui, command, query, test support
                                                    ├─ flat test preset (data, testkit only)
                                                    └─ GameTests (common code; run on the test server)
```

- **New mod `mods-src/livingkingdoms-testkit/`** (mod id `livingkingdoms_testkit`).
  - It is never added to `modpack/`. It is installed only into the e2e instance and the test server.
  - Client code lives in `client/` classes registered with `Dist.CLIENT`, per the mods-src rules.
  - The transport is a plain TCP socket with JSON lines, so neither side needs libraries.
  - It gets its own `AGENTS.md` + `CLAUDE.md`, and a row in the `mods-src/AGENTS.md` table.
- **Separate Prism instance `living-kingdoms-e2e`**: the same pack mods + testkit, and its own `options.txt`:
  - `pauseOnLostFocus:false` (otherwise the integrated server pauses when the window loses focus, see quality.md);
  - `lang:en_us`;
  - sounds 0, render and simulation distance 6, maxFps 60;
  - narrator, tutorial and onboarding off; shaders off.

  The play instance stays clean, and test saves never mix with the user's worlds.
- **The game lifecycle lives in our launcher, not in Vitest.**
  - `npm run e2e` starts the client or attaches to a running one. `--keep` leaves it open, and the next run attaches in seconds.
  - If the testkit or pack JAR hashes changed, it restarts the client, because mods cannot be hot-reloaded.
  - Vitest runs with `fileParallelism: false`; every file connects to the same game.

### Command set (protocol ops)

How ops run:
- Every op runs on the right thread: client state on the client thread, world state on the integrated server thread through a future.
- Each op returns `{ok, result}` or `{ok:false, error:{code, message, hint}}`. The hint tells an agent how to fix the problem.
- Game time is counted in ticks.

| Group | Ops |
| --- | --- |
| Session | `hello` (versions, mod list + JAR hashes, supported op list), `ping`, `state` (screen, in world, tick, dimension), `quit` |
| World | `world.create{tier, size, seed, name, gamemode, cheats, gamerules, viaUi?}`, `world.open{name}`, `world.leave` (save and quit to title), `world.reload` (leave + open), `world.delete`, `world.list` |
| Input (key path) | `input.press/hold/release{mapping, ticks}` for key mappings (`key.forward/back/left/right/jump/sneak/sprint/attack/use/drop/inventory/swapOffhand/hotbar.1-9`), `input.look{yaw,pitch}`, `input.lookAt{x,y,z}`, `input.scroll{delta}`, `input.text{chars}` |
| Player (controller path) | `player.walkTo{x,y,z,tolerance,ticks}` (straight line; no pathfinding at first), `player.selectSlot`, `player.useItem`, `player.useItemOn{pos,face}`, `player.attack{entity}`, `player.breakBlock{pos}` (holds attack until broken), `player.drop{all?}`, `player.swapHands`, `player.state` (pos, rot, health, food, xp, effects, gamemode, dimension, selected slot, on ground) |
| Containers | `container.read` (menu slots), `container.click{slot, button, type}` (pickup, quick move, swap, throw), `container.close` |
| UI | `ui.screen` (screen class, title, widgets: type, translation key, text, enabled, bounds), `ui.click{key, type?, index?}`, `ui.type{text}`, `ui.close`, `ui.waitFor{screen, ticks}` |
| Commands | `command.run{cmd, as: player\|server}` runs on the server thread with a capturing source and returns the result count and feedback lines. `chat.send{text}` goes through the real network path |
| Queries (truth from the integrated server) | `query.block{pos}` (id, state properties, optional block entity NBT), `query.entities{type?, area}`, `query.inventory`, `query.time`, `query.chat{since}`, `query.log{since, match}` |
| Test support | `arena.prepare{size, template?}` → origin, `arena.reset`, `wait.ticks{n}`, `screenshot{name}` |

Code above the ops (L2):
- **Helpers** in `harness/helpers/`: `give`, `tp`, `setBlock`, `fill`, `summon`, `clearInventory`, `effect`, `setTime`, `setWeather`, `equip`, `placeAndUse`, `openInventory`, `waitFor(cond, {ticks})`, `waitForBlock`.
- **Screen objects** in `harness/screens/`: `TitleScreen`, `CreateWorldScreen`, `WorldSizeScreen`, `InventoryScreen`.
- **AAA blocks** `arrange()`, `act()`, `assertThat()` mark which part of the test failed in the report.

### Layer boundary

Rule: use the lowest layer that can catch the bug.

| Layer | Checks | Does not check | Speed |
| --- | --- | --- | --- |
| GameTest (testkit, test server with the pack's server mods) | Block, item, entity and block-entity behaviour; recipes through `RecipeManager`; cross-mod mechanics (mana deduction, later NPC logic); events and data within one session | Client, real input, screens, world reload, real worldgen (its world is a void plot) | ms–s per test; whole suite in one server start |
| e2e (testkit driver, real client) | Player journeys through real input and screens (our size selector, containers, keybinds); client-server sync; save → quit → reopen (the "IDs survive a reload" bar); `@visual` specs; spark scenarios | Statistics over the whole map | s per test once the client is up |
| Worldgen tools (existing `/lkworld`, `validate-world`) | Structure census, borders, coasts, manifest | — | minutes |

`npm run gametest`:
- runs the test server plus the testkit JAR with `-Dneoforge.enabledGameTestNamespaces=livingkingdoms_testkit`;
- runs `/test runall` and waits until the summary appears in the log;
- the exact NeoForge 21.1 switches are checked against the NeoForge sources in step 7.

`./gradlew runGameTestServer` stays for tests of our code alone.

### Speed tactics

**World tiers** (option `world`):

| Tier | What | Use for | Cost (to measure) |
| --- | --- | --- | --- |
| `flat` (default) | Test-only flat preset, no structures | Mechanics of any mod that involve the player | create ~5–10 s |
| `cached` `{size, seed, radius}` | Copy of a world cache entry, optionally pre-generated with Chunky around spawn | Tests that need real terrain or nearby structures | copy + open ~10–20 s |
| `fresh` `{size, viaUi?}` | New world through our presets, optionally through the UI | World-creation tests | ~30–60 s + generation |

**World cache** in `runtime/e2e/worlds/`:
- The key is a hash of `modpack/index.toml`, the worldgen-relevant JARs, size, seed, Chunky radius and the fixture version. Any update of the pack or our worldgen invalidates the entry automatically.
- On a miss the launcher builds the entry once: create the world, run `chunky radius <r>` + `chunky start`, wait for completion, leave the world.
- Each use copies the entry into `saves/`. Old entries are pruned by count.
- Chunky radius defaults to 0.

**Reset level** (option `reset`):
1. `arena` (default): each test gets its own plot at an offset. It is cleared with `fill`, items and entities inside are killed, and the player's inventory, effects, health, food and gamemode are reset. Takes under 1 s, no reload.
2. `world`: leave, copy the cache entry again, open. Required for tests that change global state (R3).
3. `fresh`: a new world.

Later, only if measurements call for it: restore just the `.mca` region files under the arena while the world is closed. Vanilla 1.21.1 has no chunk-regenerate command, so this region-file restore is our way to regenerate chunks.

**Suite-level savings:**
- One client per run, or kept open across runs with `--keep` + attach.
- The launcher orders spec files by world key so each world opens once per group.
- Defaults per R2.
- Test tags `@quick` / `@slow` / `@visual`; `-t` name filter; `--bail` for agent loops.
- No retries (F3).

### Test conventions (approved 2026-11-09)

They go verbatim into the `e2e-test` skill and `tests/e2e/AGENTS.md`. Rule IDs are for reference in reviews.

**S. Layer and environment**
- **S1.** Pick the layer first: GameTest if no client is needed. e2e only for UI, real input, client-server sync, save/reload. Worldgen statistics stay with `/lkworld` and `validate-world`.
- **S2.** World tier and reset level are declared once at the top of the file: `test.use({ world, reset })`. The default is `flat` + `arena`. Anything heavier needs a comment with the reason.

**N. Names**
- **N1.** Path `tests/e2e/specs/<area>/<feature>.spec.ts`, kebab-case. One file is one feature. `describe` only for sub-groups, at most one level.
- **N2.** Test name: an English sentence in the present tense, subject first, without `should` or `test`. Example: `"altar charges a crystal on use"`.
- **N3.** The test ID is derived from the path and name, e.g. `magic/mana-pool > altar charges a crystal on use`. Never written by hand.
- **N4.** GameTest uses the same triple: package `…testkit.gametest.<area>`, class `<Feature>Tests`, camelCase behaviour method (`altarChargesCrystalOnUse`).
- **N5.** A screen object is named after the Java screen it models (`CreateWorldScreen`) and lives in `harness/screens/`. A helper is a verb for the player action (`give`, `placeAndUse`) and lives in `harness/helpers/`.

**L. Code levels**
- **L1.** No raw `mc.raw(...)` in specs. If a helper or screen object is missing, add it.
- **L2.** Three levels, depending downward only: protocol ops (`protocol.ts`) → helpers and screen objects → specs. Helpers make no assertions except on their own success.

**A. Test structure**
- **A1.** One test, one behaviour, in three blocks:
  - `arrange()`: commands and helpers; cheats are fine;
  - `act()`: only the behaviour under test, only through the game path;
  - `assertThat()`: server state via `query.*`.
- **A2.** Chat text is asserted only when the text itself is under test.
- **A3.** Act uses the controller path (`player.*`) by default. The key path (`input.*`) is for tests of keys, movement and bindings. Keys are given by key mapping name, never by key code.
- **A4.** UI is located by translation key and widget type, never by visible text or pixels.

**W. Coordinates, time, waits**
- **W1.** Coordinates only relative to the arena: `arena.at(x, y, z)`.
- **W2.** Game time is in ticks. Every wait is `waitFor(cond, { ticks })` with an upper bound. `sleep`, `setTimeout` and `expect.poll` are forbidden in specs; a lint check enforces this.
- **W3.** Each test has a wall-clock timeout in ms only as a hang guard. Longer than the default only with `@slow`.

**R. Reset and determinism**
- **R1.** Tests do not depend on order or on another test's state. Cleanup is the fixture's job, not the test's.
- **R2.** Defaults: fixed seed; `doDaylightCycle`, `doWeatherCycle`, `doMobSpawning` and `doInsomnia` off; language `en_us`. A test that needs one of them on enables it explicitly.
- **R3.** A test that changes global state (gamerules, time, the world border, saved data) declares `reset: 'world'`.

**T. Test tags**
- **T1.** Tags go in the test name. The set is fixed: `@quick` (the per-change set, under a minute with the client up), `@slow`, `@visual`. A new tag only through the glossary.

**F. Failures and artifacts**
- **F1.** A screenshot is an artifact. Pixel comparison only in `@visual` specs, with a tolerance and shaders off.
- **F2.** On failure these are collected automatically into `runtime/e2e/runs/<timestamp>/<test>/`, and `report.json` comes from the Vitest JSON reporter:
  - the AAA block and spec line where the test failed;
  - a screenshot;
  - the client and server log excerpt;
  - a `player.state` + `ui.screen` dump.
- **F3.** No retries in normal runs. `--retry` only to measure flakiness.
- **F4.** A flaky test is a bug: it gets a TD-xx row and gets fixed. It is never skipped, disabled or quarantined to get a green run.
- **F5.** Driver and mod compare protocols at `hello`. On a mismatch the run does not start.

### Documentation for agents

- **`tests/e2e/AGENTS.md` + `CLAUDE.md` (`@AGENTS.md`)**: how to run, the conventions summary with a link to the skill, where artifacts land, and `specs/_seed.spec.ts` as the example to copy.
- **The op catalog has one source**: `tests/e2e/harness/protocol.ts` (name, args type, result type, one-line description).
  - `hello` compares it with the mod's op list. On a mismatch the run fails with "op X declared in protocol.ts but not implemented in testkit".
  - `npm run e2e:catalog` writes `docs/references/e2e-protocol.md` from it, and `docs:check` fails when that file is stale (like the skill-sync check).
- **ADR-0005**: test framework. Two layers, TS + Vitest. Alternatives with reasons: JSON scenarios, MineClient Bridge, Fabric Client GameTest, node:test.
- **D-22**: the test-only flat preset is an exception to D-18; test worlds are exempt from D-20. Add a cross-reference in the D-18 and D-20 rows.
- **`docs/quality.md`**: a "Test layers" section with the boundary table.
- **Glossary**: the terms above.
- **AGENTS.md**:
  - in Verification, `npm run e2e` and `npm run gametest`, one line each;
  - in Skills, `e2e-test`;
  - in Status, nothing (this is not a milestone step).
- **ARCHITECTURE.md**: `tests/e2e/` and the testkit mod in the layout.
- **Skill `e2e-test`** in `.agents/skills/` (then `npm run skills:sync`). It covers choosing the layer → writing the spec by the conventions → running → reading the failure report → fixing the test or reporting a bug with a TD row.

### Verification

This cloud session is Linux and cannot launch the Windows Prism client, so the levels split:
- **Here (build passed):**
  - `./gradlew build` in `mods-src/`, which needs the Gradle/NeoForge downloads to be reachable;
  - `npm run docs:check`;
  - Vitest type-check / unit tests of the harness against a fake bridge (a small TCP stub implementing `hello` and a few ops);
  - the lint check.
- **On the host PC (runtime passed, by the user or a local session):**
  - `npm run smoke`: the play instance is unchanged, testkit is not in it;
  - `npm run e2e -- specs/session/hello.spec.ts` cold, then with `--keep` + attach; record both times in quality.md;
  - both seed specs pass; one deliberately broken assertion produces the F2 artifacts;
  - a second run hits the world cache, and touching `modpack/index.toml` invalidates it;
  - `npm run gametest` runs the sample test and fails on a broken assertion;
  - editing `protocol.ts` without regenerating the catalog makes `docs:check` fail.
- Every report states the level reached and what was not run.

## Steps

- [x] 0. Git: drop the unrelated WIP commit `55331b7`; work and push only on `main-e2iix5`.
- [x] 1. Docs first: ADR-0005, D-22, glossary terms, this plan, index rows; `npm run docs:check`.
- [x] 2. Testkit skeleton:
  - bridge with `hello`, `ping`, `state`, `quit`;
  - the `living-kingdoms-e2e` instance in `instance.ts`;
  - `tools/pack/src/e2e.ts` with start / attach / `--keep`;
  - the `tests/e2e` package with pinned Vitest and `specs/session/hello.spec.ts`.
- [ ] 3. World ops, the flat preset, the world cache (with Chunky radius) and reset levels `world` / `fresh`.
- [ ] 4. Input, player, container, UI, command and query ops; screenshots; arenas and the `arena` reset.
- [ ] 5. Harness: fixtures (`test.use({world, reset})`), AAA blocks, `waitFor` in ticks, helpers, screen objects, the failure-artifact collector, and the lint check for W2/L1.
- [ ] 6. Seed specs:
  - `specs/_seed.spec.ts` (flat): give an item, use it on a block, assert the block state `@quick`;
  - `specs/world/size-selector.spec.ts` (`fresh` Test size via UI): pick the size in our selector, `/lkworld info` shows `test`, save → quit → reopen keeps it.
- [ ] 7. GameTest layer: verify the NeoForge 21.1 mechanism, add `npm run gametest`, one sample test in testkit (N4 naming).
- [ ] 8. Catalog generation + the `docs:check` freshness check, the `e2e-test` skill, `tests/e2e/AGENTS.md`, quality.md test layers. Close the plan when the user has run steps 2–7 on the host PC.

## Progress log

- 2026-11-09 — Research and design with the user in a cloud session: options compared, ADR-0005 written, conventions approved; step 0 done.
- 2026-11-09 — Step 1: ADR-0005, D-22, glossary Testing terms, this plan; docs:check clean.
- 2026-11-09 — Step 2, cloud session (Linux), build passed:
  - testkit mod with the bridge and the session ops `hello`, `ping`, `state`, `quit`; `./gradlew build` passes;
  - `instance.ts` gains the `living-kingdoms-e2e` target with test `options.txt`; the testkit JAR is kept out of the play instance and the test server (`builtOwnJars`);
  - `tools/pack/src/e2e.ts` (attach / restart on a JAR or pack change / launch / `--keep`);
  - `tests/e2e` with Vitest 5.0.5 and `specs/session/hello.spec.ts`;
  - `npm run e2e:unit`: 8 harness tests pass; `tsc` passes for the harness and `tools/pack`;
  - the launcher was run end to end against a fake driver on port 25598: attach + both hello specs pass; a stale testkit hash triggers a restart.

  Not run: the real client, the Prism e2e instance, the cold-start and attach times (Windows host).

## Decision log

- 2026-11-09 — TypeScript tests in the Playwright style instead of JSON scenarios: loops, shared helpers and waits need a real language, and one long-running client is easier to drive from a Node runner (ADR-0005).
- 2026-11-09 — Vitest instead of node:test: Playwright-style fixtures, assertions and reporters for one pinned dev dependency outside the root tooling (ADR-0005).
- 2026-11-09 — Flat test preset only in testkit and test worlds exempt from D-20 (D-22): mechanics tests in seconds without worldgen; full pre-generation would make every test world cost hours.
- 2026-11-09 — Conventions approved after removing contradictions:
  - one naming scheme;
  - terms that do not collide with step, phase, stage, smoke, snapshot, Minecraft tags or Vitest's `isolate`;
  - waits in ticks instead of `expect.poll`;
  - strict F4 with no quarantine.
- 2026-11-09 — Plain TCP with JSON lines on 127.0.0.1:25598: no libraries on either side; the test server already uses 25599.
- 2026-11-09 — GameTest is disabled in production launches. NeoForge 21.1.265 `GameTestHooks.isGametestEnabled()` requires `!FMLLoader.isProduction()`, so the installer-based test server cannot run GameTests and the `npm run gametest` design in step 7 changes: run the ModDevGradle `gameTestServer` with the pack's server mods on the runtime classpath. Verify in step 7.
- 2026-11-09 — Vitest 5 replaced `test.use` with `test.override`. Convention S2 means `test.override({ world, reset })` at the top of the file; the rule is unchanged.
- 2026-11-09 — Pins:
  - vitest 5.0.5, vite 8.3.5 (Vitest 5 makes vite a peer), typescript 6.0.3, @types/node 22.20.4, all at least two weeks old;
  - `tests/e2e/.npmrc` sets `legacy-peer-deps`, because npm 10 crashes on Vitest 5's optional peers, and every peer we need is a direct devDependency.
- 2026-11-09 — The cloud environment "MinecraftLivingKingdoms" (personal) allows the NeoForge, Parchment and Mojang Maven hosts, so cloud sessions can run `./gradlew build`.
- 2026-11-10 — Merged `main` into `main-e2iix5`: `main` took D-21 for the worldgen validation sets first, so the flat test preset decision is now D-22; its text is unchanged.
