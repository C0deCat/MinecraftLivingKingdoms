// Checks the pack against its pins, the Modrinth metadata and the mod registry docs.
// Usage: npm run verify-mods [-- --offline]   (--offline skips the Modrinth API checks)
// Every failure message says how to fix it.
import { existsSync, readFileSync, readdirSync } from "node:fs";
import { createHash } from "node:crypto";
import { join, relative, sep } from "node:path";
import { MINECRAFT_VERSION, NEOFORGE_VERSION } from "./pins.ts";
import { MODPACK_DIR, REPO_ROOT } from "./lib.ts";

const GAME_MODS_DIR = join(REPO_ROOT, "runtime", "prism", "instances", "living-kingdoms", "minecraft", "mods");
const MATRIX = "docs/compatibility-matrix.md";
const LICENSES = "docs/licenses.md";

const errors: string[] = [];
const error = (message: string): void => void errors.push(message);

// ---------- tiny TOML readers for the packwiz formats (flat keys + [section] + [[files]]) ----------

type Table = Record<string, string | boolean>;

function parseToml(text: string): { root: Table; sections: Map<string, Table>; files: Table[] } {
  const root: Table = {};
  const sections = new Map<string, Table>();
  const files: Table[] = [];
  let current = root;
  for (const raw of text.split(/\r?\n/)) {
    const line = raw.trim();
    if (!line || line.startsWith("#")) continue;
    const arrayHeader = /^\[\[(.+)\]\]$/.exec(line);
    const header = /^\[(.+)\]$/.exec(line);
    if (arrayHeader) {
      current = {};
      if (arrayHeader[1] === "files") files.push(current);
    } else if (header) {
      current = sections.get(header[1]) ?? {};
      sections.set(header[1], current);
    } else {
      const kv = /^([\w-]+)\s*=\s*(.+)$/.exec(line);
      if (!kv) continue;
      const value = kv[2].trim();
      current[kv[1]] = value === "true" ? true : value === "false" ? false : value.replace(/^"(.*)"$/, "$1");
    }
  }
  return { root, sections, files };
}

const sha256 = (path: string): string => createHash("sha256").update(readFileSync(path)).digest("hex");

// ---------- markdown table reader ----------

/** Rows of the first table after `heading`, as arrays of trimmed cells (header and separator excluded). */
function tableRows(doc: string, heading: string): string[][] {
  const lines = readFileSync(join(REPO_ROOT, doc), "utf8").split(/\r?\n/);
  const start = lines.findIndex((l) => l.trim() === heading);
  if (start < 0) {
    error(`${doc}: heading "${heading}" not found. Restore it; verify-mods reads the mod table under it.`);
    return [];
  }
  const rows: string[][] = [];
  let inTable = false;
  for (const line of lines.slice(start + 1)) {
    if (line.startsWith("#")) break;
    if (!line.trim().startsWith("|")) {
      if (inTable) break;
      continue;
    }
    inTable = true;
    rows.push(line.trim().replace(/^\||\|$/g, "").split("|").map((c) => c.trim()));
  }
  return rows.slice(2);
}

// ---------- checks ----------

interface PackMod {
  metafile: string;
  name: string;
  filename: string;
  side: string;
  modrinthId?: string;
  modrinthVersion?: string;
  hashFormat: string;
  hash: string;
}

function checkPackToml(): void {
  const pack = parseToml(readFileSync(join(MODPACK_DIR, "pack.toml"), "utf8"));
  const versions = pack.sections.get("versions") ?? {};
  if (versions.minecraft !== MINECRAFT_VERSION || versions.neoforge !== NEOFORGE_VERSION) {
    error(
      `modpack/pack.toml targets minecraft=${versions.minecraft}, neoforge=${versions.neoforge}; ` +
        `tools/pack/src/pins.ts pins ${MINECRAFT_VERSION} / ${NEOFORGE_VERSION}. Change both together.`,
    );
  }
  for (const loader of ["forge", "fabric", "quilt", "liteloader"]) {
    if (versions[loader]) error(`modpack/pack.toml declares a second loader "${loader}". The pack is NeoForge only; remove it.`);
  }
  const index = pack.sections.get("index") ?? {};
  const indexPath = join(MODPACK_DIR, String(index.file ?? "index.toml"));
  if (!existsSync(indexPath) || sha256(indexPath) !== index.hash) {
    error("modpack/pack.toml: index hash is stale. Run `tools/bin/packwiz refresh` in modpack/ and commit the result.");
  }
}

