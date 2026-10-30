package trafficsim.model.road;

import trafficsim.SimConstants;
import trafficsim.util.Axis;
import trafficsim.util.LightPhase;
import trafficsim.model.light.TrafficLight;
import trafficsim.model.vehicle.Vehicle;
import trafficsim.strategy.signal.QueueResponsiveSignalStrategy;
import trafficsim.strategy.signal.SignalDecision;
import trafficsim.strategy.signal.SignalSnapshot;
import trafficsim.strategy.signal.SignalTimingStrategy;

import java.util.Objects;

/**
 * 4-way stop-and-go intersection controlled by a {@link TrafficLight}, with
 * light-touch adaptive green extension: if a burst of vehicles is queued on
 * the currently-green axis, extend the green phase a few ticks before yellow.
 */
public final class SignalisedIntersection extends Intersection {

    private final TrafficLight light;
    private int extensionsThisPhase = 0;
    private SignalTimingStrategy timingStrategy;

    public SignalisedIntersection(int x, int y, TrafficLight light) {
        this(x, y, light, new QueueResponsiveSignalStrategy(
                SimConstants.ADAPTIVE_DECISION_WINDOW,
                SimConstants.ADAPTIVE_QUEUE_HIGH,
                SimConstants.ADAPTIVE_EXTEND_TICKS,
                SimConstants.ADAPTIVE_MAX_EXTENSIONS));
    }

    public SignalisedIntersection(
            int x, int y, TrafficLight light, SignalTimingStrategy timingStrategy) {
        super(x, y);
        this.light = Objects.requireNonNull(light, "Traffic light cannot be null");
        this.timingStrategy = Objects.requireNonNull(
                timingStrategy, "Signal timing strategy cannot be null");
    }

    @Override
    public void update() {
        light.update();
        maybeExtendGreen();
    }

    @Override
    public TrafficLight getLight() {
        return light;
    }

    public void setTimingStrategy(SignalTimingStrategy timingStrategy) {
        this.timingStrategy = Objects.requireNonNull(
                timingStrategy, "Signal timing strategy cannot be null");
        extensionsThisPhase = 0;
    }

    public SignalTimingStrategy getTimingStrategy() {
        return timingStrategy;
    }

    /**
     * If we're near the end of a green phase and there's a busy queue on that
     * axis, reset the timer to buy a few extra ticks. Capped by
     * {@link SimConstants#ADAPTIVE_MAX_EXTENSIONS} per phase so the perpendicular
     * axis eventually gets its turn.
     */
    private void maybeExtendGreen() {
        if (light.getPhase() != LightPhase.GREEN) {
            extensionsThisPhase = 0;
            return;
        }
        int remaining = Math.max(0, light.getGreenDuration() - light.getTimer());
        Axis green = light.getGreenAxis();
        SignalSnapshot snapshot = new SignalSnapshot(
                light.getPhase(),
                green,
                remaining,
                queuedOnAxis(green),
                queuedOnAxis(green.other()),
                extensionsThisPhase);
        SignalDecision decision = timingStrategy.decide(snapshot);
        if (decision.extensionTicks() > 0) {
            // Rewind the timer so the State machine remains green for the policy's extension.
            light.extendCurrentPhase(decision.extensionTicks());
            extensionsThisPhase++;
        }
    }

    /** Count vehicles queued on connected roads whose direction matches an axis. */
    private int queuedOnAxis(Axis axis) {
        int count = 0;
        for (Road road : connectedRoads) {
            for (Lane lane : road.getLanes()) {
                if (Axis.of(lane.getDirection()) != axis) continue;
                for (Vehicle v : lane.getVehicles()) {
                    if (v.getSpeed() > 0.5) continue;
                    // Count only stopped vehicles close enough to be waiting for this junction.
                    double dx = v.getX() - x, dy = v.getY() - y;
                    if (Math.hypot(dx, dy) < 180) count++; // scaled for enlarged map
                }
            }
        }
        return count;
    }
}
