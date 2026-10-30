package trafficsim.strategy;

import trafficsim.SimConstants;
import trafficsim.util.LightPhase;

/** Standard-Java checks for fixed and adaptive signal strategies. */
public final class SignalTimingStrategiesTest {

    private SignalTimingStrategiesTest() {}

    public static void main(String[] args) {
        fixedTimeNeverExtendsGreen();
        adaptiveExtendsOnlyForUsefulAndFairQueuePressure();
        System.out.println("SignalTimingStrategiesTest passed");
    }

    private static void fixedTimeNeverExtendsGreen() {
        SignalTimingContext busyGreen = context(
                LightPhase.GREEN,
                1,
                0,
                20,
                0);
        require(!SignalTimingStrategies.fixedTime()
                        .shouldExtendGreen(busyGreen),
                "Fixed-time signals should never extend green.");
    }

    private static void adaptiveExtendsOnlyForUsefulAndFairQueuePressure() {
        SignalTimingStrategy adaptive =
                SignalTimingStrategies.adaptiveQueuePressure();

        require(adaptive.shouldExtendGreen(context(
                        LightPhase.GREEN, 2, 0, 5, 2)),
                "Adaptive timing should extend a late green with greater queue pressure.");
        require(!adaptive.shouldExtendGreen(context(
                        LightPhase.YELLOW, 0, 0, 5, 2)),
                "Adaptive timing should extend only a green phase.");
        require(!adaptive.shouldExtendGreen(context(
                        LightPhase.GREEN,
                        SimConstants.ADAPTIVE_EXTENSION_WINDOW + 1,
                        0,
                        5,
                        2)),
                "Adaptive timing should wait until the end of green.");
        require(!adaptive.shouldExtendGreen(context(
                        LightPhase.GREEN, 2, 0, 3, 0)),
                "Adaptive timing should require a substantial green-axis queue.");
        require(!adaptive.shouldExtendGreen(context(
                        LightPhase.GREEN, 2, 0, 5, 5)),
                "Adaptive timing should not starve an equally busy red axis.");
        require(!adaptive.shouldExtendGreen(context(
                        LightPhase.GREEN,
                        2,
                        SimConstants.ADAPTIVE_MAX_EXTENSIONS,
                        8,
                        0)),
                "Adaptive timing should respect the extension limit.");
    }

    private static SignalTimingContext context(
            LightPhase phase,
            int remaining,
            int extensions,
            int greenQueue,
            int redQueue) {
        return new SignalTimingContext(
                phase,
                remaining,
                extensions,
                greenQueue,
                redQueue);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
