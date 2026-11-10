// One command that prepares a playable client instance from the repository:
// portable Prism Launcher in runtime/prism/, an instance with Minecraft + pinned NeoForge,
// and the pack's mods installed by packwiz-installer from modpack/pack.toml.
// Usage: npm run instance   (safe to rerun; it brings the instance back in sync with the pack)
import { copyFileSync, existsSync, mkdirSync, readFileSync, readdirSync, renameSync, rmSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { E2E_INSTANCE_NAME, INSTANCE_NAME, MINECRAFT_VERSION, NEOFORGE_VERSION, PACKWIZ_INSTALLER, PACKWIZ_INSTALLER_BOOTSTRAP, PRISM_WINDOWS } from "./pins.ts";
import { MODPACK_DIR, MODS_SRC_DIR, OWN_JAR_PREFIX, RUNTIME_DIR, TOOLS_BIN, builtOwnJars, capture, fail, fetchPinned, findExecutable, run } from "./lib.ts";
import { setupTools } from "./setup-tools.ts";

export const PRISM_DIR = join(RUNTIME_DIR, "prism");
export const PRISM_EXE = join(PRISM_DIR, "prismlauncher.exe");
export const INSTANCE_DIR = join(PRISM_DIR, "instances", INSTANCE_NAME);
export const GAME_DIR = join(INSTANCE_DIR, "minecraft");

/** A Prism instance this tooling manages: the play instance, or the e2e instance with the testkit (ADR-0005). */
export interface InstanceTarget {
  name: string;
  displayName: string;
  dir: string;
  gameDir: string;
  /** Whether the dev-only testkit JAR is installed. Never true for the play instance. */
  testkit: boolean;
}

export const PLAY_INSTANCE: InstanceTarget = { name: INSTANCE_NAME, displayName: "Living Kingdoms", dir: INSTANCE_DIR, gameDir: GAME_DIR, testkit: false };
const E2E_DIR = join(PRISM_DIR, "instances", E2E_INSTANCE_NAME);
export const E2E_INSTANCE: InstanceTarget = { name: E2E_INSTANCE_NAME, displayName: "Living Kingdoms (e2e tests)", dir: E2E_DIR, gameDir: join(E2E_DIR, "minecraft"), testkit: true };

/**
 * Client options of the e2e instance (`key:value` lines of options.txt), merged on every prepare.
 * pauseOnLostFocus is off because the integrated server pauses while the window has no focus (quality.md);
 * en_us keeps chat and log text stable for the few tests that assert text (convention A2, R2).
 */
const E2E_OPTIONS: Record<string, string> = {
  pauseOnLostFocus: "false",
  lang: "en_us",
  renderDistance: "6",
  simulationDistance: "6",
  maxFps: "60",
  narrator: "0",
  tutorialStep: "none",
  onboardAccessibility: "false",
  joinedFirstServer: "true",
  skipMultiplayerWarning: "true",
  soundCategory_master: "0.0",
};
/**
 * Iris options of the e2e instance (`key=value` lines of config/iris.properties), merged after every mod sync.
 * The pack ships Complementary Reimagined switched on (M1-C2); e2e runs without shaders (convention F1, quality.md).
 * packwiz-installer preserves the file once installed, so the merge survives later syncs.
 */
const E2E_IRIS_OPTIONS: Record<string, string> = { enableShaders: "false" };
const PACK_TOML = join(MODPACK_DIR, "pack.toml");

/** Memory for the game JVM, in MiB. Tuned for the 32 GB host PC; adjust after spark measurements. */
const MAX_MEM_MIB = 6144;
const MIN_MEM_MIB = 2048;

function checkPackVersions(): void {
  const pack = readFileSync(PACK_TOML, "utf8");
  const mc = /^minecraft\s*=\s*"([^"]+)"/m.exec(pack)?.[1];
  const neo = /^neoforge\s*=\s*"([^"]+)"/m.exec(pack)?.[1];
  if (mc !== MINECRAFT_VERSION || neo !== NEOFORGE_VERSION) {
    fail(
      `modpack/pack.toml targets minecraft=${mc}, neoforge=${neo}, but tools/pack/src/pins.ts pins ` +
        `${MINECRAFT_VERSION} / ${NEOFORGE_VERSION}. Change both together (and the docs), then rerun.`,
    );
  }
}

