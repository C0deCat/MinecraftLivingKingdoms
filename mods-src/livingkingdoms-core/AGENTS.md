# AGENTS.md — livingkingdoms-core

NeoForge 1.21.1 mod `livingkingdoms_core`, the base layer of our code ([ARCHITECTURE.md](../../ARCHITECTURE.md)). Rules from the root [AGENTS.md](../../AGENTS.md) and [mods-src/AGENTS.md](../AGENTS.md) (build, server-safety) apply; this file adds what is specific here.

## Rules

- Depends only on NeoForge and Minecraft. No MCA, Farmer's Delight, worldgen or magic-mod classes, not even optional ones: feature mods reach external mods through their own adapters.
- No premature simulation code: add a system only when a milestone needs it. Since M4-A Core has stable IDs (`StableId`, written `kind:number`) and the save format version, both in `CoreSaveData` (Overworld save data, server thread only).
