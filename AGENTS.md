# AGENTS.md — Living Kingdoms

Instructions for AI agents (Codex, Claude Code and others) working in this repository. This is the primary rules file: skills and other documents extend it but never override it.

## 1. The project

**Living Kingdoms** is a Minecraft fantasy pack that gradually turns into a game with a living world: a finite map, adventure, household life, magic, living villages, states and a world that develops without players.

- **Core principle:** every milestone is a beautiful, cohesive, fun-to-play release of the pack. Mod cohesion and the amount of custom logic grow gradually.
- **Play mode:** 1–4 players in singleplayer or Open to LAN; the world is hosted by one player. A dedicated server is a long-term goal.
- **Publishing:** planned; platform not chosen.

**Current status:** planning is done; there is no code or pack manifest yet. Next stage — **M0 (infrastructure)**.

## 2. Read before working

| Document | When |
| --- | --- |
| [docs/roadmap.md](docs/roadmap.md) | Always: milestones, decisions D-01…D-17, acceptance criteria |
| [docs/decisions/](docs/decisions/README.md) | Before changing the architecture or milestone order |
| [docs/licenses.md](docs/licenses.md) | Before adding a mod or using third-party code or assets |
| [docs/compatibility-matrix.md](docs/compatibility-matrix.md) | Before adding or updating a mod |

## 3. Language

- **All documentation, code comments, commit messages and identifiers are in English.** No exceptions for new or edited files.
- The only non-English file is the historical [docs/archive/roadmap-v0.2.md](docs/archive/roadmap-v0.2.md); leave it as is.
- Chat with the user may be in any language the user writes in (usually Russian).

## 4. Platform — do not change without an ADR

- Minecraft Java **1.21.1**, **NeoForge 21.1.x** (exact version pinned in M0), **Java 21**. Why 1.21.1: see D-02 in the roadmap.
- Check every API against the NeoForge 1.21.1 documentation and the sources/JAR of the **installed** mod version. Examples from skills, forums and model memory often target other Minecraft versions — do not trust them without checking.
- Developer environment: Windows 11, PowerShell (Git Bash is also available). Installed: Java 21, Node 24, Python 3.13, Git.

## 5. Change hierarchy

Pick the lowest level that solves the task:

0. Change nothing if it already works.
1. Mod config.
2. Tags, datapack.
3. Existing addon or bridge between mods.
4. KubeJS.
5. Our own Java code or adapter (including Mixins in **our** mod).
6. Fork of another mod — only when proven necessary and the license allows it. Requires an ADR.

## 6. Licenses — hard rules

- **Never copy code of closed (All Rights Reserved) mods**, including decompiled code, and never put it into the repository or into context for code generation. Such mods may be studied in-game and through their documentation.
- Do not modify or redistribute third-party assets (structures, textures, models, texts) as our own unless the license explicitly allows it. Example: CTOV is CC BY-NC-ND 4.0.
- Before adding a mod, check its license and record it in [docs/licenses.md](docs/licenses.md).
- **Millénaire is not used** — neither as a mod nor as a source of code or assets; ideas only ([ADR-0001](docs/decisions/ADR-0001-own-settlement-mod.md)).
- Code from open mods (MIT, LGPL, GPL) may be used under the license terms; for GPL this affects our mod's license — record it in an ADR.

## 7. Adding or updating a mod

1. Find the official page (Modrinth, CurseForge, the author's GitHub). Never download JARs from re-uploads.
2. Check Minecraft 1.21.1 + NeoForge support, dependencies, side (client/server), license.
3. Add via packwiz (after M0) — never commit JARs manually.
4. Launch the game: singleplayer first, then Open to LAN with a second client; check a basic scenario.
5. Record the result in [docs/compatibility-matrix.md](docs/compatibility-matrix.md) and [docs/licenses.md](docs/licenses.md).
6. Update mods one at a time or in small batches, never all at once.

## 8. Task status — report honestly

| Status | What is confirmed |
| --- | --- |
| Code complete | Code or config is written |
| Build passed | Build and manifest checks pass |
| Runtime passed | The game launches (singleplayer/LAN) and the target action works |
| Gameplay verified | The mechanic is correct from a gameplay point of view (often needs a human) |

Never declare a task done because it compiled. Report the status reached and what was **not** verified. Zero discovered GameTests is a failed check, not a success.

## 9. Testing

- **Primary environment:** singleplayer and Open to LAN (integrated server on the host, 1–4 clients). Many bugs only show up with a remote client, so test LAN joins for anything with networking or synced state.
- **Keep code server-safe:** no client-only classes in common code, correct client/server separation. A dedicated server is a long-term target and must not require a rewrite.
- Game instances in `runtime/` are generated and never committed.
- Until M10 test worlds are **disposable**: when worldgen changes, the world is recreated.
- Changes that touch saves are tested with unload/load cycles and world reloads.
- Measure performance with spark, not by eye. The host PC runs both the client and the integrated server — budget for that.

## 10. Needs user confirmation

Propose and ask instead of doing it yourself:

- changes to milestone order, platform, or decisions D-xx;
- adding or removing a mod from the main profile;
- changes to world generation, save format, mandatory structures;
- using third-party code or assets, forking any mod;
- publishing anything externally (releases, uploads, messages to mod authors).

Architecturally significant decisions are recorded as ADRs in [docs/decisions/](docs/decisions/README.md).

## 11. Working style

- Small commits with one purpose; format `type(scope): summary` (`feat`, `fix`, `docs`, `chore`, `test`, `refactor`).
- First research what existing mods and addons already do, then write code.
- Do not build custom tools while an existing one (packwiz, Gradle, Prism Launcher) does the job.

## 12. Repository layout

Now:

```text
AGENTS.md            — this file
CLAUDE.md            — imports AGENTS.md for Claude Code
README.md
docs/
  roadmap.md         — roadmap (source of truth for milestones)
  decisions/         — ADRs
  licenses.md        — mod licenses and rules
  compatibility-matrix.md
  archive/           — previous revisions of documents
```

Planned layout — section 14 of the roadmap. Directories are created when they get real content.

## 13. Commands

Will be added in M0 (packwiz, Gradle, launching the client). There is no build yet.
