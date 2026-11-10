// The e2e protocol: one source of truth for every op the testkit driver implements (ADR-0005).
// The driver reports its op names at `hello`; checkProtocol() refuses to run when they differ from OP_DOCS.
// Adding an op: declare it in `Ops` and `OP_DOCS` here, register it in an *Ops class in
// mods-src/livingkingdoms-testkit (livingkingdoms.testkit.client), rebuild, regenerate the catalog.

/** Must equal LivingKingdomsTestkit.PROTOCOL_VERSION. Bump both when the wire format changes. */
export const PROTOCOL_VERSION = 1;

export type NoArgs = Record<string, never>;

export interface HelloResult {
  protocol: number;
  minecraft: string;
  neoforge: string;
  /** Names of every op the driver implements. */
  ops: string[];
  /** SHA-256 of our own JARs loaded in the game (`livingkingdoms_*.jar`), by file name. */
  ownJars: Record<string, string>;
  mods: { id: string; version: string }[];
}

export interface StateResult {
  /** Fully qualified class name of the open screen, or null in the world without a screen. */
  screen: string | null;
  inWorld: boolean;
  /** Game time of the client level in ticks, or null outside a world. */
  tick: number | null;
  dimension: string | null;
  paused: boolean;
  /** True while the startup resource reload is shown; no world can be opened until it is false. */
  loading: boolean;
  /** Folder name of the open save, or null. */
  world: string | null;
}

/** Whole block coordinates. */
export interface BlockPos {
  x: number;
  y: number;
  z: number;
}

/** Exact coordinates (entity positions). */
export interface Vec3 {
  x: number;
  y: number;
  z: number;
}

export type Face = "up" | "down" | "north" | "south" | "east" | "west";

/** An item stack; empty is { item: "minecraft:air", count: 0 }. */
export interface ItemStackInfo {
  item: string;
  count: number;
}

export interface PlayerState {
  pos: Vec3;
  yaw: number;
  pitch: number;
  onGround: boolean;
  gamemode: string;
  health: number;
  food: number;
  selectedSlot: number;
  dimension: string;
}

export interface ContainerState {
  containerId: number;
  /** Menu type id, e.g. minecraft:crafting, minecraft:generic_9x3; the player's own inventory is minecraft:inventory. */
  type: string;
  screen: string | null;
  slots: (ItemStackInfo & { index: number })[];
  carried: ItemStackInfo;
}

export interface EntityInfo {
  id: number;
  type: string;
  pos: Vec3;
  alive: boolean;
  health?: number;
  /** The stack of an item entity. */
  item?: ItemStackInfo;
}

export type ClickType = "pickup" | "quick_move" | "swap" | "clone" | "throw" | "quick_craft" | "pickup_all";

/** Every op: its argument and result types. */
export interface Ops {
  hello: { args: NoArgs; result: HelloResult };
  ping: { args: NoArgs; result: NoArgs };
  state: { args: NoArgs; result: StateResult };
  quit: { args: NoArgs; result: NoArgs };
  "world.create": { args: { name: string; seed?: number; gamemode?: string; difficulty?: string }; result: { name: string } };
  "world.open": { args: { name: string }; result: { name: string } };
  "world.leave": { args: NoArgs; result: NoArgs };
  "world.delete": { args: { name: string }; result: NoArgs };
  "world.list": { args: NoArgs; result: { names: string[] } };
  "input.hold": { args: { mapping: string; ticks: number }; result: NoArgs };
  "input.press": { args: { mapping: string }; result: NoArgs };
  "input.look": { args: { yaw: number; pitch: number }; result: NoArgs };
  "player.state": { args: NoArgs; result: PlayerState };
  "player.walkTo": { args: { target: Vec3; tolerance?: number; ticks?: number }; result: Vec3 };
  "player.selectSlot": { args: { slot: number }; result: NoArgs };
  "player.breakBlock": { args: { pos: BlockPos; ticks?: number }; result: NoArgs };
  "player.useItemOn": { args: { pos: BlockPos; face?: Face }; result: { result: string } };
  "player.attack": { args: { entity: number }; result: NoArgs };
  "container.read": { args: NoArgs; result: ContainerState };
  "container.click": { args: { slot: number; button?: number; type?: ClickType }; result: NoArgs };
  "container.close": { args: NoArgs; result: NoArgs };
  "command.run": { args: { cmd: string; as?: "player" | "server" }; result: { result: number; feedback: string[] } };
  "query.block": { args: { pos: BlockPos }; result: { block: string; properties: Record<string, string>; hasBlockEntity: boolean } };
  "query.inventory": { args: NoArgs; result: { slots: (ItemStackInfo & { slot: number })[]; selectedSlot: number } };
  "query.entities": { args: { from: BlockPos; to: BlockPos; type?: string }; result: { entities: EntityInfo[] } };
  "wait.ticks": { args: { n: number }; result: { serverTicks: number } };
}

