// e2e test runner (ADR-0005): owns the game lifecycle, then runs the Vitest specs in tests/e2e/.
// 1. Builds our mods (unless --no-build) and installs the e2e test dependencies on first use.
// 2. Attaches to a running e2e client when its JARs and pack match; otherwise quits it and launches a fresh one
//    from the `living-kingdoms-e2e` Prism instance (pack mods + testkit + test options).
// 3. Runs Vitest with LK_E2E_PORT (by default the track's port, tools/pack/src/track.ts) and LK_E2E_RUN_DIR (runtime/e2e/runs/<timestamp>/), then quits the game
//    unless --keep.
//
// Usage: npm run e2e -- [--keep] [--no-build] [--restart] [vitest args...]
//   --keep      leave the game running; the next run attaches in seconds
//   --no-build  use the JARs already built in mods-src
//   --restart   quit a running client even if it matches
//   anything else goes to Vitest, e.g. a spec path or `-t "<name pattern>"`
// Launching needs Windows (portable Prism) and a signed-in Prism account; attaching works anywhere.
import { existsSync, mkdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { spawn, spawnSync } from "node:child_process";
import { E2E_INSTANCE_NAME } from "./pins.ts";
import { MODPACK_DIR, MODS_SRC_DIR, REPO_ROOT, RUNTIME_DIR, builtOwnJars, fail, run, sha256File } from "./lib.ts";
import { E2E_INSTANCE, PRISM_DIR, PRISM_EXE, prepareE2eInstance } from "./instance.ts";
import { E2E_PORT } from "./track.ts";
import { GameClient } from "../../../tests/e2e/harness/client.ts";
import type { HelloResult } from "../../../tests/e2e/harness/protocol.ts";

const E2E_DIR = join(REPO_ROOT, "tests", "e2e");
const STATE_FILE = join(RUNTIME_DIR, "e2e", "client.json");
const LAUNCH_TIMEOUT_S = 600;
const QUIT_TIMEOUT_S = 60;
const CRASH_MARKER = /---- Minecraft Crash Report ----|Crash report saved|Exception in thread "main"/;
const windows = process.platform === "win32";
const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

interface Options {
  keep: boolean;
  build: boolean;
  restart: boolean;
  vitestArgs: string[];
}

function parseArgs(): Options {
  const options: Options = { keep: false, build: true, restart: false, vitestArgs: [] };
  for (const arg of process.argv.slice(2)) {
    if (arg === "--keep") options.keep = true;
    else if (arg === "--no-build") options.build = false;
    else if (arg === "--restart") options.restart = true;
    else options.vitestArgs.push(arg);
  }
  return options;
}

/** What a client must have loaded to be reused: our built JARs and the pack index it was launched with. */
interface Fingerprint {
  ownJars: Record<string, string>;
  pack: string;
}

function currentFingerprint(): Fingerprint {
  const ownJars: Record<string, string> = {};
  for (const jar of builtOwnJars({ testkit: true })) ownJars[jar.name] = sha256File(jar.path);
  return { ownJars, pack: sha256File(join(MODPACK_DIR, "index.toml")) };
}

function mismatch(hello: HelloResult, expected: Fingerprint): string | undefined {
  const launchedPack = existsSync(STATE_FILE) ? (JSON.parse(readFileSync(STATE_FILE, "utf8")) as { pack?: string }).pack : undefined;
  if (launchedPack !== expected.pack) return "the pack (modpack/index.toml) changed since the client was launched";
  for (const [name, hash] of Object.entries(expected.ownJars)) {
    if (hello.ownJars[name] !== hash) return `${name} differs from the loaded JAR`;
  }
  const extra = Object.keys(hello.ownJars).filter((name) => !(name in expected.ownJars));
  if (extra.length) return `the client has JARs that are no longer built: ${extra.join(", ")}`;
  return undefined;
}

async function tryConnect(port: number): Promise<GameClient | undefined> {
  try {
    return await GameClient.connect(port, 1_000);
  } catch {
    return undefined;
  }
}

async function quitClient(client: GameClient, port: number): Promise<void> {
  await client.call("quit", {}).catch(() => undefined);
  client.close();
  const started = Date.now();
  while (Date.now() - started < QUIT_TIMEOUT_S * 1000) {
    await sleep(1000);
    const probe = await tryConnect(port);
    if (!probe) return;
    probe.close();
  }
  fail(`The e2e client did not quit within ${QUIT_TIMEOUT_S}s. Close it by hand and rerun.`);
}

async function launchClient(port: number, expected: Fingerprint): Promise<void> {
  if (!windows) {
    fail("Launching the e2e client is implemented for Windows (portable Prism) only. Start a client with the testkit " +
      `listening on 127.0.0.1:${port} yourself and rerun: \`npm run e2e\` attaches to it.`);
  }
  if (!existsSync(PRISM_EXE)) fail("No Prism yet. Run `npm run instance` first.");
  await prepareE2eInstance({ build: false, port });
  const log = join(E2E_INSTANCE.gameDir, "logs", "latest.log");
  console.log(`Launching ${E2E_INSTANCE_NAME}...`);
  spawn(PRISM_EXE, ["--dir", PRISM_DIR, "--launch", E2E_INSTANCE_NAME], { detached: true, stdio: "ignore" }).unref();
  mkdirSync(join(RUNTIME_DIR, "e2e"), { recursive: true });
  writeFileSync(STATE_FILE, JSON.stringify({ pack: expected.pack, launchedAt: new Date().toISOString() }, null, 2) + "\n");
  const started = Date.now();
  while (Date.now() - started < LAUNCH_TIMEOUT_S * 1000) {
    await sleep(2000);
    const client = await tryConnect(port);
    if (client) {
      // The driver listens from client setup on, before the resource reload ends; specs need the title screen.
      console.log(`Driver answered after ${Math.round((Date.now() - started) / 1000)}s; waiting for the resource reload.`);
      await waitUntilLoaded(client, started);
      client.close();
      console.log(`Client ready after ${Math.round((Date.now() - started) / 1000)}s (cold start).`);
      return;
    }
    // The game starts a new latest.log on launch; an older file still holds the previous run.
    if (existsSync(log) && statSync(log).mtimeMs >= started && CRASH_MARKER.test(readFileSync(log, "utf8"))) {
      fail(`The e2e client crashed while starting. Log: ${log}`);
    }
  }
  fail(`The testkit driver did not answer on 127.0.0.1:${port} within ${LAUNCH_TIMEOUT_S}s. Log: ${log}`);
}

/** Waits until the client has finished its startup resource reload (state.loading false). */
async function waitUntilLoaded(client: GameClient, started: number): Promise<void> {
  while (Date.now() - started < LAUNCH_TIMEOUT_S * 1000) {
    if (!(await client.call("state", {})).loading) return;
    await sleep(1000);
  }
  fail(`The e2e client did not finish loading within ${LAUNCH_TIMEOUT_S}s. Log: ${join(E2E_INSTANCE.gameDir, "logs", "latest.log")}`);
}

function ensureTestDependencies(): void {
  if (existsSync(join(E2E_DIR, "node_modules", "vitest"))) return;
  console.log("Installing e2e test dependencies (tests/e2e, npm ci)...");
  run(windows ? "cmd.exe" : "npm", windows ? ["/c", "npm", "ci", "--no-fund", "--no-audit"] : ["ci", "--no-fund", "--no-audit"], { cwd: E2E_DIR });
}

function buildMods(): void {
  console.log("Building our mods (mods-src)...");
  const wrapper = join(MODS_SRC_DIR, windows ? "gradlew.bat" : "gradlew");
  run(windows ? "cmd.exe" : wrapper, windows ? ["/c", wrapper, "build", "--quiet"] : ["build", "--quiet"], { cwd: MODS_SRC_DIR });
}

const options = parseArgs();
// The port follows the track branch (ADR-0006), so two track folders can run e2e at once.
const port = E2E_PORT;
ensureTestDependencies();
if (options.build) buildMods();
const expected = currentFingerprint();
if (!Object.keys(expected.ownJars).some((name) => name.startsWith("livingkingdoms_testkit-"))) {
  fail("No testkit JAR in mods-src/livingkingdoms-testkit/build/libs. Run without --no-build.");
}

const attachStarted = Date.now();
const running = await tryConnect(port);
let attached = false;
if (running) {
  const hello = await running.call("hello", {});
  const reason = options.restart ? "--restart was given" : mismatch(hello, expected);
  if (reason) {
    console.log(`Restarting the running e2e client: ${reason}.`);
    await quitClient(running, port);
  } else {
    await waitUntilLoaded(running, Date.now());
    running.close();
    attached = true;
    console.log(`Attached to the running e2e client in ${Date.now() - attachStarted} ms.`);
  }
}
if (!attached) await launchClient(port, expected);

const startedAt = new Date().toISOString();
const runDir = join(RUNTIME_DIR, "e2e", "runs", startedAt.replace(/[:.]/g, "-"));
// Names this run's flat test world (e2e-flat-<run id>): one world per run, reused by every spec file.
const runId = startedAt.replace(/[-:]/g, "").replace("T", "-").slice(0, 15);
mkdirSync(runDir, { recursive: true });
const vitest = spawnSync(process.execPath, [join(E2E_DIR, "node_modules", "vitest", "vitest.mjs"), "run", "--config", "vitest.config.ts", ...options.vitestArgs], {
  cwd: E2E_DIR,
  stdio: "inherit",
  env: { ...process.env, LK_E2E_PORT: String(port), LK_E2E_RUN_DIR: runDir, LK_E2E_RUN_ID: runId },
});
console.log(`Run artifacts: ${runDir}`);

if (!options.keep) {
  const client = await tryConnect(port);
  if (client) await quitClient(client, port);
} else {
  console.log("The e2e client stays open (--keep); the next `npm run e2e` attaches to it.");
}
process.exitCode = vitest.status ?? 1;
