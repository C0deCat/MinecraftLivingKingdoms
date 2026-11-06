---
status: stable
last_reviewed: 2026-11-06
---

# ADR-0004: Fixed world sizes, dimension scales and the finite-world edge

**Status:** Accepted; amended 2026-11-06 with the world-edge spike result and a factual correction (see the end) · **Date:** 2026-11-06

## Context

D-03 assumed one 12,288 × 12,288 map. Three questions were open before M1 generates anything:
- how the Nether, the End and the Twilight Forest behave on a bounded map;
- whether 12,288 is enough when the map is continents and islands inside an outer ocean;
- how a player picks a world size at creation, like in Terraria, which also helps testing.

The user also wants a cohesive experience: the pack is only ever tested on our own presets, so vanilla world types are removed.

Facts checked on 2026-11-06:
- **Vanilla 1.21.1 border** (decompiled `MinecraftServer.createLevels`, `BorderChangeListener.DelegateBorderChangeListener`): the Overworld border is copied with the same numbers to every dimension and is not scaled by `coordinate_scale`. Portals compute `pos × scale` and clamp to the target dimension's border (`NetherPortalBlock`). So a bounded Overworld leaves a Nether 8× larger than portals can reach.
- **Twilight Forest** 1.21.1 NeoForge (mod 4.8, LGPL, CurseForge): its dimension type has `coordinate_scale: 0.125`; its portal scales and clamps like the Nether portal (`TFPortalBlock.getPortalDestination`).
- **End:** scale 1:1; the outer islands start about 1,000 blocks from the origin.
- **Sizes elsewhere:**
  - Terraria: S/M/L are 4,200 / 6,400 / 8,400 tiles wide, area ≈ 1 : 2.3 : 4;
  - Minecraft Legacy Console Edition: 864 / 1,024 / 3,072 / 5,120 blocks, with Nether ratios 1:3 / 1:3 / 1:6 / 1:8;
  - survival servers usually set 5k–20k borders.
- **Ocean share:** with an outer ocean ring of ~1,024 blocks and ~60% land inside it, a 12,288 map leaves ~7.9k² of land. Our M4–M6 targets (30–60 settlements, ~22M blocks² at vanilla village spacing, ~40–50M blocks² with wilderness and adventure areas) need about 16k.
- **Tectonic v3** shapes continents "thousands to tens of thousands of blocks wide", so the edge and the ocean ring have to be forced explicitly.
- **World-edge tools:**

  | Tool | Popularity | License | Notes |
  | --- | --- | --- | --- |
  | Bounded Not Free 1.3.5 | ~450 downloads, 2 stars | CC BY-NC-SA 4.0 + modpack/server permission (CurseForge wrongly shows MIT) | Per-dimension layouts, rim, required biomes/structures; one global config locked into the world at creation |
  | Lithostitched | 25M downloads; Tectonic is built on it | MIT | `wrap_density_function` modifier |
  | Continents (Stardust Labs) | 1.36M downloads | Stardust Labs License | Infinite world; reshapes continentalness, so it clashes with Tectonic |
  | NovoAtlas / WorldPainter | NovoAtlas ~4k downloads | LGPL / external tool | Image-based terrain; replaces Tectonic |
  | ChunkyBorder / dimworldborder | 389k / 59k downloads | GPL / ARR | Per-dimension borders |

## Decision

| Size | Overworld side | Nether ratio → side | Twilight Forest | End | Land (≈ square) |
| --- | --- | --- | --- | --- | --- |
| Test | 2,048 | 1:4 → 512 | 1:1 → 2,048 | 2,048 | small test island |
| Small | 8,192 | 1:4 → 2,048 | 1:1 → 8,192 | 8,192 | ~4.8k² |
| Medium | 12,288 | 1:6 → 2,048 | 1:1 → 12,288 | 12,288 | ~7.9k² |
| **Large (default)** | 16,384 | 1:8 → 2,048 | 1:1 → 16,384 | 16,384 | ~11.1k² |

- **Sizes are square sides including the outer ocean ring.** They are multiples of 2,048, so map items tile evenly, and of 128, the simulation cell.
- **Large is preselected.** Test is visible to players as a development size.
- **Nether ratio per size**, as on the consoles:
  - Small, Medium and Large all get a 2,048 Nether;
  - Large uses the vanilla Nether dimension type;
  - Test, Small and Medium use our own Nether dimension types, which differ only in `coordinate_scale`.
