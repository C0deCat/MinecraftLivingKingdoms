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
- Open items resolved here: TD-02 (MCA Reborn license and API), TD-10 (settlement counts per size), TD-19 (VillagersTradingPlus) in the [tech-debt tracker](../tech-debt-tracker.md).
- Shared libraries added on this track: none yet.
- Files that mention the slices and change only on `main` (to do there, not on this branch): the glossary row "Slice (S1–S4)", the roadmap rows that say "S1–S4", the track table and merge order of the coordination plan.

## Steps

On the branch, phase A:

- [x] Session start: fetch, merge `main` if it moved, baseline smoke before MCA Reborn.
- [x] Scope recorded: D-50, the spec with slices S1–S8, the areas in the settlement model, this plan.
- [ ] M4-A1: MCA Reborn 7.7.36 added with packwiz; `npm run verify-mods`, `npm run smoke`, a look at MCA residents in a village; compatibility matrix, licenses, pack changelog.
- [ ] M4-A2: MCA Reborn research from its sources, written as `docs/references/mca-reborn-7.7.md`:
  - residents: how to find them, what is stable across a reload, how vanilla villagers are replaced and which config controls it;
  - tasks from outside: the Brain (activities, schedules, tasks), what needs a Mixin;
  - data: what MCA keeps on the entity and in SavedData, whether attachments fit;
  - MCA villages and buildings: detection, homes, taxes, marriages and births, config switches;
  - MCA professions, guards, the link to vanilla professions and trades (input for TD-19);
  - MCA player ranks, reputation and rank tasks (input for S7 and S8);
  - dialogue and interaction: whether our entries and screens can be added.
- [ ] M4-A3: licenses and suitability of MineColonies, Structurize and two or three worker-NPC mods; rows in `licenses.md`; TD-02 closed.
- [ ] M4-A4: ADR-0030 (resident integration and the data model for all areas), shown to the user before the prototype:
  - where our data lives (recommended: SavedData keyed by the stable resident ID, an attachment on the entity only as the link);
  - how a resident gets a task;
  - per area, what of MCA is kept, switched off or extended;
  - the format of a profession and of a building template with levels;
  - where settlement GameTests live (D-51);
  - asked separately: the license of our mod, settlement counts per size (TD-10).
- [ ] M4-A5: prototype:
  - Core: stable IDs and the save format version in SavedData;
  - new project `mods-src/livingkingdoms-settlements/` with the MCA adapter package `livingkingdoms.settlements.mca`;
  - one task: a resident walks to ripe wheat, harvests it and delivers it to a chest, by tags;
  - debug command `/lksettle` (assign, status);
  - a GameTest and the e2e spec `tests/e2e/specs/settlements/resident-task-reload.spec.ts` (the task and the resident link survive save → quit → reopen).
- [ ] M4-A closed: spec open questions answered, progress log with the status reached, a gameplay check by the user, push.

On the branch, after phase A (refined when A is closed):

- [ ] M4-B reconcile: vanilla villages and villagers, MCA residents outside our villages, the M1 village structures, the Archeologist, TD-19, the walkable-layout measurement.
- [ ] M4-C slice S1: village base.
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

## Decision log

- 2026-11-10 — M4 scope widened to be close to Millénaire for one culture (D-50, the user's requirement). New slices get the numbers S5–S8 and S1–S4 keep their meaning, because ADR-0006 and the coordination plan refer to them; the order of work is stated separately in the spec.
- 2026-11-10 — The data model is designed in M4-A for all areas (professions, building levels, player standing, quests) and built slice by slice, so a later slice adds data and tasks instead of reworking an earlier one.
- 2026-11-10 — S5 comes right after S2 (the user chose professions as the first area to deepen): it needs no M2 content, and the builder needs stone and metal.
- 2026-11-10 — MCA Reborn sources are cloned into `runtime/research/` for reading instead of being browsed online: the research needs searches across the whole code base, and `runtime/` is never committed.
