package trafficsim.model.road;

import trafficsim.SimConstants;
import trafficsim.model.light.TrafficLight;
import trafficsim.strategy.SignalTimingContext;
import trafficsim.strategy.SignalTimingStrategies;
import trafficsim.strategy.SignalTimingStrategy;
import trafficsim.util.Axis;
import trafficsim.util.LightPhase;

import java.util.Objects;

/**
 * 4-way stop-and-go intersection controlled by a {@link TrafficLight}, with
 * light-touch adaptive green extension: if a burst of vehicles is queued on
 * the currently-green axis, extend the green phase a few ticks before yellow.
 */
public final class SignalisedIntersection extends Intersection {

    private final TrafficLight light;
    private SignalTimingStrategy timingStrategy;
    private int extensionsThisPhase = 0;

    public SignalisedIntersection(int x, int y, TrafficLight light) {
        this(
                x,
                y,
                light,
                SignalTimingStrategies.adaptiveQueuePressure());
    }

    public SignalisedIntersection(
            int x,
            int y,
            TrafficLight light,
            SignalTimingStrategy timingStrategy) {
        super(x, y);
        this.light = Objects.requireNonNull(light, "Traffic light cannot be null.");
        this.timingStrategy = Objects.requireNonNull(
                timingStrategy,
                "Signal timing strategy cannot be null.");
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
        int remaining = light.getGreenDuration() - light.getTimer();
        Axis greenAxis = light.getGreenAxis();
        SignalTimingContext context = new SignalTimingContext(
                light.getPhase(),
                remaining,
                extensionsThisPhase,
                queuedOnAxis(greenAxis),
                queuedOnAxis(greenAxis.other()));

        if (!timingStrategy.shouldExtendGreen(context)) {
            return;
        }
        // Rewind the timer by the extension length so the state machine sees "still fresh green".
        light.extendCurrentPhase(SimConstants.ADAPTIVE_EXTEND_TICKS);
        extensionsThisPhase++;
    }

    /** Count nearby queued vehicles travelling on the supplied road axis. */
    private int queuedOnAxis(Axis axis) {
        return (int) connectedRoads.stream()
                .flatMap(road -> road.getLanes().stream())
                .filter(lane -> Axis.of(lane.getDirection()) == axis)
                .flatMap(lane -> lane.getVehicles().stream())
                .filter(vehicle -> vehicle.getSpeed()
                        <= SimConstants.QUEUED_SPEED_THRESHOLD)
                .filter(vehicle -> Math.hypot(
                        vehicle.getX() - x,
                        vehicle.getY() - y) < SimConstants.ADAPTIVE_QUEUE_RADIUS)
                .count();
    }

    public void setTimingStrategy(SignalTimingStrategy timingStrategy) {
        this.timingStrategy = Objects.requireNonNull(
                timingStrategy,
                "Signal timing strategy cannot be null.");
    }
}
