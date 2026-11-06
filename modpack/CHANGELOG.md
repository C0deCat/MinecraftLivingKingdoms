# Pack changelog

One entry per pack change: what changed, how it was launched, the result. The commit that makes the change is the backup of the previous state. Test worlds are disposable until M11 (release).

## 0.0.1 — 2026-11-06

- Pack created: Minecraft 1.21.1, NeoForge 21.1.265.
- Added spark 1.10.124, ModernFix 5.27.27, FerriteCore 7.0.3, Lithium 0.15.4 (Modrinth, one at a time).
- Launch report: after each addition the client reached the title screen with 0 ERROR/FATAL log lines (host PC, Prism 11.1.1, Java 21.0.7 bundled by Prism). Expected warnings: vanilla "Missing sound for event: goat horn", FerriteCore mixin refmap warnings.
