# LIVING KINGDOMS
## Staged development plan for a modded Minecraft pack

**Revision:** 0.3 · **Date:** 2026-11-06 · **Status:** working architecture plan
**Target platform:** Minecraft Java 1.21.1 · NeoForge 21.1.x · Java 21
**Development principle:** install → resolve conflicts → integrate into a cohesive system → lock in the result.

> **What changed in 0.3.** Millénaire is replaced by our own settlement mod built on MCA Reborn ([ADR-0001](decisions/ADR-0001-own-settlement-mod.md)). Milestones are reordered: settlements → unloaded-world simulation → economy and states → atmosphere → cultures ([ADR-0002](decisions/ADR-0002-milestone-order.md)). Added requirements for play mode (singleplayer / LAN, 1–4 players) and publishing. Documentation switched to English. The previous revision (Russian) is kept in [archive/roadmap-v0.2.md](archive/roadmap-v0.2.md).

> **Important.** Compatibility of any specific set of JARs has not been confirmed by a launch yet. Mod availability was checked against public project pages on 2026-11-03 and 2026-11-06. This document is a plan and a technical specification, not a claim that all listed mods already work together. Versions and dependencies are pinned in the manifest only after verification.

## 1. Goal, scope and key decisions

**Living Kingdoms** is a finite fantasy world for long-term exploration where beautiful terrain, dungeons, crafts, magic, villages, population and states form one interconnected game. The player is not the center of the simulation: settlements, economy, politics and nature keep developing when the player is not nearby.

**Core requirements:**

- **Every milestone is a playable release.** After each stage the pack must be beautiful, cohesive and fun to play. Cohesion between mods and the amount of custom logic grow gradually.
- **Play mode: 1–4 players, singleplayer or Open to LAN.** The world is hosted by one player (integrated server). A dedicated server is a long-term goal, so code must stay server-safe, but it is not the primary test environment.
- **Publishing.** The pack will be published (platform and format not chosen yet). A cohesive game experience matters more than not touching mod code, but every change must stay within license limits — see [licenses.md](licenses.md).
- Finite Overworld of roughly **12,288 × 12,288 blocks** (589,824 chunks inside the square outline). The playable border may have a complex shape and be surrounded by ocean or other natural barriers.
- Completing vanilla Minecraft and key adventure chains is **guaranteed**: End portal, required resources, mandatory structures, access to bosses and modded dimensions. Not every possible biome has to exist.
- Fantasy aesthetics, a wide variety of places, cultures, crafts and magic; minimal industrial technology in the visual and narrative core.
- Residents and settlements form one model: personality and family — MCA Reborn; settlements, work, construction and economy — our own mod; politics — MCA Capitals.
- Changes in distant areas are computed abstractly. Keeping all chunks loaded is not required.
- By default we use **existing mods, configs, datapacks and addons**. Custom mechanics are built only for identified functional gaps, never "just in case".

### 1.1. Architecture decisions

| Code | Decision | Status |
| --- | --- | --- |
| D-01 | Minecraft 1.21.1 + NeoForge + Java; do not port the game to Rust | Locked |
| D-02 | Stay on 1.21.1: as of 2026-11-06 Ars Nouveau, Iron's Spells, Ars 'n Spells, Farmer's Delight, the Let's Do mods and MCA Capitals have no NeoForge builds above 1.21.1. The exact NeoForge 21.1.x version is pinned in M0 and changed only via a controlled update | Locked; revisit when the core mods move to 26.x |
| D-03 | First map about 12,288 × 12,288; geography is finite, political borders are dynamic | Working hypothesis |
| D-04 | Light-gameplay and magic mods come **before** settlements and NPCs | Locked |
| D-05 | Each stage is brought to a cohesive playable subsystem before moving on | Locked |
| D-06 | Ars Nouveau — universal modern magic; Iron's — powerful ancient magic; Occultism — efficient ritual/secret magic | Locked as design |
| D-07 | Shared personal mana for Ars and Iron's with a conversion rate; Source stays a separate machine resource; Occultism has no shared mana bar | Locked |
| D-08 | Iron's scrolls can be reproduced with the mod's own tools; no constant hunt for consumable scrolls. Complex formula research — maybe later | Locked |
| D-09 | No large new pacts, summoning risks or unique spirits for Occultism for now | Locked |
| D-10 | Do not replace Minecraft's ChunkManager; build our own **global state simulation** and materialize physically only in loaded areas | Locked |
| D-11 | Cultures are separate from states: people/culture ≠ political entity | Locked |
| D-12 | Redistributing modified third-party structures/code only with respect to licenses and permissions | Hard constraint |
| D-13 | Millénaire is not used; the settlement niche is filled by our own mod built on MCA Reborn residents ([ADR-0001](decisions/ADR-0001-own-settlement-mod.md)) | Locked |
| D-14 | Order: settlements → unloaded-world simulation → economy and states → atmosphere → cultures ([ADR-0002](decisions/ADR-0002-milestone-order.md)) | Locked |
| D-15 | Primary play and test mode: singleplayer and Open to LAN with 1–4 players. Dedicated server — long-term; code must not break on it (no client-only classes in common code) | Locked |
| D-16 | Code and assets of closed (ARR) mods are never copied or decompiled into the repository; behavior is changed via configs, datapacks, KubeJS and Mixins in our own mod | Hard constraint |
| D-17 | All documentation and code comments are in English | Locked |

### 1.2. "Ready to move on" rule

