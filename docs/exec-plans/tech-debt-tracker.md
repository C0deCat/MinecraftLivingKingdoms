---
status: active
last_reviewed: 2026-11-10
---

# Tech debt and open items

Known gaps, unverified facts and deferred decisions. Each item names where it is resolved. Remove an item when it is done, recording the outcome in the doc it affects.

While the tracks of [ADR-0006](../decisions/ADR-0006-parallel-tracks.md) are open, a new ID comes from the range of the branch that adds it: `main` TD-24–29, M2 TD-30–39, M3 TD-40–49, M4 TD-50–59.

| ID | Item | Resolve in | Added |
| --- | --- | --- | --- |
| TD-03 | Verify MCA Capitals license, 1.21.1 file and API | M6-A | 2026-11-06 |
| TD-06 | Decide on Dynamic Trees: affects worldgen and lumberjacks. Moved to M7 as a spike next to the weather mods (2026-11-07, with the user); M4 lumberjacks are built for vanilla and Terralith trees, and a Dynamic Trees adoption in M7 reopens them and M1 for a regression | M7-A (spike) | 2026-11-06 |
| TD-07 | Choose the publishing platform (Modrinth vs CurseForge) | Before the first public release | 2026-11-06 |
| TD-08 | Add CI running `npm run docs:check` once a GitHub remote exists; consider a scheduled doc-gardening run | When the remote exists | 2026-11-06 |
| TD-10 | Final per-size settlement counts for Small/Medium/Large, set when our layout places the villages (until then every generated village is a settlement, D-54), and state counts | M4 slice S2, M6 | 2026-11-06 |
| TD-11 | Coast variety beyond the world edge: the flat "+1 above sea" shore is still the most common shape (45%, as in natural Tectonic); needs surface or feature work (dunes, rocks, marsh), not the edge (ADR-0004) | M7 (atmosphere) or earlier on feedback | 2026-11-07 |
| TD-12 | The Test-size island stays close to square: 2,048 blocks leave no room for a long edge band; accepted, Test is a development size (ADR-0004 amendment 2026-11-08) | Only if Test is ever used for gameplay | 2026-11-07 |
| TD-15 | `/locate` of large When Dungeons Arise structures blocks the server for minutes (the test server runs with `max-tick-time=-1`); players meet it through explorer maps or commands. `/locate` also reports structures beyond the border (found in M1-A2). Not a worldgen defect: the validator uses the scan, not `/locate`. Does not block M1 (2026-11-10, with the user); check whether explorer or cartographer maps lead to WDA structures and decide whether players need a fix | M11-A | 2026-11-09 |
| TD-19 | VillagersTradingPlus (The Graveyard dependency) changes villager trades: reconcile with our trade. M4-B found that MCA residents get their offers through vanilla trade generation, which VillagersTradingPlus patches: all 184 residents with a trading profession in the survey had offers and the logs show no trade error. Left: the trade screen with an MCA resident in the game (M4-B gameplay check), then our own trade | M4 slice S3, M6 | 2026-11-09 |
| TD-21 | Pre-generation at world creation (D-20): a generation optimizer so a whole world (Large ≈ 1M chunks, hours and 5–10 GB with Chunky today) is generated in acceptable time, plus the creation flow (progress, resume after a crash); if it cannot be made fast enough, ship a pre-generated world as the main format | M5-A (the simulation needs the whole map); fallback decided by M11-A | 2026-11-09 |
| TD-22 | Underground hanging pieces the `grounding` processor does not cover (validator warnings): D&T bunker underground hallways and rooms that stick out of slopes; covering underground bunker pools also fills caves under them, so ask the user first. Also `graveyard:crypt` overlapping trial chambers and mineshafts underground. Surface warnings were settled in M1-D (see [world-content](../design-docs/world-content.md)) | M7 | 2026-11-09 |
| TD-23 | Credit Complementary Reimagined visibly on the pack's description page: it is enabled by default, which its license (Complementary License Agreement 1.7, 1.2a) requires to be credited | Before the first public release (M11) | 2026-11-10 |
