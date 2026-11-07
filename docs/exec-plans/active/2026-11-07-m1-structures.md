---
status: active
last_reviewed: 2026-11-07
---

# M1-A2 — structure and adventure mods

**Status:** Active

## Goal

The finite world gets its dungeons, reworked vanilla structures, points of interest and modded bosses. Done means: every mod of the agreed list is in the pack with pinned versions, smoke-tested, located on the test server, and a new Large world was checked by the user in the client (gameplay verified). Known overlaps are listed for M1-B.

## Context

- Milestone: [M1 — World and adventure](../../milestones/m1-world-and-adventure.md), step A2; follows the completed [M1-A1 plan](../completed/2026-11-06-m1-world.md).
- World sizes, borders and dimensions: [ADR-0004](../../decisions/ADR-0004-world-sizes-and-dimensions.md).
- License rules for ARR and ND mods: [licenses.md](../../licenses.md#project-rules).
- Mod workflow: [AGENTS.md](../../../AGENTS.md#workflow); packwiz: [notes](../../references/packwiz.md).
- Mod list agreed with the user on 2026-11-07 (see the decision log).

Per batch: `packwiz modrinth add` with a pinned 1.21.1 NeoForge version → `npm run verify-mods` → `npm run smoke` → `npm run test-server -- --size test --fresh` with `/locate structure` for the batch's structures → compatibility-matrix and licenses rows → commit. At most 3 mods per batch.

## Steps

- [x] Record decisions: license rules, rejected mods, deferred worldgen, Dynamic Trees to M7, M1 mods table.
- [x] 1. Tools: Structure Layout Optimizer (+ Resourceful Config), Sparse Structures (default config), Chunky; Chunky pre-generation baseline on Large.
- [x] 2. YUNG's API + YUNG's Better Strongholds: stronghold with a portal on Test and Large.
- [ ] 3. YUNG's Better Dungeons, Better Mineshafts, Bridges.
- [ ] 4. YUNG's Better Desert Temples, Jungle Temples, Witch Huts.
- [ ] 5. YUNG's Better Ocean Monuments, Better Nether Fortresses (inside the Nether border), Better End Island (dragon fight on the bounded End).
- [ ] 6. YUNG's Extras.
- [ ] 7. When Dungeons Arise: sizes vs border, mega-dungeon candidates.
- [ ] 8. Dungeons and Taverns: overrides of vanilla structures that clash with YUNG's.
- [ ] 9. Structory + Structory: Towers.
- [ ] 10. Moog's Structure Lib + Moog's Voyager Structures + Moog's End Structures (outer End islands, TD-13).
- [ ] 11. Better Archeology + Architectury.
- [ ] 12. GeckoLib, Cerbon's API, Cloth Config + Bosses of Mass Destruction: each arena reachable.
- [ ] 13. The Graveyard (check its second dependency).
- [ ] 14. Mowzie's Mobs (credit line for the release notes).
- [ ] Close: 3 seeds × Test and Large on the test server; Chunky time and spark on Large vs the batch 1 baseline (quality.md); user check in the client; overlaps into the M1-B list; matrix statuses `runtime-ok`.

## Progress log

- 2026-11-07 — Plan created from the approved Claude Code plan. Candidates checked on Modrinth for 1.21.1 NeoForge (versions, licenses, dependencies); Mowzie's Mobs license read on CurseForge. Decisions recorded in licenses.md, the compatibility matrix, the M1/M3/M4/M7 specs and the tech-debt tracker. Status: docs only.
- 2026-11-07 — Batch 1: Structure Layout Optimizer 1.0.12 (+ Resourceful Config 3.0.11), Sparse Structures 3.0, Chunky 1.4.23. Test server, Test size, fresh world, seed `livingkingdoms`: Chunky radius 512 = 4,225 chunks in 2:32 (baseline before structure mods), no ERROR. Status: runtime passed on the server; client smoke pending (Prism was open).

- 2026-11-07 — Batch 2: YUNG's API 5.1.9, YUNG's Better Strongholds 5.1.3. Test server, seed `livingkingdoms`: vanilla stronghold replaced; nearest `betterstrongholds:stronghold` at (−1,696, 128) on both Test and Large. On Test that is outside the ±1,024 border (TD-14). Status: runtime passed on the server; portal room not inspected in game.
## Decision log

- 2026-11-07 — Mod list for M1-A2 agreed with the user: YUNG's set, When Dungeons Arise, Dungeons and Taverns, Structory + Towers, Moog's Voyager + End, Better Archeology, Bosses of Mass Destruction, The Graveyard, Mowzie's Mobs, tools Structure Layout Optimizer, Sparse Structures, Chunky.
- 2026-11-07 — ARR mods (WDA, Dungeons and Taverns) stay, changed only through config, tags and placement; deep changes only in open mods (licenses.md project rules).
- 2026-11-07 — Rejected: L_Ender's Cataclysm (ND license vs planned integration), CTOV and Towns and Towers (second village source next to ours). Mowzie's Mobs accepted after reading its license (credit, faithful changes, modpack integration allowed).
- 2026-11-07 — Not taken now: The Aether + Deep Aether, Deeper and Darker (new dimensions need border code and validator coverage), Illager Invasion, Mutant Monsters, Artifacts.
- 2026-11-07 — Worldgen of M2/M3 mods is installed with those milestones, each reopening M1 for a targeted regression; Dynamic Trees becomes an M7 spike (TD-06).
- 2026-11-07 — Better Archeology needs no magic integration (artifacts are anvil enchantment catalysts, fossils are decor); an optional ingredient hook is an M3 open question; its Archeologist villager goes to M4-B.