Every milestone has four phases. **A. Install:** gather mods and dependencies, check individual mechanics. **B. Reconcile:** resolve worldgen, recipe, item, UI, performance and balance conflicts via configs/datapacks. **C. Integrate:** make the subsystem cohesive; add a small adapter only when necessary. **D. Accept and lock in:** automated tests, reproducible manifest, documentation, saved verified configuration.

Once a milestone is finished its subsystem is a **stable foundation**, not "JARs that happen to be installed". Returning to it is allowed, but as a controlled change with regression tests. Changing worldgen in existing chunks requires a migration strategy or a new world.

## 2. Roadmap

| Milestone | Cohesive result | Content / base | Custom code | Reference build |
| --- | --- | --- | --- | --- |
| M0 — Infrastructure | Pack is reproducible from Git | packwiz, NeoForge, spark, CI | Minimal | Foundation |
| M1 — Geography and adventure | Finished finite map, reachable progression | Bounded Not Free, Terralith, Tectonic, structures and bosses | World validator, targeted rules | Adventure World |
| M2 — Everyday life, farming, crafts | Cohesive household and decorative mechanics for the player | Farmer's Delight, Croptopia, Let's Do, decor | Datapacks/KubeJS if needed | Living Gameplay |
| M3 — Unified magic ecosystem | Three magic traditions with shared Ars–Iron's mana | Ars, Iron's, Occultism, existing bridges | Balance tweaks; no large new mechanics | Fantasy Magic |
| M4 — Settlements and residents | Living villages: MCA residents with professions, homes, storage and village growth | MCA Reborn, M2 content | Our mod `livingkingdoms-settlements` (slices S1–S4) | Living Settlements |
| M5 — Unloaded-world simulation | Villages live while no player is nearby; results are visible on return | Our Core | Regions, time, abstract computation, materialization | Living World |
| M6 — Economy and states | Trade between villages, states, NPCs use crafts and magic | MCA Capitals, M2–M3 content | Economy contracts, Capitals adapter | Living Kingdoms |
| M7 — Atmosphere | Seasons, weather and forests affect the world and economy | Serene Seasons, Dynamic Trees (?), one weather mod | Regional nature layer | Dynamic World |
| M8 — Cultures and looks | Our own peoples, architecture, NPC appearance | Our mod's templates, MCA systems; CTOV etc. as inspiration only | Culture content packs | Living Cultures |
| M9 — Deep autonomy | Migration, wars, recovery, history, scaling | Already accepted set | Simulator extensions | Autonomous Kingdoms |
| M10 — Release | Stable final map and pack | Pinned mods | Optimization, migrations, tools | Release Candidate |

**Parallel work:** licenses and asset provenance, profiling, backups, install automation, save testing, progression checklist, culture design. None of these may be postponed to the last milestone.

## 3. M0 — Infrastructure and contracts

**Goal:** any developer or AI agent can deploy an identical test Minecraft instance from a clean repository; the project is ready for controlled experiments.

**Mods and tools:** NeoForge for 1.21.1, spark (profiling), packwiz (pack manifest), Prism Launcher, Git/GitHub Actions, Gradle; later Chunky for world pre-generation. After compatibility checks — FerriteCore, ModernFix, Lithium. They do not replace performance measurement. [S01] [S17] [S18]

**A — install.** Create profiles `worldgen`, `gameplay`, `magic`, `civilizations`, `full` and a shared catalog of pinned versions. Download JARs via packwiz/Modrinth/CurseForge/official releases. Respect required dependencies and client/server sides.

**B — reconcile.** Remove duplicate JARs, wrong loader versions, conflicting client optimizers. Every update adds a changelog entry, a backup and a launch report.

**C — single system.** Introduce a pack check interface (`verify-mods`) and a mod registry with purpose, version, license, side and test status. Create an empty `livingkingdoms-core` or at least a Gradle project — without premature political simulation.

**D — done criteria:** one command script prepares a client instance; the world opens in singleplayer and a second client can join via Open to LAN; no missing dependencies; the verified configuration is committed; a clean install reproduces the same set of versions. A dedicated-server smoke test is optional at this stage.

**What we do NOT do:** no custom JAR manager while packwiz does the job; no global NPC entities that don't exist yet; no pre-emptive forks of other mods.

## 4. M1 — Finite world generation and adventure progression

**Goal:** after M1 the player already gets a beautiful, bounded and fully completable adventure world. Later stages extend its content instead of redesigning the base geography without need.

### 4.1. Mods

| Status | Mod | Role / comment |
| --- | --- | --- |
| Base | **Bounded Not Free** | Finite map shape, regional biome/structure distribution, borders; NeoForge 1.21.1 support confirmed [S02] |
| Base | **Terralith** | Biome variety; check the matching file and dependencies |
| Base | **Tectonic** | Large-scale terrain; the mod version includes Terralith compatibility [S03] |
| De-facto dependency | **Lithostitched** | Worldgen compatibility/registration, required by several modern worldgen mods [S04] |
| Base | **YUNG's Better Strongholds** + YUNG's API | Key strongholds with the End portal [S05] |
| Base | **When Dungeons Arise** | Large adventure structures, candidates for 1–2 mega-dungeons [S06] |
| Base | **Dungeons and Taverns** | Inns, small dungeons, lively points of interest [S07] |
| Candidate | **YUNG's Better Dungeons / Mineshafts / Bridges** | Extra structures; check compatibility and spawning on a finite map |
| Candidate | **Mowzie's Mobs** | Expressive enemies and bosses; NeoForge 1.21.1 declared [S08] |
| Candidate | **The Twilight Forest** | Separate adventure dimension and boss chain [S09] |
| Candidate | **CTOV, Towns and Towers, Structory** | Town architecture / visual landmarks; watch for conflicts with our future villages (M4) |
| Tool | **Chunky** (by pop4959) | World pre-generation and checks; not to be confused with the ore-"chunks" mod of the same name [S10] |

