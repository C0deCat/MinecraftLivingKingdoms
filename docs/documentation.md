---
status: active
last_reviewed: 2026-11-06
---

# Documentation rules

How to keep the knowledge base correct while doing tasks. The repository is the system of record ([core-beliefs §6](core-beliefs.md#6-the-repository-is-the-system-of-record)): if the docs are wrong, agents build on wrong facts. Update docs in the same change as the work, not afterwards.

## What to update when

| When this happens | Update |
| --- | --- |
| A decision is made (in chat, in a plan, during research) | [decisions/README.md](decisions/README.md): a D-xx row for a short rule, an ADR when alternatives were weighed. Do it before the work that depends on it |
| A decision changes | A new ADR or D-xx entry; mark the old ADR "Superseded"; fix every doc that relied on the old decision |
| Work spans several steps or sessions | An exec plan via the `exec-plan` skill; keep its progress and decision logs current |
| An open question in a milestone spec is answered | Remove it from "Open questions" and write the answer where it belongs (spec body, design doc or decision) |
| A milestone's scope, mods or acceptance criteria change | That milestone file in [milestones/](milestones/); the [roadmap](roadmap.md) table only if the one-line summary changes |
| A mod is added, updated or removed | [compatibility-matrix.md](compatibility-matrix.md) and [licenses.md](licenses.md) |
| A mod or approach is rejected | A mod: a row in the [Rejected mods](compatibility-matrix.md#rejected-mods) list with the reason. An approach: an ADR or a D-xx entry. Then remove recommendations of it from other docs |
| A license or permission is clarified | [licenses.md](licenses.md) registry or audit section; record written answers from authors there |
| A fact is found to be unverified or a gap is deferred | A row in [exec-plans/tech-debt-tracker.md](exec-plans/tech-debt-tracker.md) with where it gets resolved |
| A tech-debt item is resolved | Write the outcome into the doc it affects, then delete the row |
| You learn how an external API or tool really behaves in our version | A note in [references/](references/index.md), or update the existing note; re-check notes when the tool's version changes |
| A design question spans several milestones | A design doc in [design-docs/](design-docs/index.md); one-milestone details stay in the milestone spec |
| A subsystem reaches a new verification level | Its grade and gaps in [quality.md](quality.md) |
| The project phase changes (e.g. M0 starts or is accepted) | The "Status" line in [AGENTS.md](../AGENTS.md) and [README.md](../README.md) |
| Code layout or layer rules change | [ARCHITECTURE.md](../ARCHITECTURE.md); a code directory with its own conventions gets a nested `AGENTS.md` plus a `CLAUDE.md` containing `@AGENTS.md` |
| A doc turns out to be wrong | Fix it in the same change that discovered it |

## Creating a document

1. Check [index.md](index.md) first: prefer extending an existing doc over adding a new one.
2. Put it in the right place: milestone spec, design doc, decision, exec plan, reference note — see the table above.
3. Start with frontmatter (`status`, `last_reviewed`) as described in [index.md](index.md).
4. Add a row to [index.md](index.md) and, if the folder has its own index (design docs, ADRs, exec plans, references), to that index too.
5. Run `npm run docs:check`.

## Status lifecycle

- `draft` — content is a proposal or incomplete; do not build on it without checking.
- `active` — current and maintained; the default for living docs.
- `stable` — finished record that should not change (accepted ADRs, completed plans).
- `superseded` — replaced; keep it only if history matters and link to the replacement.

Outdated docs are deleted, not archived: Git history keeps old versions. What must survive is the *reason* something was dropped — that goes into the rejected list or a decision, so agents do not propose it again.

Update `last_reviewed` whenever you check a doc against reality, even if nothing changed. The linter warns after 60 days.

## Writing style

- English only (D-17).
- Plain, direct wording. Give the reason for a rule instead of emphasis; no ALL CAPS or "IMPORTANT".
- One fact, one place. Link to the source instead of repeating it.
- Short and specific: what is true, what to do, where to look next.
- AGENTS.md and CLAUDE.md stay small because they load into every session; details go into `docs/`.
- Never paste code or text from closed (ARR) mods; describe behavior instead.

## Checks

- `npm run docs:check` checks form: links and anchors, index coverage, frontmatter, English-only, size budgets, ADR index, exec-plan sections, skill sync. In Claude Code a Stop hook runs it automatically.
- Content correctness is checked by people and by the `doc-gardening` skill (or the read-only `doc-gardener` subagent).
