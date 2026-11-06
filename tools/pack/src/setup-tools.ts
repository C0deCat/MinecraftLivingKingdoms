// Prepares tools/bin/: packwiz (built from a pinned commit) and the pinned packwiz-installer jars.
// Usage: npm run tools:setup
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { PACKWIZ_COMMIT, PACKWIZ_INSTALLER, PACKWIZ_INSTALLER_BOOTSTRAP } from "./pins.ts";
import { PACKWIZ_BIN, TOOLS_BIN, capture, fail, fetchPinned, findExecutable, run } from "./lib.ts";

const STAMP = join(TOOLS_BIN, "packwiz.commit");

export async function setupTools(): Promise<void> {
  mkdirSync(TOOLS_BIN, { recursive: true });

  const built = existsSync(PACKWIZ_BIN) && existsSync(STAMP) && readFileSync(STAMP, "utf8").trim() === PACKWIZ_COMMIT;
  if (!built) {
    const go = findExecutable("go", ["C:\\Program Files\\Go\\bin\\go.exe", "/usr/local/go/bin/go"]);
    if (!go) {
      fail(
        "Go is not installed. packwiz is built from source with Go.\n" +
          "Install it (Windows: `winget install GoLang.Go`, other OS: https://go.dev/dl/) and rerun `npm run tools:setup`.",
      );
    }
    console.log(`Building packwiz @ ${PACKWIZ_COMMIT.slice(0, 7)} with ${capture(go, ["version"]).trim()}`);
    run(go, ["install", `github.com/packwiz/packwiz@${PACKWIZ_COMMIT}`], {
      env: { ...process.env, GOBIN: TOOLS_BIN, CGO_ENABLED: "0" },
    });
    writeFileSync(STAMP, PACKWIZ_COMMIT + "\n");
  }

  await fetchPinned(PACKWIZ_INSTALLER, TOOLS_BIN);
  await fetchPinned(PACKWIZ_INSTALLER_BOOTSTRAP, TOOLS_BIN);
  console.log(`tools/bin ready: packwiz @ ${PACKWIZ_COMMIT.slice(0, 7)}, ${PACKWIZ_INSTALLER.file}, ${PACKWIZ_INSTALLER_BOOTSTRAP.file}`);
}

if (import.meta.main) {
  await setupTools();
}
