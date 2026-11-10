---
status: stable
last_reviewed: 2026-11-10
---

# ADR-0030: How our settlement mod works with MCA Reborn residents

**Status:** Accepted · **Date:** 2026-11-10

## Context

[ADR-0001](ADR-0001-own-settlement-mod.md) made residents MCA Reborn entities and gave our mod `livingkingdoms-settlements` the settlement, professions, homes, work, storage and development. It left open how our mod reaches a resident. D-50 widened M4 to eight slices, so the answer must also carry professions, building levels, player standing and quests.

The M4-A2 research read MCA Reborn 7.7.36 at the tag of the installed file ([mca-reborn-7.7](../references/mca-reborn-7.7.md)). What decides this ADR:

- A resident is a vanilla `Villager` subclass with the vanilla Brain. All behaviour is set in one public static method, `VillagerTasksMCA.initializeTasks`, which runs again on every brain refresh. The task packages it combines are public static methods too.
- MCA has no events and no addon API for tasks, villages or interaction.
- MCA's own work tasks (chores) stop when the assigning player is offline.
- MCA has its own villages built from scanned rooms, and village managers that turn residents into guards, marry them, start pregnancies and collect taxes on their own timers.
- Marriage, pregnancy and profession changes are also public methods on the entity (`getRelationships()`, `Pregnancy.tryStartGestation()`, `setProfession`).
- Player hearts, village reputation, ranks and rank tasks exist in MCA; ranks and rank tasks, buttons and dialogue trees are data, interaction commands are a fixed `switch`.
- [M5](../milestones/m5-simulation.md) computes villages without loaded entities: a birth in an unloaded village creates a resident record first and the entity later.

