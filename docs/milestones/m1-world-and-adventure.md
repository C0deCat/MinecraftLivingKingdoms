---
status: active
last_reviewed: 2026-11-07
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

Rejected for M1 (2026-11-07): CTOV and Towns and Towers (a second village source next to ours), L_Ender's Cataclysm (ND license) — see [Rejected mods](../compatibility-matrix.md#rejected-mods). Considered and not taken now: The Aether, Deeper and Darker (each new dimension needs border code and validator coverage), Illager Invasion, Mutant Monsters, Artifacts.

## Phases

**A — foundation.** The base generator and the structure and adventure mods, in two steps.

**A1 — base generator.** Fixed world sizes chosen on our own creation screen: Test 2,048, Small 8,192, Medium 12,288, Large 16,384 (default), each a square including an outer ocean: open ocean on the outer ~400 blocks, coasts formed by the generator 300–2,500 blocks in, depending on the size. Per-size Nether ratio (1:4 / 1:4 / 1:6 / 1:8), Twilight Forest 1:1, End bounded like the Overworld; vanilla world types removed ([ADR-0004](../decisions/ADR-0004-world-sizes-and-dimensions.md), D-18). Continents and islands inside the outer ocean, natural limits by coasts and mountain ranges, climate zones. The simulation grid of 128 × 128-block cells scales with the size (16² to 128² cells). Required variety: mountains, large forests, rivers, plains, coasts, cold and warm areas, several large biome masses. No rule that every biome must exist.

Done 2026-11-07: gameplay verified on Large, the other sizes on the test server ([exec plan](../exec-plans/completed/2026-11-06-m1-world.md)).

**A2 — structure and adventure mods.** The Base and Tool mods of the table above that are not installed yet, in small batches agreed with the user (2026-11-07), each launched on a finite world ([exec plan](../exec-plans/active/2026-11-07-m1-structures.md)). Worldgen of later milestones (wild crops of Farmer's Delight, Croptopia and Let's Do; Iron's, Ars and Occultism structures and ores) is installed with those milestones, not here (2026-11-07, with the user); Dynamic Trees is a spike in M7 (TD-06). Until M11 (release) the world is disposable: test worlds are recreated, saves are not migrated. A mod added later that generates plants, ores, temples or mob structures reopens M1 for a targeted regression, not a redesign.

**B — reconcile.** Tectonic + Terralith + our world edge together; surface structures on steep terrain; overlapping towers and ruins; reachable entrances; conflicting stronghold changes; duplicate architecture generators. Decide whether vanilla villages and CTOV stay or their areas are reserved for our villages.

**C — integrate: World Content Manifest.** Our own content manifest plus a worldgen validator. Target requirements (tunable hypotheses):
- at least one reachable Stronghold with a valid portal;
- at least one reachable chain for blaze rods and the other End resources;
- at least 2 mega-dungeons, apart from each other and from settlements;
- 3–5 large adventure areas of different nature and difficulty;
- all mandatory places and ingredients for modded bosses reachable inside their dimensions;
- reserved areas for 30–60 settlements, 6–10 starting states, magic schools and workshops (project scale, not generated now).

The validator checks structure presence, location, physical access and input resources. A single `locate` is not a completability check. If a structure is missing: another seed, overridden generation conditions, or a safely placed template.

**D — accept.** See [Acceptance](#acceptance) below.

## Acceptance

- A chosen seed produces a reproducible, varied map.
- Core Minecraft progression and key modded adventures are possible.
- The validator report has no critical gaps.
- Generating and loading new chunks shows no critical errors; performance measured on the host PC.

## Out of scope

- Generating all future towns.
- Every possible biome.

## Open questions

- Generation on more seeds: Test and Large checked on 4 seeds (`livingkingdoms`, `alpha`, `20261107`, `kingdoms`) for stronghold, fortress, WDA monastery and BoMD arenas; Small and Medium not yet. Test lacks a reachable stronghold and fortress (TD-14).
- For M1-B (found in M1-A2): Dungeons and Taverns adds villages (birch, jungle, swamp, hamlet, remnant) and overrides `minecraft:village_taiga`; The Graveyard adds "overrun" villages; D&T `mangrove_witch_hut` next to YUNG's witch huts; `/locate` of large WDA structures can block the server for minutes; `/locate` reports structures beyond the border.
- Write the list of mandatory structures, biome roles and progression paths including Nether, End and Twilight Forest, with per-size counts (TD-10).
- Does the Twilight Forest boss chain generate correctly at 1:1 on the Test and Small sizes?