- **Twilight Forest is 1:1:** its dimension type is overridden to `coordinate_scale: 1`, so it is as large as the Overworld.
- **End is 1:1**, with a border equal to the Overworld side.
- **Only our presets exist** (D-18): one world preset per size; vanilla Default, Superflat, Amplified, Large Biomes and Single Biome are removed from the creation screen.
- **Our own size-selector screen** replaces the World Type button. Its cards show the size, the Nether size and the crossing time. "Generate Structures" is hidden and always on; Bonus Chest stays.
- **Borders are set by our code** in `livingkingdoms-world`:
  - the vanilla Overworld border equals the side, and vanilla copies it to the End and the Twilight Forest;
  - the Nether border (side / ratio) is re-applied on every start.
- **World edge:**
  - **Main path:** our edge-falloff density function, wrapped into continentalness through a Lithostitched modifier, plus a per-size Tectonic continent scale.
  - **Comparison:** Bounded Not Free is evaluated side by side on Test and Large in M1-A.
  - The result is recorded here, as an amendment section, before M1-A closes; the losing tool is removed from the pack.
- **Required structures** are guaranteed by our World Content Manifest validator (M1-C), whichever tool shapes the edge.

## Options considered

- One 12,288 map (old D-03) — rejected: too little land once the ocean ring and inner seas are inside the border.
- Size sets 2k/6k/9k/12k (too little land) and 2k/12k/16k/24k (Large ≈ 2.4M chunks to pre-generate) — rejected.
- Fixed 1:8 Nether for all sizes — rejected in favour of a Nether that stays the same size.
- Twilight Forest at its native 1:8 — rejected: a 1:1 forest gives its boss chain room on every size.
- Vanilla World Type entries as the picker — rejected: the user wants a dedicated selector with descriptions.
- Hiding the Test size — rejected: it is useful for players' own quick tests too.
- Continents — rejected: clashes with Tectonic and is infinite.
- Image-based maps — rejected for now: one hand-made map per size, no seed variety, replaces Tectonic. They may return for a curated release map in M11.
- ChunkyBorder or dimworldborder for per-dimension borders — rejected: our code is about 50 lines and needs no extra mod.

## Consequences

- Content targets scale with the size: mandatory structures (M1-C), settlement and state counts (M4, M6), the simulation grid (16² / 64² / 96² / 128² cells). Tracked as TD-10.
- Our Nether dimension types may surprise mods that check the vanilla Nether type. This is checked in M1-A and again with every later mod that touches the Nether.
- Pre-generating a Large world is about 1M chunks: hours of work and 5–10 GB. It is only needed for the M11 final map.
- The dedicated server picks a size with `level-type=livingkingdoms:<size>` (M10).

## Amendment 2026-11-06: world-edge spike result

Run on the headless test server (`npm run test-server`), seed `livingkingdoms`, maps rendered from the generator noise by `/lkworld map`:
- **Our edge falloff wins.** The density function `livingkingdoms_world:edge_falloff`, applied as `min(continents, edge_falloff)` to Tectonic's `noise/full_continents`, `noise/raw_continents` and `noise/raw_islands` through Lithostitched modifiers:
  - gives an open-ocean band in the outer half of the ring and a coast that meanders in the inner half;
  - the ring is 1,024 blocks, at most a sixth of the side; the outline is a rounded square.
- **Bounded Not Free** with Tectonic logged "Active noise router did not expose all climate fields; missing [CONTINENTALNESS, EROSION, WEIRDNESS]". Land reached the border and only biomes changed. It is removed from the pack and listed as rejected.
- **Tectonic tuning** in `modpack/config/tectonic.json`: `ocean_offset` -0.8 → -0.45 and `continents_scale` 0.13 → 0.16. Land share of the whole map on the same seed:

  | Size | Land, whole map |
  | --- | --- |
  | Test | 54.7% |
  | Small | 45.2% |
  | Medium | 42.2% |
  | Large | 41.6% |

  For Large that is about 54% of the area inside the ring. The Tectonic config is global, so all sizes share it; the 60% target stays a tunable hypothesis.
- Open: the Test island is still visibly square, because its ring is small. The look of coasts in-game needs gameplay feedback.

## Correction 2026-11-06: Twilight Forest scale direction

The context above says the Twilight Forest is "1:8 like the Nether". That is wrong in direction.
- The portal scale is `from.coordinate_scale / to.coordinate_scale` (`DimensionType.getTeleportationScale`).
- So the Overworld (1.0) → Twilight Forest (0.125) step multiplies coordinates by 8: the native forest is 8× **larger** than the Overworld, not smaller.
- The decision (TF 1:1) is unchanged and better justified: at the native scale almost every Overworld portal would land at the forest's border.

