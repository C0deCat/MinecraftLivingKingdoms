---
status: active
last_reviewed: 2026-11-10
---

# M4 track: settlements

**Status:** Active

## Goal

[M4 — Settlements](../../milestones/m4-settlements.md) is built in two parts. On the branch `m4-settlements`: phases A and B and slices S1 and S2, merged into `main` as the last track after the [merge check](2026-11-10-parallel-tracks.md#merge-check). On `main` afterwards: the B recheck on the combined pack, slice S3 on the M2 content, S4 and M4-D. Done means the [M4 acceptance](../../milestones/m4-settlements.md#acceptance) bars are met (gameplay verified by the user).

## Context

- Track rules, folder, ports and number ranges (D-50–59, TD-50–59, ADR-0030–0039): [coordination plan](2026-11-10-parallel-tracks.md). Read it at the start of every session.
- Spec: [M4](../../milestones/m4-settlements.md); decision: [ADR-0001](../../decisions/ADR-0001-own-settlement-mod.md); design: [settlement-model](../../design-docs/settlement-model.md); code rules: [mods-src/AGENTS.md](../../../mods-src/AGENTS.md).
- MCA Reborn is added on this branch only (with the user's agreement) and reaches `main` with the merge.
- The branch has no M2 or M3 content until those tracks merge. Farmers and other workers read item and block tags, not item ids, so the M2 crops fit in later.
- Mechanics are tested on the flat test preset (D-23) through GameTest and e2e ([ADR-0005](../../decisions/ADR-0005-test-framework.md)); a testkit change is made on `main`.
- Open items resolved here: TD-02 (MCA Reborn license and API), TD-10 (settlement counts per size), TD-19 (VillagersTradingPlus) in the [tech-debt tracker](../tech-debt-tracker.md).
- Shared libraries added on this track: none yet.

This is a skeleton made when the tracks were set up; the first session of the track refines the steps.

## Steps

On the branch:

- [ ] M4-A foundation: MCA Reborn research (getting residents, giving tasks from outside, our data on the entity, events, what needs Mixins), license checks (MCA Reborn, MineColonies, Structurize), prototype: a resident performs one task of our mod and survives a world reload.
- [ ] M4-B reconcile: vanilla villages and villagers, MCA residents outside our villages, the M1 village structures, the walkable-layout measurement.
- [ ] M4-C slice S1: village base (templates, residents assigned to a village, homes, shared storage, farmer and lumberjack).
- [ ] M4-C slice S2: builder (development plan, construction from templates, walkable layout).
- [ ] Merge check and pull request.

On `main` after the merge:

- [ ] M4-B recheck on the combined pack (buildings and villagers M2 and M3 added).
- [ ] M4-C slice S3: kitchen and crafts on the M2 content, trade with the player.
- [ ] M4-C slice S4: population growth, several villages.
- [ ] M4-D accept: the acceptance bars, `npm run validate-world -- --full`, TPS measured, gameplay verified by the user.

## Progress log

- 2026-11-10 — Plan skeleton created on `main` with the track setup.

## Decision log
