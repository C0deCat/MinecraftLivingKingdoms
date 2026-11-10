---
status: stable
last_reviewed: 2026-11-10
---

# ADR-0006: M2, M3 and M4 are developed in parallel tracks

**Status:** Accepted · **Date:** 2026-11-10

## Context

After M1 the roadmap continues with M2 (household), M3 (magic) and M4 (settlements). D-04, D-05 and D-14 ([ADR-0002](ADR-0002-milestone-order.md)) made them strictly sequential: a milestone starts when the previous one is accepted.

Research on 2026-11-10 showed what the three milestones share:

- M2 and M3 are mostly pack composition (`modpack/`: mods, configs, tags, KubeJS). M4 is mostly code (`mods-src/`). They rarely edit the same files.
- Real dependencies are few. Slice M4-S3 (cook, baker, brewer) needs the canonical ingredients of M2-C. The M4-B reconcile of villages must hold on the final worldgen mod set. M3-C touches M2 through Ars Unification (Farmer's Delight recipes) and Almost Unified (Occultism ores). Magic professions are M6, not M4.
- Shared hotspots are the packwiz index, the mod and license registries, the D-xx / TD-xx / ADR numbers and the status lines.
- `runtime/` lives inside the repository, and the test-server and e2e ports were fixed, so two sessions in one checkout disturb each other (seen on 2026-11-10 in the [test framework plan](../exec-plans/active/2026-11-09-test-framework.md)).

## Decision

Development order and merge order are separate things.

- **Tracks.** M2, M3 and M4 are developed at the same time, each on its own long-lived branch in its own worktree folder with its own `runtime/`: `m2-household`, `m3-magic`, `m4-settlements`. `main` is the integration line and the place for cross-cutting work (tooling, the test framework, shared docs).
- **Merge order.** M2 → M3 → M4. The user may swap M2 and M3 if M3 is ready first; M4 is always last. Every merge is a pull request that the user merges after the merge check passed on the branch with `main` merged in.
- **What merges.** M2 and M3 merge accepted (phase D done on the branch). M4 merges its standalone part: phases A and B and slices S1 and S2. M4 then continues on `main` as the only line of work: the B reconcile is rechecked on the combined pack, slice S3 is built on the M2 content, then S4 and M4-D.
- **Releases** keep the milestone order: `main` only ever holds a cohesive playable state, and settlements reach it after household and magic.
- MCA Reborn stays out of `main` until the M4 merge, because it replaces villagers for every other test.

The working rules (file ownership, number ranges, the merge check, ports) are in the [coordination plan](../exec-plans/active/2026-11-10-parallel-tracks.md); they may be tuned there without a new ADR. D-04, D-05 and D-14 are reworded to match.

## Options considered

- **Keep the strict sequence** — rejected: M4 carries the largest risk of the project (whether MCA Reborn can be driven from outside) and would start last, while its code barely overlaps with the pack work of M2 and M3.
- **Two lines: pack (M2 → M3) and code (M4)** — fewer merges and no conflict between M2 and M3 on the pack files. Rejected by the user: one track per milestone with a fixed merge order is easier to follow and to watch.
- **Three tracks sharing one runtime** (one Prism, one instance per track) — rejected: it needs a rework of the instance, smoke, e2e and test-server tooling before any track can start, and sessions would still meet in one Prism.

## Consequences

- A check passed on a track says nothing about the combined pack. The merge check repeats smoke, mod verification, e2e and the 3-world validation (D-21) after `main` is merged into the track; the full validation on everything runs at M4-D.
- Number ranges for D-xx, TD-xx and ADRs leave gaps in the numbering.
- The user signs in to Prism once per track folder, and each folder costs several GB of disk.
- The user gets questions and gameplay checks from up to four sessions; performance measurements are taken only while the other sessions are not running the game.
- M4 is not accepted when it merges: its acceptance happens on `main` after slice S3.
