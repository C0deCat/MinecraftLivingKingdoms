// World validation over several seeds (M1-C1): for each seed, a fresh world of the size on the local
// test server runs `/lkworld validate` (our world mod checks the World Content Manifest); the JSON
// reports are collected into one table per size. Exit code 1 if any world has a critical gap.
//
// Usage: npm run validate-world -- [--size <small|medium|large|test>] [--seeds a,b,c] [--full] [--no-build]
//   default: the phase standard (D-21), Large × seeds livingkingdoms,alpha,20261107 (about 15 min)
//   --full: the milestone standard (D-21), Small, Medium and Large × livingkingdoms,alpha,20261107,kingdoms
// Reports: runtime/validation/<size>-<seed>.json and .md
import { copyFileSync, existsSync, mkdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { spawnSync } from "node:child_process";
import { RUNTIME_DIR, fail } from "./lib.ts";

const args = process.argv.slice(2);
const PHASE_SEEDS = ["livingkingdoms", "alpha", "20261107"];
const FULL_SEEDS = ["livingkingdoms", "alpha", "20261107", "kingdoms"];
let size = "";
let seeds: string[] | null = null;
let full = false;
let build = true;
for (let i = 0; i < args.length; i++) {
  if (args[i] === "--size") size = args[++i];
  else if (args[i] === "--seeds") seeds = args[++i].split(",").map((s) => s.trim()).filter(Boolean);
  else if (args[i] === "--full") full = true;
  else if (args[i] === "--no-build") build = false;
}
const usage = "Usage: npm run validate-world -- [--size <small|medium|large|test>] [--seeds a,b,c] [--full] [--no-build]";
if (size && !["test", "small", "medium", "large"].includes(size)) fail(usage);
if (full && (size || seeds)) fail("--full runs its own sizes and seeds; leave out --size and --seeds");
const sizes = full ? ["small", "medium", "large"] : [size || "large"];
const seedList = seeds ?? (full ? FULL_SEEDS : PHASE_SEEDS);

const out = join(RUNTIME_DIR, "validation");
mkdirSync(out, { recursive: true });
interface Report {
  critical: number;
  warnings: number;
  entries: { id: string; reachable: number; min?: number }[];
  issues: { severity: string; entry: string; message: string }[];
  info: string[];
}
let failed = false;
let runs = 0;
for (const size of sizes) {
  const reports: [string, Report | null][] = [];
  for (const seed of seedList) reports.push([seed, runSeed(size, seed)]);
  printTable(size, reports);
  for (const [seed, r] of reports) {
    if (!r) {
      failed = true;
      console.log(`\n${size} ${seed}: no report`);
      continue;
    }
    if (r.critical > 0) failed = true;
    const critical = r.issues.filter((i) => i.severity === "critical");
    if (critical.length) console.log(`\n${size} ${seed} critical:\n${critical.map((i) => `- ${i.entry}: ${i.message}`).join("\n")}`);
  }
}
console.log(`\nReports: ${out}`);
process.exit(failed ? 1 : 0);

/** A fresh world of this size and seed on the test server, `/lkworld validate`, its JSON report. */
function runSeed(size: string, seed: string): Report | null {
  console.log(`\n== ${size}, seed ${seed}`);
  const testArgs = ["tools/pack/src/test-server.ts", "--size", size, "--fresh", "--seed", seed, "--cmd", "lkworld validate",
    "--until", "Validation (test|small|medium|large) -?\\d+:", "--wait", "60"];
  // Build once, before the first world; the later runs use the same JARs.
  if (!build || runs++ > 0) testArgs.push("--no-build");
  const result = spawnSync(process.execPath, testArgs, { stdio: "inherit" });
  if (result.status !== 0) console.log(`test server exited with ${result.status}`);
  const dir = join(RUNTIME_DIR, "test-server", `world-${size}`, "livingkingdoms");
  const json = join(dir, `validation-${size}.json`);
  if (!existsSync(json)) return null;
  copyFileSync(json, join(out, `${size}-${seed}.json`));
  copyFileSync(join(dir, `validation-${size}.md`), join(out, `${size}-${seed}.md`));
  return JSON.parse(readFileSync(json, "utf8")) as Report;
}

function printTable(size: string, reports: [string, Report | null][]): void {
  console.log(`\n# Validation, ${size}\n`);
  const ids = [...new Set(reports.flatMap(([, r]) => r?.entries.map((e) => e.id) ?? []))];
  console.log(`| Entry | ${reports.map(([seed]) => seed).join(" | ")} |`);
  console.log(`| --- | ${reports.map(() => "---").join(" | ")} |`);
  for (const id of ids) {
    const cells = reports.map(([, r]) => {
      const e = r?.entries.find((x) => x.id === id);
      if (!e) return "-";
      const short = e.min !== undefined && e.reachable < e.min;
      return `${e.reachable}${e.min !== undefined ? "/" + e.min : ""}${short ? " ✗" : ""}`;
    });
    console.log(`| ${id} | ${cells.join(" | ")} |`);
  }
  console.log(`| **critical / warnings** | ${reports.map(([, r]) => (r ? `${r.critical} / ${r.warnings}` : "no report")).join(" | ")} |`);
}
