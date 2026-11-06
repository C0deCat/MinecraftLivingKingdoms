---
status: active
last_reviewed: 2026-11-06
---

# Core beliefs

The principles behind every decision in this project. Each has a reason; when a situation is not covered by a rule, reason from these.

## 1. Every milestone is a playable release

The point of the project is a game that is fun at every stage, not infrastructure that pays off at the end. Work is cut into vertical slices that a player can feel. If a step adds nothing playable, it should be small or justified.

## 2. Existing mods before our own code

Every line of our own code has to be maintained across updates. Pick the lowest level that solves the task:

0. change nothing if it works;
1. mod config;
2. tags, datapack;
3. existing addon or bridge;
4. KubeJS;
5. our own Java code or adapter, including Mixins in our mod;
6. a fork of another mod — only when proven necessary and the license allows it, with an ADR.

## 3. One source of truth per fact

Each piece of state (a resident's identity, a chest's contents, a mod's version) has exactly one owner. Duplicated state between mods or between physical and abstract simulation is the main source of duplication bugs. The same applies to docs: link instead of copying.

## 4. Honest status

A task is only as done as what was verified: code complete → build passed → runtime passed → gameplay verified. Report the level reached and what was not checked. A green build is not a working mechanic; zero discovered tests is a failed check.

## 5. Licenses are hard limits

We respect every mod's license. Closed (ARR) mods are studied in-game, never copied or decompiled into the repo (D-16). Third-party assets are not modified and redistributed unless the license allows it. When in doubt, ask the author and record the answer in [licenses.md](licenses.md).

## 6. The repository is the system of record

Anything an agent cannot find in the repo does not exist for it. Decisions from chats, research results and user preferences are written into [decisions/](decisions/README.md), design docs or milestone specs. When a doc turns out to be wrong, it is fixed in the same change that discovered it.

## 7. Boring, verifiable technology

Prefer stable, well-documented tools (packwiz, Gradle, NeoForge APIs) whose behavior an agent can check. Always verify APIs against the docs and sources of the exact installed version; examples for other Minecraft versions are a common trap.

## 8. Disposable worlds until release

Until M10, test worlds are recreated whenever worldgen changes; player saves are not migrated. This keeps worldgen decisions cheap to revisit early.

## 9. English everywhere in the repo

All documentation, code comments, commit messages and identifiers are in English (D-17), so any agent or contributor can work with them.
