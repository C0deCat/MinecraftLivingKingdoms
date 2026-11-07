---
status: active
last_reviewed: 2026-11-07
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
| TD-12 | The Test-size island stays close to square: 2,048 blocks leave no room for a long edge band; accepted for a development size | Only if Test is used for gameplay | 2026-11-07 |
| TD-14 | The Test size has no reachable stronghold or Nether fortress: strongholds keep the vanilla concentric rings (nearest 1,700–8,600 blocks out on 4 seeds, border ±1,024), and fortresses sit 270–900 blocks from the Nether origin while the Test Nether border is ±256. Large is fine on all 4 seeds. Needs per-size placement or placed structures, or Test stays a development-only size (TD-12) | M1-C validator | 2026-11-07 |
| TD-13 | Check the End outer islands and gateways after the dragon on a bounded End, on Test (border 2,048) and Large | M1-C validator | 2026-11-07 |
