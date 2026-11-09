// World validation over several seeds (M1-C1): for each seed, a fresh world of the size on the local
// test server runs `/lkworld validate` (our world mod checks the World Content Manifest); the JSON
// reports are collected into one table. Exit code 1 if any seed has a critical gap.
//
// Usage: npm run validate-world -- --size <small|medium|large|test> [--seeds a,b,c] [--no-build]
//   default seeds: livingkingdoms,alpha,20261107,kingdoms (the M1 research seeds)
// Reports: runtime/validation/<size>-<seed>.json and .md
import { copyFileSync, existsSync, mkdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { spawnSync } from "node:child_process";
import { RUNTIME_DIR, fail } from "./lib.ts";

const args = process.argv.slice(2);
let size = "";
let seeds = ["livingkingdoms", "alpha", "20261107", "kingdoms"];
let build = true;
for (let i = 0; i < args.length; i++) {
  if (args[i] === "--size") size = args[++i];
  else if (args[i] === "--seeds") seeds = args[++i].split(",").map((s) => s.trim()).filter(Boolean);
  else if (args[i] === "--no-build") build = false;
}
if (!["test", "small", "medium", "large"].includes(size)) fail("Usage: npm run validate-world -- --size <small|medium|large|test> [--seeds a,b,c] [--no-build]");

const out = join(RUNTIME_DIR, "validation");
mkdirSync(out, { recursive: true });
interface Report {
  critical: number;
  warnings: number;
  entries: { id: string; reachable: number; min?: number }[];
  issues: { severity: string; entry: string; message: string }[];
  info: string[];
}
const reports: [string, Report | null][] = [];
for (const [index, seed] of seeds.entries()) {
  console.log(`\n== ${size}, seed ${seed}`);
  const testArgs = ["tools/pack/src/test-server.ts", "--size", size, "--fresh", "--seed", seed, "--cmd", "lkworld validate",
    "--until", "Validation (test|small|medium|large) -?\\d+:", "--wait", "60"];
  // Build once, before the first seed; the later runs use the same JARs.
  if (!build || index > 0) testArgs.push("--no-build");
  const result = spawnSync(process.execPath, testArgs, { stdio: "inherit" });
  if (result.status !== 0) console.log(`test server exited with ${result.status}`);
  const dir = join(RUNTIME_DIR, "test-server", `world-${size}`, "livingkingdoms");
  const json = join(dir, `validation-${size}.json`);
  if (!existsSync(json)) {
    reports.push([seed, null]);
    continue;
  }
  copyFileSync(json, join(out, `${size}-${seed}.json`));
  copyFileSync(join(dir, `validation-${size}.md`), join(out, `${size}-${seed}.md`));
  reports.push([seed, JSON.parse(readFileSync(json, "utf8")) as Report]);
}

console.log(`\n# Validation, ${size}\n`);
const ids = [...new Set(reports.flatMap(([, r]) => r?.entries.map((e) => e.id) ?? []))];
console.log(`| Entry | ${seeds.join(" | ")} |`);
console.log(`| --- | ${seeds.map(() => "---").join(" | ")} |`);
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
let failed = false;
for (const [seed, r] of reports) {
  if (!r) {
    failed = true;
    console.log(`\n${seed}: no report`);
    continue;
  }
  if (r.critical > 0) failed = true;
  const critical = r.issues.filter((i) => i.severity === "critical");
  if (critical.length) console.log(`\n${seed} critical:\n${critical.map((i) => `- ${i.entry}: ${i.message}`).join("\n")}`);
}
console.log(`\nReports: ${out}`);
process.exit(failed ? 1 : 0);
