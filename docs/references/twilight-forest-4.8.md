---
status: active
last_reviewed: 2026-11-07
---

# The Twilight Forest 4.8 on a bounded map

- **Version:** The Twilight Forest 4.8.3345 for NeoForge 1.21.1 (CurseForge; LGPL code). Pinned in `modpack/mods/the-twilight-forest.pw.toml`.
- **Sources:** [CurseForge](https://www.curseforge.com/minecraft/mc-mods/the-twilight-forest), [GitHub](https://github.com/TeamTwilight/twilightforest).
- **Checked:** 2026-11-06 on the test server; 2026-11-06/07 in the client by the user.

## What we use

- **Scale.**
  - The dimension type `twilightforest:twilight_forest_type` natively has `coordinate_scale` 0.125. The portal scale is `from / to`, so Overworld → forest multiplies coordinates by 8: the native forest is 8× larger than the Overworld.
  - We override the type with `coordinate_scale` 1.0 in `mods-src/livingkingdoms-world/src/main/resources/data/twilightforest/dimension_type/twilight_forest_type.json` (ADR-0004: TF 1:1).
- **Load order.** Our override wins because `livingkingdoms_world` declares `twilightforest` as an optional dependency with ordering `AFTER` in its `neoforge.mods.toml`.
- **Border.**
  - Vanilla copies the Overworld border to the forest unchanged, so the forest border equals the Overworld side.
  - The TF portal scales the destination and clamps it to the target border, like the Nether portal.
- **Verified:**
  - test server: TF `coordinate_scale` 1.0 and border = side on all four sizes;
  - client: the portal lands at the same X/Z.

## Pitfalls

- **Override path.** The override must keep TF's exact ID and path. If TF renames its dimension type, the native 1:8 scale returns silently. `/lkworld info` in the forest shows the active scale.
- **Boss chain on the smaller sizes.** Answered in M1-C1: the whole chain is present at 1:1 on every validated Small, Medium and Large world; Test is not validated.
