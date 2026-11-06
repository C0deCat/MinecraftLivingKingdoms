---
status: active
last_reviewed: 2026-11-06
---

# M11 — Stabilization, final world and release

## Goal

A pack ready for long playthroughs, not a set of experimental mods.

## Phases

**A.** Pin JAR versions, configs, datapacks, culture content, generation algorithms and seed. License check of every distributed file ([licenses.md](../licenses.md)).

**B.** Singleplayer regression, a re-run of the [M10](m10-multiplayer.md) LAN and dedicated-server tests, all adventure progression gates, several cultures, all magic systems, load testing, restore from backups, data format migrations.

**C.** Final pre-generation with Chunky after worldgen mods are locked. WorldValidator report, mandatory structure registry, route checks, client and server package export.

## Acceptance

- Reproducible install and a manifest checksum.
- 100% of mandatory structures.
- No critical duplication of population and goods.
- Stable on the agreed configuration and load.

## Open questions

- Publishing platform and format (Modrinth vs CurseForge) — see [licenses.md](../licenses.md).
