// One command that prepares a playable client instance from the repository:
// portable Prism Launcher in runtime/prism/, an instance with Minecraft + pinned NeoForge,
// and the pack's mods installed by packwiz-installer from modpack/pack.toml.
// Usage: npm run instance   (safe to rerun; it brings the instance back in sync with the pack)
import { copyFileSync, existsSync, mkdirSync, readFileSync, readdirSync, renameSync, rmSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { INSTANCE_NAME, MINECRAFT_VERSION, NEOFORGE_VERSION, PACKWIZ_INSTALLER, PACKWIZ_INSTALLER_BOOTSTRAP, PRISM_WINDOWS } from "./pins.ts";
import { MODPACK_DIR, RUNTIME_DIR, TOOLS_BIN, fail, fetchPinned, findExecutable, run } from "./lib.ts";
import { setupTools } from "./setup-tools.ts";

export const PRISM_DIR = join(RUNTIME_DIR, "prism");
export const PRISM_EXE = join(PRISM_DIR, "prismlauncher.exe");
export const INSTANCE_DIR = join(PRISM_DIR, "instances", INSTANCE_NAME);
export const GAME_DIR = join(INSTANCE_DIR, "minecraft");
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

function writeInstance(): void {
  mkdirSync(GAME_DIR, { recursive: true });
  const mmcPack = {
    components: [
      { important: true, uid: "net.minecraft", version: MINECRAFT_VERSION },
      { uid: "net.neoforged", version: NEOFORGE_VERSION },
    ],
    formatVersion: 1,
  };
  writeFileSync(join(INSTANCE_DIR, "mmc-pack.json"), JSON.stringify(mmcPack, null, 2) + "\n");

  // The pre-launch command re-syncs mods with modpack/ on every launch from Prism.
  const preLaunch =
    `"$INST_JAVA" -jar "$INST_MC_DIR/${PACKWIZ_INSTALLER_BOOTSTRAP.file}" --bootstrap-no-update ` +
    `--bootstrap-main-jar "$INST_MC_DIR/${PACKWIZ_INSTALLER.file}" -g -s client "${PACK_TOML.replaceAll("\\", "/")}"`;
  writeIni(join(INSTANCE_DIR, "instance.cfg"), "General", {
    ConfigVersion: "1.2",
    InstanceType: "OneSix",
    name: "Living Kingdoms",
    OverrideMemory: "true",
    MaxMemAlloc: String(MAX_MEM_MIB),
    MinMemAlloc: String(MIN_MEM_MIB),
    OverrideCommands: "true",
    PreLaunchCommand: preLaunch,
  });

  for (const pin of [PACKWIZ_INSTALLER, PACKWIZ_INSTALLER_BOOTSTRAP]) {
    copyFileSync(join(TOOLS_BIN, pin.file), join(GAME_DIR, pin.file));
  }
}

function syncMods(): void {
  const java = findExecutable("java") ?? fail("Java 21 is required on PATH to run packwiz-installer.");
  run(
    java,
    [
      "-jar", PACKWIZ_INSTALLER_BOOTSTRAP.file,
      "--bootstrap-no-update", "--bootstrap-main-jar", PACKWIZ_INSTALLER.file,
      "-g", "-s", "client", PACK_TOML,
    ],
    { cwd: GAME_DIR },
  );
}

export async function prepareInstance(): Promise<void> {
  checkPackVersions();
  await setupTools();
  await ensurePrism();
  writeInstance();
  syncMods();
  console.log(
    `\nInstance ready: ${INSTANCE_DIR}\n` +
      `Minecraft ${MINECRAFT_VERSION}, NeoForge ${NEOFORGE_VERSION}.\n` +
      `Start it with \`npm run launch\` (first time: sign in to your Microsoft account in Prism).`,
  );
}

if (import.meta.main) {
  await prepareInstance();
}
