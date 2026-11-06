---
status: active
last_reviewed: 2026-11-06
---

# M2 — Light gameplay: farming, food, crafts, decor

## Goal

A self-sufficient fantasy pack that is fun without NPC simulation. Food, cookware, farming, husbandry, cooking, storage and decor share one game language without incompatible ingredients.

## Playable result

"Living Gameplay": a cozy household and farming game on top of the M1 world.

## Mods

| Priority | Mod | Niche |
| --- | --- | --- |
| Base | [Farmer's Delight](https://www.curseforge.com/minecraft/mc-mods/farmers-delight) | Cooking foundation, kitchens, farming |
| Base | [Croptopia](https://www.curseforge.com/minecraft/mc-mods/croptopia) | Crop and fruit tree variety |
| Base | [[Let's Do] Farm & Charm](https://modrinth.com/mod/lets-do-farm-charm) | Farm aesthetics, husbandry; check overlaps with Farmer's Delight |
| Base | [[Let's Do] Vinery](https://modrinth.com/mod/lets-do-vinery) | Vineyards, drinks |
| Base | [[Let's Do] Meadow](https://modrinth.com/mod/lets-do-meadow) | Cheese making, alpine produce |
| Base | [[Let's Do] HerbalBrews](https://modrinth.com/mod/lets-do-herbalbrews) | Tea, coffee, medicinal drinks |
| Base | [Handcrafted](https://modrinth.com/mod/handcrafted) | Furniture and interiors |
| Base | [Supplementaries](https://modrinth.com/mod/supplementaries) + Moonlight Library | Functional decor |
| Candidate | Macaw's (selected modules) | Building elements |
| Candidate | Waystones / Sophisticated Backpacks / Lootr | Travel, carrying, multiplayer loot |
| UI | EMI, Jade | Recipe viewing, diagnostics |

Compatibility tools:
- [Pantrywork](https://modrinth.com/mod/pantrywork) — shared food tags for Farmer's Delight, Croptopia and Let's Do; test on our set first.
- [Farmer's (Delight) Croptopia](https://modrinth.com/project/farmers-croptopia) — if Pantrywork does not cover specific operations.
- [Almost Unified](https://modrinth.com/project/almostunified) — unifies identical resources; the config defines the canonical variant.
- [KubeJS](https://kubejs.com) — targeted recipe/tag/event edits; must not become an untested second codebase.

## Phases

**A — install in small batches.** Farmer's Delight + Croptopia, then the Let's Do farming mods, then decor, then convenience. Add newly generated crops and structures to the M1 worldgen regression.

**B — reconcile.** A duplicate matrix (cheese, salt, flour, oil, dough, berries, drinks, vegetables, tools, cooking stations): which items are interchangeable via tags and where recipes use a specific Item ID. Do not disable a nice mechanic just to remove a second item.

**C — integrate.** Canonical ingredients and cooking routes; fill gaps via Pantrywork/adapters, then datapacks/KubeJS. Several full chains: bread/cooking, cheese making, winemaking, herbalism. Consistent decor style without overloaded recipe tabs.

## Acceptance

- Products of 3–4 mods can be gathered and processed without unexpected incompatibilities.
- No critical resource duplication loops; recipe UI is correct.
- New resources actually exist in the finite world.
- Generation and farm costs measured.

## Out of scope

- NPC economy (M4 slice S3 and M6).

## Open questions

- Compare Pantrywork, Farmer's (Delight) Croptopia and Almost Unified on a live world.
