---
status: active
last_reviewed: 2026-11-10
---

# Parallel tracks: M2, M3 and M4 side by side

**Status:** Active

## Goal

M2, M3 and M4 are developed at the same time in separate sessions and reach `main` in the order M2 → M3 → M4 without one track breaking another ([ADR-0006](../../decisions/ADR-0006-parallel-tracks.md)). Done means all three pull requests are merged, the track folders are removed and M4 continues on `main` under its own plan.

This plan coordinates the tracks and holds the rules every track session follows. The milestone work itself is in the track plans: [M2](2026-11-10-m2-household.md), [M3](2026-11-10-m3-magic.md), [M4](2026-11-10-m4-settlements.md).

## Context

Decision and its reasons: [ADR-0006](../../decisions/ADR-0006-parallel-tracks.md). This plan is edited only on `main`.

### Tracks

| Branch | Folder | Scope | Test-server / e2e port |
| --- | --- | --- | --- |
| `main` | `MinecraftLivingKingdoms` | Merges, shared tooling, the [test framework](2026-11-09-test-framework.md), shared docs | 25599 / 25598 |
| `m2-household` | `MinecraftLivingKingdoms-m2` | [M2](../../milestones/m2-household.md), phases A–D | 25609 / 25608 |
| `m3-magic` | `MinecraftLivingKingdoms-m3` | [M3](../../milestones/m3-magic.md), phases A–D | 25619 / 25618 |
| `m4-settlements` | `MinecraftLivingKingdoms-m4` | [M4](../../milestones/m4-settlements.md): phases A and B, slices S1 and S2 (S4 if it fits) | 25629 / 25628 |

The folders are siblings of the main checkout. Each has its own `runtime/` (Prism, instances, test server, reports), so tracks never share a game. The ports follow the branch name (`tools/pack/src/track.ts`), so two tracks can run a test server or e2e at once. `npm run smoke`, `npm run instance` and `npm run e2e` look only at the Prism and the games of their own folder (by executable path), so they never stop or wait for another track's game. A track session is opened in the track's folder, not in a worktree the app creates.

### Merge order

1. **M2 → `main`**, accepted on its branch (phase D).
2. **M3 → `main`**, accepted on its branch on the M1 + M3 pack; the merge check then runs on the combined pack.
3. **M4 → `main`**: phases A and B, slices S1 and S2.
4. **M4 continues on `main`** as the only line: recheck phase B on the combined pack, slice S3 on the M2 content, S4, M4-D with the full validation.

The user may swap M2 and M3 if M3 is ready first. M4 is always last. MCA Reborn stays on the M4 branch until step 3.

### Rules for a track session

- **Start of a session:** `git fetch`; if `origin/main` has new commits, `git merge origin/main` (never rebase: the branch is pushed), then `npm run smoke`.
- **Own milestone only.** A change to the tooling, the testkit or shared rules is made on `main` in the main folder; the track then merges `main`.
- **Files a track owns:** its milestone spec, its plan, its `.pw.toml` files and mod configs, its reference notes, its design doc (magic-system for M3, settlement-model for M4), its World Content Manifest entries (one file each).
- **Files changed only on `main`:** the Status line of `AGENTS.md` and `README.md`, `roadmap.md`, `quality.md`, the pack version in `pack.toml` and `CHANGELOG.md`, this plan.
- **Append-only tables** (mods, licenses, D-xx, TD-xx, indexes): a track adds its own rows. In a merge conflict keep both sides.
- **Number ranges,** so two tracks never give out the same number:

  | Where | D-xx | TD-xx | ADR |
  | --- | --- | --- | --- |
  | `main` | D-24–29 | TD-24–29 | ADR-0007–0009 |
  | M2 | D-30–39 | TD-30–39 | ADR-0010–0019 |
  | M3 | D-40–49 | TD-40–49 | ADR-0020–0029 |
  | M4 | D-50–59 | TD-50–59 | ADR-0030–0039 |

