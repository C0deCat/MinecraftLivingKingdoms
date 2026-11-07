---
status: active
last_reviewed: 2026-11-07
---

# Lithostitched 1.8 worldgen modifiers

- **Version:** Lithostitched 1.8.1 for NeoForge 21.1 (bundles apollib). Pinned in `modpack/mods/lithostitched.pw.toml`.
- **Sources:** [Modrinth](https://modrinth.com/mod/lithostitched), [GitHub](https://github.com/Apollounknowndev/lithostitched) (MIT).
- **Checked:** 2026-11-06/07 against the installed version: our modifiers load and change terrain on the test server.

## What we use

The `wrap_density_function` modifier replaces a registered density function with a wrapper around it:

```json
{
  "type": "lithostitched:wrap_density_function",
  "target_function": "tectonic:noise/raw_continents",
  "wrapper_function": {
    "type": "minecraft:min",
    "argument1": { "type": "lithostitched:wrapped_marker" },
    "argument2": "livingkingdoms:edge_falloff"
  }
}
```

- `lithostitched:wrapped_marker` stands for the original function. It may sit inside our own density function types, for example the `input` field of `livingkingdoms_world:edge_bias`. Lithostitched swaps it through `mapAll`, so our types must pass the visitor to every child.
- Modifiers live in `data/<namespace>/lithostitched/worldgen_modifier/`. Ours are in `mods-src/livingkingdoms-world/src/main/resources/data/livingkingdoms/lithostitched/worldgen_modifier/`.
- Every place that references the target, Tectonic's own functions included, sees the wrapped version.

## Our world-edge modifiers (ADR-0004)

| Modifier | Target ([Tectonic](tectonic-3.0.md)) | Wrapper |
| --- | --- | --- |
| `edge_raw_continents` | `noise/raw_continents` | `clamp(min(edge_bias(marker), edge_falloff), -1, 2)`; bias with slope compensation and shore styles |
| `edge_raw_islands` | `noise/raw_islands` | the same shape; the bias fades islands out between 1,024 and 384 blocks from the border, without compression |
| `edge_full_continents` | `noise/full_continents` | `min(marker, edge_falloff)`: guarantees the open ocean |
| `edge_erosion` | `noise/continent/erosion` | `edge_erosion(marker)`: lifts erosion on masked stretches of coast |

Our density function types (`livingkingdoms_world:edge_falloff`, `edge_bias`, `edge_erosion`) read the world size from the save (`WorldSizeState`). With `-Dlivingkingdoms.edge=off` they all return the input unchanged; `/lkworld coast` uses that switch for the natural-coast baseline.

## Pitfalls

- **Flat cache.** Wrap the result in `minecraft:flat_cache` when the target was flat-cached. Otherwise our function runs for every block instead of once per column.
- **Bounds.** Keep each density function's `minValue`/`maxValue` true bounds. Vanilla uses them to skip work in `min`, `max` and similar.
- **Clamping.** Clamp results to the range the target's consumers expect (see the Tectonic pitfalls).
