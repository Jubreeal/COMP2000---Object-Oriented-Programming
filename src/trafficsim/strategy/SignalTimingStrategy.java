package trafficsim.strategy;

/** Strategy for deciding whether a signal should extend its current green. */
@FunctionalInterface
public interface SignalTimingStrategy {

    boolean shouldExtendGreen(SignalTimingContext context);
}
