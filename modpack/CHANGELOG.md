# Pack changelog

One entry per pack change: what changed, how it was launched, the result. The commit that makes the change is the backup of the previous state. Test worlds are disposable until M11 (release).

## Unreleased — M4

- Added MCA Reborn 7.7.36 (Modrinth, file `mca-neoforge-7.7.36-beta.3+1.21.1.jar`), M4-A1. On the `m4-settlements` branch only until the M4 merge (ADR-0006).
- Launch report: `npm run smoke` before the addition (title screen after 26 s, 51 mods) and after it (30 s, 52 mods, `mca 7.7.36-beta.3+1.21.1`); both with only the 2 known GeckoLib ERROR lines of Mowzie's Mobs. `npm run verify-mods` OK (45 mods). No world opened yet.
- Added `config/mca.json` with `launchIntoDestiny` false: MCA's first-join screen paused the game in front of every new world (found by the e2e run) and can move the player away from our spawn. The destiny command stays available once per player.
- Our mod `livingkingdoms_settlements` 0.0.1 (M4-A prototype) is built from `mods-src/` into the instance; `livingkingdoms_core` gains stable IDs and the save format version.

## 0.1.0 — 2026-11-06 (M1-A1, base generator)

- Added Lithostitched 1.8.1, Tectonic 3.0.28, Terralith 2.6.2 (Modrinth), The Twilight Forest 4.8.3345 and Bounded Not Free 1.3.5 (CurseForge), one at a time.
- Our mods `livingkingdoms_core` and `livingkingdoms_world` 0.0.1 are built from `mods-src/` into the instance.
- Launch report: `npm run smoke` after each addition: title screen reached, 0 ERROR/FATAL lines.
- Removed Bounded Not Free after the world-edge spike (cannot shape terrain with Tectonic, ADR-0004 amendment).
- Added `config/tectonic.json`: `ocean_offset` -0.45, `continents_scale` 0.16.
- Worldgen report: headless test server, seed `livingkingdoms`, all four sizes generate with an ocean ring; land 54.7% / 45.2% / 42.2% / 41.6% (Test/Small/Medium/Large); 0 ERROR lines.
- Gentler world edge (ADR-0004, variant D1): added edge bias and erosion lift; `config/tectonic.json` `ocean_offset` -0.45 → -0.38. Coast cliffs 80% → 40% (Test), 41% → 37% (Large); Large land 41.0%. Smoke: title screen, 0 ERROR.
- Natural coast profile (ADR-0004, variant F3): edge values clamped at -1, slope compensation, shore styles, island fade. Large: cliffs 30%, shore width 12, sea floor 4 blocks out -3, as natural Tectonic coasts; no land at the border on any size; land 40.7/36.2/37.7/40.0% (Test/Small/Medium/Large).

## 0.0.1 — 2026-11-06

- Pack created: Minecraft 1.21.1, NeoForge 21.1.265.
- Added spark 1.10.124, ModernFix 5.27.27, FerriteCore 7.0.3, Lithium 0.15.4 (Modrinth, one at a time).
- Launch report: after each addition the client reached the title screen with 0 ERROR/FATAL log lines (host PC, Prism 11.1.1, Java 21.0.7 bundled by Prism). Expected warnings: vanilla "Missing sound for event: goat horn", FerriteCore mixin refmap warnings.
