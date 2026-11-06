// Copies canonical skills from .agents/skills/ (read by Codex) to .claude/skills/ (read by Claude Code).
// Symlinks are avoided because they are unreliable on Windows clones.
import { cpSync, existsSync, rmSync } from "node:fs";
import { abs } from "./files.ts";

const source = abs(".agents/skills");
const target = abs(".claude/skills");

if (!existsSync(source)) {
  console.error("No .agents/skills directory; nothing to sync.");
  process.exit(1);
}
rmSync(target, { recursive: true, force: true });
cpSync(source, target, { recursive: true });
console.log("Synced .agents/skills -> .claude/skills");