function readMods(): PackMod[] {
  const index = parseToml(readFileSync(join(MODPACK_DIR, "index.toml"), "utf8"));
  const indexed = new Map(index.files.map((f) => [String(f.file), f]));

  // Every file in modpack/ (except the pack's own manifests and docs) must be indexed with a current hash.
  const walk = (dir: string): string[] =>
    readdirSync(dir, { withFileTypes: true }).flatMap((e) =>
      e.isDirectory() ? walk(join(dir, e.name)) : [relative(MODPACK_DIR, join(dir, e.name)).split(sep).join("/")],
    );
  const unindexed = new Set(["pack.toml", "index.toml", "CHANGELOG.md", ".packwizignore"]);
  for (const file of walk(MODPACK_DIR)) {
    if (unindexed.has(file)) continue;
    const entry = indexed.get(file);
    if (!entry) error(`modpack/${file} is not in index.toml. Run \`tools/bin/packwiz refresh\` in modpack/ (or delete the file).`);
    else if (entry.hash !== sha256(join(MODPACK_DIR, file))) {
      error(`modpack/${file} changed since the last index refresh. Run \`tools/bin/packwiz refresh\` in modpack/.`);
    }
  }
  for (const file of indexed.keys()) {
    if (!existsSync(join(MODPACK_DIR, file))) error(`index.toml lists missing file modpack/${file}. Run \`tools/bin/packwiz refresh\`.`);
  }

  const mods: PackMod[] = [];
  for (const [file, entry] of indexed) {
    if (!entry.metafile || !existsSync(join(MODPACK_DIR, file))) continue;
    const meta = parseToml(readFileSync(join(MODPACK_DIR, file), "utf8"));
    const download = meta.sections.get("download") ?? {};
    const modrinth = meta.sections.get("update.modrinth");
    mods.push({
      metafile: file,
      name: String(meta.root.name),
      filename: String(meta.root.filename),
      side: String(meta.root.side ?? "both"),
      modrinthId: modrinth ? String(modrinth["mod-id"]) : undefined,
      modrinthVersion: modrinth ? String(modrinth.version) : undefined,
      hashFormat: String(download["hash-format"]),
      hash: String(download.hash),
    });
    if (!modrinth && !meta.sections.has("update.curseforge")) {
      error(`modpack/${file}: no Modrinth or CurseForge source. Add mods only with \`packwiz modrinth add\` / \`packwiz curseforge add\`.`);
    }
  }
  return mods;
}

function checkDuplicates(mods: PackMod[]): void {
  const seen = new Map<string, string>();
  for (const mod of mods) {
    for (const key of [`project:${mod.modrinthId}`, `file:${mod.filename}`, `name:${mod.name.toLowerCase()}`]) {
      if (key.endsWith(":undefined")) continue;
      const other = seen.get(key);
      if (other) error(`Duplicate mod: ${mod.metafile} and ${other} share ${key}. Keep one of them.`);
      seen.set(key, mod.metafile);
    }
  }
}

interface ModrinthVersion {
  id: string;
  project_id: string;
  version_number: string;
  loaders: string[];
  game_versions: string[];
  dependencies: { project_id: string | null; dependency_type: string }[];
  files: { filename: string; hashes: Record<string, string> }[];
}

