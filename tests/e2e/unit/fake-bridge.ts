// A stand-in for the testkit driver, for unit tests of the harness without a game.
import { createServer, type Server, type Socket } from "node:net";
import { OP_DOCS, PROTOCOL_VERSION, type HelloResult } from "../harness/protocol.ts";

export type FakeOp = (args: Record<string, unknown>) => unknown | Promise<unknown>;

/** Thrown by a fake op to answer ok:false. */
export class FakeOpError extends Error {
  readonly code: string;
  readonly hint: string;
  constructor(code: string, message: string, hint: string) {
    super(message);
    this.code = code;
    this.hint = hint;
  }
}

export function fakeHello(overrides: Partial<HelloResult> = {}): HelloResult {
  return {
    protocol: PROTOCOL_VERSION,
    minecraft: "1.21.1",
    neoforge: "21.1.265",
    ops: Object.keys(OP_DOCS),
    ownJars: { "livingkingdoms_testkit-0.0.1.jar": "0".repeat(64) },
    mods: [],
    ...overrides,
  };
}

export class FakeBridge {
  readonly ops: Map<string, FakeOp>;
  private readonly server: Server;
  private readonly sockets = new Set<Socket>();

  constructor(ops: Record<string, FakeOp>) {
    this.ops = new Map(Object.entries(ops));
    this.server = createServer((socket) => this.serve(socket));
  }

  /** Listens on a free localhost port and returns it. */
  listen(): Promise<number> {
    return new Promise((resolve) => {
      this.server.listen(0, "127.0.0.1", () => {
        const address = this.server.address();
        resolve(typeof address === "object" && address ? address.port : 0);
      });
    });
  }

  close(): Promise<void> {
    for (const socket of this.sockets) socket.destroy();
    return new Promise((resolve) => this.server.close(() => resolve()));
  }

  private serve(socket: Socket): void {
    this.sockets.add(socket);
    socket.on("close", () => this.sockets.delete(socket));
    let buffer = "";
    socket.setEncoding("utf8");
    socket.on("data", (chunk: string) => {
      buffer += chunk;
      let newline: number;
      while ((newline = buffer.indexOf("\n")) >= 0) {
        const line = buffer.slice(0, newline);
        buffer = buffer.slice(newline + 1);
        if (line.trim()) void this.handle(socket, JSON.parse(line) as { id: number; op: string; args: Record<string, unknown> });
      }
    });
  }

  private async handle(socket: Socket, request: { id: number; op: string; args: Record<string, unknown> }): Promise<void> {
    const op = this.ops.get(request.op);
    let response: object;
    try {
      if (!op) throw new FakeOpError("unknown_op", `The driver does not implement op '${request.op}'.`, "Declare it in protocol.ts.");
      response = { id: request.id, ok: true, result: (await op(request.args)) ?? null };
    } catch (error) {
      const e = error instanceof FakeOpError ? error : new FakeOpError("internal", String(error), "See the client log.");
      response = { id: request.id, ok: false, error: { code: e.code, message: e.message, hint: e.hint } };
    }
    if (!socket.destroyed) socket.write(JSON.stringify(response) + "\n");
  }
}
