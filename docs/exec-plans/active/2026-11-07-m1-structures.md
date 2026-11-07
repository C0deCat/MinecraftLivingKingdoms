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
- [x] 3. YUNG's Better Dungeons, Better Mineshafts, Bridges.
- [x] 4. YUNG's Better Desert Temples, Jungle Temples, Witch Huts.
- [x] 5. YUNG's Better Ocean Monuments, Better Nether Fortresses (inside the Nether border), Better End Island (dragon fight on the bounded End).
- [x] 6. YUNG's Extras.
- [x] 7. When Dungeons Arise: sizes vs border, mega-dungeon candidates.
- [x] 8. Dungeons and Taverns: overrides of vanilla structures that clash with YUNG's.
- [x] 9. Structory + Structory: Towers.
- [x] 10. Moog's Structure Lib + Moog's Voyager Structures + Moog's End Structures (outer End islands, TD-13).
- [x] 11. Better Archeology + Architectury.
- [x] 12. GeckoLib, Cerbon's API, Cloth Config + Bosses of Mass Destruction: each arena reachable.
- [x] 13. The Graveyard (check its second dependency).
- [x] 14. Mowzie's Mobs (credit line for the release notes).
- [ ] Close: 3 seeds × Test and Large on the test server; Chunky time and spark on Large vs the batch 1 baseline (quality.md); user check in the client; overlaps into the M1-B list; matrix statuses `runtime-ok`.

## Progress log

