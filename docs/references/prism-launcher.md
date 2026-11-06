---
status: active
last_reviewed: 2026-11-06
---

# Prism Launcher

- **Version:** 11.1.1, Windows MSVC portable zip (pinned with SHA-256 in `tools/pack/src/pins.ts`).
- **Sources:** [releases](https://github.com/PrismLauncher/PrismLauncher/releases), [command-line options](https://prismlauncher.org/wiki/getting-started/command-line-interface/).
- **Checked:** 2026-11-06.

## How we use it

- `npm run instance` unpacks the portable build into `runtime/prism/` and creates `portable.txt`, so accounts, settings and instances stay there.
- The instance `runtime/prism/instances/living-kingdoms/` is written by the script:
  - `mmc-pack.json` holds the components `net.minecraft` 1.21.1 and `net.neoforged` at the pinned version; Prism resolves LWJGL and libraries on first launch.
  - `instance.cfg` sets memory (6 GiB max) and clears any pre-launch command. Keys the script does not manage are kept.
- `npm run launch` syncs mods with packwiz-installer, then runs `prismlauncher --dir runtime/prism --launch living-kingdoms`. Launching from the Prism window skips the sync, so run `npm run instance` after pulling pack changes.
- The game folder is `minecraft/` inside the instance; logs are in `minecraft/logs/latest.log`.

## Pitfalls

- The first start shows Prism's setup wizard and needs a Microsoft account (Settings → Accounts). The user signs in; agents never enter credentials. Accounts are stored in `runtime/prism/accounts.json`, which Git ignores along with all of `runtime/`.
- Two clients with the same account cannot join each other's LAN world; the LAN test needs a second account (TD-09).
- A `PreLaunchCommand` with quoted arguments (`"$INST_JAVA" -jar "..."`) fails in Prism 11.1.1 with "Process failed to start". We do not use pre-launch commands.
- Close Prism before `npm run instance`: Prism writes its in-memory instance settings back on exit, over the script's changes (seen 2026-11-06). The script refuses to run while `prismlauncher.exe` is running.
- `--launch` sent to an already running Prism may only open the instance window; close Prism first, or press Launch.
- Extract the zip with Windows `System32\tar.exe`; the GNU tar from Git Bash cannot read zips or drive-letter paths.
