---
name: doc-gardening
description: Audit the Living Kingdoms knowledge base for stale, wrong or inconsistent docs and fix or report them. Use periodically, after a milestone is accepted, or when docs seem out of date.
---

# Doc gardening

The repository is the system of record, so wrong docs are bugs. This skill finds them and fixes the safe ones. The rules for which doc owns which fact are in `docs/documentation.md`.

## 1. Mechanical pass

Run `npm run docs:check`. Fix every error. Treat warnings about stale `last_reviewed` dates as a list of docs to review in step 2.

## 2. Reality pass

Compare docs with the actual state of the repository:

- **Status:** AGENTS.md "Status" line and `docs/quality.md` grades match what exists (pack manifest, mods-src, tests).
- **Mods:** `docs/compatibility-matrix.md` matches the packwiz manifest in `modpack/` once it exists; every mod there has a row in `docs/licenses.md`; no doc recommends a mod from the Rejected mods list.
- **Decisions:** nothing in milestone specs or design docs contradicts `docs/decisions/` (D-xx and ADRs). If a newer decision exists, the older text is updated or marked superseded.
- **Plans:** active exec plans have a recent progress-log entry; finished ones are closed with the `exec-plan` skill; finished tech-debt items are removed with the outcome recorded where it belongs.
- **Duplication:** the same fact stated in two places is replaced by one statement and a link.
- **Size:** AGENTS.md stays a short map; details move to `docs/`.

For each doc you checked and found correct, update `last_reviewed` to today.

## 3. Fix or report

- Fix directly: broken links, stale status lines, index rows, obvious factual drift that the repo itself proves.
- Report to the user instead of changing: anything that alters a decision, milestone scope, mod selection, or licensing conclusions.

Finish with `npm run docs:check` passing and a short summary: what was fixed, what needs a decision.
