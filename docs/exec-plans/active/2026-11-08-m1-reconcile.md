---
status: active
last_reviewed: 2026-11-08
---

# M1-B — reconcile the world and adventure mods

**Status:** Active

## Goal

The world mods of M1-A work as one world: one village source until M4, no structure pairs cutting into each other, surface structures not hanging off cliffs or sunk in water beyond a measured, accepted level, and the renderer (Sodium + Iris) in the client. Done means: census numbers before and after on the same seeds, the fixes in our world mod, a client check by the user on Large with the renderer (gameplay verified).

## Context

- Milestone: [M1 — World and adventure](../../milestones/m1-world-and-adventure.md), phase B; follows [M1-A2](../completed/2026-11-07-m1-structures.md), whose findings are in the M1 open questions.
- World sizes and the Test size status: [ADR-0004](../../decisions/ADR-0004-world-sizes-and-dimensions.md#amendment-2026-11-08-test-is-a-development-size).
- Changing other mods' structures: [licenses.md](../../licenses.md#project-rules) (placement only for ARR mods), [Lithostitched notes](../../references/lithostitched-1.8.md#structure-modifiers).
- Approved Claude Code plan of 2026-11-08 (content copied into the steps below).

## Steps

- [x] 1. Decisions: villages until M4, Test development-only (ADR-0004 amendment, TD-14 closed), this plan, Lithostitched structure modifier notes.
- [x] 2. `/lkworld structures [radius]` census in `livingkingdoms-world`: structure starts in the radius, bounding boxes, border check, footprint relief and water share for surface structures, overlapping pairs; CSV in the world folder and a summary.
- [x] 3. Baseline census: Large, seeds `livingkingdoms` and `alpha`, Chunky radius 1,536 in the Overworld, Nether and End.
- [ ] 4. Fixes as Lithostitched modifiers in our world mod: remove the D&T village sets; then placement fixes picked from the census, one at a time with a census run after each.
- [ ] 5. Renderer: Sodium + Iris as client-side mods; verify-mods, smoke, test server without them; user check of GeckoLib mobs, structures, FPS and spark.
- [ ] 6. After-census on the same seeds; user flight over a new Large world with the renderer; close the plan, leftovers to M1-C1 or the tech-debt tracker.

## Progress log

- 2026-11-08 — Plan created from the approved Claude Code plan. Checked for it: Lithostitched 1.8.1 has `remove_structure_set_entries` and `set_structure_spawn_condition` (fields read in its MIT sources, branch `21.1`); D&T villages are three structure sets; D&T adds its End Castle to `#minecraft:eye_of_ender_located` and ships unused jigsaw pools under `minecraft:nether_fortress/*`. Decisions recorded. Status: docs only.
- 2026-11-08 — `/lkworld structures [radius]` added (`StructureCensus`). First try used whole bounding boxes: jigsaw boxes include empty space (an illager campsite of 173 × 122 "overlapped" five structures) and relief over a whole box exaggerates slope in mountains. Now overlaps need intersecting pieces and the slope is the worst piece (relief under a piece / its longer side, flagged above 0.5); water share over all piece samples (flagged above 50%). Large, seed `livingkingdoms`, radius 512: 30 starts, 1 overlapping pair (WDA bathhouse × illager campsite), 12 of 26 surface starts steep (spawn is in Tectonic mountains). Status: build passed, runtime passed on the server.
- 2026-11-08 — Baseline census (whole-box-free metric of the previous entry), Large, radius 1,536 (Nether 1,024). Overworld: `livingkingdoms` 270 starts, 5 overlapping pairs (3 surface-surface: Moog's pile × grain bin, WDA bathhouse × illager campsite, D&T bunker × Terralith fortified village), 46 of 200 surface starts steep, 11 wet; `alpha` 257 starts, 4 pairs (all surface: Moog's pond × D&T ruin town, WDA bandit towers × Moog's windmill and × Terralith rubble, Moog's dead tree × D&T desert ruins), 40 of 189 steep, 14 wet. Wet ones are by design (shipwrecks, Better Archeology underwater sites). Nether: 127 / 106 starts, 1 / 2 underground pairs (BoMD Gauntlet arena × bastion; fossils), 6 / 4 starts partly over the ±1,024 border. End: 8 / 9 starts, no pairs; slope there was measured over the void. Also found: Terralith's own fortified villages (`terralith:fortified_village`, `fortified_desert_village`).
- 2026-11-08 — `remove_dnt_villages` modifier (with a `lithostitched:mod_loaded` predicate for `mr_dungeons_andtaverns`): on seed `livingkingdoms` `/locate structure nova_structures:village_birch` now finds nothing (was 1,207 blocks), the tavern stays at 1,657. Status: runtime passed on the server.
- 2026-11-08 — User checked the steepest spots on seed `livingkingdoms` in the client: the D&T bunker (79, −868) is underground (entry shaft, rooms at y 142), the WDA bathhouse and a D&T wild ruin on a mountain sit naturally on their foundations; no generation defects seen. The slope flags were mostly false: buried pieces, small path and shaft pieces, End void. Census refined: terrain only in natural dimensions without a ceiling, only pieces at least 8 blocks long whose top reaches the ground. Status: build passed.

## Decision log

- 2026-11-08 — Villages until M4 (with the user): vanilla villages stay as the single placeholder source, including the D&T taiga variant; D&T birch, jungle and swamp villages are removed with a Lithostitched modifier; The Graveyard's overrun villages stay as hostile ruins, not living villages.
- 2026-11-08 — Test is a development-only size (with the user): completability targets and the validator cover Small, Medium and Large (ADR-0004 amendment); TD-14 closed.
- 2026-11-08 — Overlaps and placement are measured with our census command, not only by eye (with the user); the command becomes the base of the M1-C1 validator.
- 2026-11-08 — Not changed in M1-B: other mods' templates (license rule) and the slow `/locate` of large WDA structures (a known issue for commands, not a worldgen defect).
- 2026-11-08 — Terralith fortified villages stay as a vanilla-style variant in the single placeholder village source (with the user), like the D&T taiga variant; reconciled with all vanilla villages in M4-B.
- 2026-11-08 — No `max_relief` spawn condition: the steep-ground flags were false positives on in-game inspection (with the user). Steepness stays a census number for M1-C1 (reachable entrances), not a placement rule.
