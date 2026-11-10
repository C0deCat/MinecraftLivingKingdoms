---
status: active
last_reviewed: 2026-11-10
---

# M2 track: household

**Status:** Active

## Goal

[M2 — Household](../../milestones/m2-household.md) is accepted in singleplayer on the branch `m2-household` and merged into `main` as the first track. Done means the [M2 acceptance](../../milestones/m2-household.md#acceptance) bars are met (gameplay verified by the user) and the [merge check](2026-11-10-parallel-tracks.md#merge-check) passed.

## Context

- Track rules, folder, ports and number ranges (D-30–39, TD-30–39, ADR-0010–0019): [coordination plan](2026-11-10-parallel-tracks.md). Read it at the start of every session.
- Spec: [M2](../../milestones/m2-household.md). Mod workflow: [AGENTS.md](../../../AGENTS.md#workflow); every mod is agreed with the user first.
- New crops and structures change worldgen: add them to the [World Content Manifest](../../design-docs/world-content.md#manifest-categories) and check with `npm run validate-world`.
- Settlement code will read item tags, not item ids (M4-S3), so the canonical ingredients chosen here should be reachable through common tags.
- Shared libraries added on this track: none yet.

This is a skeleton made when the tracks were set up; the first session of the track refines the steps.

## Steps

- [ ] M2-A foundation: install in small batches (Farmer's Delight + Croptopia, the Let's Do farming mods, decor, convenience), smoke and `verify-mods` after each batch; new worldgen content in the manifest.
- [ ] M2-B reconcile: the duplicate matrix (which items are interchangeable through tags, where recipes use a specific item id); compare Pantrywork, Farmer's (Delight) Croptopia and Almost Unified on a live world.
- [ ] M2-C integrate: canonical ingredients and cooking routes; the full chains (bread and cooking, cheese making, winemaking, herbalism); consistent decor.
- [ ] M2-D accept: the acceptance bars, `npm run validate-world -- --full`, generation and farm costs measured, gameplay verified by the user.
- [ ] Merge check and pull request.

## Progress log

- 2026-11-10 — Plan skeleton created on `main` with the track setup.

## Decision log
