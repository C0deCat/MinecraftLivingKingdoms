// Local headless NeoForge server used as a worldgen test harness (not the M10 multiplayer work).
// It installs the pack's server-side mods plus our mods, creates/opens a world of one size preset,
// runs console commands, prints their output and stops.
//
// Usage: npm run test-server -- [--size test|small|medium|large] [--fresh] [--cmd "<command>"]... [--until "<regex>"] [--jvm "<arg>"]...
//   --fresh   delete that size's test world first (worlds are disposable until M11)
//   --cmd     console command without the slash; repeatable. Borders per dimension are logged by our mod on start.
//   --until   after the commands, wait (up to 15 min) for a log line matching this regex before stopping,
//             e.g. --until "Map (test|small|medium|large):" for the asynchronous /lkworld map
//   --seed    level seed for new worlds (default "livingkingdoms", so runs are comparable)
//   --jvm     extra JVM argument, repeatable (e.g. --jvm -Dlivingkingdoms.edge=off)
// The Minecraft EULA for this local server was accepted with the user's permission on 2026-11-06.
import { copyFileSync, existsSync, mkdirSync, readdirSync, rmSync, writeFileSync, createWriteStream } from "node:fs";
import { join } from "node:path";
import { spawn } from "node:child_process";
import { NEOFORGE_INSTALLER, NEOFORGE_VERSION, PACKWIZ_INSTALLER, PACKWIZ_INSTALLER_BOOTSTRAP } from "./pins.ts";
import { MODPACK_DIR, MODS_SRC_DIR, OWN_JAR_PREFIX, RUNTIME_DIR, TOOLS_BIN, fail, fetchPinned, findExecutable, run } from "./lib.ts";
import { setupTools } from "./setup-tools.ts";

const SERVER_DIR = join(RUNTIME_DIR, "test-server");
const SIZES = ["test", "small", "medium", "large"];
const START_TIMEOUT_MS = 15 * 60 * 1000;
const UNTIL_TIMEOUT_MS = 15 * 60 * 1000;

interface Options {
  size: string;
  fresh: boolean;
  commands: string[];
  until?: RegExp;
  jvm: string[];
  seed: string;
}

function parseArgs(): Options {
  const args = process.argv.slice(2);
  const options: Options = { size: "test", fresh: false, commands: [], jvm: [], seed: "livingkingdoms" };
  for (let i = 0; i < args.length; i++) {
    if (args[i] === "--size") options.size = args[++i];
    else if (args[i] === "--fresh") options.fresh = true;
    else if (args[i] === "--cmd") options.commands.push(args[++i]);
    else if (args[i] === "--until") options.until = new RegExp(args[++i]);
    else if (args[i] === "--jvm") options.jvm.push(args[++i]);
    else if (args[i] === "--seed") options.seed = args[++i];
    else fail(`Unknown argument ${args[i]}. See the usage comment in tools/pack/src/test-server.ts.`);
  }
  if (!SIZES.includes(options.size)) fail(`--size must be one of ${SIZES.join(", ")}`);
  return options;
}

async function installServer(java: string): Promise<void> {
  const argsFile = join(SERVER_DIR, "libraries", "net", "neoforged", "neoforge", NEOFORGE_VERSION, "win_args.txt");
  if (existsSync(argsFile)) return;
  mkdirSync(SERVER_DIR, { recursive: true });
  const installer = await fetchPinned(NEOFORGE_INSTALLER, join(RUNTIME_DIR, "downloads"));
  console.log(`Installing NeoForge ${NEOFORGE_VERSION} server into ${SERVER_DIR}`);
  run(java, ["-jar", installer, "--installServer", SERVER_DIR], { cwd: SERVER_DIR });
}

function syncServerMods(java: string): void {
  for (const pin of [PACKWIZ_INSTALLER, PACKWIZ_INSTALLER_BOOTSTRAP]) copyFileSync(join(TOOLS_BIN, pin.file), join(SERVER_DIR, pin.file));
  run(java, [
    "-jar", PACKWIZ_INSTALLER_BOOTSTRAP.file, "--bootstrap-no-update", "--bootstrap-main-jar", PACKWIZ_INSTALLER.file,
    "-g", "-s", "server", join(MODPACK_DIR, "pack.toml"),
  ], { cwd: SERVER_DIR });
  const modsDir = join(SERVER_DIR, "mods");
  mkdirSync(modsDir, { recursive: true });
  for (const old of readdirSync(modsDir).filter((f) => f.startsWith(OWN_JAR_PREFIX))) rmSync(join(modsDir, old));
  for (const project of readdirSync(MODS_SRC_DIR, { withFileTypes: true }).filter((e) => e.isDirectory())) {
    const libs = join(MODS_SRC_DIR, project.name, "build", "libs");
    if (!existsSync(libs)) continue;
    for (const jar of readdirSync(libs).filter((f) => f.startsWith(OWN_JAR_PREFIX) && f.endsWith(".jar") && !f.endsWith("-sources.jar"))) {
      copyFileSync(join(libs, jar), join(modsDir, jar));
    }
  }
}

