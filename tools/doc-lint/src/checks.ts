// Doc checks. Every problem carries a concrete fix instruction, because agents act on these messages.
import { dirname, posix } from "node:path";
import { createHash } from "node:crypto";
import { exists, isArchived, isDirectory, listFiles, read } from "./files.ts";
import { extractLinks, headingAnchors, headings, parseFrontmatter } from "./markdown.ts";

export interface Problem {
  severity: "error" | "warning";
  file: string;
  line?: number;
  message: string;
  fix: string;
}

const STATUSES = ["draft", "active", "stable", "superseded"];
const STALE_AFTER_DAYS = 60;
const LINE_BUDGETS: Record<string, number> = { "AGENTS.md": 120, "CLAUDE.md": 30 };
const TEXT_EXTENSIONS = /\.(md|ts|js|mjs|json|toml|ya?ml|java|txt|properties|gradle|kts|ps1)$/;
// Built from code points so this source file stays ASCII-only (U+0400–U+04FF).
const CYRILLIC = new RegExp(`[${String.fromCodePoint(0x0400)}-${String.fromCodePoint(0x04ff)}]`);
const EXEC_PLAN_SECTIONS = ["Goal", "Context", "Steps", "Progress log", "Decision log"];

const markdownFiles = (): string[] =>
  listFiles().filter((f) => f.endsWith(".md") && !isArchived(f));

/** Docs under docs/ that must be indexed and carry frontmatter. */
const knowledgeDocs = (): string[] =>
  listFiles("docs").filter((f) => f.endsWith(".md") && !isArchived(f));

function resolveLink(fromFile: string, target: string): { path: string; anchor?: string } {
  const [rawPath, anchor] = target.split("#", 2);
  const decoded = decodeURI(rawPath);
  const path = decoded === "" ? fromFile : posix.normalize(posix.join(dirname(fromFile), decoded));
  return { path: path.replace(/\/$/, ""), anchor: anchor || undefined };
}

export function checkLinks(): Problem[] {
  const problems: Problem[] = [];
  const anchorCache = new Map<string, Set<string>>();
  for (const file of markdownFiles()) {
    for (const link of extractLinks(read(file))) {
      if (/^[a-z][a-z0-9+.-]*:/i.test(link.target)) continue; // http:, mailto:, etc.
      const { path, anchor } = resolveLink(file, link.target);
      if (!exists(path)) {
        problems.push({
          severity: "error",
          file,
          line: link.line,
          message: `Broken link: "${link.target}" points to "${path}", which does not exist.`,
          fix: `Point the link at an existing file (paths are relative to ${file}), or create the target and add it to docs/index.md.`,
        });
        continue;
      }
      if (anchor && path.endsWith(".md") && !isDirectory(path)) {
        if (!anchorCache.has(path)) anchorCache.set(path, headingAnchors(read(path)));
        if (!anchorCache.get(path)!.has(anchor.toLowerCase())) {
          problems.push({
            severity: "error",
            file,
            line: link.line,
            message: `Broken anchor: "#${anchor}" is not a heading in ${path}.`,
            fix: `Use one of the headings of ${path} (GitHub slug: lowercase, punctuation removed, spaces → "-"), or drop the anchor.`,
          });
        }
      }
    }
  }
  return problems;
}

export function checkIndexCoverage(): Problem[] {
  const indexFile = "docs/index.md";
  if (!exists(indexFile)) {
    return [{ severity: "error", file: indexFile, message: "docs/index.md is missing.", fix: "Create docs/index.md listing every doc." }];
  }
  const linked = new Set(extractLinks(read(indexFile)).map((l) => resolveLink(indexFile, l.target).path));
  return knowledgeDocs()
    .filter((doc) => doc !== indexFile && !linked.has(doc))
    .map((doc) => ({
      severity: "error" as const,
      file: doc,
      message: `${doc} is not listed in docs/index.md.`,
      fix: `Add a row to the matching section of docs/index.md: | [${doc.slice(5)}](${doc.slice(5)}) | <purpose> | <status> |`,
    }));
}

export function checkFrontmatter(today = new Date()): Problem[] {
  const problems: Problem[] = [];
  const template = ["---", "status: active", "last_reviewed: YYYY-MM-DD", "---"].join("\n");
  for (const doc of knowledgeDocs()) {
    const fm = parseFrontmatter(read(doc));
    if (!fm.present) {
      problems.push({ severity: "error", file: doc, line: 1, message: "Missing frontmatter.", fix: `Start the file with:\n${template}` });
      continue;
    }
    const status = fm.fields.get("status");
    if (!status || !STATUSES.includes(status)) {
      problems.push({
        severity: "error",
        file: doc,
        line: 2,
        message: `Invalid or missing "status" (got "${status ?? ""}").`,
        fix: `Set status to one of: ${STATUSES.join(", ")}.`,
      });
    }
    const reviewed = fm.fields.get("last_reviewed") ?? "";
    const date = /^\d{4}-\d{2}-\d{2}$/.test(reviewed) ? new Date(`${reviewed}T00:00:00Z`) : null;
    if (!date || Number.isNaN(date.getTime())) {
      problems.push({
        severity: "error",
        file: doc,
        line: 3,
        message: `Invalid or missing "last_reviewed" (got "${reviewed}").`,
        fix: "Set last_reviewed to the date the content was last checked, as YYYY-MM-DD.",
      });
    } else {
      const ageDays = (today.getTime() - date.getTime()) / 86_400_000;
      if (ageDays > STALE_AFTER_DAYS && status !== "superseded") {
        problems.push({
          severity: "warning",
          file: doc,
          message: `Not reviewed for ${Math.floor(ageDays)} days.`,
          fix: "Check the content against the current state of the project, fix what is wrong, and update last_reviewed.",
        });
      }
    }
  }
  return problems;
}

