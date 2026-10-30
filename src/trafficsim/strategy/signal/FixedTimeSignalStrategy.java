package trafficsim.strategy.signal;

/** Baseline controller: always follows the configured fixed signal schedule. */
public final class FixedTimeSignalStrategy implements SignalTimingStrategy {
    @Override
    public SignalDecision decide(SignalSnapshot snapshot) {
        return SignalDecision.NO_CHANGE;
    }

    @Override
    public String displayName() {
        return "FIXED-TIME";
    }
}
