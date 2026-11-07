---
status: active
last_reviewed: 2026-11-07
---

# M3 — Unified magic ecosystem

## Goal

Three magic traditions with different roles work together instead of being three independent sets of items. Minimal fixes and existing addons; no large new spirit-pact system.

The design rules (roles of each tradition, shared mana) live in [design-docs/magic-system.md](../design-docs/magic-system.md).

## Playable result

"Fantasy Magic": three magic schools with one shared mana bar for Ars and Iron's.

## Mods

| Status | Mod | Purpose |
| --- | --- | --- |
| Base | [Ars Nouveau](https://modrinth.com/mod/ars-nouveau) | Modern universal magic: glyphs, Source, automation |
| Base | [Iron's Spells 'n Spellbooks](https://modrinth.com/mod/irons-spells-n-spellbooks) | Ancient powerful spells, scrolls, magic bosses |
| Base | [Occultism](https://modrinth.com/mod/occultism) | Spirits, rituals, efficient production |
| Base bridge | [Ars 'n Spells](https://github.com/otectus/ars-n-spells) | Shared Ars–Iron's mana with configurable rates |
| Check | [Ars Ocultas](https://modrinth.com/project/ars-ocultas) | Bound spirits through Ars; check it does not bypass lifetime limits |
| Check | [Ars Unification](https://modrinth.com/mod/ars-unification) | Other mods' Cut/Crush recipes for Ars glyphs; supports Occultism and Farmer's Delight |
| Dependency | Patchouli and release-specific libraries | Only via JAR metadata and packwiz |

Ars addon catalog with known incompatibilities: [Ars Nouveau 1.21.1 addons guide](https://github.com/Ars-Nouveau/Ars-Auxilia/blob/main/apps/ars.guide/src/content/docs/1.21.1/ars-addons/guide.mdx).

## Phases

**A — foundation: install.** Test the three base mods separately and together. Add Ars 'n Spells after base versions are pinned. Check Iron's structures on the finite map: towers, bosses, resources, trade.

**B — reconcile: minimal balance.**
- Restrict Ars rituals that change global weather/time (`Sunrise`, `Moonfall`, `Cloudshaping`) via datapack/recipes.
- Start from original glyph and ritual prices; raise only combinations found excessive.
- In Ars 'n Spells, disable converting Iron's spells into Ars glyphs if it breaks the separation of schools.
- No mandatory recipe discovery for Iron's scrolls.
- Check Ars Ocultas and Ars Unification against spirit lifetime limits and recipe priority; disable conflicting addon mechanics instead of forking.
- Compare Source production with Occultism processing; remove obvious exploits, keep Occultism's cost advantage.

**C — integrate.** A compatible ecosystem, not one rewritten API: shared mana, consistent recipes and attributes, clear niches, all content reachable on the finite map and tracked in the World Content Manifest.

**D — accept.** See [Acceptance](#acceptance) below.

## Acceptance

- 10+ typical spells from both Ars and Iron's use one pool; gear bonuses and cooldowns work.
- 3–5 Ars/Occultism productions compared by real cost.
- All required bosses and structures reachable.
- No free infinite resource duplication; all settings in Git.

## Out of scope

- New pacts, summoning risks and unique spirits for Occultism (D-09).
- Complex formula research for Iron's scrolls (D-08).

## Open questions

- Primary pool (`ars_primary` vs `iss_primary`) and the starting rate `k`; armor, regeneration and display checks.
- Specific minimal Ars restrictions and the Occultism recipes that really need a price increase after testing.
