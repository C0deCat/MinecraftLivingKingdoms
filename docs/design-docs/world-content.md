---
status: active
last_reviewed: 2026-11-09
---

# World content: structure density and the World Content Manifest

What the finite world contains, how dense it is, and which content is mandatory. The manifest (M1-C1) turns the agreed numbers into checks; later milestones add their structures the same way (M3 magic places, M4 settlements). The measurements are facts; changes 1–5 were agreed with the user on 2026-11-09 and are measured below ([exec plan](../exec-plans/active/2026-11-09-m1-integrate.md), steps 5 and 5a).

## How it was measured

- Pack of 2026-11-09 (M1-B state plus the `foundation` processor), Sparse Structures at its default `spreadFactor` 2 (every structure set's spacing doubled, so about a quarter of the mods' own density).
- Test server, fresh worlds: Small, Medium, Large × seeds `livingkingdoms`, `alpha`, `20261107`, `kingdoms`; `/lkworld structures border` (all starts inside the Overworld border, from the generator, no chunks generated) and `/lkworld map` (height grid). Analysis: `tools/pack/src/structure-density.ts` and the run notes in the exec plan.
- Land is the share of the height grid at or above sea level. "Distance" is from 2,000 random land points to the nearest entrance (start piece) of a structure; walking speed is about 4.3 blocks/s.
- Tiers by the largest side of the start's bounding box (a jigsaw box includes empty space, so this is rough): minor < 48, notable 48–139, major ≥ 140. Decorations (Moog's piles, haystacks, lanterns, trees, carts, benches, ponds; The Graveyard's dead and memorial trees; buried treasure) are not counted as rewards.

## Findings

**Density is the same on every size.** Starts grow with the map area: Small 1,343–1,579, Medium 3,586–3,826, Large 6,702–7,028 inside the border. On land, surface structures only:

| Tier | Per 1,000² blocks of land | Median distance | p90 |
| --- | --- | --- | --- |
| Minor | 17–24 | 121–142 blocks (~30 s walk) | 233–272 |
| Notable | 2.0–3.3 | 325–412 (~85 s) | 617–872 |
| Major | 0.7–1.2 | 487–690 (~2.3 min) | 873–1,867 |

