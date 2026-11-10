---
status: active
last_reviewed: 2026-11-10
---

# Decisions

Two kinds of decisions live here:
- **ADRs** — decisions with context and rejected alternatives, one file each.
- **Lightweight decisions (D-xx)** — short rules that need no separate rationale.

A decision made in a chat or elsewhere does not exist for agents until it is written here.

## ADRs

| ADR | Decision | Status | Date |
| --- | --- | --- | --- |
| [ADR-0001](ADR-0001-own-settlement-mod.md) | Our own settlement mod on MCA Reborn instead of Millénaire | Accepted | 2026-11-06 |
| [ADR-0002](ADR-0002-milestone-order.md) | Milestone order: settlements → simulation → economy and states → atmosphere → cultures | Accepted, amended by ADR-0003 and ADR-0006 | 2026-11-06 |
| [ADR-0003](ADR-0003-multiplayer-milestone.md) | Singleplayer first; multiplayer (LAN, dedicated server) is M10, release M11 | Accepted | 2026-11-06 |
| [ADR-0004](ADR-0004-world-sizes-and-dimensions.md) | Fixed world sizes 2k/8k/12k/16k, per-size Nether ratio, 1:1 Twilight Forest, finite-world edge | Accepted | 2026-11-06 |
| [ADR-0005](ADR-0005-test-framework.md) | Two test layers: GameTest for mechanics, our e2e driver with TypeScript tests on Vitest | Accepted | 2026-11-09 |
| [ADR-0006](ADR-0006-parallel-tracks.md) | M2, M3 and M4 are developed in parallel tracks and merged in the order M2 → M3 → M4 | Accepted | 2026-11-10 |
| [ADR-0030](ADR-0030-resident-integration.md) | How our settlement mod works with MCA Reborn residents: our record plus an attachment, tasks through the Brain with one Mixin, our rules decide and MCA carries out | Proposed | 2026-11-10 |

While the tracks of [ADR-0006](ADR-0006-parallel-tracks.md) are open, new numbers come from the range of the place where the decision is written, so two branches never give out the same one: `main` D-24–29 and ADR-0007–0009, M2 D-30–39 and ADR-0010–0019, M3 D-40–49 and ADR-0020–0029, M4 D-50–59 and ADR-0030–0039.

## Lightweight decisions

