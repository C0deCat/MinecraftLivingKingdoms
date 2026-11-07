---
status: active
last_reviewed: 2026-11-07
---

# M7 — Atmosphere: seasons, trees, weather

## Goal

Nature changes by itself and affects village economies; this is visible when the player returns to an unloaded region.

## Playable result

"Dynamic World": seasons, growing forests and weather that matter for villages.

## Mods

| Status | Mod | Notes |
| --- | --- | --- |
| Base | [Serene Seasons](https://modrinth.com/mod/serene-seasons) + GlitchCore | Seasons, seasonal vegetation and climate |
| Candidate | [Dynamic Trees](https://www.curseforge.com/minecraft/mc-mods/dynamictrees) | Forest growth; changes worldgen and affects lumberjacks — decide no later than M4-B |
| Candidate | [Immersive Weathering: Renewed](https://modrinth.com/mod/immersive-weather-renewed) | Signs of age, moss, oxidation |
| Alternative A | [Weather2](https://www.curseforge.com/minecraft/mc-mods/weather-storms-tornadoes) + CoroUtil | Storms and tornadoes |
| Alternative B | [ProtoManly's Weather](https://modrinth.com/mod/protomanlys-weather) | Spectacular storms; incompatible with shaders, heavy GPU load |
| Candidate if B | [Natural Regrowth](https://modrinth.com/mod/natural-regrowth) | Nature recovery |
| Optional | Enhanced Celestials, meteors, floods | Extra load and save risks; later |

Weather2 and ProtoManly are never installed together.

## Phases

**A — foundation: install.** Seasons first, then tree growth, then one weather mod. Test on M4–M6 settlements.

**B — reconcile.** Conflicts between weather sources, tree regeneration and crop growth. No unbounded tick updates, no free destruction of buildings. Some Dynamic Trees addons are incompatible with Ars Nouveau (see the Ars addon catalog).

**C — integrate.** `RegionClimate` and `WorldEventScheduler` in Core: climate, drought, fires, floods and forest recovery exist abstractly in unloaded regions and affect harvests. Physical traces materialize on load within safe limits. Building protection is mandatory.

**D — accept.** See [Acceptance](#acceptance) below.

## Acceptance

- After several seasons in an unloaded region, forests, fields and households change, and this is physically visible on return.
- No TPS degradation, no uncontrolled destruction.

## Open questions

- Separate test of Weather2 and ProtoManly with shaders/Iris/Distant Horizons/seasons/performance.
- Dynamic Trees vs M4 lumberjacks.
