---
status: active
last_reviewed: 2026-11-09
---

# M1-C — integrate: World Content Manifest, validator, structure polish, shader pack

**Status:** Active

## Goal

The finite world is checked, not assumed, to be completable, and is judged in its final look. Done means:
- C1: a World Content Manifest in our world mod and a `/lkworld validate` command that reports presence, counts, location, physical access and input resources of the mandatory content on Small, Medium and Large over several seeds, with no critical gaps left; structure density chosen from measurements; the hanging D&T bunker entrance fixed by our `foundation` processor (gameplay verified by the user);
- C2: a shader pack chosen with the user, installed as an optional client file, measured with and without it (gameplay verified).

## Context

- Milestone: [M1 — World and adventure](../../milestones/m1-world-and-adventure.md), phase C (C1, C2); follows [M1-B](../completed/2026-11-08-m1-reconcile.md), whose census is the base of the validator.
- World sizes, Test as a development size: [ADR-0004](../../decisions/ADR-0004-world-sizes-and-dimensions.md#amendment-2026-11-08-test-is-a-development-size).
- Changing other mods' structures: [licenses.md](../../licenses.md#project-rules) (placement only for ARR mods), [Lithostitched notes](../../references/lithostitched-1.8.md#structure-modifiers).
- Open items resolved here: TD-10, TD-13, TD-16, TD-17, TD-18 in the [tech-debt tracker](../tech-debt-tracker.md); the M1 open questions on more seeds and the Twilight Forest boss chain at 1:1.
- Approved Claude Code plan of 2026-11-09; its content is copied into the steps and notes below.

## Steps

C1
- [ ] 1. Docs: this plan, decisions in the M1 spec, processor id, Lithostitched `set_pool_element_processors` fields, ARCHITECTURE manifest location.
- [ ] 2. `StructureScan`, extracted from `StructureCensus`: full-map scan inside each dimension's border over all levels (Overworld, Nether, End, Twilight Forest), loading only candidate chunks of the structure placements (`ChunkGeneratorStructureState.possibleStructureSets()`, `StructurePlacement.isStructureChunk`, ring positions); per start also the biome at the start and the template ids of jigsaw pieces. Accepted only if it reproduces the M1-B baseline (Large, `livingkingdoms`, radius 1,536: 270 starts, 5 overlapping pairs); otherwise fall back to the chunk square.
- [ ] 3. Hanging-piece metric: per piece (longer side ≥ 8, top reaching the ground) the share of its footprint where its bottom is more than 3 blocks above the noise ground; structures whose terrain adaptation fills under them (`beard_*`, `bury`, `encapsulate`) are marked. Census CSV gets `hanging_share` and the worst piece. Baseline on Large `livingkingdoms` and `alpha`.
- [ ] 4. `livingkingdoms_world:foundation` processor (`mode` extend / drop, `block`, `max_depth`; works in `finalizeProcessing`, only inside the generated chunk's box, skips `terrain_matching` pieces) and a Lithostitched `set_pool_element_processors` modifier for `nova_structures:bunker_enterance` with a `mod_loaded` predicate. Before/after hanging numbers; user check in the client at the shrine and bunker cluster (Large, seed `livingkingdoms`, near (60, −914)).
- [ ] 5. Density research on Small, Medium and Large × seeds `livingkingdoms`, `alpha`, `20261107`, `kingdoms`: starts per 1,000² blocks of land by mod and category; distance from random land points to the nearest rewarding structure (median, p90); clusters (≥ 3 surface starts within 256 blocks) and surface overlaps; spacing per set; sizes of the WDA mega-dungeon candidates. Research note with proposed categories, per-size target bands and the mega-dungeon list, agreed with the user; density changes (if any) via Sparse Structures `customSpreadFactors`, one at a time with a rerun.
- [ ] 6. Manifest: codec, reload listener, data in `data/livingkingdoms/content_manifest/` of the world mod, entries from step 5.
- [ ] 7. `/lkworld validate`: manifest evaluation plus the checks for TD-13 (End outer islands and cities inside the border), TD-16 (eye of ender leads to the stronghold), TD-17 (no D&T pieces in Nether fortresses), TD-18 (witch huts in swamps) and the Twilight Forest boss chain; reports `validation-<size>.json` and `.md` in `<world>/livingkingdoms/`, one log line `Validation <size> <seed>: N critical, N warnings -> <path>`.
- [ ] 8. `npm run validate-world -- --size <small|medium|large> --seeds a,b,c` in `tools/pack/src/`: fresh world per seed on the test server, collected table, non-zero exit on critical gaps; documented in AGENTS.md Verification.
- [ ] 9. Validation on Small, Medium and Large × 4 seeds; systematic critical gaps fixed in generation, one fix at a time with a rerun; further `foundation` targets from the census; reference-seed candidates.
- [ ] 10. Close C1: M1 spec, quality grade, tech-debt items, M1 open questions.

C2
- [ ] 11. Shader pack research: 1–2 candidates on Modrinth (for example Complementary Reimagined, BSL): license, Iris 1.8 support, cost. Ask the user before adding; optional client packwiz file downloaded from Modrinth, never shipped as a file.
- [ ] 12. Settings; FPS and spark on Large with and without it; the user judges the look; compatibility matrix, licenses, quality.
- [ ] 13. Close the plan; M1-D (acceptance) is next.

## Notes for the steps

- **Code layout** (`livingkingdoms-world`, package `livingkingdoms.world`): `StructureScan` (collection and measurement; `footprint`, `entranceState`, `overlaps` move there), `StructureCensus` keeps its CSV and summary so `/lkworld structures` stays comparable to M1-B; `FoundationProcessor` registered with a `DeferredRegister<StructureProcessorType<?>>` like the density function types; `manifest/` package; `WorldValidator` on a background thread like the other `/lkworld` commands. Other mods only through data files or `livingkingdoms.world.compat.*`.
- **Manifest entries** (per structure id or tag): `category` (for example progression, mega_dungeon, adventure_area, boss, ruin, poi, underground, aquatic, sky, village_placeholder), `dimension`, per-size `min` / `max` for Small, Medium, Large, `spacing` (to the same group, to village placeholders), `access` (entrance `ok` | `buried_ok` | `any`, maximum hanging share, water allowed), `severity` (critical | warning).
- **Groups to fill:** vanilla progression (stronghold with portal, fortress with blaze spawners, bastion, End city); ≥ 2 mega-dungeons apart from each other and from settlements; 3–5 adventure areas; boss places (four BoMD arenas, four Mowzie's bosses, The Graveyard Lich, the Twilight Forest chain from the Naga courtyard to the final castle).
- **Settlement reserve** (project scale, not generated now): the validator counts free buildable land in 128-block simulation cells (land, gentle relief, no structure) against the 30–60 settlement target; reported, not reserved.
- **APIs** are checked in the NeoForge 21.1 sources jar before use; Lithostitched fields in the installed JAR.

## Progress log

- 2026-11-09 — Plan created from the approved Claude Code plan. Checked for it: Lithostitched 1.8.1 has `set_pool_element_processors` (fields read in the installed JAR, now in the reference note); the D&T bunker start pool is `nova_structures:bunker_enterance` (`rigid`, `terrain_adaptation: none`); Sparse Structures 3.0 runs with its default config (`spreadFactor` 2) and has per-set `customSpreadFactors`. Status: docs only.
- 2026-11-09 — Steps 2–4 code: `StructureScan` (candidate chunks of all structure placements, biome, template ids, hanging share, terrain adaptation), `/lkworld structures border|all`, `/lkworld grounding <structure> [radius]` (placed blocks under an entrance, fully generated chunks), `livingkingdoms_world:foundation` processor and the `foundation_dnt_bunker` modifier; `/lkworld map` also writes `heights-<size>.csv`. Regression on a fresh Large world, seed `livingkingdoms`, radius 1,536: 270 starts, 5 overlapping pairs (3 surface), entrances 38 buried / 4 floating, the same as the M1-B baseline, with 1,875 of 37,249 chunks loaded (132 s). New hanging numbers: 16 of 200 measured surface starts have a piece more than 25% hanging; most carry a `beard_*` adaptation (vanilla fills under them), the real ones are both D&T bunkers (75% and 100%, a piece next to the entrance, not the entrance itself), the Moog's floating island (by design), an igloo and a shipwreck. Grounding of the bunker entrance, same world: 13 of 56 bottom columns hanging without the processor, 0 of 56 with it (25 and 31 columns at (99, −909) and (461, 381)); no new errors on the server; `npm run smoke` reaches the title screen with only the two known GeckoLib errors. Status: runtime passed on the server; not seen in the client yet; the other hanging bunker piece is not covered by the entrance-only processor.
- 2026-11-09 — Scan speed. Batched chunk requests (with our own `structure_starts` ticket; the one-tick ticket of async requests cancelled the generation) did not help: vanilla 1.21.1 generates structure starts one chunk at a time, ~46 ms per candidate chunk. New default `direct` method: vanilla `ChunkGenerator.createStructures` on a throwaway `ProtoChunk` per candidate chunk, in parallel. A first version that copied the vanilla selection missed the hooks of YUNG's and Moog's mods (they cancel vanilla mineshafts, monuments, strongholds, fortresses in `tryGenerateStructure`) and found 1,847 instead of 1,593 Overworld starts; calling `createStructures` itself fixed it. Small, seed `livingkingdoms`, all four dimensions: the same 3,393 starts with the same ids and positions as loading the chunks (`-Dlivingkingdoms.scan=candidates`), 232 s instead of 637 s, and the world folder stays at 5 MB. Only the piece layouts of `mowziesmobs:monastery` and `twilightforest:knight_stronghold` differ between the runs (same starts, slightly different boxes), not checked further. Status: runtime passed on the server.
- 2026-11-09 — First Small numbers (seed `livingkingdoms`, inside the border): Overworld 1,579 starts on 24.3 M blocks² of land (36%), median distance from a random land point to the nearest entrance 86 blocks (p90 160); one stronghold (`betterstrongholds`, at (−1,765, 59)); 11 villages; no WDA mega-dungeon candidate (only smaller WDA structures, 33). Nether: 4 YUNG's fortresses, 3 bastions, 4 BoMD Gauntlet arenas. End: 15 End cities, 15 Obsidilith arenas. Twilight Forest at 1:1: the whole boss chain, at least 16 of each boss structure (115 lich towers, 16 final castles), so the M1 open question on the chain at small sizes is answered for Small on this seed.

## Decision log

- 2026-11-09 — One exec plan for M1-C, C1 first, then C2 (with the user). Adding the shader pack is asked separately, since it changes the pack.
- 2026-11-09 — Structure counts are researched, not guessed (with the user): the number of structures differs between worlds, so categories, per-size targets and the mega-dungeon list come from measurements over several seeds and sizes. The aim is a good balance of exploration and adventure: frequent and varied rewards for exploring, without cluttering the world. The user agrees the numbers before they enter the manifest.
- 2026-11-09 — A mandatory structure missing systematically (on a size, across seeds) is fixed in generation within C1, not only by choosing another seed (with the user). Each fix is a worldgen change and is shown with its numbers before it is kept.
- 2026-11-09 — Physical access is checked geometrically, without pathfinding, with rules per structure category (with the user): a ruin may have a partly buried or awkward entrance as long as it is logically intact; what must not hang in the air does not hang. The manifest says which category gets which check.
- 2026-11-09 — The processor id is `livingkingdoms_world:foundation`: registered types use the mod id namespace, like `livingkingdoms_world:edge_bias`; data files keep the `livingkingdoms` namespace.
- 2026-11-09 — The manifest ships as data of the world mod (`data/livingkingdoms/content_manifest/`), since the validator runs in the game and later milestones add entries the same way; ARCHITECTURE's planned `content/manifest/` is dropped.
