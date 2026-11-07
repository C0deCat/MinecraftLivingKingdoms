---
status: active
last_reviewed: 2026-11-07
---

# Tectonic 3.0 terrain internals

- **Version:** Tectonic 3.0.28 for NeoForge 21.1, with Terralith 2.6.2 installed. Pinned in `modpack/mods/tectonic.pw.toml`.
- **Sources:** [Modrinth](https://modrinth.com/mod/tectonic); the data pack inside the JAR, `resourcepacks/tectonic/` (MIT).
- **Checked:** 2026-11-06/07 by reading the JAR's data files and measuring with `/lkworld coast` and `/lkworld map`.

## What we use

- **Config:** `modpack/config/tectonic.json`, shipped in the pack. Ours: `ocean_offset` -0.38 and `continents_scale` 0.16 (ADR-0004 amendments). The config is global: all world sizes share it.
- **Overlays:**
  - with Terralith loaded, the `overlay.terratonic` overlay is active;
  - it replaces `tectonic:noise/raw_continents` and makes `base_terrain` = `max(tectonic:sloped_cheese, terralith:overworld/extra_terrain_sum)`. Terralith's arches, dunes and spikes add to the terrain through that `max`.
- **Functions our world edge wraps** (through [Lithostitched](lithostitched-1.8.md)):

  | Function | Meaning |
  | --- | --- |
  | `tectonic:noise/raw_continents` | Raw continentalness: `ocean_offset` + spline of the continents noise, clamped to at most 0.4. Drives the land height splines and the island/continent choice |
  | `tectonic:noise/raw_islands` | Island noise; strong only where raw continents are below -0.5 |
  | `tectonic:noise/full_continents` | The router's continents: islands where `raw_continents` < -0.5 (`island_selector`), continents elsewhere |
  | `tectonic:noise/continent/erosion` | Signed terrain erosion; its absolute value (`erosion_folded`) drives the offset, factor and jaggedness splines |

## Facts that shaped our edge

- **Coast window.** In `terrain_spline/offset/continents`, raw continents of -0.195 are about 18 blocks below sea level and -0.155 is sea level. At 0.0 the land is already at hill or mountain height when erosion is low. Natural noise crosses that window over a few hundred blocks; anything that crosses it faster makes cliffs.
- **Other erosion bands:**
  - mid erosion (`erosion_folded` about 0.35–0.8) selects Tectonic's "region" terrains (plateaus and terraces);
  - high erosion gives flat lowland.
- **Natural coast profile** (Large, edge off, 360 rays):
  - 31% cliffs; shore width p50 12 blocks;
  - sea floor -3 at 4 blocks out and -13 at 16 blocks out;
  - the flat "+1 above sea" shape is the most common (44%).
- **Islands.** Where raw continents are below -0.5, Tectonic switches to island terrain. A deep band therefore fills with islands unless they are faded out.

## Pitfalls

- **Values below -1 break the splines.** They extrapolate linearly past their first point, so feeding `raw_continents` or `raw_islands` values far below -1 gives steep walls, deep water at the shore and even land in open ocean. Our edge clamps both at -1.0.
- **A `min` cap cuts terrain.** Capping continentalness with `min(continents, cap)` makes the coast follow the cap's contour and cuts mountains into walls. Add a bias instead, and keep a cap only where the land is already gone.
- **Replacing erosion flattens variety.** Replacing erosion with one value makes every coast look alike. Shift it instead.
- **Config edits in the instance.** packwiz-installer does not restore an edited `config/tectonic.json` inside the instance (see [prism-launcher.md](prism-launcher.md)).