### 4.2. A — build the base generator

Set the seed, a square starting outline of 12,288 × 12,288, natural limits by coastlines and mountain ranges, climate zone distribution. For planning, about 96 × 96 cells of 128 × 128 blocks; each cell will be useful to the simulator later. Account for differences between the Overworld, Nether, End and new dimensions.

Required variety: mountains, large forests, rivers, plains, coasts, cold and warm areas, several large biome masses. No pointless rule like "every biome must exist".

### 4.3. B — resolve worldgen contradictions

Check the Tectonic/Terralith/Bounded Not Free combination, surface structures on steep terrain, overlapping towers and ruins, entrance accessibility, conflicting stronghold changes, duplicate architecture generators. Decide whether vanilla villages/CTOV stay in the final world or their areas are reserved for our villages (M4).

When a new mod generating plants, ores, temples or mob structures is added, M1 is reopened for a **targeted regression**, but the accepted geography is not redesigned. Regenerating individual test seeds is fine until the final world is locked.

**Mods that change worldgen in later stages** (Croptopia crops, Iron's towers, M4 villages, Dynamic Trees in M7) should preferably be decided and installed in M1 already. Until M10 the world is **disposable**: test worlds are recreated, player saves are not migrated.

### 4.4. C — a single World Content Manifest

Build our own **content manifest**, then a worldgen validator. Guarantee structures and a route for core progression, not a specific list of every small ruin. Example target requirements (tunable hypotheses, not mod properties):

- At least one reachable Stronghold with a valid portal.
- At least one reachable chain for blaze rods and other resources needed for the End.
- At least 2 mega-dungeons of the chosen format; not in one spot and not overlapping settlements.
- At least 3–5 large adventure areas of different nature and difficulty.
- All mandatory places and ingredients for modded bosses are reachable within the borders of their dimensions.
- Reserve future areas for 30–60 settlements, 6–10 starting states, magic schools and workshops. These numbers are the **project scale**, not a requirement to generate all towns now.

The validator searches for structures, checks location, physical accessibility and, where needed, input resources. If a structure did not generate — pick another seed, override generation conditions or safely place a prepared template. A single `locate` is not a sufficient completability check.

### 4.5. D — M1 acceptance

**Result:** a pre-selected seed produces a reproducible, varied map; core Minecraft progression and key modded adventures are possible; the validator report has no critical gaps; generating and loading new chunks shows no critical errors. From here on WorldGen is the stable foundation for later stages.

## 5. M2 — Light gameplay, farming, food, crafts, decor

**Goal:** a self-sufficient fantasy pack that is fun without NPC simulation. Food, cookware, farming, animal husbandry, cooking, storage and decor share one game language without incompatible ingredients.

### 5.1. Mods

| Priority | Mod | Niche |
| --- | --- | --- |
| Base | **Farmer's Delight** | Cooking foundation, kitchens, farming [S11] |
| Base | **Croptopia** | Variety of crops and fruit trees [S12] |
| Base | **[Let's Do] Farm & Charm** | Farm aesthetics, husbandry, rural blocks; check overlaps with Farmer's Delight |
| Base | **[Let's Do] Vinery** | Vineyards, berry processing, drinks [S13] |
| Base | **[Let's Do] Meadow** | Cheese making, alpine produce, furnishings [S14] |
| Base | **[Let's Do] HerbalBrews** | Tea, coffee, medicinal and household drinks [S15] |
| Base | **Handcrafted** | Furniture and interiors [S16] |
| Base | **Supplementaries** + Moonlight Library | Functional decor and household tools [S19] |
| Candidate | **Macaw's** (selected modules) | Building elements, doors, bridges, roofs; check versions |
| Candidate | **Waystones / Sophisticated Backpacks / Lootr** | Travel, carrying items, multiplayer loot; check specific versions/balance |
| UI | **EMI, Jade** | Recipe viewing and in-game diagnostics |

### 5.2. Existing compatibility tools

- **Pantrywork** — shared food tag vocabulary for Farmer's Delight, Croptopia and Let's Do; *test on our set first, then adopt*. [S20]
- **Farmer's (Delight) Croptopia** — cooking recipe and cookware compatibility between the two mods; use if Pantrywork does not cover specific operations. [S21]
- **Almost Unified** — unifies identical resources and recipe outputs; useful for ores and ingredients, but the config must define the canonical variant instead of deleting content arbitrarily. [S22]
- **KubeJS** — targeted edits of recipes/tags/events without a Java mod; must not become an untested second codebase. [S23]

### 5.3. A → B → C → D

**A.** Install in small batches: Farmer's Delight + Croptopia first, then the Let's Do farming mods, then decor, then convenience. Record how recipes, fields, trees and new blocks work. Add newly generated crops/structures to the WorldGen regression.

**B.** Build a duplicate matrix (cheese, salt, flour, oil, dough, berries, drinks, vegetables, tools, cooking stations). Check which items are interchangeable via shared tags and where recipes reference a specific Item ID. Do not disable a nice mechanic just to get rid of a second item.

**C.** Choose canonical ingredients and cooking routes. Fill missing overlaps via Pantrywork/existing adapters, then datapacks/KubeJS. Provide several full chains: bread/cooking, cheese making, winemaking, herbalism. Decor and furniture share a consistent style and do not overload recipe tabs.

**D.** Acceptance: the player can gather and process products of 3–4 mods without unexpected incompatibilities, no critical resource duplication loops, recipe UI is correct, new resource types actually exist in the finite world. Generation and farm costs are measured.

**Limitation:** there is **no** NPC economy yet. We stabilize player gameplay first and hand it to the civilization systems later.

## 6. M3 — Full magic ecosystem

**Goal:** three traditions with different roles work together instead of being three independent sets of magic items. M3 uses minimal fixes and existing addons, without building a large new spirit-pact system.

### 6.1. Mods

| Status | Mod | Purpose |
| --- | --- | --- |
| Base | **Ars Nouveau** | Modern universal magic: glyphs, Source, utility spells, automation |
| Base | **Iron's Spells 'n Spellbooks** | Ancient powerful spells, scrolls, magic bosses, adventure content |
| Base | **Occultism** | Spirit summoning, rituals, profitable production and dimensional resources |
| Base bridge | **Ars 'n Spells** | Shared Ars–Iron's mana, configurable rates, system interaction [S24] |
| Check | **Ars Ocultas** | Bound spirits via Ars infrastructure; check whether it bypasses lifetime limits [S25] |
| Check | **Ars Unification** | Other mods' Cut/Crush recipes available to matching Ars glyphs; supports Occultism and Farmer's Delight [S26] |
| By dependency | Patchouli and libraries of specific releases | Only by JAR metadata and packwiz |

### 6.2. Consistent magic philosophy

- **Ars Nouveau:** modern, learnable, flexible magic; can do a lot, but specialized powerful effects and large production should cost more. No mass nerfs without tests.
- **Iron's:** rare knowledge of ancient civilizations. Most spells are stronger than similar simple Ars combinations or easier to use at high level; keep the original Scroll Forge and reusable casting via the Spellbook. No mandatory endless hunt for single-use scrolls.
- **Occultism:** rare ritual practice with strong effects and efficient automation. Keep the mod's own initial limits (ritual structures, reagents, spirit progression, work durations). Individual pacts, random risks and otherworldly anomalies are **not written now**.

### 6.3. Shared Ars and Iron's mana

Decision made: one shared personal resource **already in M3**. Pick `ars_primary` or `iss_primary` from Ars 'n Spells as the first scheme. One mod is the authoritative owner of the pool, the other consumes it through the bridge. A rate `k` converts spell cost, regeneration and bonuses into the chosen scale. For example, with Ars as the base scale: `cost_unified(IronSpell) = k × cost_iron`.

Key checks: one displayed bar, deduction in one transaction, correct regeneration, no double regeneration, item and enchantment bonuses, death/dimension change, server/client. Ars 'n Spells already has `mana_unification_mode`, `conversion_rate_iron_to_ars`, `conversion_rate_ars_to_iron`; prefer one authoritative pool over a two-way hybrid. [S24]

**Do not merge Source with personal mana.** Occultism stays out of this system: its rituals have a different resource cycle.

### 6.4. A — install

Test the three base mods separately and together. Add Ars 'n Spells after the base mod versions are pinned. Check the official Iron's structures on the finite map: ancient towers, related bosses, resources, trade.

### 6.5. B — minimal balance and conflict resolution

- Restrict access to Ars rituals that directly change global weather/time (`Sunrise`, `Moonfall`, `Cloudshaping`) via datapack/recipes; check whether existing tablets can be used. Add a usage ban later if needed.
- Start with the original glyph and spirit-ritual prices. Raise costs only for combinations found to be excessive.
- In Ars 'n Spells, disable freely turning Iron's ancient spells into standard Ars glyphs **if** it breaks the separation of schools; check the existing toggles and the specific version.
- No mandatory recipe discovery for Iron's scrolls in M3: the original Scroll Forge crafts them, and writing into the Spellbook makes casting reusable. Complex formula research is a future option.
- Check that Ars Ocultas and Ars Unification do not break spirit lifetime limits, resource processing or recipe priority; disable individual mechanics of the conflicting addon instead of forking the core.
- Compare Source-based production with Occultism spirit processing; limit obvious exploits first, without removing Occultism's cost advantage completely.

### 6.6. C — single system and D — acceptance

A single system here means a **compatible game ecosystem**, not rewriting three APIs into one abstract magic. Shared mana, consistent recipes and attributes, clear magic niches, all content reachable on the finite map. Sources of powerful magic and rare rituals are tracked in the World Content Manifest.

Acceptance: 10+ typical spells from both sides use one pool, gear bonuses and cooldowns work; 3–5 Ars/Occultism productions are compared by real cost; all required bosses/structures are reachable; no free infinite resource duplication; all settings are stored in Git.

## 7. M4 — Settlements and residents: our own mod on MCA Reborn

**Goal:** living villages appear in the world. Their residents are MCA Reborn characters with professions, homes and a shared household, and villages grow from their own resources. At this stage everything works **only in loaded chunks**; abstract simulation of the unloaded world is M5.

**Key decision ([ADR-0001](decisions/ADR-0001-own-settlement-mod.md)):** Millénaire is not used — neither in the pack nor as a source of code or assets. The autonomous settlement niche is filled by our own mod `livingkingdoms-settlements`. Millénaire is only a source of ideas observed in-game and on its wiki.

### 7.1. Composition

| Status | Mod / module | Role |
| --- | --- | --- |
| Base | **MCA Reborn** (NeoForge 1.21.1) | The only representation of a resident: body, personality, family, relationships, age, appearance, dialogue [S29] |
| Ours | **livingkingdoms-settlements** | Settlement, resident membership, profession, home, workplace, tasks, storage, development plan |
| Ours | **livingkingdoms-core** (minimal) | Stable IDs for settlements and residents, event log, save format version |
| Research | **MineColonies / Structurize** and other open mods | Possible source of template-based construction and storage logistics code — only with a compatible license |
| Later (M6) | **MCA Capitals** | Politics, titles, dynasties |
| Reference | Millénaire | Ideas only; code, decompilation, building plans and texts are not used |

### 7.2. Data ownership

- **MCA Reborn:** personality, family, relationships, age, appearance.
- **Settlements:** which settlement a resident belongs to, profession, home, workplace, current task, storage, development plan.
- **Core:** stable IDs, event log, format version.

One record per resident identity. No parallel NPC "twins" in different mods.

### 7.3. Vertical slices

Each slice is a separate playable release of the pack.

| Slice | Content | What the player gets |
| --- | --- | --- |
| S1 — Village base | Starting villages from templates, MCA residents assigned to a village, homes, shared storage, farmer and lumberjack professions | A populated world where residents work |
| S2 — Builder | Development plan, construction of buildings from templates using storage resources, visible progress | Villages grow before your eyes |
| S3 — Kitchen and crafts | Cook, baker, brewer using Farmer's Delight and Let's Do recipes; trade with the player | Link to the M2 household gameplay |
| S4 — Population growth | MCA families produce new residents, housing, profession assignment; several villages on the map | Villages develop over years |

The scale is deliberately smaller than Millénaire: one culture, done cohesively. The second culture comes in M8.

### 7.4. A → B → C → D

**A — research.** Study MCA Reborn:
- how to get residents and give them tasks from outside;
- how to store our data on the entity (NeoForge attachments);
- which events exist and what will need Mixin patches.

Study open construction and worker-NPC mods, their licenses and code suitability. Prototype: an MCA resident performs one task of our mod and survives a world reload.

**B — reconcile.** Decide what happens to vanilla villages and villagers, vanilla professions, MCA residents spawning outside our villages, village structures from M1 (CTOV and similar). Check lumberjack compatibility with the world's trees, especially if Dynamic Trees is planned for M7.

**C — integrate.** Implement slices S1–S4 in order.

**D — acceptance:**
- several villages work for dozens of in-game days in the loaded world: growth, production, families;
- world reload without losing residents, storage or buildings;
- verified in singleplayer and in Open to LAN with 1–4 players;
- TPS measured in an agreed scenario (on the host PC, which runs both the client and the integrated server).

**License:** if the mod depends deeply on MCA Reborn code (GPLv3), we release it under GPLv3. Check in phase A.

## 8. M5 — Unloaded-world simulation

**Goal:** villages keep living while no player is nearby. On return the results are visible: new houses, stock, residents born and dead.

M5 comes after M4 on purpose: the simulation core is designed on the real data model of our settlements, not blindly.

**Scope note:** in singleplayer and LAN, in-game time only passes while the host has the world open. "Living without players" means regions nobody is near while the world runs; offline catch-up for real-world time is out of scope unless decided later.

**A.** Introduce `World`, `GeographicRegion`, `SimulationCell`, `WorldClock`, `WorldEvent`, place IDs and a save format version in Core. Describe which M4 processes have an abstract counterpart: production, consumption, construction, demography.

**B.** One authoritative state per village and region. Loaded ↔ unloaded transitions without double counting: at any moment a village is driven by either the physical or the abstract simulation, never both. A "last computed day" marker. Changing blocks and entities asynchronously off the server thread is forbidden.

**C.** Abstract village computation using the same rules as physical work: recipes or normalized costs. A `Materializer` turns accumulated changes into blocks and entities on load, with a per-tick budget. Births in an unloaded village create a resident record; the MCA entity appears on materialization. Use NeoForge's SavedData and attachments [S27].

**D — acceptance:**
- 1/10/100 unload/load cycles without duplicated residents, goods or buildings;
- accelerated simulation for N in-game days gives a plausible result;
- quitting and reloading the world mid-simulation is safe;
- 4 players in different regions at once (LAN).

**Scale:** a coarse grid of 128 × 128 blocks = 9,216 simulation cells. Villages, provinces and climate regions are separate, larger entities. The grid does not replace Minecraft's ChunkManager.

## 9. M6 — Economy and states

**Goal:** villages are connected by trade and politics, and NPCs use all M2–M3 game content as consistently as the player does.

**Mods:** MCA Capitals (exact file, license and API — to verify) [S30]; content of Farmer's Delight, Let's Do, Ars Nouveau, Iron's, Occultism.

**A.** Study MCA Capitals: hierarchy, dynasties, titles, succession. How they map onto our settlements and territory. Pick pilot industries: grain → mill/kitchen → food; vineyard → winery → export; dairy → trade; magic workshop → items and Source.

**B.** Data owners: Capitals — titles and dynasties; Settlements — settlements and territory; Core — states as simulation entities (to be confirmed in an ADR). Units of account: Item IDs and tags, commodities vs physical item stacks, storage, costs, labor, prices. The economy must not count the same chest twice — physically and abstractly.

**C.** Minimal contracts `Commodity`, `ProductionProcess`, `Profession`, `BuildingFunction`, `TradeRoute`, `MagicCapability`. Trade routes between villages, villages subordinate to states, taxes. Magic professions keep Source and spirits as real constraints. The M5 abstract simulation uses the same contracts.

**D — acceptance:** at least 3 full industries, one inter-settlement trade chain, 2 states with several villages each; no duplicated goods and no profit without costs; everything survives unloading and world reloads.

## 10. M7 — Atmosphere: seasons, trees, weather

**Goal:** nature changes by itself and affects village economies; this is visible when the player returns to an unloaded region.

### 10.1. Mods and alternatives

| Status | Mod | Notes |
| --- | --- | --- |
| Base | **Serene Seasons** + GlitchCore | Seasons, seasonal vegetation and climate; NeoForge 1.21.1 declared [S33] |
| Candidate | **Dynamic Trees** | Forest growth and spread; **changes worldgen and affects lumberjacks** — decide no later than M4-B [S34] |
| Candidate | **Immersive Weathering: Renewed** | Signs of age, moss, oxidation, weather effects [S35] |
| Alternative A | **Weather2** + CoroUtil | Heavy precipitation, storms, tornadoes; NeoForge 1.21.1 confirmed [S36] |
| Alternative B | **ProtoManly's Weather** | Spectacular storms, destructive tornadoes; **incompatible with shaders**, heavy GPU load [S37] |
| Candidate if B | **Natural Regrowth for ProtoManly's Weather** | Nature recovery [S38] |
| Optional | Enhanced Celestials, meteors, floods | Separate load and save risks; research later |

**Do not install Weather2 and ProtoManly together "just in case".**

**A.** Seasons first, then tree growth, then one weather mod. Test on M4–M6 settlements.

**B.** Resolve conflicts between weather sources, tree regeneration and crop growth. Avoid unbounded tick updates and free destruction of buildings. Account for the incompatibility of some Dynamic Trees addons with Ars Nouveau [S39].

**C.** `RegionClimate` and `WorldEventScheduler` in Core: climate, drought, fires, river floods, forest recovery exist abstractly in unloaded regions and affect village economies (seasonal harvests). On load, physical traces are materialized within safe limits. Building protection is mandatory.

**D — acceptance:** after several seasons in an unloaded region the state of forests, fields and households changes; on return this is physically visible; no TPS degradation and no uncontrolled destruction.

## 11. M8 — Our own peoples, architecture and NPC looks

**Goal:** Living Kingdoms' own cultures with recognizable architecture, clothing and way of life.

**Sources:** our mod's building templates; appearance and clothing via the MCA systems; CTOV, Towns and Towers, Structory, Dungeons and Taverns — inspiration only. **CTOV is CC BY-NC-ND 4.0** — modified structures may not be distributed [S32]. All culture assets are original.

**A.** A second, stylistically distinct people on top of the working M4 pipeline.

**B.** Separate `Culture` (language, architecture, traditions, goods), `Polity` (power) and `Settlement` (administrative-physical entity). Fix inconsistent clothing, proportions, impossible layouts on complex terrain.

**C.** Compact culture content packs: architecture templates, work spots, construction stages, professions, clothing by status and age, cultural items.

**D.** Both cultures can independently found, expand and maintain a settlement; buildings are usable for living and working; new peoples do not break the family and political model.

## 12. M9 — Long autonomous life of the world

**Goal:** a world without players produces its own consistent history. This extends working systems instead of rewriting them.

**A.** Accelerated runs for 1, 5 and 25 in-game years. Check demography, stock, relations between states, construction dynamics.

**B.** Find stability problems and exploits: vanishing resources, endless migrations, avalanche-like founding of settlements, endless wars, "ghost" NPCs.

**C.** By priority: migration, colonization, inheritance, trade treaties, wars and sieges, destruction and recovery, state magic policy.

**D.** Long run on the chosen seed; errors are explainable from the event log; pause, test snapshot rollback and diagnostics exist; materials and people never appear from nothing.

## 13. M10 — Stabilization, final world generation and release

**Goal:** a pack ready for long playthroughs, not a set of experimental mods.

**A.** Pin JAR versions, configs, datapacks, culture content, generation algorithms, seed. License check of every distributed file.

**B.** Singleplayer and LAN tests, a dedicated-server pass if it is in scope by then, checks of all adventure progression gates, several cultures and all magic systems, load testing, restore from backups, data format migrations.

**C.** Final world pre-generation with Chunky after worldgen mods are finally locked. WorldValidator report, mandatory structure registry, route checks, client (and server, if in scope) package export.

**D.** Release conditions: reproducible install, manifest checksum, 100% of mandatory structures, no critical duplication of population and goods, stable on the agreed configuration and load.

## 14. Project architecture and repository layout

```text
living-kingdoms/
├── README.md
├── docs/
│   ├── roadmap.md
│   ├── decisions/                # ADRs: versions, WorldGen, NPCs, mana...
│   ├── compatibility-matrix.md
│   ├── content-manifest.md
│   ├── licenses.md
│   └── testing/
├── modpack/
│   ├── pack.toml                 # packwiz — source of truth
│   ├── index.toml
│   ├── mods/*.pw.toml
│   ├── config/
│   ├── kubejs/
│   ├── resourcepacks/
│   └── datapacks/
│       ├── lk-worldgen/
│       ├── lk-gameplay/
│       └── lk-magic/
├── mods-src/
│   ├── livingkingdoms-core/     # regions, time, saves, events
│   ├── settlements/             # settlements, professions, construction (on MCA residents)
│   ├── economy-adapter/         # professions, industries, trade
│   └── world-materializer/      # blocks and entities on load
├── content/
│   ├── cultures/
│   ├── buildings/
│   ├── npc-visuals/
│   └── manifest/
├── tools/
│   ├── world-validator/
│   ├── mod-audit/
│   ├── sim-inspector/
│   └── build-scripts/
├── tests/
│   ├── smoke/
│   ├── worldgen/
│   ├── gameplay/
│   ├── magic/
│   ├── settlements/
│   └── longevity/
└── runtime/                      # generated, NOT committed
    ├── client/
    ├── server/
    └── snapshots/
```

This is a **logical layout**, not a commitment to create four Java projects right away. In M0 a single `livingkingdoms-core` is enough; modules are split out as real code and independent tests appear.

### 14.1. Responsibility boundaries

- **External mods:** game items, mobs, spells, blocks, actual work in a loaded chunk.
- **Compatibility layer:** tags, recipes, rates, configs, disabling conflicts.
- **Core:** IDs, single history, regions, events, unloaded-world state, saves.
- **Settlements:** settlements, professions, construction, storage; residents are MCA Reborn entities with our data attached.
- **MCA / Capitals adapters:** link to the MCA Reborn and MCA Capitals APIs, data-ownership control.
- **Materializer:** safe application of planned changes via the Minecraft API on the server thread.
- **World validator:** independent check that the reachable world actually matches the design.

### 14.2. Data core and state transitions

```text
Finite World → Geographic Region → Simulation Cell → Minecraft Chunk
               │
               ├── Settlements → Citizens / Families / Buildings
               ├── Political Entities → Ownership / Laws / Diplomacy
               ├── Economy → Stocks / Processes / Trade
               └── Climate / World Events

Loaded chunk: physical entities & blocks ↔ reconciliation ↔ abstract state
Unloaded chunk: abstract regional simulation → pending physical changes
```

**Rules:** one record per NPC identity, one source of truth per commodity/storage, save versioning, idempotent events, no physical ticking in unloaded chunks, a change budget for materialization. Investigate conflicts between the physical and abstract economies running at the same time.

## 15. AI agent strategy and reproducible install

The agent follows a safe process: `propose → install in sandbox → smoke test → commit manifest`. It must not download arbitrary "packs" from unverified links.

**Adding a mod:** find the official page and the supported Minecraft/NeoForge pair → check dependencies and license → add to packwiz → store checksums and versions → prepare a separate instance → launch the game (singleplayer, then LAN) → check a basic gameplay scenario → add an entry to the compatibility matrix → include in the main profile.

**Change hierarchy:** 0) change nothing if it works; 1) config; 2) tags/datapack; 3) existing addon; 4) KubeJS; 5) our own adapter; 6) fork of the original mod only when proven necessary and the license allows it.