- **Names by milestone:** KubeJS scripts `m2_*.js` and `m3_*.js`, a datapack folder per milestone, pack changelog entries under `## Unreleased — M2` (the version number is set at the merge).
- **Shared libraries** (Curios, Patchouli, Moonlight and the like): a track adds the library on its branch and lists it under "Shared libraries" in its plan, so the merge check compares versions.
- **`modpack/index.toml` and the index hash in `pack.toml`** are never merged by hand: take either side and run `packwiz refresh` from `modpack/`.
- **Measurements** (spark, TPS, `npm run validate-world -- --full`): ask the user first whether the other sessions are idle; a busy host gives wrong numbers.
- Asking the user before adding a mod applies as before ([AGENTS.md](../../../AGENTS.md#ask-the-user-first)).

### Merge check

On the track's folder, after `git merge origin/main`:

1. `./gradlew build` in `mods-src/`, `npm run verify-mods`, `npm run docs:check`, `npm run smoke`, `npm run e2e`.
2. `npm run validate-world` (3 Large worlds, D-21).
3. Cross-track checks:
   - **M3:** recipe and tag load errors in the log; Ars Unification with Farmer's Delight; Almost Unified with Occultism ores and dusts; KubeJS scripts of both milestones; versions of shared libraries.
   - **M4:** MCA residents with traders and villagers from M2 and M3; TD-19 (VillagersTradingPlus); buildings M2 and M3 added to villages.
4. A short gameplay check by the user on the combined pack.
5. Push the branch and open a pull request with the results; the user merges it.
6. On `main` after the merge: Status in `AGENTS.md` and `README.md`, the grade in `quality.md`, the pack version and `CHANGELOG.md`, the progress log here. The track plan moves to `completed/` (the M4 plan stays active until M4-D). The track's folder and branch are removed.

## Steps

- [x] Setup on `main`: ADR-0006, glossary terms, this plan and the three track plans, ports by branch, docs updated.
- [ ] Branches and folders created; `npm run instance` in each; the user signs in to Prism in each folder.
- [ ] Setup verified: smoke in each folder, two test servers at once, e2e on a track port, a trial `git merge main` on each branch.
- [ ] M2 merged into `main`.
- [ ] M3 merged into `main`.
- [ ] M4 (A, B, S1, S2) merged into `main`.
- [ ] Track folders and branches removed; M4 continues on `main` under its plan; this plan closed.

## Progress log

- 2026-11-10 — Research and the scheme agreed with the user in a Claude Code plan-mode session: three tracks, merge order M2 → M3 → M4, own runtime per folder, merges by pull request.
- 2026-11-10 — Setup on `main`, runtime passed for the tooling part:
  - ADR-0006, D-04 / D-05 / D-14 reworded, glossary terms, this plan, the three track plan skeletons, roadmap, AGENTS.md, README, M4 spec; `npm run docs:check` clean;
  - `tools/pack/src/track.ts`: test-server and e2e ports by branch; the e2e instance gets the port as a JVM argument;
  - `smoke.ts` and `instance.ts` scoped to their own folder. Found on the way: `stopGame` matched games by command line, which Sodium replaces at startup, so since M1-B a smoke run left its game running; games are now matched by the Java runtime under this folder's `runtime/prism/java`;
  - `tsc` for the harness and `tools/pack`, `npm run e2e:unit` (14 tests) pass; `npm run smoke` on `main`: title screen in 25 s, the 2 known GeckoLib ERROR lines of Mowzie's Mobs, game and Prism stopped afterwards;
  - `LK_E2E_PORT=25608 npm run e2e -- specs/session/hello.spec.ts` on `main`: the client listens on the port passed as a JVM argument, both specs pass.
- 2026-11-10 — Branches and folders created from `main` at `a232fa4`; `npm run instance` passed in each folder (48 pack mods, our two JARs). Ports by branch: M2 25609 / 25608, M3 25619 / 25618, M4 25629 / 25628. `npm run test-server -- --size test --cmd "lkworld info"` started in the M2 and M3 folders at the same moment: both worlds opened, on 25609 and 25619 (the two servers were listening one after the other, not for the same seconds). `git merge main` on each branch: already up to date.

  Not run: smoke and e2e in the track folders. They need the user's Prism sign-in in each folder first.

## Decision log

- 2026-11-10 — One track per milestone instead of a pack line and a code line: easier to follow and to watch (the user's choice, ADR-0006).
- 2026-11-10 — Own `runtime/` per track folder instead of one shared Prism: almost no tooling change and full isolation, for one sign-in and a few GB per folder (the user's choice).
- 2026-11-10 — Merges by pull request that the user merges: the approval and the merge moment stay visible in the history (the user's choice).
- 2026-11-10 — Number ranges instead of numbering at the merge: a decision can be written down before the work that depends on it ([documentation rules](../../documentation.md#what-to-update-when)) without a commit on `main`.
