---
status: active
last_reviewed: 2026-11-06
---

# Compatibility matrix

Registry of the pack's mods: what is installed, why, which version and how well it is verified. Updated on every mod addition, update or removal (see the workflow in [AGENTS.md](../AGENTS.md#workflow)).

**Platform:** Minecraft 1.21.1 · NeoForge 21.1.265 (pinned 2026-11-06 in `tools/pack/src/pins.ts` and `modpack/pack.toml`) · Java 21

## Mods

`npm run verify-mods` checks this table against `modpack/`: every pack mod needs a row whose Mod cell equals the packwiz `name`, whose Version is part of the JAR file name and whose Side matches the `.pw.toml`.

| Mod | Version | Stage | Purpose | Side | Dependencies | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| spark | 1.10.124 | M0 | Profiler: TPS, tick times, heap | both | — | runtime-ok | GPL-3.0; also useful on a dedicated server |
| ModernFix | 5.27.27 | M0 | Load time and memory fixes | both | — | runtime-ok | LGPL-3.0 |
| FerriteCore | 7.0.3 | M0 | Memory use of block states and models | both | — | runtime-ok | MIT; mixin "refmap could not be read" warnings at startup are expected for this build |
| Lithium | 0.15.4 | M0 | Server tick optimizations (AI, physics, chunks) | both | — | runtime-ok | LGPL-3.0; official NeoForge build |

**Side:** `both` / `client` / `server`.
**Status:** `candidate` → `installed` → `runtime-ok` (singleplayer launch) → `integrated` (conflicts resolved, subsystem is cohesive) → `accepted` (milestone accepted) → `mp-ok` (LAN and dedicated server verified in M10) · `rejected` (with a reason).

## Rejected mods

Mods we decided not to use. Check this list before proposing a mod; do not propose a rejected one again unless new facts change the reason.

| Mod | Rejected | Reason | Record |
| --- | --- | --- | --- |
| Millénaire | 2026-11-06 | Closed source and All Rights Reserved: no fork or modification, not on Modrinth. Its villagers would compete with MCA Reborn as a second NPC model, and its economy cannot be simulated off-screen without its code. Replaced by our own settlement mod. Ideas may still be studied in-game, never its code or assets | [ADR-0001](decisions/ADR-0001-own-settlement-mod.md) |

## Platform check (2026-11-06)

Highest Minecraft version with NeoForge builds on Modrinth. The pack can only move up when every base mod has moved.

| Mod | Highest NeoForge MC version |
| --- | --- |
| Ars Nouveau | 1.21.1 |
| Iron's Spells 'n Spellbooks | 1.21.1 |
| Ars 'n Spells | 1.21.1 |
| Farmer's Delight | 1.21.1 |
| Let's Do: Vinery, Farm & Charm, Meadow, HerbalBrews | 1.21.1 |
| MCA Capitals | 1.21.1 |
| Handcrafted, Supplementaries, Almost Unified | 1.21.1 |
| Mowzie's Mobs, When Dungeons Arise, Bounded Not Free | 1.21.1 |
| MCA Reborn | 26.2 |
| Occultism | 26.3 |
| KubeJS | 26.1.2 |
| Pantrywork, Dynamic Trees | 26.1.2–26.2 |
| YUNG's Better Strongholds | 26.1.2 |
| Terralith, Tectonic, Lithostitched, Dungeons and Taverns, Serene Seasons | 26.3 |
| Croptopia | 26.3 (CurseForge) |

## Known conflicts and solutions

| Mods | Problem | Solution | Recorded in |
| --- | --- | --- | --- |
| — | — | — | — |
