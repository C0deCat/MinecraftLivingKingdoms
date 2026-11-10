---
status: active
last_reviewed: 2026-11-07
---

# Glossary

Project terms and label schemes, one meaning each ([terms and labels](documentation.md#terms-and-labels)). Each row says what the term means here and which doc defines it in detail; the definition is not repeated elsewhere.

## Planning

| Term | Meaning | Defined in |
| --- | --- | --- |
| Milestone (M0–M11) | One stage of the game, brought to a cohesive playable subsystem before the next starts (D-05) | [roadmap.md](roadmap.md), [milestones/](milestones/) |
| Phase (A–D) | A part of every milestone, always in this order and with these meanings: **A** foundation (install the base mods; in code milestones research and the first model or prototype), **B** reconcile (resolve conflicts), **C** integrate (make one cohesive subsystem), **D** accept. Written `M1-B` | [roadmap.md](roadmap.md#milestones), D-19 |
| Step (M1-A1, M1-A2) | A part of one phase, numbered inside it. Phases are never split with letters again | [documentation.md](documentation.md#terms-and-labels) |
| Slice (S1–S4) | A released vertical increment of our settlement mod, built in phase M4-C | [m4-settlements.md](milestones/m4-settlements.md) |
| Exec plan | A versioned plan for multi-step or multi-session work, with progress and decision logs | [exec-plans/README.md](exec-plans/README.md) |
| TD-xx | A tech-debt item: a known gap, unverified fact or deferred decision, with where it gets resolved | [tech-debt-tracker.md](exec-plans/tech-debt-tracker.md) |
| ADR-xxxx, D-xx | A decision: an ADR when alternatives were weighed, a D-xx row for a short rule | [decisions/README.md](decisions/README.md) |

## Verification levels

Three separate scales; do not mix their words.

| Term | Meaning | Defined in |
| --- | --- | --- |
| Task status | How far one task was verified: code complete → build passed → runtime passed → gameplay verified | [core-beliefs §4](core-beliefs.md#4-honest-status) |
| Mod status | Where a mod stands in the pack: `candidate` → `installed` → `runtime-ok` → `integrated` → `accepted` → `mp-ok`, or `rejected` | [compatibility-matrix.md](compatibility-matrix.md) |
| Quality grade (—, D, C, B, A) | The state of a whole subsystem | [quality.md](quality.md) |

## World

| Term | Meaning | Defined in |
| --- | --- | --- |
| World size (Test, Small, Medium, Large) | The four fixed square Overworld sizes, side 2,048 / 8,192 / 12,288 / 16,384, chosen at world creation | [ADR-0004](decisions/ADR-0004-world-sizes-and-dimensions.md), D-03 |
| Side | The edge length of the square world, border to border, including the outer ocean | [ADR-0004](decisions/ADR-0004-world-sizes-and-dimensions.md) |
| Border | The world border of a dimension; the Nether's is side / ratio, the others equal the side | [ADR-0004](decisions/ADR-0004-world-sizes-and-dimensions.md) |
| Outer ocean | All ocean between the outermost coasts and the border, inside the border. Older records (ADR-0004, the M1-A1 plan, the pack changelog) also call it the "ocean ring" | [ADR-0004](decisions/ADR-0004-world-sizes-and-dimensions.md) |
| Open ocean | The outermost part of the outer ocean, where land is guaranteed absent (the cap) | [lithostitched-1.8.md](references/lithostitched-1.8.md) |
| Edge band | The band along the border where the edge bias sinks the land (up to 2,048 blocks in) | [lithostitched-1.8.md](references/lithostitched-1.8.md) |
| Cap | `edge_falloff`, the upper limit for continentalness that keeps the open ocean | [lithostitched-1.8.md](references/lithostitched-1.8.md) |
| Grounding | A structure standing on the ground instead of hanging over a slope: made by the `livingkingdoms_world:grounding` structure processor (named `foundation` until 2026-11-10, renamed so it does not clash with phase A), checked by `/lkworld grounding` and the hanging share of the scan | [M1 spec](milestones/m1-world-and-adventure.md#phases), [world-content](design-docs/world-content.md) |

## Testing

The words step, phase and stage belong to milestones, and smoke belongs to `npm run smoke`; tests do not reuse them. Snapshot is not used for stored worlds.

| Term | Meaning | Defined in |
| --- | --- | --- |
| Test layer | GameTest (server-side mechanics), e2e (the real client through our driver) or worldgen tools (`/lkworld`, `validate-world`); a test goes to the lowest layer that can catch its bug | [ADR-0005](decisions/ADR-0005-test-framework.md) |
| testkit | Our dev-only mod `livingkingdoms_testkit`: the e2e driver, the flat test preset and our GameTests; never part of the pack | [ADR-0005](decisions/ADR-0005-test-framework.md) |
| e2e driver | The client part of testkit that executes protocol ops sent by the TypeScript harness | [test framework plan](exec-plans/active/2026-11-09-test-framework.md) |
| Protocol op | One command of the e2e driver (`world.create`, `player.useItemOn`, `query.block`, …); the list lives in `tests/e2e/harness/protocol.ts` | [test framework plan](exec-plans/active/2026-11-09-test-framework.md) |
| Screen object | A TypeScript class that models one Minecraft screen and is named after its Java class (`CreateWorldScreen`) | [test framework plan](exec-plans/active/2026-11-09-test-framework.md) |
| World tier | Which world an e2e file runs in: `flat` (test preset), `cached` (copy of a world cache entry) or `fresh` (new world) | [test framework plan](exec-plans/active/2026-11-09-test-framework.md) |
| World cache entry | A stored test world keyed by the pack, our worldgen, size, seed and Chunky radius; invalid as soon as any of them changes | [test framework plan](exec-plans/active/2026-11-09-test-framework.md) |
| Arena | The plot of a test world that one e2e test gets for itself; coordinates in a test are relative to it | [test framework plan](exec-plans/active/2026-11-09-test-framework.md) |
| Reset level | How much is reset before an e2e test: `arena`, `world` (copy the cache entry again) or `fresh` (new world) | [test framework plan](exec-plans/active/2026-11-09-test-framework.md) |
| Test tag | `@quick`, `@slow` or `@visual` in a test name; not a Minecraft block or item tag | [test framework plan](exec-plans/active/2026-11-09-test-framework.md) |
