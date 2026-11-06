---
status: active
last_reviewed: 2026-11-06
---

# packwiz and packwiz-installer

- **Versions:** packwiz built from commit `170b22d` (2026-10-14); packwiz-installer v0.5.14; packwiz-installer-bootstrap v0.0.3. Pins live in `tools/pack/src/pins.ts`.
- **Sources:** [packwiz](https://github.com/packwiz/packwiz), [docs](https://packwiz.infra.link/), [packwiz-installer](https://github.com/packwiz/packwiz-installer), [bootstrap](https://github.com/packwiz/packwiz-installer-bootstrap).
- **Checked:** 2026-11-06.

## How we use it

- `modpack/` is the only pack. `pack.toml` pins Minecraft and NeoForge, `index.toml` lists every file with a hash, `mods/*.pw.toml` holds one mod each (download URL, sha512, Modrinth project and version IDs).
- packwiz has no binary releases. `npm run tools:setup` builds it with `go install github.com/packwiz/packwiz@<commit>` into `tools/bin/` (ignored by Git).
- Run packwiz from `modpack/`: `../tools/bin/packwiz modrinth add <slug>`, `../tools/bin/packwiz remove <name>`, `../tools/bin/packwiz refresh`.
- `npm run instance` installs the mods with packwiz-installer. It runs headless (`-g -s client`) with the instance's `minecraft/` folder as the working directory and the local path of `modpack/pack.toml` as the pack URL. `npm run launch` reruns it before each launch (Prism's pre-launch command is not used, see [prism-launcher.md](prism-launcher.md)).

## Pitfalls

- `packwiz modrinth add` sets `side = "both"` and warns when Modrinth marks one side optional. Keep `both` unless a mod truly must not load on one side; the matrix Side column has to match.
- The bootstrap updates packwiz-installer from GitHub by default. We always pass `--bootstrap-no-update --bootstrap-main-jar packwiz-installer.jar` so the pinned jar is used.
- Index hashes cover exact bytes. `.gitattributes` forces LF, so checkouts on Windows keep the hashes valid; do not edit pack files with tools that convert line endings.
- packwiz-installer removes files it installed earlier but leaves foreign JARs alone; `npm run verify-mods` reports them.
- After any manual edit in `modpack/`, run `packwiz refresh`; `verify-mods` fails on a stale index.