export type OpName = keyof Ops;

export interface OpDoc {
  group: "session" | "world" | "input" | "player" | "container" | "ui" | "command" | "query" | "test";
  summary: string;
}

/** One line per op; the op catalog (docs/references/e2e-protocol.md) is generated from this. */
export const OP_DOCS: { [K in OpName]: OpDoc } = {
  hello: { group: "session", summary: "Protocol version, Minecraft and NeoForge versions, implemented ops, hashes of our JARs, loaded mods." },
  ping: { group: "session", summary: "Answers immediately; checks that the driver is reachable." },
  state: { group: "session", summary: "Open screen, whether a world is loaded, client game time, dimension, paused and loading flags, open save name." },
  quit: { group: "session", summary: "Replies, then closes the game." },
  "world.create": { group: "world", summary: "Creates and opens a test world (name starts with e2e-) on the testkit flat preset (D-23) with the R2 game rules; returns when the player is in." },
  "world.open": { group: "world", summary: "Opens an existing save; returns when the player is in." },
  "world.leave": { group: "world", summary: "Saves and quits to the title screen, like Save and Quit; nothing outside a world." },
  "world.delete": { group: "world", summary: "Deletes a closed test world (e2e- names only)." },
  "world.list": { group: "world", summary: "Folder names of all saves." },
  "input.hold": { group: "input", summary: "Holds a key mapping (e.g. key.forward) for a number of client ticks." },
  "input.press": { group: "input", summary: "Clicks a key mapping once (e.g. key.inventory); handled on the next client tick." },
  "input.look": { group: "input", summary: "Sets the camera yaw and pitch in degrees (yaw 0 = south, +Z)." },
  "player.state": { group: "player", summary: "Server-side player: position, rotation, on ground, gamemode, health, food, selected slot, dimension." },
  "player.walkTo": { group: "player", summary: "Walks in a straight line to a point by holding key.forward and turning each tick; no pathfinding." },
  "player.selectSlot": { group: "player", summary: "Selects a hotbar slot (0–8)." },
  "player.breakBlock": { group: "player", summary: "Holds attack on a block until it breaks, through the client game mode." },
  "player.useItemOn": { group: "player", summary: "Right-clicks a block face with the held item: places blocks, opens containers." },
  "player.attack": { group: "player", summary: "Attacks an entity by id (from query.entities)." },
  "container.read": { group: "container", summary: "The open menu on the client: type, slots, carried stack, screen class." },
  "container.click": { group: "container", summary: "Clicks a menu slot (pickup, quick_move, ...) through the client game mode." },
  "container.close": { group: "container", summary: "Closes the open container or screen and tells the server." },
  "command.run": { group: "command", summary: "Runs a command on the server thread with operator rights as the player or the server; result count and feedback lines." },
  "query.block": { group: "query", summary: "Block id, state properties and whether a block entity exists, from the server." },
  "query.inventory": { group: "query", summary: "Non-empty slots of the player's inventory and the selected slot, from the server." },
  "query.entities": { group: "query", summary: "Entities in a box (optionally of one type): id, type, position, alive, health, item stack." },
  "wait.ticks": { group: "test", summary: "Completes after n integrated server ticks." },
};

export interface OpErrorBody {
  code: string;
  message: string;
  hint: string;
}

export type Response =
  | { id: number; ok: true; result: unknown }
  | { id: number; ok: false; error: OpErrorBody };
