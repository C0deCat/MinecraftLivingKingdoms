# Architecture

How the system is layered and where things go. Rationale for individual choices is in [docs/decisions/](docs/decisions/README.md); detailed models are in [docs/design-docs/](docs/design-docs/index.md).

## Layers

```text
1. Pack manifest      packwiz: which mods, versions, sides          → modpack/
2. Compatibility      configs, tags, datapacks, KubeJS scripts       → modpack/config, datapacks, kubejs
3. Our mods           livingkingdoms-core ← world, settlements ← economy…  → mods-src/
4. Content            cultures, building templates, NPC visuals      → content/
5. Runtime            generated game instances, worlds, snapshots    → runtime/ (never committed)
```

Dependency direction:
- Lower layers never depend on higher ones. A config never assumes our mods exist unless the pack profile includes them.
- `livingkingdoms-core` depends only on NeoForge. It knows nothing about MCA, Farmer's Delight or magic mods.
- Feature mods (`settlements`, later `economy`) depend on Core and reach external mods only through adapter classes, so an external API change is fixed in one place.
- Mixins into external mods live only in our mods, are minimal and are covered by tests.

## Responsibility boundaries

| Part | Owns |
| --- | --- |
| External mods | Items, mobs, spells, blocks, actual work in loaded chunks |
| Compatibility layer | Tags, recipes, rates, configs, disabling conflicts |
| Core | Stable IDs, event history, regions, time, unloaded-world state, saves |
| Settlements | Settlements, professions, construction, storage; residents are MCA entities with our data |
| MCA / Capitals adapters | Calls into MCA Reborn and MCA Capitals, data-ownership control |
| Materializer | Applying planned changes through the Minecraft API on the server thread |
| World validator | Independent check that the reachable world matches the design |

Data ownership per entity: [settlement-model](docs/design-docs/settlement-model.md), [simulation-model](docs/design-docs/simulation-model.md).

## Planned repository layout

Directories are created when they get real content.

```text
modpack/            pack.toml, index.toml, mods/*.pw.toml, config/, kubejs/, resourcepacks/, datapacks/
mods-src/           one Gradle build: livingkingdoms-core/, livingkingdoms-world/ (exist), settlements/, economy-adapter/, world-materializer/
content/            cultures/, buildings/, npc-visuals/, manifest/
tools/              doc-lint/, pack/ (exist: setup, instance, launch, verify-mods), bin/ (generated, ignored), world-validator/, sim-inspector/
tests/              smoke/, worldgen/, gameplay/, magic/, settlements/, longevity/
docs/               knowledge base — see docs/index.md
runtime/            generated, ignored by Git
```

When a code directory under `mods-src/` gets its own conventions, it gets a nested `AGENTS.md` plus a `CLAUDE.md` containing `@AGENTS.md`, so both Codex and Claude Code pick them up when working there.
