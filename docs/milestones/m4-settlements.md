---
status: active
last_reviewed: 2026-11-07
---

# M4 — Settlements and residents (our mod on MCA Reborn)

## Goal

Living villages: residents are MCA Reborn characters with professions, homes and a shared household, and villages grow from their own resources. Everything works only in loaded chunks; the unloaded world is [M5](m5-simulation.md).

Key decision: [ADR-0001](../decisions/ADR-0001-own-settlement-mod.md) — no Millénaire; our own mod `livingkingdoms-settlements`. The data model is in [design-docs/settlement-model.md](../design-docs/settlement-model.md).

## Playable result

"Living Settlements", released slice by slice:

| Slice | Content | What the player gets |
| --- | --- | --- |
| S1 — Village base | Starting villages from templates, MCA residents assigned to a village, homes, shared storage, farmer and lumberjack | A populated world where residents work |
| S2 — Builder | Development plan, construction from templates using storage resources, visible progress | Villages grow before your eyes |
| S3 — Kitchen and crafts | Cook, baker, brewer on Farmer's Delight and Let's Do recipes; trade with the player | Link to the M2 household gameplay |
| S4 — Population growth | MCA families produce new residents, housing, profession assignment; several villages | Villages develop over years |

One culture, done cohesively; the second culture comes in [M8](m8-cultures.md).

## Mods and modules

| Status | Mod / module | Role |
| --- | --- | --- |
| Base | [MCA Reborn](https://www.curseforge.com/minecraft/mc-mods/minecraft-comes-alive-reborn) | The only representation of a resident: body, personality, family, appearance, dialogue |
| Ours | `livingkingdoms-settlements` | Settlement, membership, profession, home, workplace, tasks, storage, development plan |
| Ours | `livingkingdoms-core` (minimal) | Stable IDs, event log, save format version |
| Research | MineColonies / Structurize and other open mods | Possible source of construction and logistics code if the license allows |
| Reference | Terralith fortified villages (`terralith:fortified_village`, `terralith:fortified_desert_village`) | The user likes their buildings (2026-11-09): use them by referencing Terralith's template pools by id while Terralith is in the pack, or build our own templates in a similar style. Never copy or ship Terralith's files (Stardust Labs License, [licenses.md](../licenses.md)) |
| Later | [MCA Capitals](https://www.curseforge.com/minecraft/mc-mods/mca-capitals) | Politics — [M6](m6-economy-and-states.md) |

## Phases

**A — foundation: research and prototype.**
- MCA Reborn: how to get residents and give them tasks from outside; how to store our data on the entity (NeoForge attachments); which events exist and what needs Mixins.
- Open construction and worker-NPC mods: licenses and code suitability.
- Prototype: an MCA resident performs one task of our mod and survives a world reload.

**B — reconcile.** Vanilla villages and villagers, vanilla professions, MCA residents spawning outside our villages, village structures from M1 (vanilla, Dungeons and Taverns variants, Terralith fortified villages), the Better Archeology Archeologist villager and its tents. Jigsaw villages often break walking routes: street steps of 2+ blocks where `terrain_matching` streets follow steep terrain, doors above or below the street, houses over drops (seen in M1, 2026-11-09). Measure it (share of doors reachable on foot from the village center and back) before deciding how far the placeholder villages are repaired. Lumberjacks are built for vanilla and Terralith trees; Dynamic Trees is decided in M7 (TD-06).

**C — integrate.** Slices S1–S4 in order, each released.

**D — accept.** See [Acceptance](#acceptance) below.

## Acceptance

- Several villages work for dozens of in-game days in the loaded world: growth, production, families.
- Walkable layout (with the user, 2026-11-09): from every home and workplace door a player and an MCA resident can walk to the village center and back with steps of at most one block, in generated villages and after growth. This is a rule of our layout and builder (site choice by slope, levelled plots, paths found with a step limit), not a repair afterwards.
- World reload without losing residents, storage or buildings.
- Verified in singleplayer (multiplayer checks in [M10](m10-multiplayer.md)).
- TPS measured on the host PC in an agreed scenario.

## Out of scope

- Unloaded-world simulation (M5), states and inter-village trade (M6).

## Open questions

- MCA Reborn architecture, API, events, data storage, license; can residents be given tasks from outside.
- MineColonies, Structurize and worker-NPC mods — licenses and suitability.
- License of our mod: GPLv3 if it depends deeply on MCA Reborn code.
