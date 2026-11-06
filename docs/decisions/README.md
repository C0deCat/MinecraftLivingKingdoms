---
status: active
last_reviewed: 2026-11-06
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
| [ADR-0002](ADR-0002-milestone-order.md) | Milestone order: settlements → simulation → economy and states → atmosphere → cultures | Accepted, amended by ADR-0003 | 2026-11-06 |
| [ADR-0003](ADR-0003-multiplayer-milestone.md) | Singleplayer first; multiplayer (LAN, dedicated server) is M10, release M11 | Accepted | 2026-11-06 |

## Lightweight decisions

| Code | Decision | Status |
| --- | --- | --- |
| D-01 | Minecraft 1.21.1 + NeoForge + Java; do not port the game to another engine or language | Locked |
| D-02 | Stay on 1.21.1: as of 2026-11-06 Ars Nouveau, Iron's Spells, Ars 'n Spells, Farmer's Delight, the Let's Do mods and MCA Capitals have no NeoForge builds above 1.21.1 ([check](../compatibility-matrix.md#platform-check-2026-11-06)). NeoForge pinned to 21.1.265 on 2026-11-06 (`tools/pack/src/pins.ts`, `modpack/pack.toml`) | Locked; revisit when core mods move to 26.x |
| D-03 | First map about 12,288 × 12,288; geography is finite, political borders are dynamic | Working hypothesis |
| D-04 | Light-gameplay and magic mods come before settlements and NPCs | Locked |
| D-05 | Each stage is brought to a cohesive playable subsystem before moving on | Locked |
| D-06 | Ars Nouveau — universal modern magic; Iron's — powerful ancient magic; Occultism — ritual/secret magic ([magic-system](../design-docs/magic-system.md)) | Locked as design |
| D-07 | Shared personal mana for Ars and Iron's with a conversion rate; Source separate; Occultism without a shared bar | Locked |
| D-08 | Iron's scrolls are reproducible with the mod's own tools; no hunt for consumable scrolls | Locked |
| D-09 | No new pacts, summoning risks or unique spirits for Occultism for now | Locked |
| D-10 | Do not replace Minecraft's ChunkManager; global state simulation + materialization in loaded areas only | Locked |
| D-11 | Culture ≠ political entity ≠ settlement | Locked |
| D-12 | Modified third-party structures/code are distributed only within licenses and permissions | Hard constraint |
| D-13 | Millénaire is not used; settlements are our own mod on MCA Reborn ([ADR-0001](ADR-0001-own-settlement-mod.md)) | Locked |
| D-14 | Milestone order after magic ([ADR-0002](ADR-0002-milestone-order.md)); multiplayer M10 and release M11 ([ADR-0003](ADR-0003-multiplayer-milestone.md)) | Locked |
| D-15 | Singleplayer is the only play and test mode until M10; Open to LAN (1–4 players) and the dedicated server are M10 ([ADR-0003](ADR-0003-multiplayer-milestone.md)); code stays server-safe throughout | Locked (changed 2026-11-06) |
| D-16 | Code and assets of closed (ARR) mods are never copied or decompiled into the repo; behavior changes via configs, datapacks, KubeJS, Mixins in our mod | Hard constraint |
| D-17 | All documentation, code comments, commit messages and identifiers are in English | Locked |

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
