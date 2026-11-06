---
status: active
last_reviewed: 2026-11-06
---

# M5 — Unloaded-world simulation

## Goal

Villages keep living while no player is nearby. On return the results are visible: new houses, stock, residents born and dead.

M5 comes after M4 on purpose: the simulation is designed on the real settlement data model. Rules and entities are in [design-docs/simulation-model.md](../design-docs/simulation-model.md).

## Playable result

"Living World": leave a region, come back later, see what changed.

Scope note: in singleplayer and LAN, in-game time passes only while the host has the world open. "Living without players" means regions nobody is near while the world runs; offline catch-up for real-world time is out of scope unless decided later.

## Mods and modules

- Our `livingkingdoms-core`; NeoForge [SavedData](https://docs.neoforged.net/docs/datastorage/saveddata/) and attachments.

## Phases

**A — model.** `World`, `GeographicRegion`, `SimulationCell`, `WorldClock`, `WorldEvent`, place IDs, save format version. List which M4 processes have an abstract counterpart: production, consumption, construction, demography.

**B — single authority.** One authoritative state per village and region. At any moment a village is driven by either the physical or the abstract simulation, never both. A "last computed day" marker. No async block or entity changes off the server thread.

**C — compute and materialize.** Abstract computation uses the same rules as physical work (recipes or normalized costs). A `Materializer` applies accumulated changes on load with a per-tick budget. Births in an unloaded village create a resident record; the MCA entity appears on materialization.

## Acceptance

- 1/10/100 unload/load cycles without duplicated residents, goods or buildings.
- Accelerated simulation for N in-game days gives plausible results.
- Quitting and reloading mid-simulation is safe.
- 4 players in different regions at once (LAN).

## Out of scope

- Replacing Minecraft's ChunkManager (D-10).
- Politics and trade (M6), climate (M7).
