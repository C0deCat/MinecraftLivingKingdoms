# AGENTS.md — Living Kingdoms

A Minecraft 1.21.1 NeoForge fantasy pack that grows, milestone by milestone, into a game with a living world: finite map, adventure, household life, magic, villages of MCA Reborn residents, states, and a world that changes while nobody is watching. Developed and tested in singleplayer; multiplayer (Open to LAN for 1–4 players, dedicated server) is milestone M10.

This file is a map. Details live in the linked docs; read the ones your task needs.

**Status:** M0 accepted in singleplayer. [M1 — World and adventure](docs/milestones/m1-world-and-adventure.md): step M1-A1 (base generator) done: four fixed world sizes, own size selector, per-dimension borders, natural world edge, Twilight Forest 1:1 ([ADR-0004](docs/decisions/ADR-0004-world-sizes-and-dimensions.md)). M1-A2 (structure and adventure mods) done: YUNG's set, When Dungeons Arise, Dungeons and Taverns, Structory, Moog's, Better Archeology and three boss mods ([exec plan](docs/exec-plans/completed/2026-11-07-m1-structures.md)). M1-B (reconcile) done: vanilla-style villages as the placeholder source until M4, `/lkworld structures` census, renderer Sodium + Iris ([exec plan](docs/exec-plans/completed/2026-11-08-m1-reconcile.md)). M1-C (integrate) done: World Content Manifest, `/lkworld validate`, guaranteed structures, land lift; Complementary Reimagined as the default shader pack ([exec plan](docs/exec-plans/completed/2026-11-09-m1-integrate.md)). Next: M1-D (acceptance).

## Non-negotiables

- **Platform:** Minecraft 1.21.1, NeoForge 21.1.265, Java 21 ([D-02](docs/decisions/README.md#lightweight-decisions)). Check every API against NeoForge 1.21.1 docs and the installed mod's sources; examples for other versions are a common trap.
- **English** for all docs, code comments, commit messages and identifiers (D-17). Chat with the user in their language.
- **Licenses:** never copy or decompile code of closed (ARR) mods; never redistribute modified third-party assets unless the license allows it ([licenses.md](docs/licenses.md)).
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
| Which docs to update during a task, how to add one | [docs/documentation.md](docs/documentation.md) |
| What a project term or label means | [docs/glossary.md](docs/glossary.md) |
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
3. Adding or updating a mod: first check the [Rejected mods](docs/compatibility-matrix.md#rejected-mods) list and do not propose a rejected mod again without new facts. Official source only (Modrinth, CurseForge, author's GitHub); check 1.21.1 + NeoForge support, dependencies, side and license; add it via packwiz; launch singleplayer; record the result in the compatibility matrix and licenses. One mod or a small batch at a time.
4. Testing: singleplayer is the only test environment until M10 (multiplayer milestone, D-15). Keep code server-safe anyway (logical client/server split, no client-only classes in common code, state changes on the server thread) so M10 stays a verification pass. Test worlds are disposable until M11. Measure performance with spark on the host PC.
5. Keep docs current in the same change as the work: decisions, open questions, mods, plans, status — see the table in [docs/documentation.md](docs/documentation.md). A decision made in conversation is written down before the work that depends on it; a doc that turns out to be wrong is fixed right away.

## Verification

- `npm run docs:check` — doc linter: links, index coverage, frontmatter, English-only, file size budgets, ADR index, exec-plan sections, milestone phase headings, skill sync. Run it after editing docs; its messages say how to fix each problem.
- `npm run instance` — the one setup command: builds pinned packwiz into `tools/bin/` (needs Go), installs portable Prism into `runtime/prism/`, creates the instance and installs the pack's mods. Close Prism first. Safe to rerun.
- `npm run launch` — syncs mods and starts the game (first time: the user signs in to Prism; never enter credentials). Log: `runtime/prism/instances/living-kingdoms/minecraft/logs/latest.log`.
- `npm run smoke` — rebuilds our mods, launches the game to the title screen, prints loaded mods and ERROR/FATAL log lines, closes the game (`-- --keep` leaves it open for a manual world test). Use after every mod or code change.
- `npm run verify-mods` — pack vs pins, Modrinth metadata, the mod registry docs and the instance `mods/` folder. Run after every mod change.
- `npm run test-server -- --size <test|small|medium|large> [--fresh] [--cmd "<command>"] [--until "<regex>"]` — headless local server for worldgen checks (usage in `tools/pack/src/test-server.ts`). Debug commands from our world mod: `/lkworld info` (size, border, scale), `/lkworld map` (PNG and height grid of the Overworld from noise), `/lkworld coast` (coast steepness and variety along the border), `/lkworld structures [radius|border|all]` (census of structure starts in the current dimension, its whole border, or every dimension: border check, worst piece slope, water share, hanging pieces, entrances, overlapping pairs; CSV in `<world>/livingkingdoms/`), `/lkworld grounding <structure> [radius]` (placed blocks under that structure's entrances), `/lkworld validate` (the world against the World Content Manifest; report in `<world>/livingkingdoms/validation-<size>.md`). `--no-build` reuses the built JARs, so code can change while a series runs.
- `npm run validate-world` — `/lkworld validate` on fresh worlds, one table per size, exit code 1 on a critical gap; reports in `runtime/validation/`. Default: 3 Large worlds, the check during a milestone's phases (D-21, ~15 min); `-- --full`: Small, Medium and Large × 4 seeds, only for milestone acceptance (~1 h); `-- --size <s> --seeds a,b` for anything else.
- `mods-src/`: `./gradlew build` builds all our mods ([mods-src/AGENTS.md](mods-src/AGENTS.md)); `npm run instance` also runs it.
- Mod commands: run `../tools/bin/packwiz` from `modpack/` ([packwiz notes](docs/references/packwiz.md)).

## Skills

Canonical skills live in `.agents/skills/` (Codex reads them there). Claude Code reads copies in `.claude/skills/`; after editing a skill run `npm run skills:sync`.

- `exec-plan` — create, update and close an exec plan.
- `doc-gardening` — find stale or wrong docs and fix or report them.

## Commits

Small commits with one purpose, format `type(scope): summary` (`feat`, `fix`, `docs`, `chore`, `test`, `refactor`).

## Environment

Windows 11, PowerShell (Git Bash also available). Installed: Java 21, Node 24, Python 3.13, Git.
