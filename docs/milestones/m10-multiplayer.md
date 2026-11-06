---
status: active
last_reviewed: 2026-11-06
---

# M10 — Multiplayer: LAN and dedicated server

## Goal

1–4 players play the accepted M0–M9 pack together, via Open to LAN and on a dedicated server, without desync of our mechanics or the mods we integrated. Until this milestone the project is verified in singleplayer only ([ADR-0003](../decisions/ADR-0003-multiplayer-milestone.md)).

## Playable result

Living Kingdoms with friends: one player hosts a LAN world, or a group joins a dedicated server.

## Mods and tools

- packwiz-installer with `-s server` for the server package; NeoForge server installer at the pinned version.
- spark on the server; a second Minecraft account or a second PC for each extra client.
- Candidates only if a gap is found: server-side admin/backup tools (checked against the [compatibility matrix](../compatibility-matrix.md#rejected-mods) first).

## Phases

**A — install.** Audit the side of every mod (`client`, `server`, `both`) against its page and its behavior; fix `side` in `modpack/mods/*.pw.toml` and the matrix. Add a server instance command next to `npm run instance` (server package, EULA left to the user, launch script). Smoke test: the dedicated server starts with the full pack.

**B — reconcile.** LAN and dedicated-server runs with 2–4 clients. Fix desync and server crashes caused by client-only code or client-side state. Mods whose client-only parts break servers are configured or replaced.

**C — integrate.** Multiplayer pass over every subsystem:
- shared Ars–Iron's mana for a remote client: one pool, no double regeneration, death and dimension change ([magic-system](../design-docs/magic-system.md));
- settlements and MCA residents seen by several players: tasks, storage, construction stay consistent;
- simulation and materialization with players in different regions at once ([simulation-model](../design-docs/simulation-model.md));
- economy and states: trades and ownership with several players;
- performance with 4 players on the host PC (integrated server) and on a dedicated server, measured with spark.

**D — accept.** See below.

## Acceptance

- A second client joins via Open to LAN; 4 players can play one session (moved from M0).
- A dedicated server runs the full pack; clients from `npm run instance` join it.
- Settlements keep working with 1–4 players (moved from M4).
- 4 players in different regions at once without duplicated residents, goods or buildings (moved from M5).
- Mana invariants hold for a remote client.
- TPS budget recorded for 4 players on the host PC and on a dedicated server.
- Every pack mod has status `mp-ok` in the matrix, or a recorded reason.

## Out of scope

- Public server hosting, anti-cheat, permissions systems beyond what testing needs.
- More than 4 players.

## Open questions

- Is the dedicated server required for M11, or LAN only with the server as best effort?
- Which second account or PC is used for testing (was TD-09).
