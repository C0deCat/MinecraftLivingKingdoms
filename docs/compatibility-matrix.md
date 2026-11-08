---
status: active
last_reviewed: 2026-11-07
---

# Compatibility matrix

Registry of the pack's mods: what is installed, why, which version and how well it is verified. Updated on every mod addition, update or removal (see the workflow in [AGENTS.md](../AGENTS.md#workflow)).

**Platform:** Minecraft 1.21.1 · NeoForge 21.1.265 (pinned 2026-11-06 in `tools/pack/src/pins.ts` and `modpack/pack.toml`) · Java 21

## Mods

`npm run verify-mods` checks this table against `modpack/`: every pack mod needs a row whose Mod cell equals the packwiz `name`, whose Version is part of the JAR file name and whose Side matches the `.pw.toml`.

| Mod | Version | Stage | Purpose | Side | Dependencies | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| spark | 1.10.124 | M0 | Profiler: TPS, tick times, heap | both | — | runtime-ok | GPL-3.0; also useful on a dedicated server |
| ModernFix | 5.27.27 | M0 | Load time and memory fixes | both | — | runtime-ok | LGPL-3.0 |
| FerriteCore | 7.0.3 | M0 | Memory use of block states and models | both | — | runtime-ok | MIT; mixin "refmap could not be read" warnings at startup are expected for this build |
| Lithium | 0.15.4 | M0 | Server tick optimizations (AI, physics, chunks) | both | — | runtime-ok | LGPL-3.0; official NeoForge build |
| Lithostitched | 1.8.1 | M1 | Worldgen compatibility layer; carries our world-edge modifiers (edge bias, erosion lift, cap) | both | — (bundles apollib) | runtime-ok | MIT |
| Tectonic | 3.0.28 | M1 | Large-scale terrain, continents and islands | both | Lithostitched | runtime-ok | MIT; ships its Terralith-compatible terrain variant; Modrinth marks it server-only, client optional. Tuned in `modpack/config/tectonic.json`: `ocean_offset` -0.38, `continents_scale` 0.16 (ADR-0004 amendments) |
| Terralith | 2.6.2 | M1 | Biome variety | both | Lithostitched | runtime-ok | Stardust Labs License (see licenses.md); pinned by version URL because `packwiz modrinth add` picked the older 2.5.8 |
| Structure Layout Optimizer | 1.0.12 | M1 | Faster jigsaw structure and NBT piece generation | both | Resourceful Config | runtime-ok | MIT; Modrinth marks it server-only, needed on the integrated server in singleplayer |
| Resourceful Config | 3.0.11 | M1 | Config library (Structure Layout Optimizer, later Better Archeology) | both | — | runtime-ok | MIT |
| Sparse Structures | 3.0 | M1 | One config for the spread of all structures on the finite map | both | — | runtime-ok | MIT; default config until M1-B |
| Chunky | 1.4.23 | M1 | Pre-generation for timing and worldgen checks | both | — | runtime-ok | GPL-3.0; the pop4959 world pre-generator |
| YUNG's API | 5.1.9 | M1 | Library for YUNG's structure mods | both | — | runtime-ok | LGPL-3.0 |
| YUNG's Better Strongholds | 5.1.3 | M1 | Stronghold with the End portal, replaces the vanilla one (`betterstrongholds:stronghold`, in `#minecraft:eye_of_ender_located`) | both | YUNG's API | runtime-ok | LGPL-3.0; keeps the vanilla concentric rings, so the Test size has no stronghold inside its border (Test is a development size, ADR-0004 amendment 2026-11-08) |
| YUNG's Better Dungeons | 5.1.4 | M1 | Skeleton, zombie, spider and small dungeons | both | YUNG's API | runtime-ok | LGPL-3.0 |
| YUNG's Better Mineshafts | 5.1.1 | M1 | Biome-themed mineshafts | both | YUNG's API | runtime-ok | LGPL-3.0 |
| YUNG's Bridges | 5.1.1 | M1 | Bridges over rivers | both | YUNG's API | runtime-ok | LGPL-3.0 |
| YUNG's Better Desert Temples | 4.1.5 | M1 | Reworked desert temple | both | YUNG's API | runtime-ok | LGPL-3.0 |
| YUNG's Better Jungle Temples | 3.1.2 | M1 | Reworked jungle temple | both | YUNG's API | runtime-ok | LGPL-3.0 |
| YUNG's Better Witch Huts | 4.1.1 | M1 | Reworked witch huts | both | YUNG's API | runtime-ok | LGPL-3.0 |
| YUNG's Better Ocean Monuments | 4.1.2 | M1 | Reworked ocean monument | both | YUNG's API | runtime-ok | LGPL-3.0 |
| YUNG's Better Nether Fortresses | 3.1.5 | M1 | Reworked Nether fortress (blaze spawners, progression) | both | YUNG's API | runtime-ok | LGPL-3.0 |
| YUNG's Better End Island | 3.1.2 | M1 | Reworked main End island and dragon fight | both | YUNG's API | runtime-ok | LGPL-3.0 |
| YUNG's Extras | 5.1.1 | M1 | Small vanilla+ structures and features | both | YUNG's API | runtime-ok | LGPL-3.0 |
| When Dungeons Arise | 2.1.68 | M1 | Large dungeons and landmarks (`dungeons_arise:*`, 40 structures), mega-dungeon candidates | both | — | runtime-ok | ARR: config and placement only ([licenses](licenses.md#project-rules)); logs "Couldn't load advancements" for two of its advancements |
| Dungeons and Taverns | 4.4.4 | M1 | Taverns, small dungeons, points of interest in all three dimensions (`nova_structures:*`) | both | — | runtime-ok | ARR: config and placement only; adds its own villages (birch, jungle, swamp, hamlet) and overrides `minecraft:village_taiga` — to be disabled or reserved in M1-B; structures with item frames log "Block-attached entity at invalid position" |
| Structory | 1.3.14 | M1 | Ruins, chapels, manors, small points of interest (`structory:*`) | both | — | runtime-ok | Stardust Labs License; pinned to the `1.21.x` file: the newer "26.x" universal files list 1.21.1 but Towers 1.0.17 fails to load ("Missing ModLoader") |
| Structory: Towers | 1.0.15 | M1 | Biome-themed towers and outposts (`structory_towers:*`) | both | — | runtime-ok | Stardust Labs License; pinned to the `1.21.x` file, see Structory |
| Moog's Structure Lib (moogs_structures) | 3.4.2 | M1 | Library for Moog's structure mods | both | — | runtime-ok | LGPL-3.0 |
| MVS - Moog's Voyager Structures | 5.1.3 | M1 | 130+ vanilla-style structures (`mvs:*`) | both | Moog's Structure Lib | runtime-ok | MIT; universal multi-version file |
| MES - Moog's End Structures | 2.1.1 | M1 | Structures on the outer End islands (`mes:*`) | both | Moog's Structure Lib | runtime-ok | LGPL-3.0; universal multi-version file |
| Architectury API | 13.0.11 | M1 | Library (Better Archeology) | both | — | runtime-ok | LGPL-3.0 |
| Better Archeology | 1.3.9 | M1 | Excavation sites, catacombs, fossils, artifacts, Archeologist villager | both | Architectury API, Resourceful Config | runtime-ok | MIT; Archeologist villager and camps reconciled in M4-B |
| Geckolib | 4.9.3 | M1 | Animation library (BoMD, Mowzie's Mobs, The Graveyard) | both | — | runtime-ok | MIT |
| Cloth Config API | 15.0.140 | M1 | Config library (BoMD) | both | — | runtime-ok | LGPL-3.0 |
| CERBON's API | 1.3.0 | M1 | Library for Bosses of Mass Destruction | both | — | runtime-ok | LGPL-3.0 |
| Bosses of Mass Destruction Forge | 1.3.3 | M1 | Four bosses with arenas: Night Lich tower and Void Blossom (Overworld), Gauntlet (Nether), Obsidilith (End) | both | Geckolib, Cloth Config API, CERBON's API | runtime-ok | LGPL-3.0 |
| VillagersTradingPlus | 1.1.1 | M1 | Datapack-driven villager trades (The Graveyard dependency) | both | — | runtime-ok | GPL-3.0; touches villager trades: check against MCA and our trade in M4-B and M6 |
| The Graveyard (Fabric/Neoforge) | 4.0.1 | M1 | Graveyards, crypts, haunted houses, undead, Lich boss (`graveyard:*`) | both | Geckolib, VillagersTradingPlus | runtime-ok | GPL-3.0; adds "overrun" villages (5 biomes) — M1-B village decision |
| Mowzie's Mobs | 1.8.2 | M1 | Enemies and bosses: Ferrous Wroughtnaut, Frostmaw, Umvuthi, Sculptor (`mowziesmobs:*`) | both | Geckolib | runtime-ok | Custom license: credit with a link to its CurseForge page ([licenses](licenses.md#notes-on-specific-licenses)); its own assets log two GeckoLib errors on resource load (`supernova`, `attack_single`: a `nan` in the animation expression), those two animations do not play |
| The Twilight Forest | 4.8.3345 | M1 | Adventure dimension with a boss chain, 1:1 with the Overworld (ADR-0004) | both | — | runtime-ok | CurseForge; code LGPL-2.1+, structures and sounds ARR |

**Side:** `both` / `client` / `server`.
**Status:** `candidate` → `installed` → `runtime-ok` (singleplayer launch) → `integrated` (conflicts resolved, subsystem is cohesive) → `accepted` (milestone accepted) → `mp-ok` (LAN and dedicated server verified in M10) · `rejected` (with a reason).

## Rejected mods

Mods we decided not to use. Check this list before proposing a mod; do not propose a rejected one again unless new facts change the reason.

| Mod | Rejected | Reason | Record |
| --- | --- | --- | --- |
| Bounded Not Free | 2026-11-06 | M1-A spike: with Tectonic it cannot shape terrain ("Active noise router did not expose all climate fields; missing [CONTINENTALNESS, EROSION, WEIRDNESS]"), so land reached the border and only biomes changed. Our Lithostitched-based edge falloff produced the outer ocean on the same seed. Also a very small project (~450 downloads). License CC BY-NC-SA 4.0 + modpack permission | [ADR-0004](decisions/ADR-0004-world-sizes-and-dimensions.md#amendment-2026-11-06-world-edge-spike-result) |
| L_Ender's Cataclysm | 2026-11-07 | CC BY-NC-ND 4.0: we expect add-ons and deep integration, ND forbids derivative works ([licenses](licenses.md#project-rules)) | [licenses.md](licenses.md#lenders-cataclysm) |
| CTOV (ChoiceTheorem's Overhauled Village) | 2026-11-07 | A second village source next to our own villages from M4; also CC BY-NC-ND 4.0 | [licenses.md](licenses.md#ctov-and-towns-and-towers) |
| Towns and Towers | 2026-11-07 | A second village source next to our own villages from M4; needs Cristel Lib (CC BY-NC-ND 4.0) | [licenses.md](licenses.md#ctov-and-towns-and-towers) |
| Millénaire | 2026-11-06 | Closed source and All Rights Reserved: no fork or modification, not on Modrinth. Its villagers would compete with MCA Reborn as a second NPC model, and its economy cannot be simulated off-screen without its code. Replaced by our own settlement mod. Ideas may still be studied in-game, never its code or assets | [ADR-0001](decisions/ADR-0001-own-settlement-mod.md) |

## Platform check (2026-11-06)

Highest Minecraft version with NeoForge builds on Modrinth. The pack can only move up when every base mod has moved.

| Mod | Highest NeoForge MC version |
| --- | --- |
| Ars Nouveau | 1.21.1 |
| Iron's Spells 'n Spellbooks | 1.21.1 |
| Ars 'n Spells | 1.21.1 |
| Farmer's Delight | 1.21.1 |
| Let's Do: Vinery, Farm & Charm, Meadow, HerbalBrews | 1.21.1 |
| MCA Capitals | 1.21.1 |
| Handcrafted, Supplementaries, Almost Unified | 1.21.1 |
| Mowzie's Mobs, When Dungeons Arise, Bounded Not Free | 1.21.1 |
| MCA Reborn | 26.2 |
| Occultism | 26.3 |
| KubeJS | 26.1.2 |
| Pantrywork, Dynamic Trees | 26.1.2–26.2 |
| YUNG's Better Strongholds | 26.1.2 |
| Terralith, Tectonic, Lithostitched, Dungeons and Taverns, Serene Seasons | 26.3 |
| Croptopia | 26.3 (CurseForge) |

## Known conflicts and solutions

| Mods | Problem | Solution | Recorded in |
| --- | --- | --- | --- |
| Tectonic + Bounded Not Free | BNF cannot read climate fields from the Tectonic noise router, so its rim does not shape terrain | Bounded Not Free rejected; outer ocean from our `edge_falloff` | [ADR-0004](decisions/ADR-0004-world-sizes-and-dimensions.md#amendment-2026-11-06-world-edge-spike-result) |