**Agent rule:** only agreed configs and the manifest may be applied automatically; changes that can damage saves, world generation, availability of mandatory structures or licensing go into a separate architecture decision.

## 16. End-to-end tests and numeric targets

| Area | Test set | Acceptance bar |
| --- | --- | --- |
| Install | Clean client from Git | Launches without manually downloading JARs |
| Play mode | Singleplayer + Open to LAN with a second client | Join works, no desync of our mechanics |
| Generation | Several seeds and one reference world | All mandatory structures/chains reachable |
| Recipes | Cross-mod ingredient combinations | No blocking duplicates/exploits |
| Mana | Ars + Iron's in one pool | No double regeneration or wrong deductions |
| NPCs | Families, age, construction, trade | IDs and state survive a world reload |
| Unloading | 1/10/100 chunk load cycles | No duplicated goods, NPCs, buildings |
| Performance | spark, JVM/heap, mean and p95 tick | Target 20 TPS on the host PC in the described scenario; load limits recorded |
| Long simulation | 1, 5, 25 in-game years | No accumulation of obvious contradictions |
| Disasters | Fire/flood/storm with object protection | No uncontrolled destruction |
| Release | Licenses, sources/packages, backups | No illegally included modified assets |

Settlement counts and test durations are project targets. Actual budgets will be set after profiling, to avoid fictitious performance guarantees.