export function checkEnglishOnly(): Problem[] {
  const problems: Problem[] = [];
  for (const file of listFiles()) {
    if (isArchived(file) || !TEXT_EXTENSIONS.test(file)) continue;
    read(file)
      .split(/\r?\n/)
      .forEach((line, index) => {
        if (CYRILLIC.test(line)) {
          problems.push({
            severity: "error",
            file,
            line: index + 1,
            message: "Non-English (Cyrillic) text found.",
            fix: "All repository text is English (D-17). Translate this line; only docs/archive/ may keep the original language.",
          });
        }
      });
  }
  return problems;
}

export function checkLineBudgets(): Problem[] {
  return Object.entries(LINE_BUDGETS)
    .filter(([file]) => exists(file))
    .flatMap(([file, budget]) => {
      const count = read(file).split(/\r?\n/).length;
      return count > budget
        ? [{
            severity: "error" as const,
            file,
            message: `${file} has ${count} lines; the budget is ${budget}.`,
            fix: `${file} is loaded into every agent session. Move details into docs/ and keep a one-line pointer here.`,
          }]
        : [];
    });
}

export function checkAdrIndex(): Problem[] {
  const indexFile = "docs/decisions/README.md";
  if (!exists(indexFile)) return [];
  const linked = new Set(extractLinks(read(indexFile)).map((l) => resolveLink(indexFile, l.target).path));
  return listFiles("docs/decisions")
    .filter((f) => /\/ADR-\d{4}-[^/]+\.md$/.test(f) && !linked.has(f))
    .map((f) => ({
      severity: "error" as const,
      file: f,
      message: `${f} is not listed in the ADR table of docs/decisions/README.md.`,
      fix: "Add a row: | [ADR-NNNN](ADR-NNNN-name.md) | <decision> | <status> | <date> |",
    }));
}

export function checkExecPlans(): Problem[] {
  const problems: Problem[] = [];
  const readme = "docs/exec-plans/README.md";
  const listed = exists(readme)
    ? new Set(extractLinks(read(readme)).map((l) => resolveLink(readme, l.target).path))
    : new Set<string>();
  const plans = [...listFiles("docs/exec-plans/active"), ...listFiles("docs/exec-plans/completed")].filter((f) => f.endsWith(".md"));
  for (const plan of plans) {
    const text = read(plan);
    const found = new Set(headings(text));
    const missing = EXEC_PLAN_SECTIONS.filter((s) => !found.has(s));
    if (missing.length > 0) {
      problems.push({
        severity: "error",
        file: plan,
        message: `Exec plan is missing sections: ${missing.join(", ")}.`,
        fix: `Add "## ${missing.join('", "## ')}" headings (format in docs/exec-plans/README.md).`,
      });
    }
    if (!/^\*\*Status:\*\*\s+(Active|Blocked|Completed|Abandoned)\b/m.test(text)) {
      problems.push({
        severity: "error",
        file: plan,
        message: 'Exec plan has no "**Status:** Active | Blocked | Completed | Abandoned" line.',
        fix: "Add the status line right under the title.",
      });
    }
    const inCompleted = plan.startsWith("docs/exec-plans/completed/");
    if (inCompleted && !/^\*\*Status:\*\*\s+(Completed|Abandoned)\b/m.test(text)) {
      problems.push({
        severity: "error",
        file: plan,
        message: "A plan in completed/ must have status Completed or Abandoned.",
        fix: "Update the status line, or move the plan back to active/.",
      });
    }
    if (!listed.has(plan)) {
      problems.push({
        severity: "error",
        file: plan,
        message: `Exec plan is not listed in ${readme}.`,
        fix: `Add a row to the Plans table of ${readme}.`,
      });
    }
  }
  return problems;
}

const hashTree = (dir: string): Map<string, string> =>
  new Map(
    listFiles(dir).map((f) => [f.slice(dir.length + 1), createHash("sha256").update(read(f)).digest("hex")]),
  );

export function checkSkillSync(): Problem[] {
  const source = hashTree(".agents/skills");
  const copy = hashTree(".claude/skills");
  const differing = new Set<string>();
  for (const [file, hash] of source) if (copy.get(file) !== hash) differing.add(file);
  for (const file of copy.keys()) if (!source.has(file)) differing.add(file);
  return [...differing].map((file) => ({
    severity: "error" as const,
    file: `.claude/skills/${file}`,
    message: "Claude skill copy differs from the canonical skill in .agents/skills/.",
    fix: "Edit skills only in .agents/skills/, then run `npm run skills:sync`.",
  }));
}

export function runAllChecks(): Problem[] {
  return [
    ...checkLinks(),
    ...checkIndexCoverage(),
    ...checkFrontmatter(),
    ...checkEnglishOnly(),
    ...checkLineBudgets(),
    ...checkAdrIndex(),
    ...checkExecPlans(),
    ...checkSkillSync(),
  ];
}
