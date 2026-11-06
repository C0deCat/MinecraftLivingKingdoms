---
status: active
last_reviewed: 2026-11-06
---

# Living Kingdoms — Roadmap

**Platform:** Minecraft Java 1.21.1 · NeoForge 21.1.x · Java 21

Each milestone has its own spec in [milestones/](milestones/). Decisions are in [decisions/](decisions/README.md), cross-cutting design in [design-docs/](design-docs/index.md).

## Vision

**Living Kingdoms** is a finite fantasy world for long-term exploration where terrain, dungeons, crafts, magic, villages, population and states form one interconnected game. The player is not the center of the simulation: settlements, economy, politics and nature keep developing when the player is not nearby.

## Requirements

- **Every milestone is a playable release:** beautiful, cohesive and fun. Mod cohesion and custom logic grow gradually.
- **Play mode:** singleplayer through M9. Multiplayer (Open to LAN for 1–4 players and a dedicated server) is its own milestone M10, before release; code stays server-safe meanwhile (D-15, [ADR-0003](decisions/ADR-0003-multiplayer-milestone.md)).
- **Publishing:** planned; platform not chosen. A cohesive experience matters more than not touching mod code, within license limits ([licenses.md](licenses.md)).
- **Finite world:** an Overworld of about 12,288 × 12,288 blocks with natural borders (D-03).
- **Guaranteed completion:** End portal, required resources, mandatory structures, access to bosses and modded dimensions. Not every biome has to exist.
- **Fantasy aesthetics:** many places, cultures, crafts and kinds of magic; minimal industrial technology.
- **One resident model:** MCA Reborn for personality and family, our mod for settlements and work, MCA Capitals for politics (D-13).
- **Abstract distant world:** changes far away are computed abstractly, without keeping chunks loaded (D-10).
- **Existing mods first:** custom mechanics only for identified gaps (see [core-beliefs.md](core-beliefs.md)).

## Milestones

Every milestone runs four phases: **A** install → **B** reconcile conflicts → **C** integrate into a cohesive subsystem → **D** accept and lock in. A finished milestone is a stable foundation; changing it later is a controlled change with regression tests.

| Milestone | Cohesive result | Base content | Custom code |
| --- | --- | --- | --- |
| [M0 — Infrastructure](milestones/m0-infrastructure.md) | Pack reproducible from Git | packwiz, NeoForge, spark | Minimal |
| [M1 — World and adventure](milestones/m1-world-and-adventure.md) | Finite map, reachable progression | Bounded Not Free, Terralith, Tectonic, structures, bosses | World validator |
| [M2 — Household](milestones/m2-household.md) | Farming, food, crafts, decor | Farmer's Delight, Croptopia, Let's Do | Datapacks/KubeJS |
| [M3 — Magic](milestones/m3-magic.md) | Three traditions, shared Ars–Iron's mana | Ars, Iron's, Occultism, Ars 'n Spells | Balance tweaks |
| [M4 — Settlements](milestones/m4-settlements.md) | Living villages of MCA residents | MCA Reborn, M2 content | `livingkingdoms-settlements` (S1–S4) |
| [M5 — Simulation](milestones/m5-simulation.md) | Villages live while nobody is near | Our Core | Regions, time, materialization |
| [M6 — Economy and states](milestones/m6-economy-and-states.md) | Trade, states, NPCs use crafts and magic | MCA Capitals, M2–M3 content | Economy contracts |
| [M7 — Atmosphere](milestones/m7-atmosphere.md) | Seasons, weather, forests affect the world | Serene Seasons, Dynamic Trees (?), one weather mod | Regional nature layer |
| [M8 — Cultures](milestones/m8-cultures.md) | Own peoples, architecture, NPC looks | Our templates, MCA systems | Culture content packs |
| [M9 — Autonomy](milestones/m9-autonomy.md) | Migration, wars, recovery, history | Accepted set | Simulator extensions |
| [M10 — Multiplayer](milestones/m10-multiplayer.md) | 1–4 players via LAN and dedicated server | Accepted set, server package | Sync fixes, server tooling |
| [M11 — Release](milestones/m11-release.md) | Stable final map and pack | Pinned mods | Optimization, migrations |

**Parallel work** (never postponed to the last milestone): licenses and asset provenance, profiling, backups, install automation, save testing, the progression checklist, culture design. Open items are tracked in [exec-plans/tech-debt-tracker.md](exec-plans/tech-debt-tracker.md).

## Risks

| Risk | Consequence | Mitigation |
| --- | --- | --- |
| Worldgen changes after M1 | Map seams, missing structures | Decide worldgen-affecting mods early; regression checks |
| Existing villages mixed with ours | Duplicate settlements, broken roads | Reserved zones, control over generator spawning |
| Incompatible culture/structure licenses | Pack cannot be distributed | Check permissions, use original assets |
| Our settlement mod is too large | Months without a playable result | Slices S1–S4, each a release; reuse open code if licenses allow |
| MCA Reborn API not enough | Mixin patches, fragile updates | Research in M4-A; minimal tested patches |
| Rewriting MCA too early | Expensive fork, desync | API, events, attachments first; fork as last resort |
| Shared mana breaks balance | Endless casting, double bonuses | One authoritative pool, a rate, gameplay tests |
| Uncontrolled Occultism mines | Resource geography stops mattering | Rarity config, later limits |
| Weather destroys settlements | Lost buildings and saves | Protection, one weather engine, backups |
| No physical simulation far away | Inconsistent world life | Abstract simulation + budgeted materialization |
| Host PC runs client + integrated server | Tight performance budget | Profile on the host PC from M1 |
| Multiplayer added late (M10) | Desync found late, rework of our mods | Server-safe code and server-authoritative state from the start; M10 test list |
| "Everything at once" profiles | Errors cannot be localized | Isolated profiles, staged enabling |
| Unnecessary custom development | Maintenance cost | Check existing addons first |
