package livingkingdoms.testkit;

import net.neoforged.fml.common.Mod;

/**
 * Dev-only test kit (ADR-0005). Common entry point; the e2e driver is client-only and lives in
 * {@code livingkingdoms.testkit.client}. This mod is never part of the pack.
 */
@Mod(LivingKingdomsTestkit.MOD_ID)
public final class LivingKingdomsTestkit {
    public static final String MOD_ID = "livingkingdoms_testkit";

    /** Version of the e2e wire protocol; must equal PROTOCOL_VERSION in tests/e2e/harness/protocol.ts. */
    public static final int PROTOCOL_VERSION = 1;

    public LivingKingdomsTestkit() {}
}
