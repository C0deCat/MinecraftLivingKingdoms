---
status: active
last_reviewed: 2026-11-07
---

# Quality

Per-subsystem state and known gaps, plus the end-to-end test bars every subsystem must eventually meet. Updated at milestone acceptance and by doc gardening.

## Subsystem grades

Grades: `—` not started · `D` exists, unverified · `C` runtime passed · `B` gameplay verified · `A` accepted milestone with regression tests.

| Subsystem | Grade | Known gaps |
| --- | --- | --- |
| Docs and agent harness | C | Doc linter and Stop hook exist; no CI yet (no GitHub remote) |
| Infrastructure (M0) | C | Accepted in singleplayer; clean clone reproduces identical JARs. No CI. LAN/server are M10 |
| World and adventure (M1) | C | M1-A1 (base generator) done: sizes, selector, borders, dimension scales and the world edge are gameplay-verified by the user on Large; Test/Small/Medium generation checked on the test server only. No structure mods (M1-A2), no reconcile pass (M1-B), no content manifest or validator (M1-C), one short client spark reading (see Performance baselines) |
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
| 2026-11-07 | 0.1.0 (8 mods + our 2) | Large world with the F3 edge, singleplayer, `/spark health` ~80 s after load, host PC | 20.0 (5 m: 19.94) | 5.5 / 10.7 (10 s); 5.4 / 14.3, max 259.6 (1 m, includes load) | 1.4 / 6.0 GB |

The 2026-11-06 row is preliminary: the window included spawn generation and was shorter than the planned 2–3 minutes.

## End-to-end test bars

| Area | Test set | Acceptance bar |
| --- | --- | --- |
| Install | Clean client from Git | Launches without manually downloading JARs |
| Play mode | Singleplayer (M0–M9); Open to LAN with 2–4 clients and a dedicated server (M10) | World opens and plays; from M10 joins work without desync of our mechanics |
| Generation | Several seeds and one reference world | All mandatory structures and chains reachable |
| Recipes | Cross-mod ingredient combinations | No blocking duplicates or exploits |
| Mana | Ars + Iron's in one pool | No double regeneration or wrong deductions |
| NPCs | Families, age, construction, trade | IDs and state survive a world reload |
| Unloading | 1/10/100 chunk load cycles | No duplicated goods, NPCs, buildings |
| Performance | spark, JVM/heap, mean and p95 tick | 20 TPS on the host PC in the described scenario; limits recorded |
| Long simulation | 1, 5, 25 in-game years | No accumulation of obvious contradictions |
| Disasters | Fire, flood, storm with object protection | No uncontrolled destruction |
| Release | Licenses, packages, backups | No illegally included modified assets |

Numbers of settlements and test durations are targets; real budgets are set after profiling.