## 17. Risks and decisions before the corresponding stages

| Risk | Consequence | Mitigation |
| --- | --- | --- |
| Worldgen changes after M1 | Map seams, missing structures | Stabilize WorldGen; regression checks for new biomes/ores |
| Mixing existing villages with our villages | Duplicate settlements, broken roads | Reserved zones, control over generator spawning |
| Incompatible licenses of cultures/structures | The pack cannot be distributed | Check permissions and use original assets |
| Our settlement mod is too large | Months without a playable result | Vertical slices S1–S4, each a release; look for open code (MineColonies/Structurize) |
| MCA Reborn API is not enough for resident tasks | Mixin patches, fragility on updates | Research in M4-A; patches minimal and covered by tests |
| Rewriting MCA too early | Expensive fork, desync | API, events and attachments first, then Mixins; a fork is the last resort |
| Shared mana breaks balance | Endless casting, double bonuses | One authoritative pool, a rate, gameplay tests |
| Uncontrolled Occultism mines | Resource geography stops mattering | Performance/rarity config, later limits |
| Weather destroys settlements | Lost buildings and saves | Protection/limits, one weather engine, backups |
| No physical simulation away from players | Unnoticeable or inconsistent world life | Abstract simulation + budgeted materialization |
| Host PC runs client and integrated server | Tighter performance budget than a dedicated server | Profile on the host PC from M1; budget heavy mods early |
| "Everything at once" profiles | Errors cannot be localized | Isolated profiles and staged enabling |
| Unnecessary custom development | Growing maintenance and costs | Check existing addons before writing code |

