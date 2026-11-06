---
name: exec-plan
description: Create, update or close an execution plan in docs/exec-plans/ for multi-step or multi-session work in Living Kingdoms. Use when starting work that spans several steps or sessions, when resuming such work, or when finishing it.
---

# Exec plan

Execution plans keep multi-session work resumable by any agent. The format is defined in `docs/exec-plans/README.md`; read it first.

## Create

1. Check `docs/exec-plans/active/` and `docs/exec-plans/tech-debt-tracker.md` for an existing plan or item covering this work. Continue it instead of starting a duplicate.
2. Create `docs/exec-plans/active/YYYY-MM-DD-short-slug.md` with:
   - frontmatter `status: active` and `last_reviewed: <today>`;
   - a title, then `**Status:** Active`;
   - the sections `## Goal`, `## Context`, `## Steps`, `## Progress log`, `## Decision log`.
3. Goal: what "done" means for a player or developer, with the acceptance level (runtime passed, gameplay verified, …).
4. Context: link the milestone spec, ADRs and design docs involved. Do not copy their content.
5. Steps: ordered `- [ ]` checkboxes small enough to finish and verify one at a time.
6. Add the plan to the Plans table in `docs/exec-plans/README.md` and to `docs/index.md` under Plans.
7. If the plan came from Claude Code plan mode, copy its content here; the plan-mode file outside the repo is not visible to other agents.

## Update

- After each finished step: tick it, and add a progress-log line `- YYYY-MM-DD — what was done; status reached; what was not verified`.
- Every decision taken during the work goes into the decision log with its reason. If it should outlive the plan, also add it as a D-xx entry or an ADR in `docs/decisions/`.
- If the plan is blocked, set `**Status:** Blocked` and say what it waits for.

## Close

1. Set `**Status:** Completed` (or `Abandoned`, with the reason in the decision log).
2. Move the file to `docs/exec-plans/completed/` and fix the links in `docs/exec-plans/README.md` and `docs/index.md`.
3. Move leftovers to `docs/exec-plans/tech-debt-tracker.md`; update `docs/quality.md` if a subsystem grade changed.
4. Run `npm run docs:check` and fix everything it reports.