async function ensurePrism(): Promise<void> {
  if (existsSync(PRISM_EXE)) return;
  if (process.platform !== "win32") {
    fail("Automatic Prism setup is implemented for Windows only. Install Prism Launcher manually and import modpack/ via packwiz-installer.");
  }
  const zip = await fetchPinned(PRISM_WINDOWS, join(RUNTIME_DIR, "downloads"));
  const staging = join(RUNTIME_DIR, "prism-extract");
  rmSync(staging, { recursive: true, force: true });
  mkdirSync(staging, { recursive: true });
  // Use the Windows bsdtar explicitly: GNU tar from Git Bash cannot read zips or drive-letter paths.
  const tar = join(process.env.SystemRoot ?? "C:\\Windows", "System32", "tar.exe");
  if (!existsSync(tar)) fail(`${tar} not found (it ships with Windows 10 and later).`);
  run(tar, ["-xf", zip, "-C", staging]);
  // The zip may contain the files directly or inside a single top-level folder.
  const entries = readdirSync(staging);
  const root = entries.length === 1 && !existsSync(join(staging, "prismlauncher.exe")) ? join(staging, entries[0]) : staging;
  if (!existsSync(join(root, "prismlauncher.exe"))) fail(`prismlauncher.exe not found in ${PRISM_WINDOWS.file}`);
  rmSync(PRISM_DIR, { recursive: true, force: true });
  renameSync(root, PRISM_DIR);
  rmSync(staging, { recursive: true, force: true });
  // portable.txt makes Prism keep all its data (accounts, settings, instances) next to the executable.
  writeFileSync(join(PRISM_DIR, "portable.txt"), "");
  console.log(`Prism Launcher installed to ${PRISM_DIR}`);
}

/** Merges managed keys into an INI file, keeping keys Prism or the user added. */
function writeIni(path: string, section: string, values: Record<string, string>): void {
  const lines = existsSync(path) ? readFileSync(path, "utf8").split(/\r?\n/) : [`[${section}]`];
  if (!lines.some((l) => l.trim() === `[${section}]`)) lines.unshift(`[${section}]`);
  for (const [key, value] of Object.entries(values)) {
    const index = lines.findIndex((l) => l.startsWith(`${key}=`));
    if (index >= 0) lines[index] = `${key}=${value}`;
    else lines.splice(lines.findIndex((l) => l.trim() === `[${section}]`) + 1, 0, `${key}=${value}`);
  }
  writeFileSync(path, lines.filter((l, i) => l !== "" || i < lines.length - 1).join("\n") + "\n");
}

function writeInstance(target: InstanceTarget): void {
  mkdirSync(target.gameDir, { recursive: true });
  const mmcPack = {
    components: [
      { important: true, uid: "net.minecraft", version: MINECRAFT_VERSION },
      { uid: "net.neoforged", version: NEOFORGE_VERSION },
    ],
    formatVersion: 1,
  };
  writeFileSync(join(target.dir, "mmc-pack.json"), JSON.stringify(mmcPack, null, 2) + "\n");

  // No Prism pre-launch command: Prism 11 mangles quoted arguments in it ("Process failed to start").
  // Mods are synced by `npm run instance` and `npm run launch` instead.
  writeIni(join(target.dir, "instance.cfg"), "General", {
    ConfigVersion: "1.2",
    InstanceType: "OneSix",
    name: target.displayName,
    OverrideMemory: "true",
    MaxMemAlloc: String(MAX_MEM_MIB),
    MinMemAlloc: String(MIN_MEM_MIB),
    OverrideCommands: "false",
    PreLaunchCommand: "",
  });

  for (const pin of [PACKWIZ_INSTALLER, PACKWIZ_INSTALLER_BOOTSTRAP]) {
    copyFileSync(join(TOOLS_BIN, pin.file), join(target.gameDir, pin.file));
  }
  if (target.testkit) writeOptions(join(target.gameDir, "options.txt"), E2E_OPTIONS, ":");
}

