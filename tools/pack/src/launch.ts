// Syncs mods with modpack/, rebuilds our mods and starts the Living Kingdoms instance in the portable Prism Launcher.
// Usage: npm run launch   (run `npm run instance` first)
// Launching from the Prism window directly skips the sync; run `npm run instance` after pulling changes.
import { existsSync } from "node:fs";
import { spawn } from "node:child_process";
import { INSTANCE_NAME } from "./pins.ts";
import { fail } from "./lib.ts";
import { INSTANCE_DIR, PRISM_DIR, PRISM_EXE, buildOwnMods, syncMods } from "./instance.ts";

if (!existsSync(PRISM_EXE) || !existsSync(INSTANCE_DIR)) fail("No instance yet. Run `npm run instance` first.");
syncMods();
buildOwnMods();
const child = spawn(PRISM_EXE, ["--dir", PRISM_DIR, "--launch", INSTANCE_NAME], { detached: true, stdio: "ignore" });
child.unref();
console.log(`Launching ${INSTANCE_NAME} in Prism Launcher. Game log: ${INSTANCE_DIR}\\minecraft\\logs\\latest.log`);
