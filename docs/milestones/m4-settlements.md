---
status: active
last_reviewed: 2026-11-10
---

# M4 — Settlements and residents (our mod on MCA Reborn)

## Goal

Living villages: residents are MCA Reborn characters with professions, homes and a shared household, and villages grow from their own resources. Everything works only in loaded chunks; the unloaded world is [M5](m5-simulation.md).

Key decision: [ADR-0001](../decisions/ADR-0001-own-settlement-mod.md) — no Millénaire; our own mod `livingkingdoms-settlements`. The data model is in [design-docs/settlement-model.md](../design-docs/settlement-model.md).

Scope (D-50, with the user, 2026-11-10): at the end of M4 the mod is close to Millénaire in what a village does and what a player can do with it, for one culture. Millénaire is a reference for mechanics seen in the game and on its wiki, never for code or assets (ADR-0001).

## Playable result

"Living Settlements", released slice by slice:

| Slice | Content | What the player gets |
| --- | --- | --- |
| S1 — Village base | Starting villages from templates, MCA residents assigned to a village, homes, shared storage, the profession framework (a profession is data plus a set of tasks), farmer and lumberjack | A populated world where residents work |
| S2 — Builder | Development plan, construction from templates using storage resources, visible progress, building levels on one or two upgrade lines | Villages grow before your eyes |
| S3 — Kitchen and crafts | Cook, baker, brewer on Farmer's Delight and Let's Do recipes; simple trade with the player from the village stock | Link to the M2 household gameplay |
| S4 — Population growth | MCA families produce new residents, housing, profession assignment; several villages | Villages develop over years |
| S5 — Mining and trades | Miner, mason, smith, herder, guard; the chain ore → ingot → tool → worker, so a village supplies its own stone, metal and tools | Villages that depend on nothing but their land |
| S6 — Village development | The full building tree of the culture: hamlet → village → town, town hall, workshops, walls | A village visibly changes its rank over the years |
| S7 — Player and village | Reputation, a village panel (population, needs, building projects), buildings ordered by the player, the player's own house | The player is a member of the village, not a visitor |
| S8 — Quests | Tasks from residents (bring, deliver, kill, build) rewarded with reputation and goods | Reasons to come back to a village |

One culture, done cohesively; the second culture comes in [M8](m8-cultures.md).

### Order of the slices

The numbers are names, not the order: S1–S4 keep the meaning other docs rely on ([ADR-0006](../decisions/ADR-0006-parallel-tracks.md)), and the slices added by D-50 got the next numbers. The order of work is:

| Order | Slice | Where | Why here |
| --- | --- | --- | --- |
| 1 | S1 | Track branch | The base everything else needs |
| 2 | S2 | Track branch | Needs storage and residents |
| 3 | S5 | Track branch | Needs no M2 content; without stone and metal the builder has only wood |
| 4 | S4 | Track branch while M2 and M3 are not merged, otherwise `main` | Needs no M2 content; the building tree needs people |
| 5 | S6 | Same as S4 | Builds on S2, S5 and S4 |
| 6 | S3 | `main` | Needs the M2 content |
| 7 | S7 | `main` | Builds on the trade of S3 |
| 8 | S8 | `main` | Builds on the reputation of S7 |

### What deepens where

Each area gets a first pass, is deepened later in M4 and continues in a later milestone. The data model is designed in phase A for all the areas at once, so a later slice adds data and tasks instead of reworking an earlier one.

| Area | First pass | Deepened in M4 | After M4 |
| --- | --- | --- | --- |
| Professions | S1: framework, farmer, lumberjack; S2: builder | S5: miner, mason, smith, herder, guard; S3: cook, baker, brewer | M6: merchant and magic professions; M8: professions of other cultures |
| Buildings and development | S1: starting village; S2: development plan and building levels | S6: the full tree of one culture | M8: a tree per culture |
| Player and village | S3: simple trade | S7: reputation, village panel, ordered buildings, own house | M6: currency, prices from the economy, titles |
| Quests | — | S8 | M6: quests tied to politics |
| Population | S1: a fixed set of residents | S4: families, children, new homes, several villages | M5: life in unloaded chunks |

## Mods and modules

