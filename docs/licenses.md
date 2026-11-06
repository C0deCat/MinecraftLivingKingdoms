---
status: active
last_reviewed: 2026-11-06
---

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
- No paid access to the pack or parts of it is planned: some mod licenses explicitly forbid it.
- Our mod's license will most likely be GPLv3 if it depends deeply on MCA Reborn code (GPLv3).

## 2. Registry

| Mod | License | Hosted on | In pack | Fork | Assets | Checked |
| --- | --- | --- | --- | --- | --- | --- |
| MCA Reborn | GPLv3 (unverified, early research) | CurseForge, Modrinth | ? | ? | ? | Not checked |
| MCA Capitals | GPLv3 (unverified, early research) | CurseForge, Modrinth | ? | ? | ? | Not checked |
| CTOV | CC BY-NC-ND 4.0 (unverified, early research) | Modrinth | ? | No | No (ND) | Not checked |
| spark | GPL-3.0-only | Modrinth | Yes (link) | No | No | 2026-11-06, Modrinth page |

Other mods are added as they are audited. "Unverified" means the value still needs to be confirmed on the mod's own page.

## 3. Rejected mods

License findings for mods we decided not to use. The full list with reasons is in the [compatibility matrix](compatibility-matrix.md#rejected-mods).

### Millénaire

Checked 2026-11-06: All Rights Reserved (modpack inclusion allowed, forking and modification not), Java sources closed, not hosted on Modrinth. Not used; see [ADR-0001](decisions/ADR-0001-own-settlement-mod.md). Source: [CurseForge license page](https://www.curseforge.com/minecraft/mc-mods/millenaire/license).