- 2026-11-07 — Plan created from the approved Claude Code plan. Candidates checked on Modrinth for 1.21.1 NeoForge (versions, licenses, dependencies); Mowzie's Mobs license read on CurseForge. Decisions recorded in licenses.md, the compatibility matrix, the M1/M3/M4/M7 specs and the tech-debt tracker. Status: docs only.
- 2026-11-07 — Batch 1: Structure Layout Optimizer 1.0.12 (+ Resourceful Config 3.0.11), Sparse Structures 3.0, Chunky 1.4.23. Test server, Test size, fresh world, seed `livingkingdoms`: Chunky radius 512 = 4,225 chunks in 2:32 (baseline before structure mods), no ERROR. Status: runtime passed on the server; client smoke pending (Prism was open).
- 2026-11-07 — Batch 2: YUNG's API 5.1.9, YUNG's Better Strongholds 5.1.3. Test server, seed `livingkingdoms`: vanilla stronghold replaced; nearest `betterstrongholds:stronghold` at (−1,696, 128) on both Test and Large. On Test that is outside the ±1,024 border (TD-14). Status: runtime passed on the server; portal room not inspected in game.
- 2026-11-07 — Batches 3–6 added in one go (10 YUNG's mods, not in groups of 3; no error to bisect). Fresh Large world, seed `livingkingdoms`: nearest Better Dungeons (small 1,153, spider 2,065, zombie 2,243, skeleton 2,752 blocks), lush mineshaft 1,508, desert temple 3,444, jungle temple 2,865, witch hut 6,827, ocean monument (−5,472, 6,736) inside the border, `betterfortresses:fortress` 151 blocks from the Nether origin (vanilla fortress replaced), End city 1,642. Chunky radius 512 on Large 2:39, no ERROR. Status: runtime passed on the server; End island fight and bridges not seen in game.
- 2026-11-07 — Batches 7–8: When Dungeons Arise 2.1.68, Dungeons and Taverns 4.4.4. Large, seed `livingkingdoms`: Chunky radius 512 in 2:32, no FATAL. New ERROR lines (non-fatal): WDA advancements fail to load, "Block-attached entity at invalid position" and one sculk shrieker parse error from underground structures. `/locate`: WDA monastery 3,313 and coliseum 5,751 blocks; Keep Kayra, Shiraz Palace, Plague Asylum and Mining System not found within the locate radius (validator question, M1-C). D&T: tavern 1,657, `village_birch` 1,207, Nether Keep 629, End Castle 1,777. A locate of Keep Kayra blocked one tick for over 60 s and the watchdog stopped the server; the test server now runs with `max-tick-time=-1`. Status: runtime passed on the server.
- 2026-11-07 — For M1-B: D&T adds villages (birch, jungle, swamp, hamlet, remnant village) and overrides `minecraft:village_taiga`, against the "one village source" decision; D&T `mangrove_witch_hut` next to YUNG's witch huts; slow `/locate` for large WDA structures also affects players.
- 2026-11-07 — Batches 9–11: Structory 1.3.14, Structory: Towers 1.0.15, Moog's Structure Lib 3.4.2, Moog's Voyager Structures 5.1.3, Moog's End Structures 2.1.1, Architectury API 13.0.11, Better Archeology 1.3.9. `packwiz modrinth add` chose the "26.2" universal Structory files; Towers 1.0.17 failed to load ("Missing ModLoader"), so both are pinned to the `1.21.x` files. Large, seed `livingkingdoms`: Chunky radius 512 in 2:39, only the known ERROR lines. `/locate`: chapel 1,728, manor 2,920, ancient temple 592, castle ruins 7,096, windmill 643, catacombs 5,107, archeologist camp 2,141, End spire 5,674; wizard tower not found. `/locate` also reports structures beyond the border (End mega ship at x −11,712, border ±8,192), so the M1-C validator must check positions against the border. Status: runtime passed on the server.
- 2026-11-07 — Batches 12–14: Geckolib 4.9.3, Cloth Config API 15.0.140, CERBON's API 1.3.0, Bosses of Mass Destruction 1.3.3, The Graveyard 4.0.1 (+ VillagersTradingPlus 1.1.1, its required dependency), Mowzie's Mobs 1.8.2. Large, seed `livingkingdoms`: Chunky radius 512 in 2:46, only the known ERROR lines. `/locate`: BoMD lich tower 5,744, void blossom 1,495, Gauntlet arena 121 from the Nether origin, Obsidilith arena 1,641 in the End; Graveyard Lich Prison at (−7,632, −7,680) near the Large edge (validator: may be in the outer ocean ring); large graveyard not found (search ran over 3 minutes); Mowzie's wrought chamber 329, Frostmaw 1,905, Umvuthana grove 3,860, Sculptor monastery 1,529. Status: runtime passed on the server; client smoke and in-game look pending.
- 2026-11-07 — For M1-B and M4-B: The Graveyard adds "overrun" villages in five biomes; VillagersTradingPlus changes villager trades.

## Decision log

- 2026-11-07 — Mod list for M1-A2 agreed with the user: YUNG's set, When Dungeons Arise, Dungeons and Taverns, Structory + Towers, Moog's Voyager + End, Better Archeology, Bosses of Mass Destruction, The Graveyard, Mowzie's Mobs, tools Structure Layout Optimizer, Sparse Structures, Chunky.
- 2026-11-07 — ARR mods (WDA, Dungeons and Taverns) stay, changed only through config, tags and placement; deep changes only in open mods (licenses.md project rules).
- 2026-11-07 — Rejected: L_Ender's Cataclysm (ND license vs planned integration), CTOV and Towns and Towers (second village source next to ours). Mowzie's Mobs accepted after reading its license (credit, faithful changes, modpack integration allowed).
- 2026-11-07 — Not taken now: The Aether + Deep Aether, Deeper and Darker (new dimensions need border code and validator coverage), Illager Invasion, Mutant Monsters, Artifacts.
- 2026-11-07 — Worldgen of M2/M3 mods is installed with those milestones, each reopening M1 for a targeted regression; Dynamic Trees becomes an M7 spike (TD-06).
- 2026-11-07 — Better Archeology needs no magic integration (artifacts are anvil enchantment catalysts, fossils are decor); an optional ingredient hook is an M3 open question; its Archeologist villager goes to M4-B.
