---
status: active
last_reviewed: 2026-11-06
---

# M0 — Infrastructure

**Status:** Active

## Goal

Any developer or agent runs one command on a clean clone and gets a Prism Launcher instance with Minecraft 1.21.1, the pinned NeoForge and the pack's mods, with identical versions every time. `verify-mods` checks the pack against the mod registry, and an empty `livingkingdoms-core` Gradle project builds. Target level: runtime passed in singleplayer; LAN join verified later (see TD-09).

## Context

- Spec: [M0 — Infrastructure](../../milestones/m0-infrastructure.md). Platform: D-02 in [decisions](../../decisions/README.md#lightweight-decisions).
- Layout: [ARCHITECTURE.md](../../../ARCHITECTURE.md#planned-repository-layout). Mod registry: [compatibility-matrix](../../compatibility-matrix.md), [licenses](../../licenses.md).
- Facts checked 2026-11-06: NeoForge 21.1.265 is the newest 21.1.x; Prism Launcher 11.1.1; packwiz has no binary releases (newest commit `170b22d`, 2026-10-14); packwiz-installer-bootstrap v0.0.3; spark, ModernFix, FerriteCore and Lithium have NeoForge 1.21.1 builds on Modrinth.
- Tooling follows `tools/doc-lint`: Node 24 TypeScript with type stripping, no dependencies.

## Steps

- [ ] Exec plan, spec deviations, TD item for the LAN test, ignore `tools/bin/`.
- [ ] Install Go; `npm run tools:setup` builds packwiz at a pinned commit and fetches packwiz-installer-bootstrap into `tools/bin/`.
- [ ] `packwiz init` in `modpack/` with the pinned NeoForge; pin it in D-02, the matrix and the spec; close TD-01.
- [ ] `npm run instance`: portable Prism in `runtime/prism/`, instance with NeoForge, mods installed by packwiz-installer; singleplayer world opens.
- [ ] Add spark, ModernFix, FerriteCore, Lithium one at a time; launch after each; matrix, licenses and `modpack/CHANGELOG.md` updated; spark baseline in a fresh world.
- [ ] `npm run verify-mods`, including a negative test.
- [ ] `mods-src/livingkingdoms-core` from the NeoForge 1.21.1 MDK; `gradlew build` passes; nested AGENTS.md and CLAUDE.md.
- [ ] Clean clone reproduces the same NeoForge version and mod JAR hashes.
- [ ] Reference notes (NeoForge, packwiz, Prism), AGENTS.md status and commands, quality grade, `docs:check`.
- [ ] LAN: a second client joins via Open to LAN (TD-09).

## Progress log

- 2026-11-06 — Plan created from the approved Claude Code plan.

## Decision log

- 2026-11-06 — Profiles (`worldgen`, `gameplay`, `magic`, `civilizations`) are deferred; `modpack/` is the single pack (= `full`). Milestones are cumulative and with a handful of mods a subset isolates nothing. A profile mechanism (one catalog, generated subsets) is added when a real subset is needed, e.g. for bisecting conflicts.
- 2026-11-06 — The LAN check is deferred until a second account or PC is available (TD-09). M0 is runtime-verified in singleplayer first; this plan stays open until the LAN test.
- 2026-11-06 — packwiz is built with `go install` at a pinned commit, because packwiz publishes no release binaries and nightly CI artifacts are not a stable pin.
