---
status: active
last_reviewed: 2026-11-07
---

# Docs index

Every document in `docs/` is listed here. The doc linter fails if a file is missing from this table.

Every doc starts with frontmatter:

```yaml
---
status: draft | active | stable | superseded
last_reviewed: YYYY-MM-DD
---
```

`last_reviewed` is updated whenever the content is checked against reality, not only when it is edited.

## Start here

| Doc | Purpose | Status |
| --- | --- | --- |
| [core-beliefs.md](core-beliefs.md) | Principles behind every decision | active |
| [documentation.md](documentation.md) | What to update when, how to create docs, style | active |
| [glossary.md](glossary.md) | Project terms and label schemes, one meaning each | active |
| [roadmap.md](roadmap.md) | Vision, requirements, milestone table, risks | active |
| [quality.md](quality.md) | Subsystem grades and end-to-end test bars | active |

## Milestones

| Doc | Purpose | Status |
| --- | --- | --- |
| [milestones/m0-infrastructure.md](milestones/m0-infrastructure.md) | Reproducible pack from Git | stable |
| [milestones/m1-world-and-adventure.md](milestones/m1-world-and-adventure.md) | Finite world, adventure progression | stable |
| [milestones/m2-household.md](milestones/m2-household.md) | Farming, food, crafts, decor | active |
| [milestones/m3-magic.md](milestones/m3-magic.md) | Magic ecosystem | active |
| [milestones/m4-settlements.md](milestones/m4-settlements.md) | Our settlement mod on MCA Reborn | active |
| [milestones/m5-simulation.md](milestones/m5-simulation.md) | Unloaded-world simulation | active |
| [milestones/m6-economy-and-states.md](milestones/m6-economy-and-states.md) | Trade, states, NPC crafts and magic | active |
| [milestones/m7-atmosphere.md](milestones/m7-atmosphere.md) | Seasons, trees, weather | active |
| [milestones/m8-cultures.md](milestones/m8-cultures.md) | Own peoples and architecture | active |
| [milestones/m9-autonomy.md](milestones/m9-autonomy.md) | Long autonomous world life | active |
| [milestones/m10-multiplayer.md](milestones/m10-multiplayer.md) | LAN and dedicated server | active |
| [milestones/m11-release.md](milestones/m11-release.md) | Stabilization and release | active |

## Design and decisions

| Doc | Purpose | Status |
| --- | --- | --- |
| [design-docs/index.md](design-docs/index.md) | Catalogue of cross-cutting design docs | active |
| [design-docs/magic-system.md](design-docs/magic-system.md) | Magic roles and shared mana | active |
| [design-docs/settlement-model.md](design-docs/settlement-model.md) | Residents and villages | draft |
| [design-docs/simulation-model.md](design-docs/simulation-model.md) | Unloaded-world simulation | draft |
| [design-docs/world-content.md](design-docs/world-content.md) | Structure density, mandatory content, manifest categories | active |
| [decisions/README.md](decisions/README.md) | ADR index and lightweight decisions D-xx | active |
| [decisions/ADR-0001-own-settlement-mod.md](decisions/ADR-0001-own-settlement-mod.md) | Own settlement mod instead of Millénaire | stable |
| [decisions/ADR-0002-milestone-order.md](decisions/ADR-0002-milestone-order.md) | Milestone order after magic | stable |
| [decisions/ADR-0003-multiplayer-milestone.md](decisions/ADR-0003-multiplayer-milestone.md) | Singleplayer first, multiplayer milestone M10 | stable |
| [decisions/ADR-0004-world-sizes-and-dimensions.md](decisions/ADR-0004-world-sizes-and-dimensions.md) | World sizes, dimension scales, world edge | stable |
| [decisions/ADR-0005-test-framework.md](decisions/ADR-0005-test-framework.md) | Test layers: GameTest and our e2e driver | stable |

## Plans

| Doc | Purpose | Status |
| --- | --- | --- |
| [exec-plans/README.md](exec-plans/README.md) | Exec plan format and list | active |
| [exec-plans/tech-debt-tracker.md](exec-plans/tech-debt-tracker.md) | Open items and unverified facts | active |
| [exec-plans/completed/2026-11-10-m1-accept.md](exec-plans/completed/2026-11-10-m1-accept.md) | M1-D accept: surface foundations, full validation, gameplay check | stable |
| [exec-plans/active/2026-11-09-test-framework.md](exec-plans/active/2026-11-09-test-framework.md) | Test framework: testkit mod, e2e driver, GameTest layer, conventions | active |
| [exec-plans/completed/2026-11-09-m1-integrate.md](exec-plans/completed/2026-11-09-m1-integrate.md) | M1-C integrate: content manifest, validator, foundation processor, shader pack | stable |
| [exec-plans/completed/2026-11-08-m1-reconcile.md](exec-plans/completed/2026-11-08-m1-reconcile.md) | M1-B reconcile: villages, census, renderer | stable |
| [exec-plans/completed/2026-11-07-m1-structures.md](exec-plans/completed/2026-11-07-m1-structures.md) | M1-A2 structure and adventure mods | stable |
| [exec-plans/completed/2026-11-06-m1-world.md](exec-plans/completed/2026-11-06-m1-world.md) | M1-A1 base generator: world sizes, dimensions, world edge | stable |
| [exec-plans/completed/2026-11-06-m0-infrastructure.md](exec-plans/completed/2026-11-06-m0-infrastructure.md) | M0 infrastructure work | stable |
| [exec-plans/completed/2026-11-06-harness-docs.md](exec-plans/completed/2026-11-06-harness-docs.md) | This knowledge-base restructure | stable |

## Mods and references

| Doc | Purpose | Status |
| --- | --- | --- |
| [compatibility-matrix.md](compatibility-matrix.md) | Installed mods, versions, platform check | active |
| [licenses.md](licenses.md) | License rules, registry, audits | active |
| [references/index.md](references/index.md) | Version-pinned notes on external APIs and tools | active |
| [references/packwiz.md](references/packwiz.md) | packwiz and packwiz-installer: usage and pitfalls | active |
| [references/prism-launcher.md](references/prism-launcher.md) | Portable Prism instance setup and pitfalls | active |
| [references/lithostitched-1.8.md](references/lithostitched-1.8.md) | Lithostitched modifiers and our world-edge modifiers | active |
| [references/tectonic-3.0.md](references/tectonic-3.0.md) | Tectonic terrain functions, coast facts and pitfalls | active |
| [references/twilight-forest-4.8.md](references/twilight-forest-4.8.md) | Twilight Forest scale, border and our override | active |

Outside `docs/`: [AGENTS.md](../AGENTS.md), [ARCHITECTURE.md](../ARCHITECTURE.md), [README.md](../README.md).
