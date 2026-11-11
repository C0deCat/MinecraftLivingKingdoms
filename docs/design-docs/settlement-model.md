---
status: draft
last_reviewed: 2026-11-11
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

## The villages the world generates today

Measured in M4-B2 on 2026-11-11 with `/lksettle survey` on fresh Large worlds of the three D-21 seeds (179 villages; data in `runtime/survey/`, not committed). The villages are the placeholder sources of M1: vanilla, Dungeons and Taverns, Terralith. A door counts as reachable when it can be walked to from the largest walkable area around the village bell with steps of at most one block; the rule is symmetric, so such a door can be walked back from.

| Source | Villages | Doors reachable | Fully walkable villages | Below 80% | Residents per village |
| --- | --- | --- | --- | --- | --- |
| `minecraft:village_plains` | 65 | 846 / 936 (90%) | 32 | 11 | 9.2 |
| `minecraft:village_taiga` (the Dungeons and Taverns override) | 32 | 413 / 499 (83%) | 10 | 6 | 9.6 |
| `nova_structures:village_birch` | 25 | 249 / 270 (92%) | 18 | 4 | 7.7 |
| `terralith:fortified_village` | 14 | 346 / 459 (75%) | 0 | 4 | 21.1 |
| `minecraft:village_desert` | 13 | 214 / 230 (93%) | 8 | 1 | 12.4 |
| `minecraft:village_savanna` | 11 | 140 / 142 (99%) | 8 | 0 | 5.5 |
| `minecraft:village_snowy` | 8 | 116 / 151 (77%) | 0 | 3 | 10.0 |
| `terralith:fortified_desert_village` | 7 | 85 / 102 (83%) | 5 | 1 | 20.1 |
| `nova_structures:village_jungle` | 4 | 37 / 37 (100%) | 4 | 0 | 6.0 |
| **All** | **179** | **2,446 / 2,826 (87%)** | **85 (47%)** | **30** | **10.4** |

What it says:

- **Count.** A Large world has 52, 53 and 74 villages inside the border, far above the manifest minimum of 8. No swamp village appeared in the three worlds.
- **Walkability.** 87% of the doors are reachable, but only 47% of the villages meet the [M4 acceptance rule](../milestones/m4-settlements.md#acceptance) for every door. Of the 380 unreachable doors, 100 sit 2 to 6 blocks above or below the nearest walkable position, 102 are level with it or one block off but cut off, and 178 have no walkable position within three blocks. Terralith's fortified villages and snowy villages are the worst; none of them is fully walkable.
- **Residents.** All 1,858 villagers are MCA Reborn villagers: no vanilla villager and no other NPC is left in a village. A village has 9 residents in the median, the fortified ones about 20.
- **MCA's own villages form by themselves.** Ten seconds after loading, 26 residents were already `mca:guard`: a resident finds the bell, MCA registers a village and its guard manager starts. With ADR-0030 the pack switches that manager off.
- **Professions and trades.** At that moment most residents have no profession yet (they take job sites over time); the 184 with a profession all had trade offers. Whether the VillagersTradingPlus trades reach them is still open (TD-19).
- The measurement does not cover the Archeologist of Better Archeology, who lives in his own camps, and it is a snapshot of freshly loaded villages.

## Open design questions

Answered by [ADR-0030](../decisions/ADR-0030-resident-integration.md): tasks reach a resident through the vanilla Brain with one Mixin into MCA; buildings are vanilla structure templates plus our definition.

- How MCA's own births and deaths interact with village population rules.
- MCA Reborn has its own villages and buildings, professions, player ranks and rank tasks. For each area above: do we keep MCA's system, switch it off or extend it? One owner per piece of data either way.
