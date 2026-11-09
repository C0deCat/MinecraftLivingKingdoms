// Connection to the testkit driver: newline-delimited JSON over TCP on 127.0.0.1.
// Dependency-free (node:net only), so tools/pack/src/e2e.ts can use it too.
import { connect, type Socket } from "node:net";
import type { OpErrorBody, OpName, Ops, Response } from "./protocol.ts";

export const DEFAULT_PORT = 25598;
/** Wall-clock guard for one op (W3): an op that never answers means a hung game, not a slow test. */
const DEFAULT_OP_TIMEOUT_MS = 60_000;

/** An op the driver answered with ok:false. The hint says how to fix the cause. */
export class OpFailure extends Error {
  readonly op: string;
  readonly code: string;
  readonly hint: string;

  constructor(op: string, body: OpErrorBody) {
    super(`${op} failed [${body.code}]: ${body.message}\nHint: ${body.hint}`);
    this.name = "OpFailure";
    this.op = op;
    this.code = body.code;
    this.hint = body.hint;
  }
}

interface Pending {
  op: string;
  resolve: (value: unknown) => void;
  reject: (error: Error) => void;
  timer: NodeJS.Timeout;
}

export class GameClient {
  readonly port: number;
  private readonly socket: Socket;
  private readonly pending = new Map<number, Pending>();
  private nextId = 1;
  private buffer = "";
  private closed = false;

  private constructor(socket: Socket, port: number) {
    this.socket = socket;
    this.port = port;
    socket.setEncoding("utf8");
    socket.on("data", (chunk: string) => this.onData(chunk));
    socket.on("close", () => this.failAll(new Error("The game closed the connection (did it quit or crash? See its logs/latest.log).")));
    socket.on("error", (error) => this.failAll(error));
  }

  /** Connects to a running game. Rejects quickly when nothing listens on the port. */
  static connect(port = DEFAULT_PORT, timeoutMs = 2_000): Promise<GameClient> {
    return new Promise((resolve, reject) => {
      const socket = connect({ host: "127.0.0.1", port });
      const timer = setTimeout(() => {
        socket.destroy();
        reject(new Error(`No answer from 127.0.0.1:${port} within ${timeoutMs} ms.`));
      }, timeoutMs);
      socket.once("connect", () => {
        clearTimeout(timer);
        resolve(new GameClient(socket, port));
      });
      socket.once("error", (error) => {
        clearTimeout(timer);
        reject(new Error(`Cannot connect to the testkit driver on 127.0.0.1:${port} (${error.message}). ` +
          "Start the game with `npm run e2e` (it launches or attaches to the e2e instance)."));
      });
    });
  }

  /** Calls a declared op with typed args and result. Specs use helpers, not this (convention L1). */
  call<K extends OpName>(op: K, args: Ops[K]["args"], options: { timeoutMs?: number } = {}): Promise<Ops[K]["result"]> {
    return this.raw(op, args, options) as Promise<Ops[K]["result"]>;
  }

  /** Sends any op name; for diagnostics and unit tests of the protocol itself. */
  raw(op: string, args: object, options: { timeoutMs?: number } = {}): Promise<unknown> {
    if (this.closed) return Promise.reject(new Error(`Cannot send ${op}: the connection is closed.`));
    const id = this.nextId++;
    const timeoutMs = options.timeoutMs ?? DEFAULT_OP_TIMEOUT_MS;
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.pending.delete(id);
        reject(new Error(`${op} got no answer within ${timeoutMs} ms; the game may be hung (check its logs/latest.log).`));
      }, timeoutMs);
      this.pending.set(id, { op, resolve, reject, timer });
      this.socket.write(JSON.stringify({ id, op, args }) + "\n");
    });
  }

  close(): void {
    this.closed = true;
    this.socket.end();
  }

  private onData(chunk: string): void {
    this.buffer += chunk;
    let newline: number;
    while ((newline = this.buffer.indexOf("\n")) >= 0) {
      const line = this.buffer.slice(0, newline).trim();
      this.buffer = this.buffer.slice(newline + 1);
      if (line) this.onResponse(JSON.parse(line) as Response);
    }
  }

  private onResponse(response: Response): void {
    const pending = this.pending.get(response.id);
    if (!pending) return;
    this.pending.delete(response.id);
    clearTimeout(pending.timer);
    if (response.ok) pending.resolve(response.result);
    else pending.reject(new OpFailure(pending.op, response.error));
  }

  private failAll(error: Error): void {
    this.closed = true;
    for (const [id, pending] of this.pending) {
      clearTimeout(pending.timer);
      pending.reject(new Error(`${pending.op}: ${error.message}`));
      this.pending.delete(id);
    }
  }
}
