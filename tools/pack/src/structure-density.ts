// Structure density research (M1-C1): reads the Overworld census CSV and the height grid that
// `/lkworld structures` and `/lkworld map` write into `<world>/livingkingdoms/`, and prints how dense
// and how evenly spread the structures are on the land.
//
// Usage: node tools/pack/src/structure-density.ts <world-livingkingdoms-dir> <size> [--decor "<regex>"] [--points N]
//   --decor   structure ids that do not count as a reward (small decorations), regex; default: none
//   --points  random land points for the distance-to-reward measure (default 4000)
// Output (stdout, Markdown): land area; starts per 1,000² blocks of land by mod; distance from random
// land points to the nearest rewarding start (median, p90, share beyond 512 blocks); clusters of
// surface starts; per-structure counts.
import { readFileSync } from "node:fs";
import { join } from "node:path";

const SEA_LEVEL = 63;
const CLUSTER_RADIUS = 256;

interface Start {
  id: string;
  namespace: string;
  surface: boolean;
  x: number;
  z: number;
  sizeX: number;
  sizeZ: number;
  inside: boolean;
}

function parseArgs() {
  const args = process.argv.slice(2);
  if (args.length < 2) throw new Error("usage: structure-density.ts <dir> <size> [--decor regex] [--points N]");
  let decor: RegExp | undefined;
  let points = 4000;
  for (let i = 2; i < args.length; i++) {
    if (args[i] === "--decor") decor = new RegExp(args[++i]);
    else if (args[i] === "--points") points = Number(args[++i]);
  }
  return { dir: args[0], size: args[1], decor, points };
}

function readStarts(file: string): Start[] {
  const lines = readFileSync(file, "utf8").trim().split(/\r?\n/);
  const header = lines[0].split(",");
  const col = (name: string) => header.indexOf(name);
  return lines.slice(1).map((line) => {
    const f = line.split(",");
    const minX = Number(f[col("min_x")]);
    const maxX = Number(f[col("max_x")]);
    const minZ = Number(f[col("min_z")]);
    const maxZ = Number(f[col("max_z")]);
    return {
      id: f[col("id")],
      namespace: f[col("namespace")],
      surface: f[col("step")] === "surface_structures",
      // The entrance (start piece) is where a player meets the structure.
      x: Number(f[col("entrance_x")]),
      z: Number(f[col("entrance_z")]),
      sizeX: maxX - minX + 1,
      sizeZ: maxZ - minZ + 1,
      inside: f[col("inside_border")] === "true",
    };
  });
}

interface Heights {
  origin: number;
  step: number;
  rows: number[][];
}

function readHeights(file: string): Heights {
  const lines = readFileSync(file, "utf8").trim().split(/\r?\n/);
  const match = /# origin (-?\d+),(-?\d+) step (\d+)/.exec(lines[0]);
  if (!match) throw new Error(`bad header in ${file}`);
  return { origin: Number(match[1]), step: Number(match[3]), rows: lines.slice(1).map((l) => l.split(",").map(Number)) };
}

