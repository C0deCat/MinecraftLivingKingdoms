---
status: active
last_reviewed: 2026-11-09
---

# Compatibility matrix

Registry of the pack's mods: what is installed, why, which version and how well it is verified. Updated on every mod addition, update or removal (see the workflow in [AGENTS.md](../AGENTS.md#workflow)).

**Platform:** Minecraft 1.21.1 · NeoForge 21.1.265 (pinned 2026-11-06 in `tools/pack/src/pins.ts` and `modpack/pack.toml`) · Java 21

## Mods

`npm run verify-mods` checks this table against `modpack/`: every pack mod needs a row whose Mod cell equals the packwiz `name`, whose Version is part of the JAR file name and whose Side matches the `.pw.toml`.

| Mod | Version | Stage | Purpose | Side | Dependencies | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| spark | 1.10.124 | M0 | Profiler: TPS, tick times, heap | both | — | accepted | GPL-3.0; also useful on a dedicated server |
| ModernFix | 5.27.27 | M0 | Load time and memory fixes | both | — | accepted | LGPL-3.0 |
| FerriteCore | 7.0.3 | M0 | Memory use of block states and models | both | — | accepted | MIT; mixin "refmap could not be read" warnings at startup are expected for this build |
| Lithium | 0.15.4 | M0 | Server tick optimizations (AI, physics, chunks) | both | — | accepted | LGPL-3.0; official NeoForge build |
| Lithostitched | 1.8.1 | M1 | Worldgen compatibility layer; carries our world-edge modifiers (edge bias, erosion lift, cap) | both | — (bundles apollib) | accepted | MIT |
| Tectonic | 3.0.28 | M1 | Large-scale terrain, continents and islands | both | Lithostitched | accepted | MIT; ships its Terralith-compatible terrain variant; Modrinth marks it server-only, client optional. Tuned in `modpack/config/tectonic.json`: `ocean_offset` -0.38, `continents_scale` 0.16 (ADR-0004 amendments) |
| Terralith | 2.6.2 | M1 | Biome variety | both | Lithostitched | accepted | Stardust Labs License (see licenses.md); pinned by version URL because `packwiz modrinth add` picked the older 2.5.8 |
| Structure Layout Optimizer | 1.0.12 | M1 | Faster jigsaw structure and NBT piece generation | both | Resourceful Config | accepted | MIT; Modrinth marks it server-only, needed on the integrated server in singleplayer |
| Resourceful Config | 3.0.11 | M1 | Config library (Structure Layout Optimizer, later Better Archeology) | both | — | accepted | MIT |
| Sparse Structures | 3.0 | M1 | One config for the spread of all structures on the finite map | both | — | accepted | MIT; tuned in `modpack/config/sparsestructures.json5` (M1-C1): global factor 2 kept, strongholds 1, Wrought chambers 11, Void Blossom 4.5 ([world-content](design-docs/world-content.md)) |
| Chunky | 1.4.23 | M1 | Pre-generation for timing and worldgen checks | both | — | accepted | GPL-3.0; the pop4959 world pre-generator |
| YUNG's API | 5.1.9 | M1 | Library for YUNG's structure mods | both | — | accepted | LGPL-3.0 |
| YUNG's Better Strongholds | 5.1.3 | M1 | Stronghold with the End portal, replaces the vanilla one (`betterstrongholds:stronghold`, in `#minecraft:eye_of_ender_located`) | both | YUNG's API | accepted | LGPL-3.0; keeps the vanilla concentric rings, so the Test size has no stronghold inside its border (Test is a development size, ADR-0004 amendment 2026-11-08) |
| YUNG's Better Dungeons | 5.1.4 | M1 | Skeleton, zombie, spider and small dungeons | both | YUNG's API | accepted | LGPL-3.0 |
| YUNG's Better Mineshafts | 5.1.1 | M1 | Biome-themed mineshafts | both | YUNG's API | accepted | LGPL-3.0 |
| YUNG's Bridges | 5.1.1 | M1 | Bridges over rivers | both | YUNG's API | accepted | LGPL-3.0 |
| YUNG's Better Desert Temples | 4.1.5 | M1 | Reworked desert temple | both | YUNG's API | accepted | LGPL-3.0 |
| YUNG's Better Jungle Temples | 3.1.2 | M1 | Reworked jungle temple | both | YUNG's API | accepted | LGPL-3.0 |
| YUNG's Better Witch Huts | 4.1.1 | M1 | Reworked witch huts | both | YUNG's API | accepted | LGPL-3.0 |
| YUNG's Better Ocean Monuments | 4.1.2 | M1 | Reworked ocean monument | both | YUNG's API | accepted | LGPL-3.0 |
| YUNG's Better Nether Fortresses | 3.1.5 | M1 | Reworked Nether fortress (blaze spawners, progression) | both | YUNG's API | accepted | LGPL-3.0 |
| YUNG's Better End Island | 3.1.2 | M1 | Reworked main End island and dragon fight | both | YUNG's API | accepted | LGPL-3.0 |
| YUNG's Extras | 5.1.1 | M1 | Small vanilla+ structures and features | both | YUNG's API | accepted | LGPL-3.0 |
| When Dungeons Arise | 2.1.68 | M1 | Large dungeons and landmarks (`dungeons_arise:*`, 40 structures), mega-dungeon candidates | both | — | accepted | ARR: config and placement only ([licenses](licenses.md#project-rules)); logs "Couldn't load advancements" for two of its advancements. M1-C1: 9 large structures moved to our `livingkingdoms:mega_dungeons` set; foundry, mining_complex and plague_asylum get overworld biomes in their tags (they start in Terralith cave biomes); foundry logs a missing pool `foundry_corridor_gears` |
| Dungeons and Taverns | 4.4.4 | M1 | Taverns, small dungeons, points of interest in all three dimensions (`nova_structures:*`) | both | — | accepted | ARR: config and placement only; adds its own villages (birch, jungle, swamp, hamlet) and overrides `minecraft:village_taiga` — kept as variants of the placeholder village source until M4-B (M1-B decision); structures with item frames log "Block-attached entity at invalid position"; its advancements `minecraft:wander_add_map` and `minecraft:give_quest_trader_trade` name a parent `minecraft:root` that does not exist, so they appear in the "Couldn't load advancements" line next to the two of When Dungeons Arise |
| Structory | 1.3.14 | M1 | Ruins, chapels, manors, small points of interest (`structory:*`) | both | — | accepted | Stardust Labs License; pinned to the `1.21.x` file: the newer "26.x" universal files list 1.21.1 but Towers 1.0.17 fails to load ("Missing ModLoader") |
| Structory: Towers | 1.0.15 | M1 | Biome-themed towers and outposts (`structory_towers:*`) | both | — | accepted | Stardust Labs License; pinned to the `1.21.x` file, see Structory |
| Moog's Structure Lib (moogs_structures) | 3.4.2 | M1 | Library for Moog's structure mods | both | — | accepted | LGPL-3.0 |
| MVS - Moog's Voyager Structures | 5.1.3 | M1 | 130+ vanilla-style structures (`mvs:*`) | both | Moog's Structure Lib | accepted | MIT; universal multi-version file |
| MES - Moog's End Structures | 2.1.1 | M1 | Structures on the outer End islands (`mes:*`) | both | Moog's Structure Lib | accepted | LGPL-3.0; universal multi-version file |
| Architectury API | 13.0.11 | M1 | Library (Better Archeology) | both | — | accepted | LGPL-3.0 |
| Better Archeology | 1.3.9 | M1 | Excavation sites, catacombs, fossils, artifacts, Archeologist villager | both | Architectury API, Resourceful Config | accepted | MIT; Archeologist villager and camps reconciled in M4-B |
| Geckolib | 4.9.3 | M1 | Animation library (BoMD, Mowzie's Mobs, The Graveyard) | both | — | accepted | MIT |
| Cloth Config API | 15.0.140 | M1 | Config library (BoMD) | both | — | accepted | LGPL-3.0 |
| CERBON's API | 1.3.0 | M1 | Library for Bosses of Mass Destruction | both | — | accepted | LGPL-3.0 |
| Bosses of Mass Destruction Forge | 1.3.3 | M1 | Four bosses with arenas: Night Lich tower and Void Blossom (Overworld), Gauntlet (Nether), Obsidilith (End) | both | Geckolib, Cloth Config API, CERBON's API | accepted | LGPL-3.0; M1-C1: the Lich tower moved to our `livingkingdoms:night_lich_tower` set (2 / 3 / 4 per Small / Medium / Large), Void Blossom rarer via Sparse Structures |
| VillagersTradingPlus | 1.1.1 | M1 | Datapack-driven villager trades (The Graveyard dependency) | both | — | accepted | GPL-3.0; touches villager trades: check against MCA and our trade in M4-B and M6 |
| The Graveyard (Fabric/Neoforge) | 4.0.1 | M1 | Graveyards, crypts, haunted houses, undead, Lich boss (`graveyard:*`) | both | Geckolib, VillagersTradingPlus | accepted | GPL-3.0; adds "overrun" villages (5 biomes) — M1-B village decision |
| Mowzie's Mobs | 1.8.2 | M1 | Enemies and bosses: Ferrous Wroughtnaut, Frostmaw, Umvuthi, Sculptor (`mowziesmobs:*`) | both | Geckolib | accepted | Custom license: credit with a link to its CurseForge page ([licenses](licenses.md#notes-on-specific-licenses)); its own assets log two GeckoLib errors on resource load (`supernova`, `attack_single`: a `nan` in the animation expression), those two animations do not play |
| Sodium | 0.8.13 | M1 | Renderer: chunk rendering and FPS | client | — | accepted | PolyForm Shield 1.0.0 (non-compete, modpacks allowed); client only, the test server runs without it; gameplay verified with the GeckoLib mods 2026-11-08 |
| Iris Shaders | 1.8.14-beta.1 | M1 | Shader loader for the M1-C2 shader pack | client | Sodium | accepted | LGPL-3.0; beta build for NeoForge 1.21.1; shader packs in `modpack/shaderpacks/`; `config/iris.properties` (preserved) turns Complementary Reimagined on for new installs |
| Complementary Shaders - Reimagined | r5.9.3 | M1 | M1-C2 shader pack candidate: vanilla-like and fantasy look, presets from potato to high | client | Iris | accepted | Shader pack (`shaderpacks/`), optional file; the default since 2026-11-10 (enabled by `config/iris.properties`); credit on the pack page required (TD-23); High profile 92 FPS with steady frame times on the host PC |
| Photon Shaders | v1.4 | M1 | M1-C2 shader pack candidate: semi-realistic, supports Distant Horizons and Voxy | client | Iris | accepted | Shader pack (`shaderpacks/`), optional file; the alternative to the default since 2026-11-10; High profile 114 FPS but p99.5 at 26 FPS on the host PC |
| The Twilight Forest | 4.8.3345 | M1 | Adventure dimension with a boss chain, 1:1 with the Overworld (ADR-0004) | both | — | accepted | CurseForge; code LGPL-2.1+, structures and sounds ARR |
| MCA Reborn | 7.7.36 | M4 | The only representation of a resident: body, personality, family, appearance, dialogue ([ADR-0001](decisions/ADR-0001-own-settlement-mod.md)) | both | — | runtime-ok | GPL-3.0-only; on the M4 track only until its merge ([ADR-0006](decisions/ADR-0006-parallel-tracks.md)); Modrinth marks 7.7.36 a release although its file is named `7.7.36-beta.3`; replaces vanilla villagers; `config/mca.json` turns the first-join destiny screen off (it pauses the game); our mod `livingkingdoms_settlements` patches one of its methods ([ADR-0030](decisions/ADR-0030-resident-integration.md)); notes in [mca-reborn-7.7](references/mca-reborn-7.7.md) |

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
