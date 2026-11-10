---
status: draft
last_reviewed: 2026-11-10
---

# Settlement model

How residents and villages are represented. Decision: [ADR-0001](../decisions/ADR-0001-own-settlement-mod.md). Delivery: [M4](../milestones/m4-settlements.md).

## Data ownership

| Data | Owner |
| --- | --- |
| Personality, family, relationships, age, appearance, dialogue | MCA Reborn |
| Settlement membership, profession, home, workplace, current task, storage, development plan | `livingkingdoms-settlements` |
| Stable IDs, event log, save format version | `livingkingdoms-core` |
| Titles, dynasties, succession (from M6) | MCA Capitals |

Rules:
- One record per resident identity. No parallel NPC "twins" in different mods.
- [ADR-0030](../decisions/ADR-0030-resident-integration.md): a resident is a record in our SavedData keyed by a stable ID from Core; the MCA entity carries only that ID as a NeoForge attachment. Our settlement rules decide (profession, home, guards, marriage, births) and MCA carries them out.
- `Culture` ≠ `Polity` ≠ `Settlement` (D-11): a culture is a people and its traditions, a polity is power, a settlement is an administrative-physical place.

## Areas the model covers

M4 builds the areas below slice by slice (D-50, [what deepens where](../milestones/m4-settlements.md#what-deepens-where)). The model is designed for all of them in M4-A, so a later slice adds data and tasks and does not rework an earlier one.

| Area | What the model must hold | First needed in |
| --- | --- | --- |
| Profession | A definition as data: workplace type, tasks, inputs and outputs by tag, tools it needs | S1; many more in S5 and S3 |
| Building | A template with levels, its cost per level, the jobs and homes each level gives | S1 (placed), S2 (built and upgraded), S6 (the full tree) |
| Development plan | What a settlement builds next and why (needs, population, rank) | S2, S6 |
| Player standing | Reputation per player and settlement, what it unlocks (trade, orders, a house) | S7; S3 trade must not block it |
| Quest | A task offered by a resident, its state per player, its reward | S8 |
| Population | Who lives where, births, growing up, profession assignment | S1 (fixed), S4 (growth) |

## Open design questions

Answered by [ADR-0030](../decisions/ADR-0030-resident-integration.md): tasks reach a resident through the vanilla Brain with one Mixin into MCA; buildings are vanilla structure templates plus our definition.

- How MCA's own births and deaths interact with village population rules.
- MCA Reborn has its own villages and buildings, professions, player ranks and rank tasks. For each area above: do we keep MCA's system, switch it off or extend it? One owner per piece of data either way.
