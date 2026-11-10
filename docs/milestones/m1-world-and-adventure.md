---
status: stable
last_reviewed: 2026-11-10
---

# M1 — Finite world generation and adventure progression

## Goal

A beautiful, bounded and fully completable adventure world. Later stages extend its content instead of redesigning the base geography.

## Playable result

"Adventure World": explore a finite fantasy map with dungeons, bosses and complete vanilla progression.

## Mods

| Status | Mod | Role |
| --- | --- | --- |
| Rejected | [Bounded Not Free](https://www.curseforge.com/minecraft/mc-mods/bounded-not-free) | Cannot shape terrain with Tectonic; replaced by our `edge_falloff` (ADR-0004 amendment) |
| Base | [Terralith](https://modrinth.com/mod/terralith) | Biome variety |
| Base | [Tectonic](https://modrinth.com/mod/tectonic) | Large-scale terrain; includes Terralith compatibility |
| Base | [Lithostitched](https://modrinth.com/mod/lithostitched) | Worldgen compatibility layer (Tectonic dependency); carries our world-edge modifiers |
| Base | [YUNG's Better Strongholds](https://modrinth.com/mod/yungs-better-strongholds) + YUNG's API | Strongholds with the End portal |
| Base | YUNG's Better Dungeons, Mineshafts, Bridges, Desert Temples, Jungle Temples, Witch Huts, Ocean Monuments, Nether Fortresses, End Island, Extras | Reworked vanilla structures; LGPL-3.0 |
| Base | [When Dungeons Arise](https://modrinth.com/mod/when-dungeons-arise) | Large structures, candidates for 1–2 mega-dungeons; ARR, config and placement changes only |
| Base | [Dungeons and Taverns](https://modrinth.com/mod/dungeons-and-taverns) | Inns, small dungeons, points of interest; ARR, config and placement changes only |
| Base | [Structory](https://modrinth.com/mod/structory) + [Structory: Towers](https://modrinth.com/mod/structory-towers) | Ruins, small points of interest, biome-themed towers |
| Base | [Moog's Voyager Structures](https://modrinth.com/mod/moogs-voyager-structures) + [Moog's End Structures](https://modrinth.com/mod/mes-moogs-end-structures) | Vanilla-style structures between the large ones; content for the outer End islands |
| Base | [Better Archeology](https://modrinth.com/mod/better-archeology) | Excavation sites, artifacts, fossils; its Archeologist villager is reconciled in M4-B |
| Base | [Bosses of Mass Destruction](https://modrinth.com/mod/bosses-of-mass-destruction-forge) | Four bosses with their own arenas in the Overworld, Nether and End; LGPL-3.0 |
| Base | [The Graveyard](https://modrinth.com/mod/the-graveyard-fabric) | Graveyards, crypts, undead, a Lich boss; GPL-3.0 |
| Base | [Mowzie's Mobs](https://modrinth.com/mod/mowzies-mobs) | Expressive enemies and bosses; custom license, credit required ([licenses](../licenses.md#notes-on-specific-licenses)) |
| Base | [The Twilight Forest](https://www.curseforge.com/minecraft/mc-mods/the-twilight-forest) | Adventure dimension with a boss chain, 1:1 with the Overworld |
| Tool | [Chunky](https://github.com/pop4959/Chunky) (pop4959) | Pre-generation and checks; not the ore-"chunks" mod with the same name |
| Tool | [Structure Layout Optimizer](https://modrinth.com/mod/structure-layout-optimizer), [Sparse Structures](https://modrinth.com/mod/sparsestructures) | Faster jigsaw generation; one config for structure density on the finite map |
| Base (client) | [Sodium](https://modrinth.com/mod/sodium) + [Iris](https://modrinth.com/mod/iris) | Renderer and shader loader, installed in M1-B (2026-11-08). NeoForge 1.21.1 builds exist (Sodium 0.8.13, Iris 1.8.14 beta, checked 2026-11-08). Sodium is PolyForm Shield 1.0.0 (non-compete, modpacks allowed), Iris LGPL-3.0. Embeddium is the fallback renderer (LGPL, last 1.21.1 file 2025-01); Oculus has no 1.21.1 build |
| Base (client, optional) | Shader packs [Complementary Reimagined](https://modrinth.com/shader/complementary-reimagined) (default) and [Photon](https://modrinth.com/shader/photon-shader) (alternative) | Chosen in M1-C2; optional packwiz files, so the pack runs without them. Custom licenses: downloaded from Modrinth, never shipped as files; Complementary must be credited (TD-23) |

Rejected for M1 (2026-11-07): CTOV and Towns and Towers (a second village source next to ours), L_Ender's Cataclysm (ND license) — see [Rejected mods](../compatibility-matrix.md#rejected-mods). Considered and not taken now: The Aether, Deeper and Darker (each new dimension needs border code and validator coverage), Illager Invasion, Mutant Monsters, Artifacts.

## Phases

**A — foundation.** The base generator and the structure and adventure mods, in two steps.

**A1 — base generator.** Fixed world sizes chosen on our own creation screen: Test 2,048, Small 8,192, Medium 12,288, Large 16,384 (default), each a square including an outer ocean: open ocean on the outer ~400 blocks, coasts formed by the generator 300–2,500 blocks in, depending on the size. Per-size Nether ratio (1:4 / 1:4 / 1:6 / 1:8), Twilight Forest 1:1, End bounded like the Overworld; vanilla world types removed ([ADR-0004](../decisions/ADR-0004-world-sizes-and-dimensions.md), D-18). Continents and islands inside the outer ocean, natural limits by coasts and mountain ranges, climate zones. The simulation grid of 128 × 128-block cells scales with the size (16² to 128² cells). Required variety: mountains, large forests, rivers, plains, coasts, cold and warm areas, several large biome masses. No rule that every biome must exist.

Done 2026-11-07: gameplay verified on Large, the other sizes on the test server ([exec plan](../exec-plans/completed/2026-11-06-m1-world.md)).

**A2 — structure and adventure mods.** The Base and Tool mods of the table above that are not installed yet, in small batches agreed with the user (2026-11-07), each launched on a finite world ([exec plan](../exec-plans/completed/2026-11-07-m1-structures.md)). Worldgen of later milestones (wild crops of Farmer's Delight, Croptopia and Let's Do; Iron's, Ars and Occultism structures and ores) is installed with those milestones, not here (2026-11-07, with the user); Dynamic Trees is a spike in M7 (TD-06). Until M11 (release) the world is disposable: test worlds are recreated, saves are not migrated. A mod added later that generates plants, ores, temples or mob structures reopens M1 for a targeted regression, not a redesign.

Done 2026-11-08: all Base and Tool mods installed, gameplay verified on Large for the visited structures, Test and Large checked on 4 seeds on the test server ([exec plan](../exec-plans/completed/2026-11-07-m1-structures.md)).

**B — reconcile.** Tectonic + Terralith + our world edge together; surface structures on steep terrain; overlapping towers and ruins; reachable entrances; conflicting stronghold changes; duplicate architecture generators. Villages until M4 (decided 2026-11-08): vanilla villages are the placeholder source, with the Dungeons and Taverns variants (taiga override; birch, jungle and swamp villages, which vanilla lacks) and Terralith's fortified villages, all reconciled in M4-B; The Graveyard's overrun villages stay as hostile ruins (CTOV and Towns and Towers are rejected). Overlaps and placement are measured with `/lkworld structures` and fixed with Lithostitched modifiers in our world mod. Test is a development size, not part of the completability targets ([ADR-0004](../decisions/ADR-0004-world-sizes-and-dimensions.md#amendment-2026-11-08-test-is-a-development-size)). Plan: [exec plan](../exec-plans/completed/2026-11-08-m1-reconcile.md). Install the renderer (Sodium + Iris) as client-side mods and check it against the GeckoLib mods (bosses, Mowzie's Mobs) and the structures; take a new client FPS and spark reading, since earlier readings are without it (2026-11-08, with the user).

Done 2026-11-09: the census found rare overlaps and mostly false steep flags, so no placement modifier was needed; D&T villages stay as placeholder variants; the renderer is gameplay verified by the user on Large. One placement defect, the hanging D&T bunker entrance, is fixed in C1; other leftovers are TD-15 to TD-20 ([exec plan](../exec-plans/completed/2026-11-08-m1-reconcile.md)).

**C — integrate.** The content manifest with its validator, then the visual profile, in two steps, in one [exec plan](../exec-plans/completed/2026-11-09-m1-integrate.md).

**C1 — World Content Manifest.** Our own content manifest plus a worldgen validator. Target requirements (tunable hypotheses):
- at least one reachable Stronghold with a valid portal;
- at least one reachable chain for blaze rods and the other End resources;
- at least 2 mega-dungeons, apart from each other and from settlements;
- 3–5 large adventure areas of different nature and difficulty;
- all mandatory places and ingredients for modded bosses reachable inside their dimensions;
- reserved areas for 30–60 settlements, 6–10 starting states, magic schools and workshops (project scale, not generated now).

The validator checks structure presence, location, physical access and input resources. A single `locate` is not a completability check. If a structure is missing: another seed, overridden generation conditions, or a safely placed template. It is built on the `/lkworld structures` census of M1-B and takes over its numbers: entrances buried or floating, steep pieces, water share, and overlaps between structures of different sets, which a set's single `exclusion_zone` cannot prevent (seen: the D&T bunker cut into a Terralith fortified village, 2026-11-09). Unverified facts for it were TD-13, TD-16, TD-17 and TD-18.

Rules for C1 (2026-11-09, with the user):
- **Counts are researched.** How many structures of each kind a world gets differs between seeds, so categories, per-size targets and the mega-dungeon list come from measurements over several seeds and sizes. The aim: frequent and varied rewards for exploring without cluttering the world. The user agrees the numbers before they enter the manifest.
- **Systematic gaps are fixed in generation.** A mandatory structure missing on a size across seeds is fixed within C1 (overridden generation conditions or a safely placed structure), not only by choosing another seed.
- **Access is geometric, per category.** No pathfinding. Each manifest category says which checks apply: a ruin may have a partly buried or awkward entrance as long as it is logically intact; what must not hang in the air does not hang.
- **The manifest is data of the world mod** (`data/livingkingdoms/content_manifest/`), so the in-game validator reads it and later milestones add entries the same way.

Structure polish, also in C1 (from M1-B, 2026-11-09, with the user): a `livingkingdoms_world:foundation` structure processor (registered types use the mod id namespace) in our world mod. In a piece's bottom layer it either extends hanging columns down to the ground or drops them (`mode` extend / drop, `block`, `max_depth`); it only touches columns inside the chunk being generated, and skips `terrain_matching` pieces. It is attached to other mods' pools by id with Lithostitched `set_pool_element_processors`, never by copying their files. First target: the Dungeons and Taverns bunker start pool `nova_structures:bunker_enterance` (a ruin of pillars that hangs over slopes, since the bunker is `rigid` with `terrain_adaptation: none`); then other entrances the census flags. Rejected for this: a wrapper structure with `beard_thin` (applies to all 94 bunker pieces, including underground rooms) and a relief placement condition (hides the defect, fewer bunkers in mountains).

State 2026-11-09: the manifest (20 entries: vanilla progression, mega-dungeons, adventure areas, every boss place, the Twilight Forest chain), `/lkworld validate` and `npm run validate-world` are done; 0 critical gaps on Small, Medium and Large × 4 seeds, after fixes in generation: strongholds, mega-dungeons, the Night Lich tower and four boss places get fixed counts per size by our `fixed_count` placement, and every world has at least 25% land. Counts and how they are made: [world-content](../design-docs/world-content.md). The processor fixes the bunker entrance (gameplay verified by the user on Large, 2026-11-10). Other hanging pieces are warnings, TD-22. Guaranteed points, the land lift and a noise height grid are computed once at world creation and stored in the save. Worldgen checks follow D-21: 3 Large worlds during the phases, 12 worlds at acceptance. C1 done 2026-11-10.

**C2 — visual profile.** Choose the pack's shader pack before M1 is accepted, so the world is judged in its final look (2026-11-08, with the user): 1–2 candidates from Modrinth, licenses recorded, settings, FPS on the host PC. The shader pack is an optional packwiz file; the pack must look and run acceptably without it. Weather, seasons, fog and Distant Horizons are re-checked with it in [M7](m7-atmosphere.md).

Chosen 2026-11-10 with the user: **Complementary Reimagined** is the default shader pack, enabled for new installs by the pack's `config/iris.properties` (kept by packwiz-installer once the player changes it); **Photon** stays as an alternative. Both are optional client files downloaded from Modrinth; without them Iris runs with shaders off. Measured on the host PC: about 600 FPS in the open without shaders, 92 FPS with Complementary on High (`quality.md`). C2 done 2026-11-10.

**D — accept.** See [Acceptance](#acceptance) below; the world is accepted with the renderer and the chosen shader pack enabled ([exec plan](../exec-plans/completed/2026-11-10-m1-accept.md)).

Decided 2026-11-10 with the user:
- Surface pieces that hang over slopes are fixed before acceptance (the outcome is below: most flags came from the scan, and the processor covers ruin towns, bathhouse bases and campsite tents; Typhon was left as is after a look in game); underground bunker hallways and rooms and `graveyard:crypt` overlaps stay warnings for M7 (TD-22).
- End gateways and outer islands after the dragon (was TD-13) are not checked separately: End cities and Obsidilith arenas are validated inside the End border on every size, and gateways are vanilla behaviour.
- Teleport stalls into fresh terrain (was TD-20) are not a defect of the finished pack: D-20 pre-generates every world at creation (TD-21).
- The slow `/locate` of large WDA structures and `/locate` results beyond the border do not block; they are TD-15 for M11.
- The gameplay bar is a creative and teleport checklist by the user on a fresh Large world, not a survival run.

Done 2026-11-10, M1 accepted in singleplayer. The hanging scan no longer counts `terrain_matching` pieces and allows for beards, which removed most surface warnings (they came from the scan, not the world); the `foundation` processor now also covers D&T ruin towns and WDA bathhouse bases and campsite tents. The `fixed_count` search at world creation runs on 4 threads, after a 4 GB test server ran out of heap, and the pack allocates 8 GB to the game (D-22). Leftovers: TD-22 (underground pieces, M7), TD-15 (`/locate`, M11).

## Acceptance

- A chosen seed produces a reproducible, varied map.
- Core Minecraft progression and key modded adventures are possible.
- The validator report has no critical gaps on Small, Medium and Large × 4 seeds (`npm run validate-world -- --full`, D-21).
- Generating and loading new chunks shows no critical errors; performance measured on the host PC, with and without the shader pack.

Result 2026-11-10 ([exec plan](../exec-plans/completed/2026-11-10-m1-accept.md)):
- Reproducible: Large `livingkingdoms` created twice gives the same entries, counts and positions; varied: 17–19 kinds of adventure areas and 110 surface biomes on a Large world.
- Progression gameplay verified by the user on a fresh Large world (random seed, Complementary on): eye of ender to a stronghold, a YUNG's Nether fortress, the dragon killed, the Naga courtyard and the Lich tower in the Twilight Forest, a WDA monastery, a BoMD arena and a Mowzie's place; the fixed ruin town, bathhouse and campsite and an untouched Typhon look right.
- `npm run validate-world -- --full`: 0 critical gaps on all 12 worlds; the user's world: 0 critical, 82 warnings.
- No new ERROR/FATAL lines in generation; spark on the host PC with and without the shader pack in [quality](../quality.md#performance-baselines).

## Out of scope

- Generating all future towns.
- Every possible biome.

## Open questions

- Generation on more seeds: answered in C1, every size validated on 4 seeds (`livingkingdoms`, `alpha`, `20261107`, `kingdoms`) with no critical gap; any seed is validated the same way with `npm run validate-world`. Test is a development size and is not validated (ADR-0004 amendment 2026-11-08).
- `/locate` reports structures beyond the border (found in M1-A2): moved to TD-15 (M11) in M1-D; the other M1-A2 findings were settled in M1-B or moved to TD-15 to TD-19.
- Write the list of mandatory structures, biome roles and progression paths including Nether, End and Twilight Forest, with per-size counts: answered in C1 by the World Content Manifest and [world-content](../design-docs/world-content.md); settlement and state counts stay TD-10.
- Does the Twilight Forest boss chain generate correctly at 1:1 on the Test and Small sizes? Answered in C1 for Small, Medium and Large: the whole chain is present on every validated world; Test is not validated.