## 18. Priority research tasks

1. **M1 / WorldGen:** pin specific JARs and dependencies of Bounded Not Free, Terralith, Tectonic, YUNG's, Dungeons and Taverns, When Dungeons Arise; test generation on 3–5 seeds.
2. **M1 / Manifest:** describe the set of mandatory structures, biome roles and progression paths including the Nether/End/dimensions.
3. **M2 / Food compatibility:** compare Pantrywork, Farmer's (Delight) Croptopia and Almost Unified — check overlaps in a live world.
4. **M3 / Mana:** choose the primary pool in Ars 'n Spells and the starting rate `k`, check armor/regeneration and display.
5. **M3 / Magic roles:** write down the specific minimal Ars restrictions and the Occultism recipes that really need a price increase after testing.
6. **M4 / MCA Reborn:** architecture, API, events, data storage, license; can residents be given tasks from outside.
7. **M4 / Open code:** MineColonies, Structurize and worker-NPC mods — licenses and suitability of construction and logistics code.
8. **M4 / Prototype:** an MCA resident performs a task of our mod and survives a world reload.
9. **M6 / MCA Capitals:** real API, 1.21.1 version, license.
10. **M7 / Weather:** separate test of Weather2 and ProtoManly with shaders/Iris/DH/seasons/performance; Dynamic Trees compatibility with M4 lumberjacks.

