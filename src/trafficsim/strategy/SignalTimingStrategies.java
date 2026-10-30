package trafficsim.strategy;

import trafficsim.SimConstants;
import trafficsim.util.LightPhase;

import java.util.Objects;

/** Reusable signal-timing strategies expressed through lambda functions. */
public final class SignalTimingStrategies {

    private static final SignalTimingStrategy FIXED_TIME = context -> false;

    private static final SignalTimingStrategy ADAPTIVE_QUEUE_PRESSURE = context -> {
        Objects.requireNonNull(context, "Signal timing context cannot be null.");
        return context.phase() == LightPhase.GREEN
                && context.remainingGreenTicks()
                        <= SimConstants.ADAPTIVE_EXTENSION_WINDOW
                && context.extensionsThisPhase()
                        < SimConstants.ADAPTIVE_MAX_EXTENSIONS
                && context.queuedOnGreenAxis()
                        >= SimConstants.ADAPTIVE_QUEUE_HIGH
                && context.queuedOnGreenAxis() > context.queuedOnRedAxis();
    };

    private SignalTimingStrategies() {}

    public static SignalTimingStrategy fixedTime() {
        return FIXED_TIME;
    }

    public static SignalTimingStrategy adaptiveQueuePressure() {
        return ADAPTIVE_QUEUE_PRESSURE;
    }
}