| Status | Mod / module | Role |
| --- | --- | --- |
| Base | [MCA Reborn](https://www.curseforge.com/minecraft/mc-mods/minecraft-comes-alive-reborn) | The only representation of a resident: body, personality, family, appearance, dialogue |
| Ours | `livingkingdoms-settlements` | Settlement, membership, profession, home, workplace, tasks, storage, development plan |
| Ours | `livingkingdoms-core` (minimal) | Stable IDs, event log, save format version |
| Reference | MineColonies / Structurize (GPL-3.0) | Ideas for construction and logistics; code only under GPL-3.0 and after asking the user ([licenses](../licenses.md#construction-and-worker-mods-as-code-sources)). Not pack mods |
| Reference | Terralith fortified villages (`terralith:fortified_village`, `terralith:fortified_desert_village`) | The user likes their buildings (2026-11-09): use them by referencing Terralith's template pools by id while Terralith is in the pack, or build our own templates in a similar style. Never copy or ship Terralith's files (Stardust Labs License, [licenses.md](../licenses.md)) |
| Later | [MCA Capitals](https://www.curseforge.com/minecraft/mc-mods/mca-capitals) | Politics — [M6](m6-economy-and-states.md) |

## Phases

**A — foundation: research and prototype.**
- MCA Reborn: how to get residents and give them tasks from outside; how to store our data on the entity (NeoForge attachments); which events exist and what needs Mixins.
- What MCA Reborn already has in the areas of the slices: its own villages and buildings, professions (guards among them), player ranks and reputation, rank tasks, dialogue. For each: keep, switch off or extend.
- Open construction and worker-NPC mods: licenses and code suitability.
- The data model for all the areas of the [slices](#what-deepens-where): profession, building template with levels, reputation, quest.
- Prototype: an MCA resident performs one task of our mod and survives a world reload.

**B — reconcile.** Vanilla villages and villagers, vanilla professions, MCA residents spawning outside our villages, village structures from M1 (vanilla, Dungeons and Taverns variants, Terralith fortified villages), the Better Archeology Archeologist villager and its tents. Jigsaw villages often break walking routes: street steps of 2+ blocks where `terrain_matching` streets follow steep terrain, doors above or below the street, houses over drops (seen in M1, 2026-11-09). Measure it (share of doors reachable on foot from the village center and back) before deciding how far the placeholder villages are repaired. Lumberjacks are built for vanilla and Terralith trees; Dynamic Trees is decided in M7 (TD-06).

**C — integrate.** Slices S1–S8 in the [order of the slices](#order-of-the-slices), each released. S1, S2 and S5 are built on the M4 track; S4 and S6 too while M2 and M3 are not merged yet. The track merges into `main` after M2 and M3. S3 needs the M2 content, so S3, S7, S8 and phase D are done on `main` after that merge, together with a recheck of phase B on the combined pack ([ADR-0006](../decisions/ADR-0006-parallel-tracks.md), [plan](../exec-plans/active/2026-11-10-m4-settlements.md)).

**D — accept.** See [Acceptance](#acceptance) below.

## Acceptance

- Several villages work for dozens of in-game days in the loaded world: growth, production, families.
- A village supplies itself: food, wood, stone, metal and tools come from its own residents (S1, S3, S5).
- A village started as a hamlet reaches the top of the building tree without the player's help (S2, S6).
- The player can earn reputation, trade, order a building, get a house and complete quests in a village (S3, S7, S8).
- Walkable layout (with the user, 2026-11-09): from every home and workplace door a player and an MCA resident can walk to the village center and back with steps of at most one block, in generated villages and after growth. This is a rule of our layout and builder (site choice by slope, levelled plots, paths found with a step limit), not a repair afterwards.
- World reload without losing residents, storage or buildings.
- Verified in singleplayer (multiplayer checks in [M10](m10-multiplayer.md)).
- TPS measured on the host PC in an agreed scenario.

## Out of scope

- Unloaded-world simulation (M5), states and inter-village trade (M6).

## Open questions

Answered in M4-A: how MCA Reborn works inside ([mca-reborn-7.7](../references/mca-reborn-7.7.md)), the licenses of construction and worker mods ([licenses](../licenses.md#construction-and-worker-mods-as-code-sources)), the license of our mod (D-52).

- Does the integration of [ADR-0030](../decisions/ADR-0030-resident-integration.md) hold in the game: our brain for a resident, the attachment, a task that survives a reload (prototype M4-A5).
- The remaining MCA Reborn config switches for the pack: the first-join screen, inn guests, taxes, bounty hunters (M4-B).
- How many settlements per world size, and where the starting villages come from (TD-10, M4-B).
