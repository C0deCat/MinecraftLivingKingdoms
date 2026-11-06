# Pack changelog

One entry per pack change: what changed, how it was launched, the result. The commit that makes the change is the backup of the previous state. Test worlds are disposable until M11 (release).

## 0.1.0 — 2026-11-06 (M1-A, in progress)

- Added Lithostitched 1.8.1, Tectonic 3.0.28, Terralith 2.6.2 (Modrinth), The Twilight Forest 4.8.3345 and Bounded Not Free 1.3.5 (CurseForge), one at a time.
- Our mods `livingkingdoms_core` and `livingkingdoms_world` 0.0.1 are built from `mods-src/` into the instance.
- Launch report: `npm run smoke` after each addition: title screen reached, 0 ERROR/FATAL lines. World generation is checked with the size presets (next step), not yet.

## 0.0.1 — 2026-11-06

- Pack created: Minecraft 1.21.1, NeoForge 21.1.265.
- Added spark 1.10.124, ModernFix 5.27.27, FerriteCore 7.0.3, Lithium 0.15.4 (Modrinth, one at a time).
- Launch report: after each addition the client reached the title screen with 0 ERROR/FATAL log lines (host PC, Prism 11.1.1, Java 21.0.7 bundled by Prism). Expected warnings: vanilla "Missing sound for event: goat horn", FerriteCore mixin refmap warnings.
