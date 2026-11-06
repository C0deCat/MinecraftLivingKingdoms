// Starts the Living Kingdoms instance in the portable Prism Launcher.
// Usage: npm run launch   (run `npm run instance` first)
import { existsSync } from "node:fs";
import { spawn } from "node:child_process";
import { INSTANCE_NAME } from "./pins.ts";
import { fail } from "./lib.ts";
import { INSTANCE_DIR, PRISM_DIR, PRISM_EXE } from "./instance.ts";

if (!existsSync(PRISM_EXE) || !existsSync(INSTANCE_DIR)) fail("No instance yet. Run `npm run instance` first.");
const child = spawn(PRISM_EXE, ["--dir", PRISM_DIR, "--launch", INSTANCE_NAME], { detached: true, stdio: "ignore" });
child.unref();
console.log(`Launching ${INSTANCE_NAME} in Prism Launcher. Game log: ${INSTANCE_DIR}\\minecraft\\logs\\latest.log`);
