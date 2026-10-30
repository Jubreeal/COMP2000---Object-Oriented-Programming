package trafficsim.strategy;

import trafficsim.util.LightPhase;

import java.util.Objects;

/** Immutable facts supplied to a signal-timing strategy for one update. */
public record SignalTimingContext(
        LightPhase phase,
        int remainingGreenTicks,
        int extensionsThisPhase,
        int queuedOnGreenAxis,
        int queuedOnRedAxis) {

    public SignalTimingContext {
        Objects.requireNonNull(phase, "Light phase cannot be null.");
        if (remainingGreenTicks < 0
                || extensionsThisPhase < 0
                || queuedOnGreenAxis < 0
                || queuedOnRedAxis < 0) {
            throw new IllegalArgumentException(
                    "Signal timing values cannot be negative.");
        }
    }
}
