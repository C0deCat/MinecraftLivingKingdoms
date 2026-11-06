# ADR-0001: Our own settlement mod on MCA Reborn instead of Millénaire

**Status:** Accepted · **Date:** 2026-11-06

## Context

Roadmap v0.2 built living villages on three mods: Millénaire (settlements, work, construction), MCA Reborn (resident personality and family), MCA Capitals (politics). Our own unloaded-world simulation was supposed to run on top of them.

The Millénaire license audit (details in [licenses.md](../licenses.md#millénaire)) showed:

- the license is **All rights reserved**, with permission to include the mod in modpacks;
- the Java sources are **closed**: the public GitLab only contains content files, last updated in 2018;
- forking, modifying and re-uploading the JAR are forbidden;
- the mod is not on Modrinth, and the CurseForge version lags behind the website (9.0.2 vs 9.0.3).

Consequences for the project goals:

- simulating unloaded villages would require re-implementing Millénaire's economy without seeing its code;
- Millénaire and MCA residents are two different NPC models that would have to be glued together;
- integration with Farmer's Delight and magic is limited to what sub-mods allow, or requires blind patches;
- every Millénaire update may break the patches.

## Decision

1. Millénaire is **not used** — neither in the pack nor as a source of code (including decompilation) or assets. Studying its ideas and mechanics in-game and on its wiki is allowed.
2. The settlement niche is filled by our own mod `livingkingdoms-settlements`.
3. **Residents are MCA Reborn entities.** MCA owns personality, family, relationships, appearance; our mod owns settlement membership, profession, home, work, storage and village development. Each resident exists exactly once.
4. Politics — MCA Capitals, added in M6.
5. The mod is developed in vertical slices S1–S4 (see roadmap, section 7), each one a playable release.

## Options considered

- **Millénaire + MCA + adapters** (plan v0.2) — rejected for the reasons above.
- **Millénaire as a temporary solution in early alphas** — rejected: two village systems in one world cause confusion, and replacing it later would require a new world and redoing integrations.
- **Our own mod with its own NPCs, without MCA** — rejected: we would have to rewrite personality, family, appearance and dialogue that MCA already provides.

## Consequences

- This is the largest amount of custom code in the project; the scope is deliberately smaller than Millénaire (one culture first).
- The settlement data model is designed for abstract simulation (M5) from day one.
- If the mod depends deeply on MCA Reborn code (GPLv3), it is released under GPLv3. Check in M4-A.
- To research: the MCA Reborn API (external tasks for residents, data storage, events), open construction and logistics code (MineColonies, Structurize, etc.) and their licenses.
