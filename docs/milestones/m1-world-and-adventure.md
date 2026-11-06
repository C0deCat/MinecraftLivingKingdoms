---
status: active
last_reviewed: 2026-11-06
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
| Base | [When Dungeons Arise](https://www.curseforge.com/minecraft/mc-mods/when-dungeons-arise) | Large structures, candidates for 1–2 mega-dungeons |
| Base | [Dungeons and Taverns](https://modrinth.com/mod/dungeons-and-taverns) | Inns, small dungeons, points of interest |
| Candidate | YUNG's Better Dungeons / Mineshafts / Bridges | Extra structures; check spawning on a finite map |
| Candidate | [Mowzie's Mobs](https://modrinth.com/mod/mowzies-mobs) | Expressive enemies and bosses |
| Base | [The Twilight Forest](https://www.curseforge.com/minecraft/mc-mods/the-twilight-forest) | Adventure dimension with a boss chain, 1:1 with the Overworld |
| Candidate | [CTOV](https://modrinth.com/mod/ct-overhaul-village), Towns and Towers, Structory | Town architecture; may conflict with our villages (M4); CTOV is CC BY-NC-ND |
| Tool | [Chunky](https://github.com/pop4959/Chunky) (pop4959) | Pre-generation and checks; not the ore-"chunks" mod with the same name |

## Phases

**A — base generator.** Fixed world sizes chosen on our own creation screen: Test 2,048, Small 8,192, Medium 12,288, Large 16,384 (default), each a square including an outer ocean ring of about 1,024 blocks. Per-size Nether ratio (1:4 / 1:4 / 1:6 / 1:8), Twilight Forest 1:1, End bounded like the Overworld; vanilla world types removed ([ADR-0004](../decisions/ADR-0004-world-sizes-and-dimensions.md), D-18). Continents and islands inside the ring, natural limits by coasts and mountain ranges, climate zones. The simulation grid of 128 × 128-block cells scales with the size (16² to 128² cells). Required variety: mountains, large forests, rivers, plains, coasts, cold and warm areas, several large biome masses. No rule that every biome must exist.

**B — resolve contradictions.** Tectonic + Terralith + Bounded Not Free together; surface structures on steep terrain; overlapping towers and ruins; reachable entrances; conflicting stronghold changes; duplicate architecture generators. Decide whether vanilla villages and CTOV stay or their areas are reserved for our villages.

Mods that change worldgen in later stages (Croptopia crops, Iron's towers, M4 villages, Dynamic Trees in M7) should be decided and installed here already. Until M11 (release) the world is disposable: test worlds are recreated, saves are not migrated. A mod added later that generates plants, ores, temples or mob structures reopens M1 for a targeted regression, not a redesign.

**C — World Content Manifest.** Our own content manifest plus a worldgen validator. Target requirements (tunable hypotheses):
- at least one reachable Stronghold with a valid portal;
- at least one reachable chain for blaze rods and the other End resources;
- at least 2 mega-dungeons, apart from each other and from settlements;
- 3–5 large adventure areas of different nature and difficulty;
- all mandatory places and ingredients for modded bosses reachable inside their dimensions;
- reserved areas for 30–60 settlements, 6–10 starting states, magic schools and workshops (project scale, not generated now).

The validator checks structure presence, location, physical access and input resources. A single `locate` is not a completability check. If a structure is missing: another seed, overridden generation conditions, or a safely placed template.

## Acceptance

- A chosen seed produces a reproducible, varied map.
- Core Minecraft progression and key modded adventures are possible.
- The validator report has no critical gaps.
- Generating and loading new chunks shows no critical errors; performance measured on the host PC.

## Out of scope

- Generating all future towns.
- Every possible biome.

## Open questions

- Pin JARs and dependencies of Terralith, Tectonic, YUNG's, Dungeons and Taverns, When Dungeons Arise; test generation on 3–5 seeds per size.
- Write the list of mandatory structures, biome roles and progression paths including Nether, End and Twilight Forest, with per-size counts (TD-10).
- Does the Twilight Forest boss chain generate correctly at 1:1 on the Test size?
