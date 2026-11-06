---
status: stable
last_reviewed: 2026-11-06
---

# ADR-0003: Singleplayer first, multiplayer as its own milestone

**Status:** Accepted · **Date:** 2026-11-06

## Context

D-15 made singleplayer and Open to LAN (1–4 players) the primary play and test mode from M0 on. In practice every LAN check needs a second licensed account or a second PC, and M0 acceptance stalled on exactly that. Repeating LAN runs in every milestone slows each one down, while the systems most likely to desync (shared mana, settlements, the unloaded-world simulation) do not exist yet.

## Decision

- Singleplayer is the only play and test mode for M0–M9.
- A new milestone **M10 — Multiplayer** brings Open to LAN for 1–4 players and the dedicated server: mod side audit, server package, sync and desync tests of all our mechanics, performance with several players.
- Release moves from M10 to **M11**, so the first public release already supports multiplayer.
- Code stays server-safe throughout: logical client/server separation, no client-only classes in common code, state changes on the server thread. Singleplayer runs an integrated server, so this split is needed anyway, and it keeps M10 a verification milestone instead of a rewrite.

| # | Milestone |
| --- | --- |
| M0–M9 | Unchanged content ([ADR-0002](ADR-0002-milestone-order.md)), verified in singleplayer |
| M10 | Multiplayer: LAN and dedicated server |
| M11 | Release |

## Options considered

- Keep LAN tests in every milestone (old D-15) — rejected: blocks milestones on a second account and repeats costly manual runs.
- Multiplayer after the first release — rejected: the first public release should already be playable together.
- Multiplayer right after M3 — rejected: the sync-sensitive systems (M4–M6) would still be missing, so the tests would have to be repeated.

## Consequences

- Desync bugs are found late. Mitigation: the server-safe rule, server-authoritative state in our designs, and the M10 test list that collects every multiplayer check moved out of earlier milestones.
- Milestone acceptance criteria and the compatibility status ladder no longer require LAN; `mp-ok` marks mods verified in M10.
- "Disposable test worlds" now lasts until M11.
