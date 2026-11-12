# AGENTS.md — livingkingdoms-settlements

NeoForge 1.21.1 mod `livingkingdoms_settlements`: settlements whose residents are MCA Reborn villagers. Decisions: [ADR-0001](../../docs/decisions/ADR-0001-own-settlement-mod.md), [ADR-0030](../../docs/decisions/ADR-0030-resident-integration.md); spec: [M4](../../docs/milestones/m4-settlements.md); model: [settlement-model](../../docs/design-docs/settlement-model.md); MCA facts: [mca-reborn-7.7](../../docs/references/mca-reborn-7.7.md). Rules from the root [AGENTS.md](../../AGENTS.md) and [mods-src/AGENTS.md](../AGENTS.md) apply.

State: slice S1 in progress. Every generated village becomes a settlement and its MCA villagers its residents (`settlement` package), open lava next to its buildings hardens (`SiteSafety`, TD-51); one order from the prototype (harvest crops, deliver to a container); the debug command `/lksettle` with the village survey (`survey` package).

## Rules

- License GPL-3.0-only (D-52): this mod calls and patches MCA Reborn. Code from other GPL-3.0 mods may be added only after asking the user; code of any other license never.
- MCA classes are named only in `livingkingdoms.settlements.mca` (the adapter) and `livingkingdoms.settlements.mca.mixin`. Everything else is written against vanilla types (`Villager`, `Brain`, `Behavior`), so an MCA update is fixed in one package.
- One Mixin into MCA: `VillagerTasksMcaMixin` on `VillagerTasksMCA.initializeTasks`. A second one needs a reason in ADR-0030 or a new decision, and a test that fails when the target changes.
- A resident is a `ResidentRecord` in `SettlementSaveData` (Overworld save data); the entity carries only the record's stable ID as a data attachment. Read and change residents through `Residents`, on the server thread. After a change that affects behaviour, call `McaAdapter.refreshBrain`.
- Tasks are vanilla `Behavior`s that keep nothing a reload would lose: what must survive is in the record, the rest is decided again from the world and the inventory.
- Blocks and items are recognised by tags (`minecraft:crops`, `c:crops`, `c:seeds`), not by IDs, so crops of other mods fit in.
- The MCA version this mod is compiled against is `mca_modrinth_version` in `gradle.properties`; it must equal the pack's pin in `modpack/mods/minecraft-comes-alive-reborn.pw.toml`.

## Tests

- GameTests live here (D-51), in `livingkingdoms.settlements.gametest.<area>`, class `<Feature>Tests`, one behaviour per method. Run: `./gradlew :livingkingdoms-settlements:runGameTestServer` from `mods-src/`; MCA Reborn is loaded from the dev classpath, and the task fails when a required test fails.
- GameTest coordinates start at the structure block, one below the template: the floor of the template `platform` (9×5×9, grass floor, `data/livingkingdoms_settlements/structure/platform.nbt`) is y=1.
- Spawn test villagers with `McaAdapter.spawnAdult`, not `helper.spawn`: MCA sets up name, gender and family only in its own spawn path.
- Save and reload is checked by the e2e spec `tests/e2e/specs/settlements/resident-task-reload.spec.ts`.
