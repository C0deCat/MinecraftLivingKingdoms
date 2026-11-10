// The three blocks of a test (convention A1). Each labels a failure with its block, so the report shows
// whether the setup, the behaviour under test or the assertion failed.

async function block<T>(name: "arrange" | "act" | "assertThat", fn: () => Promise<T> | T): Promise<T> {
  try {
    return await fn();
  } catch (error) {
    if (error instanceof Error && !error.message.startsWith("[")) error.message = `[${name}] ${error.message}`;
    throw error;
  }
}

/** Setup through commands and helpers; cheats are fine here. */
export function arrange<T>(fn: () => Promise<T> | T): Promise<T> {
  return block("arrange", fn);
}

/** Only the behaviour under test, only through the game path (player.* or input.*). */
export function act<T>(fn: () => Promise<T> | T): Promise<T> {
  return block("act", fn);
}

/** Assertions on server state through query.* helpers. */
export function assertThat<T>(fn: () => Promise<T> | T): Promise<T> {
  return block("assertThat", fn);
}
