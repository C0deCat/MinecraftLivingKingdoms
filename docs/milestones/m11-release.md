---
status: active
last_reviewed: 2026-11-07
---

# M11 — Stabilization, final world and release

## Goal

A pack ready for long playthroughs, not a set of experimental mods.

## Phases

**A — foundation: lock and license.** Pin JAR versions, configs, datapacks, culture content and generation algorithms; players choose the seed (D-20), the fallback pre-generated world pins its own. License check of every distributed file ([licenses.md](../licenses.md)).

**B — reconcile: regression.** Singleplayer regression, a re-run of the [M10](m10-multiplayer.md) LAN and dedicated-server tests, all adventure progression gates, several cultures, all magic systems, load testing, restore from backups, data format migrations.

**C — integrate: final map and packages.** Pre-generation at world creation (D-20) measured on the locked worldgen; if it is too slow, the fallback pre-generated world is built here with Chunky. WorldValidator report, mandatory structure registry, route checks, client and server package export.

**D — accept.** See [Acceptance](#acceptance) below.

## Acceptance

- Reproducible install and a manifest checksum.
- 100% of mandatory structures.
- No critical duplication of population and goods.
- Stable on the agreed configuration and load.

## Open questions

- Publishing platform and format (Modrinth vs CurseForge) — see [licenses.md](../licenses.md).
