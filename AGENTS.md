# AGENTS.md — Living Kingdoms

A Minecraft 1.21.1 NeoForge fantasy pack that grows, milestone by milestone, into a game with a living world: finite map, adventure, household life, magic, villages of MCA Reborn residents, states, and a world that changes while nobody is watching. Played by 1–4 people in singleplayer or Open to LAN.

This file is a map. Details live in the linked docs; read the ones your task needs.

**Status:** planning done, no pack or code yet. Next: [M0 — Infrastructure](docs/milestones/m0-infrastructure.md).

## Non-negotiables

- **Platform:** Minecraft 1.21.1, NeoForge 21.1.x, Java 21 ([D-02](docs/decisions/README.md#lightweight-decisions)). Check every API against NeoForge 1.21.1 docs and the installed mod's sources; examples for other versions are a common trap.
- **English** for all docs, code comments, commit messages and identifiers (D-17). Chat with the user in their language.
- **Licenses:** never copy or decompile code of closed (ARR) mods; never redistribute modified third-party assets unless the license allows it ([licenses.md](docs/licenses.md)). Millénaire is not used at all ([ADR-0001](docs/decisions/ADR-0001-own-settlement-mod.md)).
- **Existing mods first:** config → datapack → addon → KubeJS → our code → fork, as described in [core-beliefs](docs/core-beliefs.md#2-existing-mods-before-our-own-code).
- **Honest status:** report code complete / build passed / runtime passed / gameplay verified, and what was not checked ([core-beliefs](docs/core-beliefs.md#4-honest-status)).

## Ask the user first

- Changing milestone order, the platform, or any D-xx decision.
- Adding or removing a mod in the main profile.
- Changing world generation, save format or mandatory structures.
- Using third-party code or assets, or forking any mod.
- Publishing anything outside the repo (releases, uploads, messages to mod authors).

## Where to look

| You need | Read |
| --- | --- |
| Why we do things this way | [docs/core-beliefs.md](docs/core-beliefs.md) |
| What we are building and in which order | [docs/roadmap.md](docs/roadmap.md) → [docs/milestones/](docs/milestones/) |
| How the system is layered, where code goes | [ARCHITECTURE.md](ARCHITECTURE.md) |
| Decisions and their reasons | [docs/decisions/README.md](docs/decisions/README.md) |
| Magic, settlement or simulation design | [docs/design-docs/index.md](docs/design-docs/index.md) |
| Current work and open items | [docs/exec-plans/](docs/exec-plans/README.md), [tech-debt-tracker](docs/exec-plans/tech-debt-tracker.md) |
| Mods, versions, conflicts | [docs/compatibility-matrix.md](docs/compatibility-matrix.md) |
| Can we use / ship this mod | [docs/licenses.md](docs/licenses.md) |
| Notes on external APIs and tools | [docs/references/index.md](docs/references/index.md) |
| Quality state and test bars | [docs/quality.md](docs/quality.md) |
| Every doc | [docs/index.md](docs/index.md) |

## Workflow

1. Read the milestone spec and the design docs your task touches.
2. Small change: just do it and describe it in the commit. Multi-step or multi-session work: create an exec plan with the `exec-plan` skill and keep its progress log current.
3. Adding or updating a mod: official source only (Modrinth, CurseForge, author's GitHub); check 1.21.1 + NeoForge support, dependencies, side and license; add it via packwiz; launch singleplayer, then Open to LAN with a second client; record the result in the compatibility matrix and licenses. One mod or a small batch at a time.
4. Testing: singleplayer and LAN are the primary environment; keep code server-safe (no client-only classes in common code) because a dedicated server is a long-term target. Test worlds are disposable until M10. Measure performance with spark on the host PC.
5. A decision made in conversation goes into [docs/decisions/](docs/decisions/README.md) before the work that depends on it.
6. A doc that turns out to be wrong is fixed in the same change.

## Verification

- `npm run docs:check` — doc linter: links, index coverage, frontmatter, English-only, file size budgets, ADR index, exec-plan sections, skill sync. Run it after editing docs; its messages say how to fix each problem.
- Game build and launch commands will be added in M0.

## Skills

Canonical skills live in `.agents/skills/` (Codex reads them there). Claude Code reads copies in `.claude/skills/`; after editing a skill run `npm run skills:sync`.

- `exec-plan` — create, update and close an exec plan.
- `doc-gardening` — find stale or wrong docs and fix or report them.

## Commits

Small commits with one purpose, format `type(scope): summary` (`feat`, `fix`, `docs`, `chore`, `test`, `refactor`).

## Environment

Windows 11, PowerShell (Git Bash also available). Installed: Java 21, Node 24, Python 3.13, Git.
