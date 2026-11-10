---
status: active
last_reviewed: 2026-11-10
---

# MCA Reborn 7.7 for our settlement mod

- **Version:** MCA Reborn 7.7.36 for NeoForge 1.21.1, file `mca-neoforge-7.7.36-beta.3+1.21.1.jar`, mod id `mca`, GPL-3.0-only. Pinned in `modpack/mods/minecraft-comes-alive-reborn.pw.toml`.
- **Sources:** [Modrinth](https://modrinth.com/mod/minecraft-comes-alive-reborn), [GitHub](https://github.com/Luke100000/minecraft-comes-alive) at the tag `7.7.36-beta.3+1.21.1` (commit `80bfe7d`), root package `net.conczin.mca`.
- **Checked:** 2026-11-10, by reading the sources at that tag. Nothing below was run in the game yet; the prototype of M4-A5 verifies the parts it uses.

Class names below are relative to `net.conczin.mca`. The code is GPL: read it, never copy it into this repository (our mod's license is decided in ADR-0030).

## Which sources to read

- The installed file is built from the tag, not from the head of the branch `1.21.1`. The two differ in 77 files: the tag has the room and structure scanner (`server.world.data.Structure`, `BuildingRoomScanner` and others) and a reworked `Village` and `VillageManager`. Check out the tag in `runtime/research/mca-reborn/` before reading.
- Modrinth marks 7.7.36 a release although the file name says `beta.3`. The changelog says 7.7.35 and later are not save-compatible with 7.7.32 and earlier.
- The newest Modrinth file for NeoForge 1.21.1 is from 2026-08-16; newer work goes to 26.x and 1.20.1. A tag `8.1.7+1.21.1` exists in the repository but has no Modrinth file.

## Residents

- `entity.VillagerEntityMCA extends net.minecraft.world.entity.npc.Villager`. A resident is a vanilla villager subclass: vanilla `Brain`, vanilla `VillagerProfession`, POI job sites, vanilla trade offers. Entity types: `mca:male_villager`, `mca:female_villager` (`registry.EntitiesMCA`).
- Identity: the entity UUID. MCA keys its own records by it: `server.world.data.FamilyTree`, `VillagerTrackerManager` (UUID → last position, SavedData `mca_villager_tracker` on the Overworld), the resident list of a `Village`.
- Inventory: 27 slots (`getInventory()`), saved with the entity.
- Entity data is saved through `util.network.datasync.CDataManager` parameters (synced and written to NBT) plus `addAdditionalSaveData`. MCA stores nothing of ours; our data needs its own place.
- Replacement of vanilla villagers: `mixin.MixinServerWorld` cancels `ServerLevel.addEntity` for a plain `Villager` and queues it in `server.SpawnQueue`, which spawns an MCA villager with the same profession, level, offers, age and tags, one per tick. Config: `overwriteOriginalVillagers` (default true), `moddedVillagerWhitelist`, `fractionOfVanillaVillages`, `villagerDimensionBlacklist`, `allowedSpawnReasons`. Only the exact class `Villager` is replaced, so modded villager subclasses stay unless whitelisted.

## Giving a resident a task

- All behaviour is set up in one public static method: `entity.ai.brain.VillagerTasksMCA.initializeTasks(VillagerEntityMCA, Brain<VillagerEntityMCA>)`. It picks a schedule (`entity.ai.SchedulesMCA`) and adds the task packages per activity with `brain.addActivity(...)`.
- It runs from `makeBrain` and again from `VillagerEntityMCA.refreshBrain(ServerLevel)`, which makes a new brain with `copyWithoutBehaviors()` (memories survive, behaviours do not). `refreshBrain` is called on a profession change, on growing up, on follow / stay / move and when a chore starts or stops. So behaviours added from outside are lost on the next refresh unless they are added after every `initializeTasks`.
- There is no event and no registry for extra behaviours. The smallest hook is one Mixin at the return of `initializeTasks` that lets our adapter add an activity to the brain. Everything else goes through public API: `Brain.addActivity`, `Brain.setSchedule`, memories.
- Work as MCA does it: during `Activity.WORK` an adult with a job site runs the vanilla work package of its profession (`VillagerGoalPackages.getWorkPackage`), so mods that change that package also change MCA villagers.
- Chores are MCA's own work tasks: activity `mca:chore` (`entity.ai.ActivitiesMCA.CHORE`), enum `entity.ai.Chore` (prospecting, harvesting, chopping, hunting, fishing), tasks in `entity.ai.brain.tasks.chore` (`HarvestingTask`, `ChoppingTask`, `FishingTask`, `HuntingTask`; prospecting has no task). A chore is started by a player (`VillagerBrain.assignJob(chore, player)`) and `AbstractChoreTask` stops it when that player is not online, so chores cannot drive village work. They are a working example of a `Behavior<VillagerEntityMCA>` that walks, uses a tool and fills the inventory.
- Schedules: default 10 idle, 2000 work, 9000 meet, 11000 idle, 12500 rest; night owls and guards have their own.
- Navigation is MCA's own (`entity.ai.navigation.MCAGroundPathNavigation`, `MCAWalkNodeEvaluator`), with door handling in `SmarterOpenDoorsTask`; pathfinding range `villagerPathfindingDistance` (80).

## MCA villages and buildings

- `server.world.data.VillageManager` (SavedData per dimension) holds `Village` objects; a village holds structures and rooms (`Structure`, `Building`), a resident list (UUID → name), reputation per player, a tax rate and thresholds for marriage and population.
- A village is not a worldgen structure. It appears when buildings are scanned: a resident that finds a bell (`MEETING_POINT`) reports it, and a player can scan with the blueprint screen. Auto scan of homes by residents is off by default (`enableAutoScanByDefault` false). `minimumBuildingsToBeConsideredAVillage` is 3.
- Building types are data: `data/mca/building_types/*.json`, a type is a set of block counts (a house is one bed; storage is a lectern and four chests; blacksmith is a grindstone, two blast furnaces and an anvil). 26 types, among them bakery, armory, inn, library, infirmary, prison, graveyard, town_center.
- Every 1,200 ticks a loaded village runs `villageComponents`: `VillageGuardsManager.spawnGuards` (turns `guardSpawnFraction`, 17.5%, of residents into guards and archers), `VillageInnManager` (adventurers, cultists, wandering traders at an inn), `VillageMarriageManager.marry`, `VillageProcreationManager.procreate` (a married woman starts a pregnancy while the population is below beds × threshold). `VillageTaxesManager` puts items from `taxesMap` into the village storage buffer every `taxSeason` when a storage building exists.
- Homes: `entity.ai.Residency` keeps the home village id on the entity; the bed is the vanilla `HOME` memory; the workplace is the vanilla `JOB_SITE` memory.

## Professions and trade

- Professions are vanilla `VillagerProfession` values. MCA adds six without a job site (`registry.ProfessionsMCA`): `outlaw`, `guard`, `archer`, `adventurer`, `mercenary`, `cultist`. Guards have their own schedule, packages (patrol, melee, bow, crossbow) and equipment sets by village rank.
- An adult without an important profession takes a job from a free job-site POI like a vanilla villager (`LazyFindPointOfInterestTask`, `AssignProfessionFromJobSite`) and loses it again (`LoseUnimportantJobTask`); `professionConversionsMap` remaps professions.
- Trade is the vanilla offer list of the profession, opened by the `trade` interaction. Datapack trades for vanilla professions (VillagersTradingPlus, TD-19) should therefore reach MCA residents; to be confirmed in M4-B.

## Player standing in MCA

- Hearts per resident and player (`entity.ai.Memories`, inside the entity data) feed a village reputation per player (`Village.getReputation`, `pushHearts`).
- Ranks: `resources.Rank` (outlaw, peasant, merchant, noble, mayor, monarch). The rank of a player in a village is computed, not stored: `resources.Tasks.getRank` returns the highest rank whose required tasks are all complete.
- Rank tasks are data: `data/mca/tasks/<rank>.json`, types `reputation`, `building`, `population`, `advancement` (`resources.data.tasks`). Defaults: merchant needs reputation 400, a big house and a storage; monarch needs reputation 2,500, 30 residents, a prison, a blacksmith and an advancement.
- Ranks unlock blueprint-screen options (taxes, marriage and population thresholds), tax notices and some interactions and gifts. Low reputation sends bounty hunters (`bountyHunterInterval`, `bountyHunterHearts`).

## Dialogue and interaction

- Interaction buttons are data in a resource pack: `assets/mca/api/gui/*.json` (`main`, `interact`, `work`, `professions`, `locations`), each with an identifier, a position and constraints (`entity.interaction.Constraint`: age, relation, rank and the like).
- Dialogue trees are data in a datapack: `data/mca/dialogues/*.json`, answers with conditions and actions `say` and `command`.
- The commands themselves are a fixed `switch` in `entity.interaction.VillagerCommandHandler` (`trade`, `gift`, `sethome`, `setworkplace`, `hire_short`, `profession.guard` and others). A new button or dialogue line can be added by data, but a new command needs a Mixin there or our own screen and packet.

## What MCA does not have

- No events on the NeoForge bus and no addon API for tasks, villages or interaction. The `resources.API` class only serves MCA's own data.
- No production chains, construction, shared storage use by residents, or village development: buildings are recognised, never built.

## Pitfalls

- `launchIntoDestiny` (default true) opens MCA's character and spawn screen on a player's first join. It changes where a new player starts and puts a screen in front of every e2e world; set per pack in M4-B, and switched off for the e2e instance if it blocks tests.
- Optional online features exist and are off by default: chat AI (`enableVillagerChatAI`), online TTS (`enableOnlineTTS`), a skin library (`immersiveLibraryUrl`). Keep them off in the pack config.
- MCA's village managers and our settlement rules would both assign guards, marriages, births and homes. One owner per piece of data (ADR-0001): ADR-0030 says which side owns what.
- MCA has no config switch for its village managers as a whole; the levers are `guardSpawnFraction`, `marriageChancePerMinute`, `villagerProcreationChancePerMinute`, `innSpawnsAdventurers`, `innSpawnsCultists`, `innSpawnsWanderingTraders`, `taxesFactor`, `bountyHunterInterval`.
