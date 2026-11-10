// Unit tests of the harness itself; no game needed.
import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    include: ["unit/**/*.test.ts"],
  },
});