About 80% of surface starts have at least two other surface starts within 256 blocks; the crowding is in the minor tier (Moog's small structures alone are ~13 per 1,000² of land).

**Land share varies by seed on Small.** 36%, 35%, 28% and 10% (seed `20261107`: 6.6 M blocks² of land, a few islands). Medium 31–46%, Large 38–49%.

**Rare or missing (per world, min–max over the four seeds):**

| Structure | Small | Medium | Large |
| --- | --- | --- | --- |
| Stronghold (`betterstrongholds:stronghold`) | 0–1 | 0–3 | 2–7 |
| Night Lich tower (BoMD) | 0–1 | 0–1 | 1–2 |
| Woodland mansion | 0 | 0–1 | 0–2 |
| YUNG's witch hut | 0 | 0–1 | 0–1 |
| WDA keep_kayra, shiraz_palace, mechanical_nest, thornborn_towers | 0 | 0–1 | 0–2 |
| WDA plague_asylum, foundry, mining_complex, mining_system, giant_mushroom, ceryneian_hind | 0 | 0 | 0 |

Causes found:
- YUNG's strongholds use their own spread placement (spacing 85 chunks, first ring at 80 chunks ≈ 1,280 blocks from the origin), doubled by Sparse Structures, so a Small or Medium map has room for very few, and some fall in the ocean.
- All 32 WDA "major" structures share one structure set (spacing 50 chunks, doubled): each region gets one of them by weight, so a single large one is rare and some never win a region in our biomes.

**Plentiful, including bosses (per world):**

| Structure | Small | Medium | Large |
| --- | --- | --- | --- |
| Wrought chamber (Mowzie's Ferrous Wroughtnaut boss) | 21–61 | 123–175 | 251–342 |
| Void Blossom (BoMD boss) | 14–16 | 29–31 | 46–57 |
| Trial chambers | 48–58 | 108–126 | 196–209 |
| Ancient city | 1–8 | 10–25 | 30–43 |
| Mowzie's monastery (Sculptor), Umvuthana grove, Frostmaw | 0–4 each | 1–13 each | 3–18 each |
| D&T bunker | 4–11 | 14–21 | 30–42 |

**Other dimensions** (Small, seed `livingkingdoms`): Nether 4 YUNG's fortresses, 3 bastions, 4 Gauntlet arenas; End 15 End cities, 15 Obsidilith arenas; Twilight Forest at 1:1 the whole boss chain, at least 16 of each boss structure.

## Agreed changes

1. **Overall density stays.** Minor content every ~130 blocks, notable every ~350, major every ~600 on land is frequent and varied. The clutter is the minor tier; thin the Moog's decorations only if the user finds them crowded in the client.
2. **Strongholds:** at least 2 reachable on every size. A Sparse Structures factor of 1 still left one Small seed with a single stronghold and up to 23 on Large, so they moved to our own placement (below).
3. **Mega-dungeons:** the largest WDA structures in their own structure set (our data, by id; removed from WDA's shared set with Lithostitched), spaced so Small gets at least 2 and Large about 6–10, apart from each other and from villages. The final list of 9 is below.
4. **Bosses as rarities:** Wrought chambers and Void Blossoms about 5–15 per Large world (in proportion on smaller sizes), and at least 1 Night Lich tower on every size.
5. **Land share:** players choose any seed and every world is pre-generated at creation (D-20), so the generator itself guarantees a minimum land share; the validator reports it.

## How it is implemented

- **Rarity:** Sparse Structures factors in `modpack/config/sparsestructures.json5` (Wrought chambers 11, Void Blossom 4.5).
- **Guaranteed content:** our placement type `livingkingdoms_world:fixed_count` places a set number of starts per world size: the map inside the open-ocean ring is split into a grid of about that many cells, each chosen cell gets one point moved to a preferred biome. Our structure sets in `livingkingdoms-world` use it; the structures are removed from their mods' own sets with Lithostitched:

  | Set | Structures | Small / Medium / Large |
  | --- | --- | --- |
  | `strongholds` | YUNG's stronghold | 3 / 4 / 6 |
  | `mega_dungeons` | WDA monastery, mechanical_nest, scorched_mines, thornborn_towers, keep_kayra, shiraz_palace | 2 / 3 / 5 |
  | `mega_dungeons_underground` | WDA plague_asylum, foundry, mining_complex | 1 / 1 / 2 |
  | `night_lich_tower` | BoMD Lich tower | 2 / 3 / 4 |
  | `boss_guarantee_*` | Mowzie's Umvuthana grove, Frostmaw, Sculptor monastery; The Graveyard altar | 1 / 1 / 1, on top of their mods' own sets |

  A point is taken only where one of the set's structures really generates and stands on the ground (the validator's own measurement; WDA's flying mechanical_nest is exempt): up to 8 random points per cell moved to a preferred biome, then a 128-block lattice over the cell with the biome read at the surface (biomes are 3D: under a mountain, y 64 is an underground biome). The count is missed only if a whole cell has no fitting place; the validator checks the minimum. The search runs once, when a new save's level loads, all sets and cells in parallel, and its points are stored in the save (`data/livingkingdoms_fixed_count.dat` of the dimension); a loaded world reads them and never searches again (decided with the user, 2026-11-09), so a later change of the code or the counts cannot move structures in a generated world.
- **Cold bosses on small maps.** A Small map can have almost no open snowy land (seed `kingdoms`: 2.7% cold biomes, no snowy plains or ice spikes). The Graveyard altar (snowy plains and ice spikes only) and Frostmaw (Mowzie's config excluded snowy forests and taigas) then had no place. Our tag file adds snowy taiga, snowy slopes, grove, frozen peaks and Terralith cold biomes to the altar's biomes, and `modpack/config/mowziesmobs-common.toml` allows Frostmaw in snowy forests and taigas.
- **Biome checks underground.** YUNG's stronghold and the three underground WDA structures check their biome at their start height, where Terralith places its cave biomes, which their tags lack; the Lich tower checks at the chunk center just under the surface. These structures are placed only by our sets, whose points already sit in fitting surface biomes, so our tag files add `#minecraft:is_overworld` to their biome tags. Without that, WDA's underground ones never generated and most stronghold points failed.
- **`abandoned_temple`** is in WDA's minor set as well and common (7–14 per Large world), so it is not a mega-dungeon.
- **Locating.** Vanilla structure search (`/locate`, eyes of ender, explorer maps) only knows concentric rings and random spread; a mixin in our world mod adds the `fixed_count` points to it.
- **Land share:** `LandLift` ([ADR-0004 amendment](../decisions/ADR-0004-world-sizes-and-dimensions.md#amendment-2026-11-09-minimum-land-share)).
- **Result** (2026-11-09, Small / Medium / Large × 4 seeds): 0 critical gaps on all 12 worlds. Strongholds, mega-dungeons and Lich towers exactly at the set counts; Wroughtnaut 2–3 / 4–8 / 6–13, Void Blossom 3–4 / 4–6 / 7–14; reachable adventure areas 90–97 / 192–232 / 402–461 of 19 kinds. Open warnings: hanging pieces of some structures and underground overlaps (TD-22).

## Manifest categories

The manifest is data of the world mod, `data/livingkingdoms/content_manifest/*.json`, one entry per file; the format is documented in `ContentManifest.java`. `/lkworld validate` checks a world against it and `npm run validate-world` runs it over several seeds. Access rules are geometric (decided 2026-11-09):

| Category | Examples | Access rule |
| --- | --- | --- |
| progression | stronghold, fortress, bastion, End city, ocean monument | present, inside the border; entrance any (underground by design) |
| boss | BoMD arenas, Mowzie's bosses, The Graveyard Lich altar, Twilight Forest chain | present, inside the border; surface ones must not hang |
| mega_dungeon | the list above | ≥ 2, apart; entrance `ok`; must not hang |
| adventure_area | WDA notable, D&T shrines, bunkers, ruin towns | entrance `ok` or `buried_ok`; must not hang |
| ruin | Structory, Terralith rubble, D&T wild ruins | entrance any; must not hang |
| poi, decor | Moog's small structures, wells, camps | counted, not checked |
| underground, aquatic, sky | Terralith underground, shipwrecks, floating islands | by design; counted |
| village_placeholder | vanilla, D&T and Terralith villages | counted; reconciled in M4-B |
