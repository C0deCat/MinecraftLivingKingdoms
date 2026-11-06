---
status: active
last_reviewed: 2026-11-06
---

# M6 — Economy and states

## Goal

Villages are connected by trade and politics, and NPCs use all M2–M3 content as consistently as the player.

## Playable result

"Living Kingdoms": villages trade, belong to states, and use crafts and magic.

## Mods

- [MCA Capitals](https://www.curseforge.com/minecraft/mc-mods/mca-capitals) — hierarchy, dynasties, titles, succession (exact file, license and API to verify).
- Content of Farmer's Delight, Let's Do, Ars Nouveau, Iron's, Occultism.

## Phases

**A — research.** MCA Capitals and how it maps onto our settlements and territory. Pilot industries: grain → mill/kitchen → food; vineyard → winery → export; dairy → trade; magic workshop → items and Source.

**B — data owners and units.** Capitals — titles and dynasties; Settlements — settlements and territory; Core — states as simulation entities (confirm in an ADR). Units: Item IDs and tags, commodities vs physical stacks, storage, costs, labor, prices. Never count the same chest twice (physically and abstractly).

**C — contracts.** `Commodity`, `ProductionProcess`, `Profession`, `BuildingFunction`, `TradeRoute`, `MagicCapability`. Trade routes, subordination to states, taxes. Magic professions keep Source and spirits as real constraints. The M5 simulation uses the same contracts.

## Acceptance

- At least 3 full industries, one inter-settlement trade chain, 2 states with several villages each.
- No duplicated goods, no profit without costs.
- Everything survives unloading and world reloads.

## Open questions

- MCA Capitals: real API, 1.21.1 version, license.
