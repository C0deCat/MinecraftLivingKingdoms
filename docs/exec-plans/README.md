---
status: active
last_reviewed: 2026-11-07
---

# Execution plans

Plans are versioned artifacts, so work can continue across sessions and agents without outside context.

- **Small change** (a few files, one session): no exec plan; describe it in the commit message.
- **Multi-step or multi-session work:** an exec plan in `active/`. When done, move it to `completed/` and set `Status: Completed`.
- Plans made in Claude Code plan mode live outside the repo; copy an approved multi-session plan into `active/`.
- Known debt and open questions that are not a plan yet go into [tech-debt-tracker.md](tech-debt-tracker.md).

The `exec-plan` skill (`.agents/skills/exec-plan/`) walks through creating and closing a plan.

## File name

`YYYY-MM-DD-short-slug.md`, dated by the day the plan was created.

## Required sections

Each plan starts with the doc frontmatter (`status`, `last_reviewed`) and contains these headings in order:

```markdown
# Plan title

**Status:** Active | Blocked | Completed | Abandoned

## Goal
What "done" means, from the player's or developer's point of view.

## Context
Why now, constraints, links to milestones, ADRs and design docs.

## Steps
- [ ] Ordered, checkable steps.

## Progress log
- YYYY-MM-DD — what was done, what was verified (task status level).

## Decision log
- YYYY-MM-DD — decision and reason. Promote lasting ones to an ADR or a D-xx entry.
```

## Plans

| Plan | Status |
| --- | --- |
| [2026-11-07-m1-structures](completed/2026-11-07-m1-structures.md) | Completed |
| [2026-11-06-m1-world](completed/2026-11-06-m1-world.md) | Completed |
| [2026-11-06-m0-infrastructure](completed/2026-11-06-m0-infrastructure.md) | Completed |
| [2026-11-06-harness-docs](completed/2026-11-06-harness-docs.md) | Completed |
