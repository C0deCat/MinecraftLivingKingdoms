@AGENTS.md

## Claude Code notes

- A Stop hook (`.claude/settings.json`) runs the doc linter when docs, skills or agent config changed. If it blocks the end of a turn, fix the listed problems.
- `.claude/skills/` holds synced copies; edit skills in `.agents/skills/`, then run `npm run skills:sync`.
- Plan-mode plans are stored outside the repo. For multi-session work, copy the approved plan into `docs/exec-plans/active/` using the `exec-plan` skill.
- The `doc-gardener` subagent audits docs and reports findings without editing.
