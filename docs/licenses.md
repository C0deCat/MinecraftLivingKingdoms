# Mod licenses and usage rules

A reference for "can we include, modify, distribute this" decisions. This is not legal advice: when in doubt, ask the mod author in writing (Discord, issues) and record the answer here.

## 1. General rules

### How the pack is distributed

- **Modrinth (`.mrpack`)** and **CurseForge** packs store links, not JARs: the launcher downloads mods from the platform. So even mods with a closed license can usually be included if they are hosted on the same platform.
- **Modrinth:** mods not hosted on Modrinth may be put inside the pack (overrides) only with the author's permission.
- **CurseForge:** only mods hosted on CurseForge whose authors allow third-party modpacks can be included.

### Levels of intervention

| Approach | What we distribute | Risk |
| --- | --- | --- |
| Configs, datapacks, KubeJS | Our own files | Practically none |
| Our mod with Mixin patches of another mod | Only our code | Low: no third-party code is distributed. Fragile across updates |
| Fork of an open mod (MIT, LGPL, GPL) | Modified third-party code | Allowed with conditions: GPL requires the fork to be GPL with open sources |
| Fork of a closed mod (ARR) | Modified third-party code | **Not allowed** without the author's permission |
| Modifying third-party assets | Modified structures, textures, models | Depends on the license; ND licenses forbid it |

### Project rules

- Code of closed mods is never copied or decompiled into the repository (D-16).
- No paid access to the pack or parts of it is planned: some licenses (e.g. Millénaire) explicitly forbid it.
- Our mod's license will most likely be GPLv3 if it depends deeply on MCA Reborn code (GPLv3).

## 2. Registry

| Mod | License | Hosted on | In pack | Fork | Assets | Checked |
| --- | --- | --- | --- | --- | --- | --- |
| Millénaire | ARR with modpack permission | CurseForge (9.0.2), millenaire.org (9.0.3) | Yes, with conditions | No | No | 2026-11-06 — **not used** ([ADR-0001](decisions/ADR-0001-own-settlement-mod.md)) |
| MCA Reborn | GPLv3 (per v0.2) | CurseForge, Modrinth | ? | ? | ? | Not checked |
| MCA Capitals | GPLv3 (per v0.2) | CurseForge, Modrinth | ? | ? | ? | Not checked |
| CTOV | CC BY-NC-ND 4.0 (per v0.2) | Modrinth | ? | No | No (ND) | Not checked |

Other roadmap mods are added as they are audited. "Per v0.2" means data from the roadmap that still needs to be confirmed on the mod's page.

## 3. Audits

### Millénaire

**Date:** 2026-11-06 · **Outcome:** not included in the pack; used only as a source of ideas.

- **License** (CurseForge): All rights reserved, but including it in modpacks is allowed if:
  - the pack links to the CurseForge page, the Discord or the mod's website;
  - all authors and maintainers are credited;
  - bugs are checked in the pack first and only then reported.
- **Forbidden:** claiming the mod as your own; limiting gameplay behind payment; re-uploading the JAR to other websites (except "modpack downloads" — the wording is vague).
- **Sources:** closed. The "Source" link points to a GitLab with content files (buildings, villagers, languages, quests), last commit in 2018, no Java code.
- **Versions:** 9.0.3 (2026-10-21, NeoForge 21.1.226) — only on millenaire.org; CurseForge has 9.0.2.
- **Extensibility:** sub-mods via `millenaire-custom`; in 9.0.3 sub-mods can hook in crops, trees and animals from other mods, and custom cultures can have their own NPCs, but only with already-coded actions.

Sources:
- https://www.curseforge.com/minecraft/mc-mods/millenaire/license
- https://www.millenaire.org/downloads
- https://gitlab.com/Millenaire/Public
