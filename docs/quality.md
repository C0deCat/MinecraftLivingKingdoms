---
status: active
last_reviewed: 2026-11-06
---

# Quality

Per-subsystem state and known gaps, plus the end-to-end test bars every subsystem must eventually meet. Updated at milestone acceptance and by doc gardening.

## Subsystem grades

Grades: `—` not started · `D` exists, unverified · `C` runtime passed · `B` gameplay verified · `A` accepted milestone with regression tests.

| Subsystem | Grade | Known gaps |
| --- | --- | --- |
| Docs and agent harness | C | Doc linter and Stop hook exist; no CI yet (no GitHub remote) |
| Infrastructure (M0) | — | No pack manifest, no launch scripts |
| World and adventure (M1) | — | — |
| Household (M2) | — | — |
| Magic (M3) | — | — |
| Settlements (M4) | — | — |
| Simulation (M5) | — | — |
| Economy and states (M6) | — | — |
| Atmosphere (M7) | — | — |
| Cultures (M8) | — | — |

## End-to-end test bars

| Area | Test set | Acceptance bar |
| --- | --- | --- |
| Install | Clean client from Git | Launches without manually downloading JARs |
| Play mode | Singleplayer + Open to LAN with a second client | Join works, no desync of our mechanics |
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