Nothing here was run in the game when the ADR was written. The prototype M4-A5 tests points 1 to 3; its result is in the [verification](#verification-2026-11-10) section.

## Decision

1. **A resident is a record of ours; the entity is its body.**
   - The record lives in SavedData of our mod, keyed by a stable resident ID that Core gives out. It holds everything our mod owns: settlement, profession, home, workplace, current task, and later standing and quest state per player.
   - The MCA entity carries one NeoForge data attachment: the resident ID. The record stores the entity UUID, which is the key into MCA's own data (family tree, tracker).
   - A record can exist without an entity (unloaded chunk, M5). An MCA villager without the attachment is not a resident of any settlement and our mod leaves it alone.

2. **Tasks go through the vanilla Brain, with one Mixin as the hook.**
   - One Mixin at `VillagerTasksMCA.initializeTasks` hands the villager and its brain to our adapter. For a non-resident the adapter does nothing.
   - For a resident the adapter builds the brain itself from MCA's public packages (core, rest, meet, idle, panic, self-defence, guard) and our own activity `labor` with our own schedule. It leaves out MCA's job-site package, so a resident neither takes nor loses a profession on its own.
   - Our tasks are vanilla `Behavior` classes written against `Villager`; only the adapter package `livingkingdoms.settlements.mca` names MCA classes ([ARCHITECTURE.md](../../ARCHITECTURE.md)).
   - A task is a small state machine in memory. What must survive a reload is in the record (the task and its target); the behaviour restarts from the record.

3. **Our settlement rules decide; MCA carries out.**

   | Area | Owner of the decision | How it is carried out |
   | --- | --- | --- |
   | Body, personality, family tree, hearts, appearance, dialogue | MCA | Unchanged |
   | Settlement, membership, storage, development plan | Ours | Our records |
   | Profession | Ours: a definition in a datapack registry (workplace, tasks, inputs and outputs by tag, tools) | Mapped to a vanilla or MCA `VillagerProfession` for looks, trades and guard behaviour, set with `setProfession` |
   | Home and workplace | Ours | The vanilla `HOME` memory with MCA's forced-home flag; the workplace is in our record |
   | Guards | Ours (profession assignment, S5) | MCA's guard behaviour and equipment; `guardSpawnFraction` 0 in the pack config |
   | Marriage and births | Ours (rules of S4, the same rules M5 computes off-screen) | MCA's relationship and pregnancy methods; `marriageChancePerMinute` and `villagerProcreationChancePerMinute` 0 |
   | Player standing | Ours: a standing per player and settlement, fed by MCA hearts and by trade, orders and quests (S7) | MCA's rank system is not used for our rules; whether its rank tasks are emptied by a datapack is decided in S7 |
   | Quests | Ours (S8) | Offered through MCA dialogue data where it fits, otherwise our own screen |
   | Taxes, inn guests, bounty hunters | Decided in M4-B per config switch | — |

   MCA villages may still appear where a bell is found. They are not our settlements and nothing of ours reads them; with the managers above switched off they only serve MCA's own blueprint screen.

4. **Buildings are vanilla structure templates plus our definition.** One structure NBT file per building level, and a datapack definition per building: levels, cost per level, jobs and homes per level, door positions for the walkable-layout rule. No Structurize, and nothing copied from Terralith; its template pools may be referenced by id for starting villages.

5. **Settlement GameTests live in the settlement mod** (D-51): its own `gametest` package run by `./gradlew :livingkingdoms-settlements:runGameTestServer` with MCA Reborn on the development classpath. [ADR-0005](ADR-0005-test-framework.md) puts GameTests into the testkit, which changes only on `main` and whose `npm run gametest` is not built yet. Naming follows convention N4. When the testkit runner exists on `main`, it picks these tests up or they move.

6. **`livingkingdoms-settlements` is released under GPL-3.0-only** (D-52, the user's decision, 2026-11-10). It calls MCA Reborn classes and patches one of its methods, and MCA Reborn is GPL-3.0-only. Its sources are published with the first public release of the mod. `livingkingdoms-core`, `livingkingdoms-world` and the testkit do not touch MCA; their license is chosen before the first public release ([licenses](../licenses.md)).

## Options considered

- **Our data in entity attachments only** — rejected: M5 needs resident data while the entity is not loaded, and a birth off-screen has no entity yet.
- **Driving residents from a tick handler, outside the Brain** — rejected: it fights MCA's own behaviours every tick (walk targets, sleep, panic) and breaks whenever MCA changes one of them.
- **Reusing MCA chores** — rejected: they are bound to an online player and to five fixed kinds.
- **Adding our activity to MCA's finished brain instead of building it** — kept as the fallback. It is less code, but MCA's job-site package would keep reassigning professions from nearby workstations.
- **MCA villages as our settlements** — rejected: an MCA village is a set of scanned rooms with no storage use, construction or plan, its managers run on their own timers, and M5 cannot compute them off-screen.
- **Letting MCA's managers marry residents and start pregnancies** — rejected: two owners for population, and no way to run the same rule for an unloaded village.
- **Forking MCA Reborn** — the last resort ([core-beliefs](../core-beliefs.md#2-existing-mods-before-our-own-code)); one Mixin and public methods are enough as far as the sources show.
- **Structurize blueprints** — rejected for now: three more GPL mods in the pack for a format vanilla already covers.
- **GPL-3.0 for all our mods** — not chosen: it would also open the sources of the world generation at the first release without a need. **Deciding the license later** — not chosen: until then no GPL code could be taken, only called.

## Consequences

- Our mod depends on MCA classes in one package and on one Mixin target. An MCA update can break exactly these; the prototype's GameTest and e2e spec catch it.
- Pack config `config/mca.json` becomes part of the design: the switches in point 3 must stay set, and M4-B decides the remaining ones (`launchIntoDestiny`, inn guests, taxes, bounty hunters, online features off).
- The data model of the [settlement model](../design-docs/settlement-model.md) is the record of point 1; its areas table says what each slice adds.
- The MCA rank screen and our standing can show different things to a player until S7 settles what MCA's ranks mean in our villages.
- If the prototype shows that building the brain ourselves misses behaviour MCA adds elsewhere, we fall back to adding our activity to MCA's brain and handle the profession reassignment separately.

## Verification (2026-11-10)

The M4-A5 prototype ran points 1, 2 and 5 in the game; the decisions stand unchanged.

- Point 1: a resident record in our save data and the ID attachment on the MCA entity both survive save → quit → reopen (e2e spec `tests/e2e/specs/settlements/resident-task-reload.spec.ts`).
- Point 2: the Mixin applies in development and in the production instance; a resident with a brain built by our adapter harvests ripe wheat, replants it and delivers the harvest to a chest during our labor activity, and goes on after a reload; an MCA villager that is not a resident keeps MCA's brain and leaves the field alone (GameTests `HarvestOrderTests`).
- Point 5: `./gradlew :livingkingdoms-settlements:runGameTestServer` loads MCA Reborn from the Modrinth Maven and runs the tests in about a second.
- Not run yet: point 3 beyond the brain (profession mapping, homes, guards, marriage and births, the config switches), point 4, and a resident's whole day (rest, meeting, panic) inside a real village. Slices S1, S2, S4 and S5 cover them.
