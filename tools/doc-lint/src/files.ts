// File-system helpers shared by the doc checks.
import { readdirSync, readFileSync, statSync, existsSync } from "node:fs";
import { join, relative, sep } from "node:path";

export const REPO_ROOT = join(import.meta.dirname, "..", "..", "..");

/** Directories never scanned. */
const IGNORED_DIRS = new Set([".git", "node_modules", "runtime", "build", ".gradle", "out"]);

/** Repo-relative path with forward slashes. */
export function rel(absPath: string): string {
  return relative(REPO_ROOT, absPath).split(sep).join("/");
}

export function abs(relPath: string): string {
  return join(REPO_ROOT, ...relPath.split("/"));
}

/** Recursively lists files under `dir` (repo-relative), skipping ignored directories. */
export function listFiles(dir = ""): string[] {
  const start = abs(dir);
  if (!existsSync(start)) return [];
  const out: string[] = [];
  const walk = (current: string): void => {
    for (const entry of readdirSync(current, { withFileTypes: true })) {
      if (entry.isDirectory()) {
        if (!IGNORED_DIRS.has(entry.name)) walk(join(current, entry.name));
      } else if (entry.isFile()) {
        out.push(rel(join(current, entry.name)));
      }
    }
  };
  walk(start);
  return out.sort();
}

export function read(relPath: string): string {
  return readFileSync(abs(relPath), "utf8");
}

export function exists(relPath: string): boolean {
  return existsSync(abs(relPath));
}

export function isDirectory(relPath: string): boolean {
  return exists(relPath) && statSync(abs(relPath)).isDirectory();
}