function writeServerConfig(size: string, jvm: string[], seed: string): void {
  writeFileSync(join(SERVER_DIR, "eula.txt"), "# Accepted by the user for this local test server (2026-11-06).\neula=true\n");
  writeFileSync(join(SERVER_DIR, "user_jvm_args.txt"), ["-Xmx4G", ...jvm].join("\n") + "\n");
  writeFileSync(
    join(SERVER_DIR, "server.properties"),
    [
      `level-name=world-${size}`,
      `level-type=livingkingdoms\\:${size}`,
      `level-seed=${seed}`,
      "server-ip=127.0.0.1",
      "server-port=25599",
      "max-players=1",
      "view-distance=6",
      "simulation-distance=6",
      "spawn-protection=0",
      "motd=Living Kingdoms worldgen test server",
      "",
    ].join("\n"),
  );
}

async function runServer(java: string, commands: string[], until?: RegExp): Promise<void> {
  const logPath = join(SERVER_DIR, "test-run.log");
  const logFile = createWriteStream(logPath);
  const argsFile = `@libraries/net/neoforged/neoforge/${NEOFORGE_VERSION}/${process.platform === "win32" ? "win" : "unix"}_args.txt`;
  const child = spawn(java, ["@user_jvm_args.txt", argsFile, "nogui"], { cwd: SERVER_DIR, stdio: ["pipe", "pipe", "pipe"] });

  let output = "";
  let ready = false;
  let afterReady = "";
  const onData = (chunk: Buffer): void => {
    const text = chunk.toString();
    logFile.write(text);
    output += text;
    if (ready) afterReady += text;
    if (!ready && /Done \([0-9.]+s\)! For help, type "help"/.test(output)) ready = true;
  };
  child.stdout.on("data", onData);
  child.stderr.on("data", onData);
  const exited = new Promise<number | null>((resolve) => child.on("exit", resolve));

  const started = Date.now();
  while (!ready) {
    if (child.exitCode !== null) fail(`Server exited before it was ready. Log: ${logPath}`);
    if (Date.now() - started > START_TIMEOUT_MS) { child.kill(); fail(`Server not ready after ${START_TIMEOUT_MS / 60000} min. Log: ${logPath}`); }
    await new Promise((r) => setTimeout(r, 1000));
  }
  console.log(`Server ready after ${Math.round((Date.now() - started) / 1000)}s`);
  for (const command of commands) {
    child.stdin.write(command + "\n");
    await new Promise((r) => setTimeout(r, 2500));
  }
  if (until) {
    const waitStart = Date.now();
    while (!until.test(afterReady) && child.exitCode === null && Date.now() - waitStart < UNTIL_TIMEOUT_MS) {
      await new Promise((r) => setTimeout(r, 1000));
    }
    if (!until.test(afterReady)) console.log(`WARNING: --until pattern not seen within ${UNTIL_TIMEOUT_MS / 60000} min`);
  }
  child.stdin.write("stop\n");
  await exited;
  logFile.end();

  const interesting = (output.match(/^.*(livingkingdoms|ERROR|FATAL|Exception).*$/gm) ?? []).filter((l) => !/Stopping|Saving/.test(l));
  console.log("--- mod log lines (livingkingdoms / errors) ---");
  for (const line of interesting.slice(0, 60)) console.log(line);
  console.log("--- command output ---");
  for (const line of afterReady.split(/\r?\n/).filter((l) => l.includes("/INFO]") && !/Stopping|Saving|ThreadedAnvil|Unloading/.test(l))) console.log(line);
  console.log(`Full log: ${logPath}`);
}

const { size, fresh, commands, until, jvm, seed } = parseArgs();
const java = findExecutable("java") ?? fail("Java 21 is required on PATH.");
await setupTools();
await installServer(java);
syncServerMods(java);
const windows = process.platform === "win32";
const wrapper = join(MODS_SRC_DIR, windows ? "gradlew.bat" : "gradlew");
run(windows ? "cmd.exe" : wrapper, windows ? ["/c", wrapper, "build", "--quiet"] : ["build", "--quiet"], { cwd: MODS_SRC_DIR });
syncServerMods(java);
if (fresh) rmSync(join(SERVER_DIR, `world-${size}`), { recursive: true, force: true });
writeServerConfig(size, jvm, seed);
await runServer(java, commands, until);
