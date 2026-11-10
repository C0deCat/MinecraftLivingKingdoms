// Pinned versions of every external tool the pack tooling downloads or builds.
// Changing a pin is a pack update: add a modpack/CHANGELOG.md entry and relaunch.

export const MINECRAFT_VERSION = "1.21.1";
export const NEOFORGE_VERSION = "21.1.265";

/** packwiz has no binary releases; it is built with `go install` at this commit. */
export const PACKWIZ_COMMIT = "170b22d785959dda5876b54b7f16e1b807a57fb6";

export interface PinnedDownload {
  url: string;
  sha256: string;
  file: string;
}

export const PACKWIZ_INSTALLER: PinnedDownload = {
  url: "https://github.com/packwiz/packwiz-installer/releases/download/v0.5.14/packwiz-installer.jar",
  sha256: "c9f646908d340d84773948a9a7d98bc1dae250d35e1016dc6e2b8459760b5598",
  file: "packwiz-installer.jar",
};

export const PACKWIZ_INSTALLER_BOOTSTRAP: PinnedDownload = {
  url: "https://github.com/packwiz/packwiz-installer-bootstrap/releases/download/v0.0.3/packwiz-installer-bootstrap.jar",
  sha256: "a8fbb24dc604278e97f4688e82d3d91a318b98efc08d5dbfcbcbcab6443d116c",
  file: "packwiz-installer-bootstrap.jar",
};

/** Portable Prism Launcher (MSVC x64 build) for Windows. */
export const PRISM_WINDOWS: PinnedDownload = {
  url: "https://github.com/PrismLauncher/PrismLauncher/releases/download/11.1.1/PrismLauncher-Windows-MSVC-Portable-11.1.1.zip",
  sha256: "ab35a770fb06d89d2ccc098079db5db329fb4e68f42b72babd8b095efde3d2d7",
  file: "PrismLauncher-Windows-MSVC-Portable-11.1.1.zip",
};

export const INSTANCE_NAME = "living-kingdoms";
/** Prism instance for e2e tests: the pack plus the dev-only testkit mod (ADR-0005). */
export const E2E_INSTANCE_NAME = "living-kingdoms-e2e";

/** NeoForge installer, used only for the local headless worldgen test server (npm run test-server). */
export const NEOFORGE_INSTALLER: PinnedDownload = {
  url: `https://maven.neoforged.net/releases/net/neoforged/neoforge/${NEOFORGE_VERSION}/neoforge-${NEOFORGE_VERSION}-installer.jar`,
  sha256: "a547ccc30a80a675d1c61d04b55d10c45c39a86971e09731482c50a24399d1a2",
  file: `neoforge-${NEOFORGE_VERSION}-installer.jar`,
};
