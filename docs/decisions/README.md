# Architecture Decision Records (ADR)

Decisions that are expensive to change live here: base mods, milestone order, data models, license constraints. Short decisions without a separate rationale live in the D-xx table of the [roadmap](../roadmap.md#11-architecture-decisions).

| ADR | Decision | Status | Date |
| --- | --- | --- | --- |
| [ADR-0001](ADR-0001-own-settlement-mod.md) | Our own settlement mod on MCA Reborn instead of Millénaire | Accepted | 2026-11-06 |
| [ADR-0002](ADR-0002-milestone-order.md) | Milestone order: settlements → simulation → economy and states → atmosphere → cultures | Accepted | 2026-11-06 |

## Template

File `ADR-NNNN-short-name.md`:

```markdown
# ADR-NNNN: Title

**Status:** Proposed | Accepted | Superseded by ADR-XXXX · **Date:** YYYY-MM-DD

## Context
The problem and its constraints.

## Decision
What we chose.

## Options considered
What else was considered and why it was rejected.

## Consequences
What gets easier, what gets harder, what needs to be checked.
```

An accepted ADR is never rewritten after the fact: if the decision changes, a new ADR is created and the old one is marked "Superseded".
