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