## 19. Sources and mod checks

Official pages or project pages. These are references as of 2026-11-03 (and 2026-11-06 for version checks), not guarantees that the selected releases are compatible with each other. For a multi-mod pack the final check is launching specific JARs and playtesting.

- **[S01]** packwiz — official repository: https://github.com/packwiz/packwiz
- **[S02]** Bounded Not Free: https://www.curseforge.com/minecraft/mc-mods/bounded-not-free
- **[S03]** Tectonic and Terralith compatibility: https://modrinth.com/mod/Tectonic
- **[S04]** Lithostitched: https://modrinth.com/mod/lithostitched
- **[S05]** YUNG's Better Strongholds: https://modrinth.com/mod/yungs-better-strongholds
- **[S06]** When Dungeons Arise: https://www.curseforge.com/minecraft/mc-mods/when-dungeons-arise
- **[S07]** Dungeons and Taverns: https://modrinth.com/mod/dungeons-and-taverns
- **[S08]** Mowzie's Mobs: https://modrinth.com/mod/mowzies-mobs
- **[S09]** The Twilight Forest: https://www.curseforge.com/minecraft/mc-mods/the-twilight-forest
- **[S10]** Chunky pre-generator: https://github.com/pop4959/Chunky
- **[S11]** Farmer's Delight: https://www.curseforge.com/minecraft/mc-mods/farmers-delight
- **[S12]** Croptopia: https://www.curseforge.com/minecraft/mc-mods/croptopia
- **[S13]** Vinery: https://modrinth.com/mod/lets-do-vinery
- **[S14]** Meadow: https://modrinth.com/mod/lets-do-meadow
- **[S15]** HerbalBrews: https://modrinth.com/mod/lets-do-herbalbrews
- **[S16]** Handcrafted: https://modrinth.com/mod/handcrafted
- **[S17]** spark: https://modrinth.com/mod/spark
- **[S18]** ModernFix / FerriteCore / Lithium: https://modrinth.com/mod/modernfix ; https://modrinth.com/mod/ferrite-core ; https://modrinth.com/mod/lithium
- **[S19]** Supplementaries: https://modrinth.com/mod/supplementaries
- **[S20]** Pantrywork: https://modrinth.com/mod/pantrywork
- **[S21]** Farmer's (Delight) Croptopia: https://modrinth.com/project/farmers-croptopia
- **[S22]** Almost Unified: https://modrinth.com/project/almostunified
- **[S23]** KubeJS: https://kubejs.com/wiki/other/source-code
- **[S24]** Ars 'n Spells — shared mana and configuration: https://github.com/otectus/ars-n-spells/blob/main/README.md
- **[S25]** Ars Ocultas: https://modrinth.com/project/ars-ocultas
- **[S26]** Ars Unification: https://modrinth.com/mod/ars-unification
- **[S27]** NeoForge SavedData: https://docs.neoforged.net/docs/datastorage/saveddata/
- **[S28]** Millénaire 9.0.3 (idea reference only, not in the pack): https://www.millenaire.org/downloads
- **[S29]** MCA Reborn: https://www.curseforge.com/minecraft/mc-mods/minecraft-comes-alive-reborn
- **[S30]** MCA Capitals: https://www.curseforge.com/minecraft/mc-mods/mca-capitals
- **[S31]** Millénaire license: https://www.curseforge.com/minecraft/mc-mods/millenaire/license
- **[S32]** CTOV (CC BY-NC-ND 4.0): https://modrinth.com/mod/ct-overhaul-village
- **[S33]** Serene Seasons: https://modrinth.com/mod/serene-seasons
- **[S34]** Dynamic Trees: https://www.curseforge.com/minecraft/mc-mods/dynamictrees
- **[S35]** Immersive Weathering Renewed: https://modrinth.com/mod/immersive-weather-renewed
- **[S36]** Weather2: https://www.curseforge.com/minecraft/mc-mods/weather-storms-tornadoes
- **[S37]** ProtoManly's Weather: https://modrinth.com/mod/protomanlys-weather
- **[S38]** Natural Regrowth for ProtoManly: https://modrinth.com/mod/natural-regrowth
- **[S39]** Ars Nouveau 1.21.1 addon catalog with warnings: https://github.com/Ars-Nouveau/Ars-Auxilia/blob/main/apps/ars.guide/src/content/docs/1.21.1/ars-addons/guide.mdx
- **[S40]** Modrinth API version data (platform check, 2026-11-06): https://api.modrinth.com/v2/project/{slug}/version

---

**The project in one line:** finish WorldGen → get a cohesive adventure world; finish light gameplay → get a cohesive household game; finish magic → get a cohesive magic ecosystem; finish settlements → get living villages; then the world lives without players, villages join into an economy and states, nature comes alive, our own peoples and an autonomous history appear. Each next step extends a finished system instead of rebuilding the previous one.
