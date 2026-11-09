---
status: active
last_reviewed: 2026-11-09
---

# Tech debt and open items

Known gaps, unverified facts and deferred decisions. Each item names where it is resolved. Remove an item when it is done, recording the outcome in the doc it affects.

| ID | Item | Resolve in | Added |
| --- | --- | --- | --- |
| TD-02 | Verify MCA Reborn license, platform hosting and API on its pages ([licenses.md](../licenses.md)) | M4-A | 2026-11-06 |
| TD-03 | Verify MCA Capitals license, 1.21.1 file and API | M6-A | 2026-11-06 |
| TD-06 | Decide on Dynamic Trees: affects worldgen and lumberjacks. Moved to M7 as a spike next to the weather mods (2026-11-07, with the user); M4 lumberjacks are built for vanilla and Terralith trees, and a Dynamic Trees adoption in M7 reopens them and M1 for a regression | M7-A (spike) | 2026-11-06 |
| TD-07 | Choose the publishing platform (Modrinth vs CurseForge) | Before the first public release | 2026-11-06 |
| TD-08 | Add CI running `npm run docs:check` once a GitHub remote exists; consider a scheduled doc-gardening run | When the remote exists | 2026-11-06 |
| TD-10 | Per-size content targets: mandatory structures (M1-C), settlement and state counts (M4, M6) for Test/Small/Medium/Large | M1-C, M4-A | 2026-11-06 |
| TD-11 | Coast variety beyond the world edge: the flat "+1 above sea" shore is still the most common shape (45%, as in natural Tectonic); needs surface or feature work (dunes, rocks, marsh), not the edge (ADR-0004) | M7 (atmosphere) or earlier on feedback | 2026-11-07 |
| TD-12 | The Test-size island stays close to square: 2,048 blocks leave no room for a long edge band; accepted, Test is a development size (ADR-0004 amendment 2026-11-08) | Only if Test is ever used for gameplay | 2026-11-07 |
| TD-13 | Check the End outer islands and gateways after the dragon on a bounded End, on Test (border 2,048) and Large | M1-C validator | 2026-11-07 |
| TD-15 | `/locate` of large When Dungeons Arise structures blocks the server for minutes (the test server runs with `max-tick-time=-1`); players meet it through explorer maps or commands. Not a worldgen defect: the validator uses the census, not `/locate`; decide whether players need a fix | M1-D | 2026-11-09 |
| TD-16 | Dungeons and Taverns adds `nova_structures:end_castle` to `#minecraft:eye_of_ender_located` next to the stronghold: check that eyes of ender in the Overworld still lead to the stronghold | M1-C1 validator | 2026-11-09 |
| TD-17 | Dungeons and Taverns ships jigsaw pools under `minecraft:nether_fortress/*`; YUNG's replaces the fortress, so they should stay unused. Not verified: look for D&T pieces in Nether fortresses | M1-C1 validator | 2026-11-09 |
| TD-18 | YUNG's witch huts and the D&T `mangrove_witch_hut` share swamp biomes; the census shows no overlap, but density in swamps was not checked | M1-C1 validator | 2026-11-09 |
| TD-19 | VillagersTradingPlus (The Graveyard dependency) changes villager trades: reconcile with MCA residents and our trade | M4-B, M6 | 2026-11-09 |
| TD-20 | Every teleport into fresh terrain stalls the integrated server 2.4–9.5 s (Large, 2026-11-08); walking spreads the cost. Decide on pre-generation (Chunky) per world size or a lighter worldgen setting | M1-D | 2026-11-09 |