| Code | Decision | Status |
| --- | --- | --- |
| D-01 | Minecraft 1.21.1 + NeoForge + Java; do not port the game to another engine or language | Locked |
| D-02 | Stay on 1.21.1: as of 2026-11-06 Ars Nouveau, Iron's Spells, Ars 'n Spells, Farmer's Delight, the Let's Do mods and MCA Capitals have no NeoForge builds above 1.21.1 ([check](../compatibility-matrix.md#platform-check-2026-11-06)). NeoForge pinned to 21.1.265 on 2026-11-06 (`tools/pack/src/pins.ts`, `modpack/pack.toml`) | Locked; revisit when core mods move to 26.x |
| D-03 | Four fixed square world sizes including the outer ocean: Test 2,048, Small 8,192, Medium 12,288, Large 16,384 (default); geography is finite, political borders are dynamic ([ADR-0004](ADR-0004-world-sizes-and-dimensions.md)) | Locked (changed 2026-11-06) |
| D-04 | Light-gameplay and magic mods are merged into `main` and released before settlements and NPCs; their development may overlap in tracks ([ADR-0006](ADR-0006-parallel-tracks.md)) | Locked (changed 2026-11-10) |
| D-05 | Each stage is brought to a cohesive playable subsystem before it merges into `main`, so `main` only ever holds a cohesive playable state ([ADR-0006](ADR-0006-parallel-tracks.md)) | Locked (changed 2026-11-10) |
| D-06 | Ars Nouveau — universal modern magic; Iron's — powerful ancient magic; Occultism — ritual/secret magic ([magic-system](../design-docs/magic-system.md)) | Locked as design |
| D-07 | Shared personal mana for Ars and Iron's with a conversion rate; Source separate; Occultism without a shared bar | Locked |
| D-08 | Iron's scrolls are reproducible with the mod's own tools; no hunt for consumable scrolls | Locked |
| D-09 | No new pacts, summoning risks or unique spirits for Occultism for now | Locked |
| D-10 | Do not replace Minecraft's ChunkManager; global state simulation + materialization in loaded areas only | Locked |
| D-11 | Culture ≠ political entity ≠ settlement | Locked |
| D-12 | Modified third-party structures/code are distributed only within licenses and permissions | Hard constraint |
| D-13 | Millénaire is not used; settlements are our own mod on MCA Reborn ([ADR-0001](ADR-0001-own-settlement-mod.md)) | Locked |
| D-14 | Milestone order after magic ([ADR-0002](ADR-0002-milestone-order.md)); multiplayer M10 and release M11 ([ADR-0003](ADR-0003-multiplayer-milestone.md)); M2, M3 and M4 are developed in parallel tracks and merged in that order ([ADR-0006](ADR-0006-parallel-tracks.md)) | Locked (changed 2026-11-10) |
| D-15 | Singleplayer is the only play and test mode until M10; Open to LAN (1–4 players) and the dedicated server are M10 ([ADR-0003](ADR-0003-multiplayer-milestone.md)); code stays server-safe throughout | Locked (changed 2026-11-06) |
| D-16 | Code and assets of closed (ARR) mods are never copied or decompiled into the repo; behavior changes via configs, datapacks, KubeJS, Mixins in our mod | Hard constraint |
| D-17 | All documentation, code comments, commit messages and identifiers are in English | Locked |
| D-18 | No vanilla world types: worlds are created only from our size presets through our selector screen; the pack is never tuned or tested for vanilla types ([ADR-0004](ADR-0004-world-sizes-and-dimensions.md)); test-only exception: D-23 | Locked |
| D-19 | One term, one meaning ([terms and labels](../documentation.md#terms-and-labels), [glossary](../glossary.md)). Every milestone runs phases A foundation → B reconcile → C integrate → D accept; parts of a phase are steps labelled M1-A1, M1-A2 | Locked (2026-11-07) |
| D-20 | Every new world, of any size and any seed (the player's choice, random allowed), has all its chunks generated with Chunky at creation, because the settlement simulation needs the whole map. This needs our own generation optimizer; if it cannot be made fast enough, the fallback is a pre-generated world shipped as the main format, with generating one's own world as an option (2026-11-09, with the user; TD-21); test worlds are exempt (D-23) | Locked |
| D-21 | Worldgen validation sets: a change during a milestone's phases is checked on 3 Large worlds of different seeds (`npm run validate-world`: seeds `livingkingdoms`, `alpha`, `20261107`, about 15 min); Small, Medium and Large × 4 seeds (`npm run validate-world -- --full`, about 1 h) run only when a milestone is accepted (phase D). Test is not validated (ADR-0004) | Locked (2026-11-09) |
| D-22 | The pack allocates 8 GB to the game by default, which is also its minimum requirement (the user plays with 8–12 GB); the local test server runs with 6 GB, 2 GB below the client, because in singleplayer client and integrated server share one heap (2026-11-10, with the user, after a 4 GB test server ran out of heap at world creation) | Locked |
| D-23 | Test worlds only: the testkit mod (never part of the pack) has a flat test world preset for mechanics tests, an exception to D-18 that players never see and that is never used to check worldgen; test worlds are exempt from D-20, and each test decides its Chunky radius ([ADR-0005](ADR-0005-test-framework.md), 2026-11-09, with the user) | Locked |

| D-50 | M4 scope: at the end of M4 our settlement mod is close to Millénaire in what a village does and what a player can do with it, for one culture. Slices S5 (mining and trades), S6 (village development), S7 (player and village) and S8 (quests) are added to S1–S4; the data model is designed in M4-A for all of them and built slice by slice ([M4](../milestones/m4-settlements.md#what-deepens-where)). This widens the scope [ADR-0001](ADR-0001-own-settlement-mod.md) called deliberately smaller than Millénaire; its other points stand (2026-11-10, with the user) | Locked |
| D-51 | GameTests of the settlement mod live in `livingkingdoms-settlements` and run with `./gradlew :livingkingdoms-settlements:runGameTestServer` with MCA Reborn on the development classpath, because the testkit changes only on `main` and its GameTest runner is not built yet ([ADR-0030](ADR-0030-resident-integration.md), an addition to [ADR-0005](ADR-0005-test-framework.md)) | Proposed with ADR-0030 |
| D-52 | `livingkingdoms-settlements` is released under GPL-3.0-only, because it calls and patches MCA Reborn (GPL-3.0-only); its sources are published with its first public release. The license of our other mods is chosen before the first public release (2026-11-10, with the user; [ADR-0030](ADR-0030-resident-integration.md)) | Locked |

## ADR template

File `ADR-NNNN-short-name.md`, with the same frontmatter as other docs:

```markdown
# ADR-NNNN: Title

**Status:** Proposed | Accepted | Superseded by ADR-XXXX · **Date:** YYYY-MM-DD

## Context
## Decision
## Options considered
## Consequences
```

An accepted ADR is never rewritten: a changed decision gets a new ADR, and the old one is marked "Superseded".
