package livingkingdoms.testkit.client;

/**
 * An expected op failure. {@code code} is stable and machine-readable; {@code hint} tells the test
 * author (often an agent) how to fix the cause.
 */
public final class OpError extends RuntimeException {
    private final String code;
    private final String hint;

    public OpError(String code, String message, String hint) {
        super(message);
        this.code = code;
        this.hint = hint;
    }

    public String code() {
        return code;
    }

    public String hint() {
        return hint;
    }
}
