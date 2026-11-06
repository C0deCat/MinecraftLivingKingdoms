---
status: active
last_reviewed: 2026-11-06
---

# Magic system design

How the three magic mods fit together. Implementation stages are in [M3](../milestones/m3-magic.md); magic in the NPC economy is in [M6](../milestones/m6-economy-and-states.md).

## Roles of the traditions (D-06)

- **Ars Nouveau — modern, learnable, flexible.** It can do a lot, but specialized powerful effects and large-scale production should cost more. No mass nerfs without tests.
- **Iron's Spells 'n Spellbooks — rare knowledge of ancient civilizations.** Most spells are stronger than similar simple Ars combinations or easier to use at high level. Keep the original Scroll Forge and reusable casting via the Spellbook; no endless hunt for single-use scrolls (D-08).
- **Occultism — rare ritual practice** with strong effects and efficient automation. Keep the mod's own limits (ritual structures, reagents, spirit progression, work durations). No new pacts, random risks or anomalies for now (D-09).

## Shared mana (D-07)

- One shared personal resource for Ars and Iron's, using [Ars 'n Spells](https://github.com/otectus/ars-n-spells).
- One mod is the authoritative owner of the pool (`ars_primary` or `iss_primary`); the other consumes it through the bridge. Prefer one authoritative pool over a two-way hybrid.
- A rate `k` converts spell cost, regeneration and bonuses into the chosen scale. With Ars as the base: `cost_unified(IronSpell) = k × cost_iron`.
- Relevant config keys: `mana_unification_mode`, `conversion_rate_iron_to_ars`, `conversion_rate_ars_to_iron`.
- **Source stays separate** from personal mana: it is a machine/world resource.
- **Occultism has no shared mana bar**: its rituals run on a different resource cycle.

## Invariants to test

- One displayed bar; deduction in one transaction; no double regeneration.
- Item and enchantment bonuses apply once.
- Correct behavior on death and dimension change; for a remote LAN client in [M10](../milestones/m10-multiplayer.md).
- Global weather/time rituals (`Sunrise`, `Moonfall`, `Cloudshaping`) are restricted, because they conflict with the world simulation.
