---
status: active
last_reviewed: 2026-11-10
---

# Quality

Per-subsystem state and known gaps, plus the end-to-end test bars every subsystem must eventually meet. Updated at milestone acceptance and by doc gardening.

## Subsystem grades

Grades: `—` not started · `D` exists, unverified · `C` runtime passed · `B` gameplay verified · `A` accepted milestone with regression tests.

| Subsystem | Grade | Known gaps |
| --- | --- | --- |
| Docs and agent harness | C | Doc linter and Stop hook exist; no CI yet (no GitHub remote) |
| Infrastructure (M0) | C | Accepted in singleplayer; clean clone reproduces identical JARs. No CI. LAN/server are M10 |
| World and adventure (M1) | C | M1-A1 (base generator) done: sizes, selector, borders, dimension scales and the world edge are gameplay-verified by the user on Large; Test/Small/Medium generation checked on the test server only. M1-A2 structure and boss mods installed (40 pack mods): gameplay verified by the user on Large for a stronghold, a WDA dungeon and a BoMD arena; other structures checked by `/locate` on the test server only. M1-B (reconcile) done: `/lkworld structures` census on the test server, renderer (Sodium + Iris) gameplay verified by the user on Large; M1-C1 done: World Content Manifest and `/lkworld validate`, 0 critical gaps on Small/Medium/Large × 4 seeds and on the D-21 phase set on the test server; bunker entrance fix and guaranteed structures gameplay verified by the user on Large; hanging pieces of other structures are warnings (TD-22). M1-C2 done: Complementary Reimagined as the default shader pack, Photon as the alternative, both optional (gameplay verified by the user). M1-D (acceptance) in progress |
| Household (M2) | — | — |
| Magic (M3) | — | — |
| Settlements (M4) | — | — |
| Simulation (M5) | — | — |
| Economy and states (M6) | — | — |
| Atmosphere (M7) | — | — |
| Cultures (M8) | — | — |
| Multiplayer (M10) | — | Not started: no LAN or dedicated-server runs yet |

## Performance baselines

| Date | Pack | Scenario | TPS | Tick median / p95 (ms) | Heap |
| --- | --- | --- | --- | --- | --- |
| 2026-11-06 | 0.0.1 (4 mods) | New default world, singleplayer, ~30 s after join, host PC (Core Ultra 9 275HX, 32 GB, RTX 5070 Ti Laptop) | 20.0 | 4.8 / 10.4 (10 s window) | 1.0 / 6.0 GB |
| 2026-11-08 | 40 mods + our 2 (M1-A2) | Large world, singleplayer, `/spark health` ~1 min after the last of three teleports, host PC | 20.0 (5 m: 16.85, incl. teleports) | 3.5 / 4.6 (10 s); 4.4 / 6.9, max 41.4 (1 m) | 1.5 / 6.0 GB |
| 2026-11-09 | 42 mods + our 2 (M1-B, Sodium + Iris, shader pack off) | Large world, singleplayer, `/spark health` ~7 min after the last teleport, standing in a cluster of a D&T shrine, a D&T bunker and a Terralith fortified village, host PC; client 430–690 FPS, 290 at a Mowzie's grove with particles | not usable: the integrated server pauses when the game loses focus (5 s: 20.0) | 10.5 / 16.8, max 20.7 (10 s); 10.4 / 15.9, max 74.4 (1 m) | 1.7 / 6.0 GB |
| 2026-11-10 | 44 files + our 2 (M1-C2, shader packs) | Large world, seed `livingkingdoms`, lake in an old growth pine taiga near (−995, 64, −4400), noon, `/spark health` with Complementary Reimagined on (shaders are client-side) | 20.0 (5 s; 10 s and 1 m low while the game was paused in the shader menu) | 5.3 / 6.9, max 9.7 (10 s) | 1.1 / 6.0 GB |
| 2026-11-07 | 0.1.0 (8 mods + our 2) | Large world with the F3 edge, singleplayer, `/spark health` ~80 s after load, host PC | 20.0 (5 m: 19.94) | 5.5 / 10.7 (10 s); 5.4 / 14.3, max 259.6 (1 m, includes load) | 1.4 / 6.0 GB |

