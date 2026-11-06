// Entry point: `node tools/doc-lint/src/main.ts [--hook=stop]`.
//
// Default mode prints all problems and exits 1 on errors.
// Stop-hook mode (Claude Code) reads the hook payload from stdin, skips when nothing relevant changed
// or when the hook already fired this turn, and exits 2 so stderr goes back to the agent as instructions.
import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";
import { REPO_ROOT } from "./files.ts";
import { runAllChecks, type Problem } from "./checks.ts";

const RELEVANT_CHANGE = /(\.md$|^\.agents\/|^\.claude\/|^tools\/doc-lint\/)/;

function format(problems: Problem[]): string {
  return problems
    .map((p) => {
      const where = p.line ? `${p.file}:${p.line}` : p.file;
      return `${p.severity.toUpperCase()} ${where}\n  ${p.message}\n  Fix: ${p.fix.replace(/\n/g, "\n       ")}`;
    })
    .join("\n");
}

function changedFiles(): string[] {
  const output = execFileSync("git", ["status", "--porcelain", "--untracked-files=all"], { cwd: REPO_ROOT, encoding: "utf8" });
  return output
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => line.slice(3).replace(/^"|"$/g, "").split(" -> ").pop()!);
}

function readHookInput(): { stop_hook_active?: boolean } {
  try {
    const raw = readFileSync(0, "utf8");
    return raw.trim() ? JSON.parse(raw) : {};
  } catch {
    return {};
  }
}

function runStopHook(): number {
  const input = readHookInput();
  if (input.stop_hook_active) return 0; // already blocked once this turn; avoid loops
  if (!changedFiles().some((f) => RELEVANT_CHANGE.test(f))) return 0;
  const errors = runAllChecks().filter((p) => p.severity === "error");
  if (errors.length === 0) return 0;
  process.stderr.write(
    `Doc check failed (${errors.length} problem${errors.length > 1 ? "s" : ""}). Fix these before finishing the turn:\n${format(errors)}\n`,
  );
  return 2;
}

function runCli(): number {
  const problems = runAllChecks();
  const errors = problems.filter((p) => p.severity === "error");
  const warnings = problems.filter((p) => p.severity === "warning");
  if (problems.length > 0) console.log(format(problems));
  console.log(`\ndocs:check — ${errors.length} error(s), ${warnings.length} warning(s).`);
  return errors.length > 0 ? 1 : 0;
}

process.exitCode = process.argv.includes("--hook=stop") ? runStopHook() : runCli();
