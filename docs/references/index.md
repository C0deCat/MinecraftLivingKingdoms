---
status: active
last_reviewed: 2026-11-07
---

# References

Condensed, version-pinned notes on external tools and APIs, written for agents: what we use, how, and the traps for our exact versions. They save re-reading upstream docs and prevent using APIs from other Minecraft versions.

## Format of a reference note

- File name: `<tool>-<version>.md`, e.g. `neoforge-21.1.md`, `packwiz.md`, `mca-reborn-api.md`.
- Frontmatter like every doc, plus at the top: the exact version covered, upstream source URLs, and the date checked.
- Content: only what this project uses — key APIs or commands, examples verified against the installed version, known pitfalls.
- Never paste code from closed (ARR) mods; summarize behavior instead.

## Notes

| Note | Covers | Version |
| --- | --- | --- |
| [packwiz.md](packwiz.md) | packwiz, packwiz-installer, bootstrap | commit `170b22d`, v0.5.14, v0.0.3 |
| [prism-launcher.md](prism-launcher.md) | Portable Prism instance | 11.1.1 |
| [lithostitched-1.8.md](lithostitched-1.8.md) | Worldgen modifiers; our world-edge modifiers | 1.8.1 |
| [tectonic-3.0.md](tectonic-3.0.md) | Tectonic terrain functions, coast facts, pitfalls | 3.0.28 |
| [twilight-forest-4.8.md](twilight-forest-4.8.md) | Twilight Forest scale, border and override | 4.8.3345 |
| [mca-reborn-7.7.md](mca-reborn-7.7.md) | MCA Reborn residents, brain and tasks, villages, professions, ranks, dialogue | 7.7.36 |