Client FPS on the host PC (2560×1369, render distance 16, RTX 5070 Ti Laptop), M1-C2, 2026-11-10, read from F3:

| Shader pack | Profile | FPS | p98 / p99.5 |
| --- | --- | --- | --- |
| None | — | about 600 in the open, 800–1,000 in closed spaces (the user's observation) | — |
| Complementary Reimagined (default) | High | 92 | 76 / 73 |
| Photon (alternative) | High | 114 (another view) | 56 / 26 |

Worldgen speed on the test server (Chunky, radius 512 = 4,225 chunks, fresh world, seed `livingkingdoms`, host PC):

| Date | Pack | Size | Time |
| --- | --- | --- | --- |
| 2026-11-07 | 12 mods, no structure mods | Test | 2:32 |
| 2026-11-07 | + YUNG's set | Large | 2:39 |
| 2026-11-07 | + WDA, Dungeons and Taverns | Large | 2:32 |
| 2026-11-07 | + Structory, Moog's, Better Archeology | Large | 2:39 |
| 2026-11-07 | + BoMD, The Graveyard, Mowzie's Mobs (40 mods) | Large | 2:46 |

Structure mods cost little in generation time (about 10% over the run-to-run spread). In the client, teleports into ungenerated chunks cause one 2–6 s "Can't keep up" stall each; normal flight was not measured separately.

The 2026-11-06 row is preliminary: the window included spawn generation and was shorter than the planned 2–3 minutes.

e2e driver speed on the host PC (`npm run e2e`, 44 pack files + our 2 + testkit, shaders off; [ADR-0005](decisions/ADR-0005-test-framework.md)):

| Date | Case | Time |
| --- | --- | --- |
| 2026-11-10 | Cold start: Prism launch until the driver answers (it starts during client setup, before the title screen) | 26–28 s |
| 2026-11-10 | Cold start until the startup resource reload ends (the launcher's "Client ready") | 30–34 s |
| 2026-11-10 | Attach to a client left open with `--keep` | 5–11 ms |
| 2026-11-10 | All 12 specs (2 hello + 10 simple), attached; includes creating the run's flat world (about 5 s) | 12–13 s |
| 2026-11-10 | All 12 specs, cold: launch, specs, quit | 58 s |
| 2026-11-10 | One simple spec after the world exists (arena reset included) | 0.3–0.8 s; walking 2.7–4.2 s, save → quit → reopen 2–3 s |

## End-to-end test bars

| Area | Test set | Acceptance bar |
| --- | --- | --- |
| Install | Clean client from Git | Launches without manually downloading JARs |
| Play mode | Singleplayer (M0–M9); Open to LAN with 2–4 clients and a dedicated server (M10) | World opens and plays; from M10 joins work without desync of our mechanics |
| Generation | Within a milestone: 3 Large worlds of different seeds; at its acceptance: Small, Medium and Large × 4 seeds (D-21). No reference world: every world of any seed is checked by its own numbers (D-20) | `npm run validate-world` reports no critical gap: all mandatory structures and chains reachable |
| Recipes | Cross-mod ingredient combinations | No blocking duplicates or exploits |
| Mana | Ars + Iron's in one pool | No double regeneration or wrong deductions |
| NPCs | Families, age, construction, trade | IDs and state survive a world reload |
| Unloading | 1/10/100 chunk load cycles | No duplicated goods, NPCs, buildings |
| Performance | spark, JVM/heap, mean and p95 tick | 20 TPS on the host PC in the described scenario; limits recorded |
| Long simulation | 1, 5, 25 in-game years | No accumulation of obvious contradictions |
| Disasters | Fire, flood, storm with object protection | No uncontrolled destruction |
| Release | Licenses, packages, backups | No illegally included modified assets |

Numbers of settlements and test durations are targets; real budgets are set after profiling.
