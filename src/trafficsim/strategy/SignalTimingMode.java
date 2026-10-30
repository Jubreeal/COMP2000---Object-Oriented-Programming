package trafficsim.strategy;

/**
 * User-selectable signal-timing modes and the strategies they represent.
 */
public enum SignalTimingMode {
    FIXED_TIME("FIXED TIME", SignalTimingStrategies.fixedTime()),
    ADAPTIVE_QUEUE_PRESSURE(
            "ADAPTIVE",
            SignalTimingStrategies.adaptiveQueuePressure());

    private final String displayName;
    private final SignalTimingStrategy strategy;

    SignalTimingMode(String displayName, SignalTimingStrategy strategy) {
        this.displayName = displayName;
        this.strategy = strategy;
    }

    public String displayName() {
        return displayName;
    }

    public SignalTimingStrategy strategy() {
        return strategy;
    }

    /** Return the next available mode, wrapping back to the first. */
    public SignalTimingMode next() {
        SignalTimingMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }
}