async function checkModrinth(mods: PackMod[]): Promise<void> {
  const withIds = mods.filter((m) => m.modrinthVersion);
  if (withIds.length === 0) return;
  const ids = encodeURIComponent(JSON.stringify(withIds.map((m) => m.modrinthVersion)));
  let versions: ModrinthVersion[];
  try {
    const response = await fetch(`https://api.modrinth.com/v2/versions?ids=${ids}`, {
      headers: { "User-Agent": "living-kingdoms-verify-mods" },
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    versions = (await response.json()) as ModrinthVersion[];
  } catch (e) {
    error(`Modrinth API unreachable (${(e as Error).message}). Retry, or run with \`-- --offline\` to skip these checks.`);
    return;
  }
  const byId = new Map(versions.map((v) => [v.id, v]));
  const projects = new Set(mods.map((m) => m.modrinthId));
  for (const mod of withIds) {
    const version = byId.get(mod.modrinthVersion!);
    if (!version) {
      error(`${mod.metafile}: Modrinth version ${mod.modrinthVersion} not found (deleted upstream?). Re-add the mod with packwiz.`);
      continue;
    }
    if (!version.loaders.includes("neoforge")) {
      error(`${mod.metafile}: ${version.version_number} is for ${version.loaders.join("/")}, not NeoForge. Pick a NeoForge file.`);
    }
    if (!version.game_versions.includes(MINECRAFT_VERSION)) {
      error(`${mod.metafile}: ${version.version_number} does not list Minecraft ${MINECRAFT_VERSION}. Pick a ${MINECRAFT_VERSION} file.`);
    }
    const file = version.files.find((f) => f.filename === mod.filename);
    if (!file || file.hashes[mod.hashFormat] !== mod.hash) {
      error(`${mod.metafile}: file name or ${mod.hashFormat} hash differs from Modrinth's. Re-add the mod with packwiz.`);
    }
    for (const dep of version.dependencies) {
      if (dep.dependency_type === "required" && dep.project_id && !projects.has(dep.project_id)) {
        error(`${mod.name} requires Modrinth project ${dep.project_id}, which is not in the pack. Add it with \`packwiz modrinth add ${dep.project_id}\`.`);
      }
      if (dep.dependency_type === "incompatible" && dep.project_id && projects.has(dep.project_id)) {
        error(`${mod.name} declares Modrinth project ${dep.project_id} incompatible, but both are in the pack. Remove one and record why in ${MATRIX}.`);
      }
    }
  }
}

function checkRegistry(mods: PackMod[]): void {
  const matrix = tableRows(MATRIX, "## Mods").filter((r) => r[0] !== "—");
  const licenses = tableRows(LICENSES, "## 2. Registry");
  for (const mod of mods) {
    const row = matrix.find((r) => r[0].toLowerCase() === mod.name.toLowerCase());
    if (!row) {
      error(`${MATRIX}: no row for "${mod.name}". Add one (Mod | Version | Stage | Purpose | Side | Dependencies | Status | Notes).`);
    } else {
      if (!row[1] || !mod.filename.includes(row[1])) {
        error(`${MATRIX}: "${mod.name}" version "${row[1]}" does not match ${mod.filename}. Update the Version cell.`);
      }
      if (row[4] !== mod.side) error(`${MATRIX}: "${mod.name}" side "${row[4]}" differs from ${mod.metafile} (side = "${mod.side}").`);
    }
    if (!licenses.some((r) => r[0].toLowerCase() === mod.name.toLowerCase())) {
      error(`${LICENSES}: no registry row for "${mod.name}". Add its license from the mod page.`);
    }
  }
  for (const row of matrix) {
    if (row[6] === "rejected") continue;
    if (!mods.some((m) => m.name.toLowerCase() === row[0].toLowerCase())) {
      error(`${MATRIX}: "${row[0]}" is listed but not in the pack. Remove the row or mark it rejected (move it to Rejected mods).`);
    }
  }
}

function checkInstance(mods: PackMod[]): void {
  if (!existsSync(GAME_MODS_DIR)) return; // no instance yet: nothing to compare
  const expected = new Set(mods.filter((m) => m.side !== "server").map((m) => m.filename));
  const present = readdirSync(GAME_MODS_DIR).filter((f) => f.endsWith(".jar"));
  for (const jar of present) {
    if (!expected.has(jar)) {
      error(`Instance has ${jar}, which is not in the pack. Delete it from ${relative(REPO_ROOT, GAME_MODS_DIR)} or add the mod via packwiz.`);
    }
  }
  for (const jar of expected) {
    if (!present.includes(jar)) error(`Instance is missing ${jar}. Run \`npm run instance\`.`);
  }
}

// ---------- main ----------

const offline = process.argv.includes("--offline");
checkPackToml();
const mods = readMods();
checkDuplicates(mods);
if (!offline) await checkModrinth(mods);
checkRegistry(mods);
checkInstance(mods);

if (errors.length > 0) {
  for (const message of errors) console.error(`ERROR: ${message}`);
  console.error(`\nverify-mods — ${errors.length} problem(s).`);
  // exitCode instead of process.exit(): exiting while fetch sockets close trips a libuv assertion on Windows.
  process.exitCode = 1;
} else console.log(
  `verify-mods — OK: ${mods.length} mod(s), Minecraft ${MINECRAFT_VERSION}, NeoForge ${NEOFORGE_VERSION}` +
    (offline ? " (offline: Modrinth checks skipped)." : "."),
);
