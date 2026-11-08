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
- [ ] 2. `/lkworld structures [radius]` census in `livingkingdoms-world`: structure starts in the radius, bounding boxes, border check, footprint relief and water share for surface structures, overlapping pairs; CSV in the world folder and a summary.
- [ ] 3. Baseline census: Large, seeds `livingkingdoms` and `alpha`, Chunky radius 1,536 in the Overworld, Nether and End.
- [ ] 4. Fixes as Lithostitched modifiers in our world mod: remove the D&T village sets; then placement fixes picked from the census, one at a time with a census run after each.
- [ ] 5. Renderer: Sodium + Iris as client-side mods; verify-mods, smoke, test server without them; user check of GeckoLib mobs, structures, FPS and spark.
- [ ] 6. After-census on the same seeds; user flight over a new Large world with the renderer; close the plan, leftovers to M1-C1 or the tech-debt tracker.

## Progress log

- 2026-11-08 — Plan created from the approved Claude Code plan. Checked for it: Lithostitched 1.8.1 has `remove_structure_set_entries` and `set_structure_spawn_condition` (fields read in its MIT sources, branch `21.1`); D&T villages are three structure sets; D&T adds its End Castle to `#minecraft:eye_of_ender_located` and ships unused jigsaw pools under `minecraft:nether_fortress/*`. Decisions recorded. Status: docs only.

## Decision log

- 2026-11-08 — Villages until M4 (with the user): vanilla villages stay as the single placeholder source, including the D&T taiga variant; D&T birch, jungle and swamp villages are removed with a Lithostitched modifier; The Graveyard's overrun villages stay as hostile ruins, not living villages.
- 2026-11-08 — Test is a development-only size (with the user): completability targets and the validator cover Small, Medium and Large (ADR-0004 amendment); TD-14 closed.
- 2026-11-08 — Overlaps and placement are measured with our census command, not only by eye (with the user); the command becomes the base of the M1-C1 validator.
- 2026-11-08 — Not changed in M1-B: other mods' templates (license rule) and the slow `/locate` of large WDA structures (a known issue for commands, not a worldgen defect).
