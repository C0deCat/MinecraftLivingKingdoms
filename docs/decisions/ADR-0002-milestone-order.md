---
status: stable
last_reviewed: 2026-11-06
---

# ADR-0002: Milestone order after magic

**Status:** Accepted · **Date:** 2026-11-06

## Context

In v0.2 magic (M3) was followed by: simulation core → Millénaire/MCA/Capitals → cultures → economy → weather → autonomy. Problems:

- the simulation core would have been designed before the settlement data model was known;
- the simulation core stage adds almost nothing to the game, while the core principle of the project is a playable release at every stage;
- a poll of players: reworked residents and their simulation are more interesting than weather.

## Decision

| # | Milestone |
| --- | --- |
| M0–M3 | Unchanged: infrastructure, world and adventure, household life, magic |
| M4 | Settlements and residents (our mod on MCA Reborn), loaded chunks only |
| M5 | Unloaded-world simulation for M4 settlements |
| M6 | Economy and states (MCA Capitals, NPCs use crafts and magic) |
| M7 | Atmosphere: seasons, trees, weather |
| M8 | Our own peoples, architecture, NPC looks |
| M9 | Deep autonomy |
| M10 | Release |

## Options considered

- Atmosphere before settlements (cheap and pretty) — rejected based on the player poll.
- Economy right after settlements, before simulation — partially accepted: professions based on Farmer's Delight recipes are already part of slice M4-S3; the full economy and states come after the simulation.

## Consequences

- Mods that change world generation in later stages (especially Dynamic Trees) must be evaluated early — no later than M4-B, because they affect lumberjacks and villages.
- Until M10 test worlds are disposable.
