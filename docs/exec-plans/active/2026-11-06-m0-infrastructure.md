---
status: active
last_reviewed: 2026-11-06
---

# M0 — Infrastructure

**Status:** Blocked — waits for the LAN test (TD-09)

## Goal

Any developer or agent runs one command on a clean clone and gets a Prism Launcher instance with Minecraft 1.21.1, the pinned NeoForge and the pack's mods, with identical versions every time. `verify-mods` checks the pack against the mod registry, and an empty `livingkingdoms-core` Gradle project builds. Target level: runtime passed in singleplayer; LAN join verified later (see TD-09).

## Context

- Spec: [M0 — Infrastructure](../../milestones/m0-infrastructure.md). Platform: D-02 in [decisions](../../decisions/README.md#lightweight-decisions).
- Layout: [ARCHITECTURE.md](../../../ARCHITECTURE.md#planned-repository-layout). Mod registry: [compatibility-matrix](../../compatibility-matrix.md), [licenses](../../licenses.md).
- Facts checked 2026-11-06: NeoForge 21.1.265 is the newest 21.1.x; Prism Launcher 11.1.1; packwiz has no binary releases (newest commit `170b22d`, 2026-10-14); packwiz-installer-bootstrap v0.0.3; spark, ModernFix, FerriteCore and Lithium have NeoForge 1.21.1 builds on Modrinth.
- Tooling follows `tools/doc-lint`: Node 24 TypeScript with type stripping, no dependencies.

## Steps

- [x] Exec plan, spec deviations, TD item for the LAN test, ignore `tools/bin/`.
- [x] Install Go; `npm run tools:setup` builds packwiz at a pinned commit and fetches packwiz-installer-bootstrap into `tools/bin/`.
- [x] `packwiz init` in `modpack/` with the pinned NeoForge; pin it in D-02, the matrix and the spec; close TD-01.
- [x] `npm run instance`: portable Prism in `runtime/prism/`, instance with NeoForge, mods installed by packwiz-installer; singleplayer world opens.
- [x] Add spark, ModernFix, FerriteCore, Lithium one at a time; launch after each; matrix, licenses and `modpack/CHANGELOG.md` updated; spark baseline in a fresh world.
- [x] `npm run verify-mods`, including a negative test.
- [x] `mods-src/livingkingdoms-core` from the NeoForge 1.21.1 MDK; `gradlew build` passes; nested AGENTS.md and CLAUDE.md.
- [x] Clean clone reproduces the same NeoForge version and mod JAR hashes.
- [x] Reference notes (packwiz, Prism; NeoForge note deferred to the first real Core code), AGENTS.md status and commands, quality grade, `docs:check`.
- [ ] LAN: a second client joins via Open to LAN (TD-09).

## Progress log

- 2026-11-06 — Plan created from the approved Claude Code plan.
- 2026-11-06 — Go 1.27.3 via winget; `tools:setup` builds packwiz `170b22d`. Pack initialised on NeoForge 21.1.265. `npm run instance` installs portable Prism 11.1.1 and the instance. Status: runtime passed.
- 2026-11-06 — spark, ModernFix, FerriteCore, Lithium added one at a time; after each, the client reached the title screen with 0 ERROR lines (checked from latest.log). Singleplayer world created by the user; `/spark tps` and `/spark health` recorded in quality.md (preliminary, ~30 s window). Status: runtime passed (SP).
- 2026-11-06 — `verify-mods` passes; negative test (removed mod) reports index, registry and instance problems. `livingkingdoms-core` builds with `gradlew build`. Clean clone: `npm run instance` + `verify-mods` pass, SHA-256 of all 4 JARs identical, same component versions. Not verified: LAN join, dedicated server, `runClient` of the core mod.

## Decision log

- 2026-11-06 — Profiles (`worldgen`, `gameplay`, `magic`, `civilizations`) are deferred; `modpack/` is the single pack (= `full`). Milestones are cumulative and with a handful of mods a subset isolates nothing. A profile mechanism (one catalog, generated subsets) is added when a real subset is needed, e.g. for bisecting conflicts.
- 2026-11-06 — The LAN check is deferred until a second account or PC is available (TD-09). M0 is runtime-verified in singleplayer first; this plan stays open until the LAN test.
- 2026-11-06 — No Prism pre-launch command: Prism 11.1.1 mangled the quoted command ("Process failed to start"), and Prism rewrites instance.cfg on exit. Mods are synced by `npm run instance` / `npm run launch`; `instance` refuses to run while Prism is open.
- 2026-11-06 — `livingkingdoms-core` carries a placeholder license until the user chooses one (GPLv3 is the likely candidate, see licenses.md).
- 2026-11-06 — packwiz is built with `go install` at a pinned commit, because packwiz publishes no release binaries and nightly CI artifacts are not a stable pin.
