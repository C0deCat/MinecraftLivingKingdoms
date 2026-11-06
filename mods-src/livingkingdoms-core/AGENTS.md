# AGENTS.md — livingkingdoms-core

NeoForge 1.21.1 mod `livingkingdoms_core`, the base layer of our code ([ARCHITECTURE.md](../../ARCHITECTURE.md)). Rules from the root [AGENTS.md](../../AGENTS.md) apply; this file adds what is specific here.

## Rules

- Depends only on NeoForge and Minecraft. No MCA, Farmer's Delight or magic-mod classes, not even optional ones: feature mods reach external mods through their own adapters.
- Server-safe: no `net.minecraft.client.*` or other client-only classes in common code. Client code goes into a separate class guarded by `@Mod(dist = Dist.CLIENT)` or `@EventBusSubscriber(value = Dist.CLIENT)`.
- No premature simulation code: add a system only when a milestone needs it (M0 ships the empty `@Mod` class).
- Check every API against NeoForge 21.1 sources (`./gradlew` downloads them; open in the IDE) — examples for 1.20.x or 1.21.4+ often do not compile here.

## Build

- `./gradlew build` (Windows: `gradlew.bat build`) → `build/libs/livingkingdoms_core-<version>.jar`.
- `./gradlew runClient` / `runServer` start a dev game from `run/` (ignored).
- Versions live in `gradle.properties`; `neo_version` must equal `NEOFORGE_VERSION` in `tools/pack/src/pins.ts`.

## Origin

Scaffolded from the official [NeoForge 1.21.1 ModDevGradle MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle) at commit `e93d7f0` (MIT, see `TEMPLATE_LICENSE.txt`). The mod's own license is not chosen yet (placeholder in `gradle.properties`, see [licenses.md](../../docs/licenses.md)).
