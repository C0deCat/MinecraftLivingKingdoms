---
status: active
last_reviewed: 2026-11-10
---

# M4 track: settlements

**Status:** Active

## Goal

[M4 — Settlements](../../milestones/m4-settlements.md) is built in two parts. On the branch `m4-settlements`: phases A and B and the slices that need no M2 content (S1, S2, S5, then S4 and S6 while M2 and M3 are not merged), merged into `main` as the last track after the [merge check](2026-11-10-parallel-tracks.md#merge-check). On `main` afterwards: the B recheck on the combined pack, the remaining slices and M4-D. Done means the [M4 acceptance](../../milestones/m4-settlements.md#acceptance) bars are met (gameplay verified by the user).

## Context

- Track rules, folder, ports and number ranges (D-50–59, TD-50–59, ADR-0030–0039): [coordination plan](2026-11-10-parallel-tracks.md). Read it at the start of every session.
- Spec: [M4](../../milestones/m4-settlements.md); decision: [ADR-0001](../../decisions/ADR-0001-own-settlement-mod.md); design: [settlement-model](../../design-docs/settlement-model.md); code rules: [mods-src/AGENTS.md](../../../mods-src/AGENTS.md).
- Scope (D-50): close to Millénaire for one culture, slices S1–S8. The order of the slices and what each area gets when are in the spec: [order](../../milestones/m4-settlements.md#order-of-the-slices), [what deepens where](../../milestones/m4-settlements.md#what-deepens-where).
- MCA Reborn is added on this branch only (agreed with the user on 2026-11-10) and reaches `main` with the merge.
- The branch has no M2 or M3 content until those tracks merge. Farmers and other workers read item and block tags, not item ids, so the M2 crops fit in later.
- Mechanics are tested on the flat test preset (D-23) through GameTest and e2e ([ADR-0005](../../decisions/ADR-0005-test-framework.md)); a testkit change is made on `main`.
- MCA Reborn sources are read from a clone in `runtime/research/mca-reborn/` (ignored by Git, branch `1.21.1` of the author's GitHub repository). Its code is GPL-3.0 and is not copied into this repository.
- Open items in the [tech-debt tracker](../tech-debt-tracker.md): TD-10 (final settlement counts per size, in S2) and TD-19 (VillagersTradingPlus and our trade, in S3). TD-02 (MCA Reborn license and API) was closed in M4-A3.
- Shared libraries added on this track: none yet.
- Files that mention the slices and change only on `main` (to do there, not on this branch): the glossary row "Slice (S1–S4)", the roadmap rows that say "S1–S4", the track table and merge order of the coordination plan.

## Steps

On the branch, phase A:

- [x] Session start: fetch, merge `main` if it moved, baseline smoke before MCA Reborn.
- [x] Scope recorded: D-50, the spec with slices S1–S8, the areas in the settlement model, this plan.
- [x] M4-A1: MCA Reborn 7.7.36 added with packwiz; `npm run verify-mods`, `npm run smoke`, a look at MCA residents in a village; compatibility matrix, licenses, pack changelog.
- [x] M4-A2: MCA Reborn research from its sources, written as `docs/references/mca-reborn-7.7.md`:
  - residents: how to find them, what is stable across a reload, how vanilla villagers are replaced and which config controls it;
  - tasks from outside: the Brain (activities, schedules, tasks), what needs a Mixin;
  - data: what MCA keeps on the entity and in SavedData, whether attachments fit;
  - MCA villages and buildings: detection, homes, taxes, marriages and births, config switches;
  - MCA professions, guards, the link to vanilla professions and trades (input for TD-19);
  - MCA player ranks, reputation and rank tasks (input for S7 and S8);
  - dialogue and interaction: whether our entries and screens can be added.
- [x] M4-A3: licenses and suitability of MineColonies, Structurize and two or three worker-NPC mods; rows in `licenses.md`; TD-02 closed.
- [x] M4-A4: ADR-0030 (resident integration and the data model for all areas), shown to the user before the prototype:
  - where our data lives (recommended: SavedData keyed by the stable resident ID, an attachment on the entity only as the link);
  - how a resident gets a task;
  - per area, what of MCA is kept, switched off or extended;
  - the format of a profession and of a building template with levels;
  - where settlement GameTests live (D-51);
  - asked separately: the license of our mod, settlement counts per size (TD-10).
- [x] M4-A5: prototype:
  - Core: stable IDs and the save format version in SavedData;
  - new project `mods-src/livingkingdoms-settlements/` with the MCA adapter package `livingkingdoms.settlements.mca`;
  - one task: a resident walks to ripe wheat, harvests it and delivers it to a chest, by tags;
  - debug command `/lksettle` (assign, status);
  - a GameTest and the e2e spec `tests/e2e/specs/settlements/resident-task-reload.spec.ts` (the task and the resident link survive save → quit → reopen).
- [x] M4-A closed: spec open questions answered, progress log with the status reached, a gameplay check by the user, push.

On the branch, after phase A (refined when A is closed):

- [x] M4-B1: the measurement tool `/lksettle survey` (village starts inside the border, doors reachable on foot from the centre, who lives in each village, professions, trades), CSV in `<world>/livingkingdoms/`.
- [x] M4-B2: survey of the M1 villages on Large worlds of the three D-21 seeds: walkable share per village source, residents MCA / vanilla / other, the Archeologist, professions and trades with VillagersTradingPlus (TD-19).
- [x] M4-B3: decisions with the user, one question each, from the survey: what the M1 villages become when our settlements exist (the source of the starting villages), settlement counts per size (TD-10), the remaining MCA config switches (inn guests, taxes, bounty hunters, the first-join screen).
- [ ] M4-B4: the decisions applied (pack config, docs), smoke and e2e (done); M4-B closed with a gameplay check by the user: a generated village with MCA residents, the trade screen with a resident (TD-19), the destiny command without the teleport.
- [ ] M4-C slice S1, village base, in steps:
  - [x] S1-1 settlements: a settlement record for every generated village (D-53, D-54), found when one of its chunks loads; MCA villagers inside it become its residents; `/lksettle status` lists settlements; all of it survives a reload.
  - [x] S1-2 site safety (TD-51): open lava next to the buildings of an adopted village is removed chunk by chunk as the village loads, a smithy's own lava stays.
  - [ ] S1-3 profession framework: a profession is data (workplace, tasks, inputs and outputs by tag, tools) mapped to a villager profession; a settlement gives professions to its adult residents; a resident without work from us keeps MCA's brain.
  - [ ] S1-4 shared storage: the stock of a settlement lives in its record, reachable through a storage block the mod places at the centre.
  - [ ] S1-5 farmer: works the fields of its settlement and delivers to the storage (the prototype order becomes a profession task).
  - [ ] S1-6 lumberjack: fells and replants trees around the settlement (vanilla and Terralith trees, TD-06) and delivers to the storage.
  - [ ] S1-7 homes: a resident has a bed in its settlement.
  - [ ] S1-8 accept the slice: survey of Large worlds with settlements, GameTests, e2e, smoke, a gameplay check by the user, the pack changelog.
- [ ] M4-C slice S2: builder, walkable layout, building levels.
- [ ] M4-C slice S5: mining and trades.
- [ ] M4-C slices S4 and S6, as far as they fit before M2 and M3 are merged.
- [ ] Merge check and pull request.

On `main` after the merge:

- [ ] M4-B recheck on the combined pack (buildings and villagers M2 and M3 added).
- [ ] M4-C slices S4 and S6 if not done on the branch.
- [ ] M4-C slice S3: kitchen and crafts on the M2 content, simple trade.
- [ ] M4-C slice S7: player and village.
- [ ] M4-C slice S8: quests.
- [ ] M4-D accept: the acceptance bars, `npm run validate-world -- --full`, TPS measured, gameplay verified by the user.

## Progress log

- 2026-11-10 — Plan skeleton created on `main` with the track setup.
- 2026-11-10 — First session of the track, planned with the user in a Claude Code plan-mode session:
  - `git fetch`: `origin/main` has no new commits;
  - baseline `npm run smoke` before MCA Reborn: title screen after 26 s, 51 mods, the 2 known GeckoLib ERROR lines of Mowzie's Mobs; runtime passed;
  - scope widened with the user (D-50): slices S5–S8, the order of the slices and the areas table in the spec, the areas in the settlement model, phase A steps refined here.

  Checked while planning, read-only: MCA Reborn has a NeoForge 1.21.1 release 7.7.36 on Modrinth (2026-08-16), GPL-3.0-only, sources on GitHub (branch `1.21.1`, package `net.conczin.mca`, built against NeoForge 21.1.234), with its own village classes (`Village`, `Building`, `VillageManager`).
- 2026-11-10 — M4-A1, runtime passed for the launch: MCA Reborn 7.7.36 added with packwiz (Modrinth version `S2Ln2tIn`); `npm run smoke` reaches the title screen in 30 s with 52 mods and no new ERROR lines; `npm run verify-mods` OK. Not checked: a world with MCA residents (the prototype and M4-B do that).
- 2026-11-10 — M4-A2, research only: [mca-reborn-7.7](../../references/mca-reborn-7.7.md) written from the sources at the tag `7.7.36-beta.3+1.21.1`. Main findings: a resident is a vanilla `Villager` subclass with the vanilla Brain; all behaviour is set in one public static method that runs again on every brain refresh, so one Mixin there is the hook for our tasks; MCA chores need an online player and cannot drive village work; MCA has its own villages (scanned rooms), guards, marriages, births, taxes, ranks and rank tasks; buttons and dialogue are data, commands are a fixed switch; MCA fires no events. Nothing was run in the game.
- 2026-11-10 — M4-A3, research only: MineColonies and Structurize are GPL-3.0 with NeoForge 1.21.1 branches; the worker-NPC mods checked are closed, not on 1.21.1 or not needed ([licenses](../../licenses.md#construction-and-worker-mods-as-code-sources)). Outcome: own construction and logistics on vanilla structure templates and the vanilla Brain; their code only under GPL-3.0 and after asking the user. TD-02 closed. Their code was not read in detail.
- 2026-11-10 — M4-A4, proposed: [ADR-0030](../../decisions/ADR-0030-resident-integration.md) written from the research (resident record plus attachment, tasks through the Brain with one Mixin, our rules decide and MCA carries out, vanilla structure templates, D-51 for GameTests). The license of the settlement mod is decided with the user: GPL-3.0-only (D-52). The ADR waits for the user's acceptance; the prototype starts after it. Nothing of the ADR was run in the game.
- 2026-11-10 — ADR-0030 accepted by the user; D-51 locked. The prototype M4-A5 starts.
- 2026-11-10 — M4-A5 prototype, runtime passed:
  - Core: `StableId` (`kind:number`) and `CoreSaveData` (save format version 1, ID counters);
  - new mod `livingkingdoms_settlements` 0.0.1 (GPL-3.0-only): resident records in save data, the ID attachment, the labor activity and schedule, the harvest-and-deliver task (tags, item-handler capability), the MCA adapter with its one Mixin, `/lksettle spawn | assign | dismiss | status`;
  - `./gradlew build` passes; `./gradlew :livingkingdoms-settlements:runGameTestServer`: 2 GameTests pass (a resident harvests three wheat, replants and delivers; a villager without an order leaves the field alone). The first of them failed while the villager could not work, so it does detect a broken task;
  - `npm run e2e -- specs/settlements/resident-task-reload.spec.ts` passes in about 20 s: harvest, delivery, save → quit → reopen, second harvest, one resident record; the whole `npm run e2e` passes (13 tests), `npm run e2e:unit` 14 tests;
  - `npm run smoke`: title screen after 28 s, 53 mods, only the 2 known GeckoLib ERROR lines; `npm run verify-mods` OK; `npm run test-server -- --size test --cmd "lksettle status"` answers on the dedicated test server;
  - found on the way: MCA's first-join screen pauses the game and hung the e2e world, so the pack now ships `config/mca.json` with `launchIntoDestiny` false; GameTest coordinates start one below the template.

  Not checked: gameplay by the user; a resident's whole day (rest, meeting, panic, night) and behaviour inside a real village; several residents on one field; a full or missing chest beyond the code path; performance. `npm run validate-world` was not run (worldgen is unchanged).
- 2026-11-11 — M4-A closed, gameplay verified by the user for the task: in the play instance a villager spawned with `/lksettle spawn` and given an order harvested the wheat, replanted it and put wheat and seeds into the chest. The user did not report a save and reload in the play instance; that part rests on the e2e spec. Next: M4-B.
- 2026-11-11 — M4-B1, runtime passed: `/lksettle survey [max]` in the settlement mod (structure tag `village_source`, `Walkability`, `VillageSurvey`); 3 GameTests with known layouts (5 GameTests in all pass). The first run reported 0 reachable doors in two villages: the centre was the position nearest to the bell, often the rim of the well. The centre is now the largest walkable area around the bell, at its level or below.
- 2026-11-11 — M4-B2, measured on Large worlds of the seeds `livingkingdoms`, `alpha`, `20261107` (test server, about 20 min each): 179 villages, 87% of doors reachable, 47% of villages fully walkable, all 1,858 residents are MCA villagers. Table and findings: [settlement-model](../../design-docs/settlement-model.md#the-villages-the-world-generates-today). Not measured: VillagersTradingPlus trades on MCA residents (TD-19), the Archeologist camps, villages after residents have lived in them for days.
- 2026-11-11 — M4-B3, decided with the user, one question each: starting villages are the generated ones in S1 and our own from S2 (D-53); every generated village is a settlement until then, about 11 / 32 / 52–74 on Small / Medium / Large (D-54; Small and Medium counted on one seed each); the destiny screen is a character editor by command without the teleport (D-55). TD-19 for this phase: MCA residents get their offers through vanilla trade generation, which VillagersTradingPlus patches; all 184 residents with a trading profession in the survey had offers.
- 2026-11-11 — M4-B4, runtime passed for the changes: `config/mca.json` with `allowDestinyTeleportation` false; `npm run verify-mods`, `npm run e2e` (13 tests), `npm run smoke` (title screen after 24 s, the 2 known ERROR lines), 5 GameTests. `npm run validate-world` was not run: phase B changed no worldgen, and MCA Reborn ships no worldgen data. Not checked: the trade screen with an MCA resident and the destiny command in the game (the user's gameplay check).
- 2026-11-12 — M4-B gameplay check by the user, partly done: villages hold MCA residents, a farmer's MCA menu and the trade screen open (TD-19 for this phase). Not reported: the destiny command. The user found two worldgen problems, both reproduced on the test server from the world's seed: the guaranteed Lich altar inside a WDA coliseum (TD-50, a fix for `main`) and a village burning beside a lava lake (TD-51). The survey gained open-lava and fire counts next to buildings: 6 of 56 villages of that world stand at open lava.
- 2026-11-12 — S1-1 and S1-2, runtime passed: settlement records (`settlement` package, own save data), adoption of every `village_source` structure when a chunk with a reference to it loads, MCA villagers inside the bounds joined as residents on entity join, `SiteSafety` per loaded chunk, `/lksettle status` lists settlements. 7 GameTests pass. On a regenerated copy of the user's Large world (test server, all 56 villages loaded by the survey): 56 settlements, 503 residents of 510 MCA villagers counted near the villages, villages with open lava beyond a forge 6 -> 0 (one jungle village keeps a pool of 3, its own), none burning. The first rule (any lava inside a building's box is a forge) left three lakes that reach under buildings; a forge is now a pool of at most 4 lava blocks inside a box. Not checked: in the client, on other seeds, the 7 villagers that were not joined (outside the bounds when they appeared).

## Decision log

- 2026-11-10 — M4 scope widened to be close to Millénaire for one culture (D-50, the user's requirement). New slices get the numbers S5–S8 and S1–S4 keep their meaning, because ADR-0006 and the coordination plan refer to them; the order of work is stated separately in the spec.
- 2026-11-10 — The data model is designed in M4-A for all areas (professions, building levels, player standing, quests) and built slice by slice, so a later slice adds data and tasks instead of reworking an earlier one.
- 2026-11-10 — S5 comes right after S2 (the user chose professions as the first area to deepen): it needs no M2 content, and the builder needs stone and metal.
- 2026-11-10 — MCA Reborn sources are cloned into `runtime/research/` for reading instead of being browsed online: the research needs searches across the whole code base, and `runtime/` is never committed.
- 2026-11-10 — The clone is read at the tag `7.7.36-beta.3+1.21.1`, not at the head of the branch `1.21.1`: the installed file is built from the tag, and the two differ in 77 files (village and room scanner, navigation).
- 2026-11-10 — TD-10 (settlement counts per size) moves from M4-A to M4-B: the numbers depend on the census of the M1 villages and on where our starting villages come from, which phase B measures.
- 2026-11-10 — `livingkingdoms-settlements` is GPL-3.0-only and our other mods are decided before the first public release (D-52, the user's choice): only the settlement mod touches MCA Reborn, and the world generation sources need not be opened for that.
- 2026-11-10 — For residents our adapter builds the brain from MCA's public packages and leaves out MCA's job-site package (ADR-0030, proposed): otherwise MCA would keep assigning and removing professions next to ours. Adding our activity to MCA's finished brain is the fallback.
- 2026-11-10 — Marriages, births, guards and homes are decided by our settlement rules and carried out through MCA's public methods, with MCA's own village managers set to 0 in the pack config (ADR-0030, proposed): one owner per decision, and M5 can run the same rules off-screen.
- 2026-11-10 — The pack sets MCA's `launchIntoDestiny` to false: the screen pauses singleplayer in front of every new world, which hangs e2e, and it can move the player away from our spawn. The destiny command stays available once per player; M4-B decides with the user whether the pack offers the screen another way.
- 2026-11-10 — The prototype's order keeps no task state in the record: each trip is chosen again from the order, the world and the inventory. It survives a reload without a saved state machine; S1 adds saved task state only where a profession needs it.
- 2026-11-10 — GameTests sit in the main source set of the settlement mod (package `gametest`), not in a separate source set: NeoForge only looks for them when GameTest is enabled, which a production launch never is, and one source set keeps the build simple.
- 2026-11-10 — `/lksettle spawn` exists because `/summon` with NBT skips MCA's spawn setup and without NBT gives a random age: tests need an adult made the way MCA makes one.
- 2026-11-11 — MCA's inn guests, taxes and bounty hunters keep MCA's defaults until S7: they only act where a player scanned buildings with MCA's blueprint or has a bad reputation, and S7 settles what MCA's ranks mean in our villages.
- 2026-11-11 — MCA's guard, marriage and birth managers are switched off in the slice that brings our rule (S5, S4), not in phase B: switching them off now would leave the villages without guards and births until then.
- 2026-11-11 — The walkable-layout measurement takes the largest walkable area around the bell as the village centre and counts a door as reachable when its own position is in the area reached with steps of at most one block. Doors and fence gates count as open; water is not walked through. A stricter rule (for example a resident's real pathfinding) can replace it when our layout is built in S2.
- 2026-11-12 — A resident without work from us keeps the brain MCA builds (`Residents.hasWork`): adopting a village must not take professions, trades and daily life from its villagers before our professions exist (S1-3).
- 2026-11-12 — Villages are adopted from structure references of loaded chunks, not by scanning the world: every chunk a structure reaches into points to its start, so a village becomes a settlement whichever of its chunks loads first, and work on a chunk is queued to the next server tick because loading the start chunk inside a chunk-load event can deadlock.
- 2026-11-12 — Lava next to buildings hardens (source to obsidian, flowing to cobblestone) instead of being removed: no hole is left and nothing flows in from the rest of the lake.
