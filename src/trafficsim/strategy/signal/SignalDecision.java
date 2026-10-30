package trafficsim.strategy.signal;

/** A policy's decision for the current signal tick. */
public record SignalDecision(int extensionTicks, String reason) {
    public static final SignalDecision NO_CHANGE = new SignalDecision(0, "keep schedule");

    public SignalDecision {
        if (extensionTicks < 0) {
            throw new IllegalArgumentException("Extension cannot be negative");
        }
        reason = reason == null || reason.isBlank() ? "unspecified" : reason;
    }

    public static SignalDecision extend(int ticks, String reason) {
        if (ticks <= 0) throw new IllegalArgumentException("Extension must be positive");
        return new SignalDecision(ticks, reason);
    }
}
