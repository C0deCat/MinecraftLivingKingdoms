---
status: stable
last_reviewed: 2026-11-06
---

# Harness-style knowledge base

**Status:** Completed

## Goal

Rebuild the repository docs so that any agent (Codex or Claude Code) starts from a short map, finds deeper knowledge by following links, and gets mechanical feedback when docs drift. Done = the new structure exists, nothing from roadmap v0.3 is lost, and `npm run docs:check` passes and blocks a Claude Code turn on errors.

## Context

- Inspired by OpenAI's "Harness engineering" post: AGENTS.md as a table of contents, `docs/` as the system of record, plans as artifacts, linters whose messages are fix instructions, recurring doc gardening.
- Checked against Claude Code docs: CLAUDE.md with `@AGENTS.md` shares one rules file with Codex; layout trees do not belong in CLAUDE.md; plain wording over emphasis; Stop hooks with exit code 2 feed stderr back to the agent; skills are read from `.claude/skills` (Claude) and `.agents/skills` (Codex).
- Related decisions: D-17 (English), [ADR-0001](../../decisions/ADR-0001-own-settlement-mod.md), [ADR-0002](../../decisions/ADR-0002-milestone-order.md).

## Steps

- [x] Commit the English translation separately.
- [x] Split roadmap v0.3 into `docs/milestones/m0…m10`, design docs, the decision table, ARCHITECTURE.md and quality.md; keep a short roadmap.
- [x] Write core-beliefs, docs index, references index, exec-plan format, tech-debt tracker; add frontmatter to every doc.
- [x] Rewrite AGENTS.md as a ~90-line map and CLAUDE.md as an import plus Claude notes.
- [x] Build `tools/doc-lint` (TypeScript, zero dependencies) with `docs:check` and `skills:sync`.
- [x] Add the Stop hook, the `exec-plan` and `doc-gardening` skills, and the `doc-gardener` subagent.
- [x] Verify with positive and negative runs; commit.

## Progress log

- 2026-11-06 — Structure, linter, hook, skills and subagent created. `docs:check` passes; negative tests (broken link, unindexed doc, Cyrillic text, missing frontmatter) fail with fix instructions; Stop-hook mode exits 2 with instructions and exits 0 when `stop_hook_active` is set. Status: runtime passed. Not verified: the hook firing in a live Claude Code session (needs a new session after `.claude/settings.json` is loaded).

## Decision log

- 2026-11-06 — Skills are canonical in `.agents/skills/` and copied to `.claude/skills/` by a script, because symlinks are unreliable on Windows clones. The linter fails on drift.
- 2026-11-06 — Path-scoped `.claude/rules/` are not used, because Codex does not read them; nested `AGENTS.md` + `CLAUDE.md` pairs will be used in code directories instead.
- 2026-11-06 — The linter runs on Node 24 type stripping without a build step or dependencies.
- 2026-11-06 — The doc-gardener subagent is read-only; fixes are applied by the main agent through the `doc-gardening` skill.
