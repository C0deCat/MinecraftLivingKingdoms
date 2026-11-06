---
status: draft
last_reviewed: 2026-11-06
---

# Simulation model

How the world keeps changing outside loaded chunks. Delivery: [M5](../milestones/m5-simulation.md), extended in M6, M7 and M9.

## Entities

```text
Finite World → Geographic Region → Simulation Cell → Minecraft Chunk
               │
               ├── Settlements → Citizens / Families / Buildings
               ├── Political Entities → Ownership / Laws / Diplomacy
               ├── Economy → Stocks / Processes / Trade
               └── Climate / World Events

Loaded chunk: physical entities & blocks ↔ reconciliation ↔ abstract state
Unloaded chunk: abstract regional simulation → pending physical changes
```

- Coarse grid: 128 × 128-block cells, 9,216 cells on the 12,288² map. Villages, provinces and climate regions are separate, larger entities.
- The grid does not replace Minecraft's ChunkManager (D-10).

## Rules

- One record per NPC identity; one source of truth per commodity and storage.
- At any moment a village is driven by either the physical or the abstract simulation, never both.
- Abstract computation uses the same rules as physical work (recipes or normalized costs).
- Events are idempotent; saves are versioned; a "last computed day" marker per entity.
- No physical ticking in unloaded chunks; no block or entity changes off the server thread.
- Materialization on load has a per-tick change budget and is logged.
- In singleplayer/LAN, in-game time passes only while the host runs the world; no offline real-time catch-up.

## Open design questions

- How physical and abstract economies reconcile when a chunk loads mid-process.
- Storage: SavedData per region vs one world-level store.
