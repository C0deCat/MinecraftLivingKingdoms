---
status: active
last_reviewed: 2026-11-06
---

# M0 — Infrastructure and contracts

## Goal

Any developer or AI agent can deploy an identical test Minecraft instance from a clean repository, and the project is ready for controlled experiments.

## Playable result

None by itself — M0 is the foundation every later release is built on.

## Mods and tools

- NeoForge for 1.21.1 (pin the exact 21.1.x version here — D-02).
- [packwiz](https://github.com/packwiz/packwiz) — pack manifest, source of truth for the mod list.
- [spark](https://modrinth.com/mod/spark) — profiling.
- Prism Launcher for local instances; Git; Gradle; GitHub Actions later.
- After compatibility checks: [ModernFix](https://modrinth.com/mod/modernfix), [FerriteCore](https://modrinth.com/mod/ferrite-core), [Lithium](https://modrinth.com/mod/lithium). They do not replace measuring performance.
- Later: [Chunky](https://github.com/pop4959/Chunky) for pre-generation.

## Phases

**A — install.** Create profiles `worldgen`, `gameplay`, `magic`, `civilizations`, `full` and a shared catalog of pinned versions. Download JARs only via packwiz, Modrinth, CurseForge or official releases. Respect required dependencies and client/server sides.

**B — reconcile.** Remove duplicate JARs, wrong loader versions and conflicting client optimizers. Every update adds a changelog entry, a backup and a launch report.

**C — single system.** A pack check command (`verify-mods`) and the mod registry ([compatibility-matrix.md](../compatibility-matrix.md), [licenses.md](../licenses.md)) with purpose, version, license, side and test status. An empty `livingkingdoms-core` Gradle project — no premature simulation code.

**D — accept.** See below.

## Acceptance

- One command prepares a client instance from the repository.
- The world opens in singleplayer, and a second client can join via Open to LAN.
- No missing dependencies; the verified configuration is committed.
- A clean install reproduces the same set of versions.
- A dedicated-server smoke test is optional at this stage.

## Out of scope

- A custom JAR manager while packwiz does the job.
- Global NPC entities that do not exist yet.
- Pre-emptive forks of other mods.

## Open questions

- Exact NeoForge 21.1.x version (see [tech-debt-tracker](../exec-plans/tech-debt-tracker.md)).
- Prism Launcher vs another instance manager for scripted setup.
