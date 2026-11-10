# AGENTS.md — mods-src

Our NeoForge 1.21.1 mods, one Gradle build with one project per mod. Rules from the root [AGENTS.md](../AGENTS.md) apply.

| Project | Mod id | Role |
| --- | --- | --- |
| `livingkingdoms-core` | `livingkingdoms_core` | Stable IDs, regions, time, world state; depends only on NeoForge |
| `livingkingdoms-world` | `livingkingdoms_world` | World sizes, borders, world edge, content manifest ([ADR-0004](../docs/decisions/ADR-0004-world-sizes-and-dimensions.md)) |
| `livingkingdoms-settlements` | `livingkingdoms_settlements` | Settlements of MCA Reborn residents: resident records, work, the MCA adapter ([ADR-0030](../docs/decisions/ADR-0030-resident-integration.md)); GPL-3.0-only |
| `livingkingdoms-testkit` | `livingkingdoms_testkit` | Dev-only: e2e driver, flat test preset, GameTests; never in the pack ([ADR-0005](../docs/decisions/ADR-0005-test-framework.md)) |

## Build

- `./gradlew build` here (Windows: `gradlew.bat build`) → `<project>/build/libs/<mod_id>-<version>.jar`. `npm run instance` runs it and copies the JARs into the game instance.
- `./gradlew :livingkingdoms-world:runClient` starts a dev client from `<project>/run/` (ignored).
- Shared versions: `gradle.properties` here; `neo_version` must equal `NEOFORGE_VERSION` in `tools/pack/src/pins.ts`. Mod-specific values: `<project>/gradle.properties`. Shared build logic: `gradle/neoforge-mod.gradle`.
- A new mod project: add it to `settings.gradle`, give it `build.gradle` (apply the shared script), `gradle.properties`, `src/main/templates/META-INF/neoforge.mods.toml`, and an `AGENTS.md` plus `CLAUDE.md` containing `@AGENTS.md`; add it to the table above.

## Rules for all our mods

- Server-safe: no `net.minecraft.client.*` in common code; client code in separate classes registered with `@Mod(dist = Dist.CLIENT)` or `@EventBusSubscriber(value = Dist.CLIENT)`; world state changes only on the server thread.
- Check APIs against NeoForge 21.1 sources (`build/moddev/artifacts/neoforge-*-sources.jar` after a build); examples for other versions often do not compile here.
- Licenses: `livingkingdoms-settlements` is GPL-3.0-only (D-52); the other mods are not decided yet (placeholder in `gradle.properties`, see [licenses.md](../docs/licenses.md)). Do not move code out of the settlement mod into the others.
- `./gradlew :livingkingdoms-settlements:runGameTestServer` runs the settlement GameTests with MCA Reborn (D-51).

## Origin

Build logic derived from the official [NeoForge 1.21.1 ModDevGradle MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle) at commit `e93d7f0` (MIT, see `TEMPLATE_LICENSE.txt`).
