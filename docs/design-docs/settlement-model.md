---
status: draft
last_reviewed: 2026-11-06
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
- Our data is attached to the MCA entity (NeoForge attachments) or kept in Core SavedData keyed by the stable ID — to be decided in M4-A.
- `Culture` ≠ `Polity` ≠ `Settlement` (D-11): a culture is a people and its traditions, a polity is power, a settlement is an administrative-physical place.

## Open design questions

- Can MCA residents receive tasks from outside, or does their AI need Mixin patches?
- Building templates: own format, vanilla structure NBT, or Structurize (license permitting)?
- How MCA's own births and deaths interact with village population rules.
