// Smoke test: launch the instance to the title screen, report loaded mods and ERROR/FATAL log lines, close the game.
// Usage: npm run smoke [-- --keep]   (--keep leaves the game running, e.g. for a manual world test)
// Needs a signed-in Prism account. Exit code 1 if the title screen is not reached or errors are logged.
import { existsSync, readFileSync, rmSync } from "node:fs";
import { join } from "node:path";
import { spawn, spawnSync } from "node:child_process";
import { INSTANCE_NAME } from "./pins.ts";
import { fail } from "./lib.ts";
import { GAME_DIR, INSTANCE_DIR, PRISM_DIR, PRISM_EXE, buildOwnMods, syncMods } from "./instance.ts";

const LOG = join(GAME_DIR, "logs", "latest.log");
const TIMEOUT_S = 300;
const TITLE_MARKER = /Sound engine started/;
const FAILURE_MARKER = /---- Minecraft Crash Report ----|Crash report saved|Exception in thread "main"|Stopping!/;

function stopGame(): void {
  if (process.platform !== "win32") return;
  const script =
    "Get-CimInstance Win32_Process -Filter \"Name='javaw.exe' OR Name='java.exe'\" | " +
    `Where-Object { $_.CommandLine -like '*${INSTANCE_NAME}*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }; ` +
    "Stop-Process -Name prismlauncher -Force -ErrorAction SilentlyContinue";
  spawnSync("powershell.exe", ["-NoProfile", "-Command", script], { stdio: "ignore" });
}

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

if (!existsSync(PRISM_EXE) || !existsSync(INSTANCE_DIR)) fail("No instance yet. Run `npm run instance` first.");
stopGame();
await sleep(2000);
syncMods();
buildOwnMods();
rmSync(LOG, { force: true });
spawn(PRISM_EXE, ["--dir", PRISM_DIR, "--launch", INSTANCE_NAME], { detached: true, stdio: "ignore" }).unref();

let log = "";
const started = Date.now();
while (Date.now() - started < TIMEOUT_S * 1000) {
  await sleep(1000);
  if (!existsSync(LOG)) continue;
  log = readFileSync(LOG, "utf8");
  if (TITLE_MARKER.test(log) || FAILURE_MARKER.test(log)) break;
}

const seconds = Math.round((Date.now() - started) / 1000);
const reached = TITLE_MARKER.test(log);
const errors = log.split(/\r?\n/).filter((l) => /\/(ERROR|FATAL)\]/.test(l));
const mods = [...log.matchAll(/^\t\t(.+?) \(([a-z0-9_]+)\)$/gm)].map((m) => `${m[2]} ${m[1].split(" ").pop()}`);

console.log(`Title screen: ${reached ? `reached after ${seconds}s` : `NOT reached (${seconds}s)`}`);
console.log(`Mods loaded (${mods.length}): ${mods.filter((m) => !/^(minecraft|neoforge) /.test(m)).join(", ")}`);
console.log(`ERROR/FATAL lines: ${errors.length}`);
for (const line of errors.slice(0, 20)) console.log(`  ${line}`);
console.log(`Log: ${LOG}`);

if (!process.argv.includes("--keep")) stopGame();
if (!reached || errors.length > 0) process.exitCode = 1;
