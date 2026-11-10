// Parallel tracks (ADR-0006): M2, M3 and M4 are developed in separate worktree folders, each with its own runtime/.
// The only things two folders still share are local ports, so the ports follow the branch name.
import { capture, REPO_ROOT } from "./lib.ts";

/** Port offset per track branch; `main` and any other branch use the base ports. */
const TRACK_PORT_OFFSETS: Record<string, number> = {
  "m2-household": 10,
  "m3-magic": 20,
  "m4-settlements": 30,
};

const TEST_SERVER_BASE_PORT = 25599;
const E2E_BASE_PORT = 25598;

function currentBranch(): string {
  try {
    return capture("git", ["rev-parse", "--abbrev-ref", "HEAD"], { cwd: REPO_ROOT }).trim();
  } catch {
    return "";
  }
}

const offset = TRACK_PORT_OFFSETS[currentBranch()] ?? 0;

/** Port of the local test server (`npm run test-server`, `npm run validate-world`). */
export const TEST_SERVER_PORT = TEST_SERVER_BASE_PORT + offset;
/** Port of the testkit e2e driver, unless LK_E2E_PORT overrides it. */
export const E2E_PORT = Number(process.env.LK_E2E_PORT ?? E2E_BASE_PORT + offset);
