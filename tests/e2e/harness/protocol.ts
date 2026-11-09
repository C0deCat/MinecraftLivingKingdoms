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
}

/** Every op: its argument and result types. */
export interface Ops {
  hello: { args: NoArgs; result: HelloResult };
  ping: { args: NoArgs; result: NoArgs };
  state: { args: NoArgs; result: StateResult };
  quit: { args: NoArgs; result: NoArgs };
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
  state: { group: "session", summary: "Open screen, whether a world is loaded, client game time, dimension, paused flag." },
  quit: { group: "session", summary: "Replies, then closes the game." },
};

export interface OpErrorBody {
  code: string;
  message: string;
  hint: string;
}

export type Response =
  | { id: number; ok: true; result: unknown }
  | { id: number; ok: false; error: OpErrorBody };