/** Merges managed `key<sep>value` lines into an options file, keeping every other line the game wrote. */
function writeOptions(path: string, values: Record<string, string>, separator: ":" | "="): void {
  const lines = existsSync(path) ? readFileSync(path, "utf8").split(/\r?\n/).filter((l) => l !== "") : [];
  for (const [key, value] of Object.entries(values)) {
    const index = lines.findIndex((l) => l.startsWith(`${key}${separator}`));
    if (index >= 0) lines[index] = `${key}${separator}${value}`;
    else lines.push(`${key}${separator}${value}`);
  }
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, lines.join("\n") + "\n");
}

export function syncMods(target: InstanceTarget = PLAY_INSTANCE): void {
  const java = findExecutable("java") ?? fail("Java 21 is required on PATH to run packwiz-installer.");
  run(
    java,
    [
      "-jar", PACKWIZ_INSTALLER_BOOTSTRAP.file,
      "--bootstrap-no-update", "--bootstrap-main-jar", PACKWIZ_INSTALLER.file,
      "-g", "-s", "client", PACK_TOML,
    ],
    { cwd: target.gameDir },
  );
}

/**
 * Builds our mods from mods-src/ and replaces their JARs in the instance. They are not packwiz-managed
 * before M11 (exec plan 2026-11-06-m1-world), so packwiz-installer leaves them alone.
 * The testkit JAR goes only into an instance with `testkit: true` (the e2e instance).
 */
export function buildOwnMods(target: InstanceTarget = PLAY_INSTANCE, options: { build: boolean } = { build: true }): void {
  const windows = process.platform === "win32";
  if (options.build) {
    console.log("Building our mods (mods-src)...");
    // Full path: cmd.exe may not search the current directory (NoDefaultCurrentDirectoryInExePath).
    const wrapper = join(MODS_SRC_DIR, windows ? "gradlew.bat" : "gradlew");
    run(windows ? "cmd.exe" : wrapper, windows ? ["/c", wrapper, "build", "--quiet"] : ["build", "--quiet"], { cwd: MODS_SRC_DIR });
  }
  const modsDir = join(target.gameDir, "mods");
  mkdirSync(modsDir, { recursive: true });
  for (const old of readdirSync(modsDir).filter((f) => f.startsWith(OWN_JAR_PREFIX))) rmSync(join(modsDir, old));
  for (const jar of builtOwnJars({ testkit: target.testkit })) {
    copyFileSync(jar.path, join(modsDir, jar.name));
    console.log(`  installed ${jar.name}`);
  }
}

/** Prism keeps instance settings in memory and writes them back on exit, overwriting our instance.cfg. */
function refuseIfPrismRunning(): void {
  if (process.platform !== "win32") return;
  const tasks = capture("tasklist", ["/FI", "IMAGENAME eq prismlauncher.exe", "/NH"]);
  if (tasks.toLowerCase().includes("prismlauncher.exe")) {
    fail("Prism Launcher is running. Close it completely (it rewrites instance settings on exit), then rerun `npm run instance`.");
  }
}

export async function prepareInstance(): Promise<void> {
  refuseIfPrismRunning();
  checkPackVersions();
  await setupTools();
  await ensurePrism();
  writeInstance(PLAY_INSTANCE);
  syncMods(PLAY_INSTANCE);
  buildOwnMods(PLAY_INSTANCE);
  console.log(
    `\nInstance ready: ${INSTANCE_DIR}\n` +
      `Minecraft ${MINECRAFT_VERSION}, NeoForge ${NEOFORGE_VERSION}.\n` +
      `Start it with \`npm run launch\` (first time: sign in to your Microsoft account in Prism).`,
  );
}

/**
 * Brings the e2e instance in sync: the same pack mods as the play instance, plus the testkit and test options.
 * Called by `npm run e2e` before it launches the game; Prism must not be running (it rewrites instance.cfg).
 */
export async function prepareE2eInstance(options: { build: boolean }): Promise<void> {
  refuseIfPrismRunning();
  checkPackVersions();
  await setupTools();
  await ensurePrism();
  writeInstance(E2E_INSTANCE);
  syncMods(E2E_INSTANCE);
  writeOptions(join(E2E_INSTANCE.gameDir, "config", "iris.properties"), E2E_IRIS_OPTIONS, "=");
  buildOwnMods(E2E_INSTANCE, options);
}

if (import.meta.main) {
  await prepareInstance();
}
