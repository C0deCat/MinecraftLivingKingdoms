// Shared helpers for the pack tooling: paths, pinned downloads, running tools.
import { createHash } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { spawnSync, type SpawnSyncOptions } from "node:child_process";
import type { PinnedDownload } from "./pins.ts";

export const REPO_ROOT = join(import.meta.dirname, "..", "..", "..");
export const TOOLS_BIN = join(REPO_ROOT, "tools", "bin");
export const MODPACK_DIR = join(REPO_ROOT, "modpack");
export const RUNTIME_DIR = join(REPO_ROOT, "runtime");
export const MODS_SRC_DIR = join(REPO_ROOT, "mods-src");
/** File-name prefix of the JARs built from mods-src/ (mod ids start with livingkingdoms_). */
export const OWN_JAR_PREFIX = "livingkingdoms_";
/** The dev-only testkit mod (ADR-0005): installed only into the e2e instance, never into the play instance or the pack. */
export const TESTKIT_JAR_PREFIX = "livingkingdoms_testkit";

/** JARs built from mods-src/ (`<project>/build/libs`), without sources JARs; the testkit only when asked for. */
export function builtOwnJars(options: { testkit: boolean }): { name: string; path: string }[] {
  const jars: { name: string; path: string }[] = [];
  for (const project of readdirSync(MODS_SRC_DIR, { withFileTypes: true }).filter((e) => e.isDirectory())) {
    const libs = join(MODS_SRC_DIR, project.name, "build", "libs");
    if (!existsSync(libs)) continue;
    for (const name of readdirSync(libs)) {
      if (!name.startsWith(OWN_JAR_PREFIX) || !name.endsWith(".jar") || name.endsWith("-sources.jar")) continue;
      if (name.startsWith(TESTKIT_JAR_PREFIX) && !options.testkit) continue;
      jars.push({ name, path: join(libs, name) });
    }
  }
  return jars;
}

const EXE = process.platform === "win32" ? ".exe" : "";
export const PACKWIZ_BIN = join(TOOLS_BIN, `packwiz${EXE}`);

export function sha256File(path: string): string {
  return createHash("sha256").update(readFileSync(path)).digest("hex");
}

/** Downloads a pinned file into `dir` unless an identical copy is already there. Fails on hash mismatch. */
export async function fetchPinned(pin: PinnedDownload, dir: string): Promise<string> {
  const target = join(dir, pin.file);
  if (existsSync(target) && sha256File(target) === pin.sha256) return target;
  console.log(`Downloading ${pin.url}`);
  const response = await fetch(pin.url, { redirect: "follow" });
  if (!response.ok) throw new Error(`Download failed (${response.status}): ${pin.url}`);
  const bytes = Buffer.from(await response.arrayBuffer());
  const actual = createHash("sha256").update(bytes).digest("hex");
  if (actual !== pin.sha256) {
    throw new Error(
      `SHA-256 mismatch for ${pin.file}: expected ${pin.sha256}, got ${actual}. ` +
        `Do not bypass this: re-check the upstream release and update tools/pack/src/pins.ts deliberately.`,
    );
  }
  mkdirSync(dirname(target), { recursive: true });
  writeFileSync(target, bytes);
  return target;
}

/** Runs a command, streaming output; throws on a non-zero exit code. */
export function run(command: string, args: string[], options: SpawnSyncOptions = {}): void {
  const result = spawnSync(command, args, { stdio: "inherit", ...options });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(" ")} exited with code ${result.status}`);
  }
}

/** Runs a command and returns its stdout; throws on a non-zero exit code. */
export function capture(command: string, args: string[], options: SpawnSyncOptions = {}): string {
  const result = spawnSync(command, args, { encoding: "utf8", ...options });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(" ")} exited with code ${result.status}:\n${result.stderr}`);
  }
  return String(result.stdout);
}

/** Finds an executable on PATH or in known install locations. */
export function findExecutable(name: string, extraCandidates: string[] = []): string | undefined {
  const pathDirs = (process.env.PATH ?? "").split(process.platform === "win32" ? ";" : ":");
  for (const candidate of [...pathDirs.map((d) => join(d, name + EXE)), ...extraCandidates]) {
    if (candidate && existsSync(candidate)) return candidate;
  }
  return undefined;
}

export function fail(message: string): never {
  console.error(message);
  process.exit(1);
}
