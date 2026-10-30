package trafficsim.strategy.signal;

import trafficsim.util.LightPhase;

/**
 * Extends a nearly finished green phase when its queue is both substantial and
 * at least as large as the perpendicular queue. The extension cap guarantees
 * that sustained demand on one axis cannot starve the other axis indefinitely.
 */
public final class QueueResponsiveSignalStrategy implements SignalTimingStrategy {
    private final int decisionWindow;
    private final int queueThreshold;
    private final int extensionTicks;
    private final int maximumExtensions;

    public QueueResponsiveSignalStrategy(
            int decisionWindow,
            int queueThreshold,
            int extensionTicks,
            int maximumExtensions) {
        if (decisionWindow <= 0 || queueThreshold <= 0
                || extensionTicks <= 0 || maximumExtensions <= 0) {
            throw new IllegalArgumentException("Adaptive signal settings must be positive");
        }
        this.decisionWindow = decisionWindow;
        this.queueThreshold = queueThreshold;
        this.extensionTicks = extensionTicks;
        this.maximumExtensions = maximumExtensions;
    }

    @Override
    public SignalDecision decide(SignalSnapshot snapshot) {
        if (snapshot.phase() != LightPhase.GREEN
                || snapshot.ticksRemaining() > decisionWindow
                || snapshot.extensionsUsed() >= maximumExtensions
                || snapshot.greenQueue() < queueThreshold
                || snapshot.greenQueue() < snapshot.redQueue()) {
            return SignalDecision.NO_CHANGE;
        }
        return SignalDecision.extend(extensionTicks,
                "green queue " + snapshot.greenQueue()
                        + " >= red queue " + snapshot.redQueue());
    }

    @Override
    public String displayName() {
        return "QUEUE-RESPONSIVE";
    }
}