/** Deterministic random numbers, so runs are comparable. */
function mulberry32(seed: number) {
  return () => {
    seed |= 0;
    seed = (seed + 0x6d2b79f5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function quantile(sorted: number[], q: number): number {
  if (sorted.length === 0) return NaN;
  return sorted[Math.min(sorted.length - 1, Math.floor(q * sorted.length))];
}

function main() {
  const { dir, size, decor, points } = parseArgs();
  const starts = readStarts(join(dir, `structures-overworld-${size}.csv`)).filter((s) => s.inside);
  const heights = readHeights(join(dir, `heights-${size}.csv`));
  const land: [number, number][] = [];
  heights.rows.forEach((row, py) =>
    row.forEach((h, px) => {
      if (h >= SEA_LEVEL) land.push([heights.origin + px * heights.step + heights.step / 2, heights.origin + py * heights.step + heights.step / 2]);
    }),
  );
  const landArea = land.length * heights.step * heights.step;
  const perKm = (n: number) => (n / landArea) * 1_000_000;
  const rewards = starts.filter((s) => !decor || !decor.test(s.id));

  console.log(`## ${size}: ${starts.length} starts inside the border, land ${(landArea / 1e6).toFixed(1)} M blocks²\n`);
  console.log(`Rewarding starts (not decor): ${rewards.length}, ${perKm(rewards.length).toFixed(2)} per 1,000² of land; surface ${rewards.filter((s) => s.surface).length}, underground or other ${rewards.filter((s) => !s.surface).length}.\n`);

  // Distance from random land points to the nearest rewarding entrance.
  const random = mulberry32(20261109);
  const distances: number[] = [];
  const surfaceDistances: number[] = [];
  const surfaceRewards = rewards.filter((s) => s.surface);
  for (let i = 0; i < points && land.length > 0; i++) {
    const [x, z] = land[Math.floor(random() * land.length)];
    let best = Infinity;
    let bestSurface = Infinity;
    for (const s of rewards) {
      const d = Math.hypot(s.x - x, s.z - z);
      if (d < best) best = d;
      if (s.surface && d < bestSurface) bestSurface = d;
    }
    distances.push(best);
    surfaceDistances.push(bestSurface);
  }
  distances.sort((a, b) => a - b);
  surfaceDistances.sort((a, b) => a - b);
  const beyond = (list: number[], d: number) => ((100 * list.filter((v) => v > d).length) / Math.max(1, list.length)).toFixed(1);
  console.log("| Nearest rewarding entrance from a random land point | median | p90 | > 256 | > 512 |");
  console.log("| --- | --- | --- | --- | --- |");
  console.log(`| any | ${quantile(distances, 0.5).toFixed(0)} | ${quantile(distances, 0.9).toFixed(0)} | ${beyond(distances, 256)}% | ${beyond(distances, 512)}% |`);
  console.log(`| surface | ${quantile(surfaceDistances, 0.5).toFixed(0)} | ${quantile(surfaceDistances, 0.9).toFixed(0)} | ${beyond(surfaceDistances, 256)}% | ${beyond(surfaceDistances, 512)}% |\n`);

  // Clutter: surface starts with at least two other surface starts within the cluster radius.
  const surface = starts.filter((s) => s.surface);
  let crowded = 0;
  for (const s of surface) {
    const near = surface.filter((o) => o !== s && Math.hypot(o.x - s.x, o.z - s.z) <= CLUSTER_RADIUS).length;
    if (near >= 2) crowded++;
  }
  console.log(`Clutter: ${crowded} of ${surface.length} surface starts have ≥ 2 other surface starts within ${CLUSTER_RADIUS} blocks.\n`);

  const byMod = new Map<string, number>();
  for (const s of starts) byMod.set(s.namespace, (byMod.get(s.namespace) ?? 0) + 1);
  console.log("| Mod | Starts | Per 1,000² of land |");
  console.log("| --- | --- | --- |");
  for (const [mod, n] of [...byMod].sort((a, b) => b[1] - a[1])) console.log(`| ${mod} | ${n} | ${perKm(n).toFixed(2)} |`);

  const byId = new Map<string, Start[]>();
  for (const s of starts) byId.set(s.id, [...(byId.get(s.id) ?? []), s]);
  console.log("\n| Structure | Starts | Mean footprint (blocks) | Decor |");
  console.log("| --- | --- | --- | --- |");
  for (const [id, list] of [...byId].sort((a, b) => b[1].length - a[1].length)) {
    const footprint = list.reduce((sum, s) => sum + s.sizeX * s.sizeZ, 0) / list.length;
    console.log(`| ${id} | ${list.length} | ${Math.round(Math.sqrt(footprint))}² | ${decor?.test(id) ? "yes" : ""} |`);
  }
}

main();
