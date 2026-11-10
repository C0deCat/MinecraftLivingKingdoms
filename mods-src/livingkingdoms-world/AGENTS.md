# AGENTS.md — livingkingdoms-world

NeoForge 1.21.1 mod `livingkingdoms_world`: fixed world sizes, world presets, per-dimension borders, the finite-world edge, the size selector screen, the World Content Manifest with `/lkworld validate`, `fixed_count` placement and the `foundation` structure processor. Design: [ADR-0004](../../docs/decisions/ADR-0004-world-sizes-and-dimensions.md). Rules from the root [AGENTS.md](../../AGENTS.md) and [mods-src/AGENTS.md](../AGENTS.md) apply.

## Rules

- Depends on `livingkingdoms-core`. Worldgen mods (Lithostitched, Tectonic, Bounded Not Free, Twilight Forest) are reached only through data files or small adapter classes in `livingkingdoms.world.compat.*`, so an upstream change is fixed in one place.
- The world size is a fact of the save: read it from the save, never from a client setting or a global config.
- Client-only code (the selector screen) lives in `livingkingdoms.world.client` and is registered only on the client dist.
- Data (world presets, dimension types, tags, Lithostitched modifiers) lives in `src/main/resources/data/`; keep vanilla and other mods' IDs exact and check them against the installed versions.
