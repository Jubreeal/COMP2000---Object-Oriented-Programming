package trafficsim.strategy.signal;

import trafficsim.util.Axis;
import trafficsim.util.LightPhase;

import java.util.Objects;

/**
 * Immutable input supplied to a {@link SignalTimingStrategy} once per signal tick.
 * Keeping the strategy's input immutable makes policies easy to test and prevents
 * them from reaching into, and accidentally mutating, the road network.
 */
public record SignalSnapshot(
        LightPhase phase,
        Axis greenAxis,
        int ticksRemaining,
        int greenQueue,
        int redQueue,
        int extensionsUsed) {

    public SignalSnapshot {
        Objects.requireNonNull(phase, "Signal phase cannot be null");
        Objects.requireNonNull(greenAxis, "Green axis cannot be null");
        if (ticksRemaining < 0 || greenQueue < 0 || redQueue < 0 || extensionsUsed < 0) {
            throw new IllegalArgumentException("Signal snapshot counts cannot be negative");
        }
    }
}
