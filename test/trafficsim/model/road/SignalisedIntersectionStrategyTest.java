package trafficsim.model.road;

import trafficsim.model.light.TrafficLight;
import trafficsim.model.vehicle.Car;
import trafficsim.strategy.SignalTimingStrategies;
import trafficsim.strategy.SignalTimingStrategy;
import trafficsim.util.Axis;
import trafficsim.util.Direction;

import java.util.List;

/** Integration checks for interchangeable intersection timing strategies. */
public final class SignalisedIntersectionStrategyTest {

    private SignalisedIntersectionStrategyTest() {}

    public static void main(String[] args) {
        fixedTimeLeavesTheTimerUnchanged();
        adaptiveQueuePressureExtendsTheGreen();
        strategyCanBeChangedAtRuntime();
        System.out.println("SignalisedIntersectionStrategyTest passed");
    }

    private static void fixedTimeLeavesTheTimerUnchanged() {
        TrafficLight light = new TrafficLight(5, 2, 1, Axis.HORIZONTAL);
        SignalisedIntersection intersection = busyIntersection(
                light,
                SignalTimingStrategies.fixedTime());

        intersection.update();

        require(light.getTimer() == 1,
                "Fixed-time strategy should leave the updated timer unchanged.");
    }

    private static void adaptiveQueuePressureExtendsTheGreen() {
        TrafficLight light = new TrafficLight(5, 2, 1, Axis.HORIZONTAL);
        SignalisedIntersection intersection = busyIntersection(
                light,
                SignalTimingStrategies.adaptiveQueuePressure());

        intersection.update();

        require(light.getTimer() == 0,
                "Adaptive strategy should rewind the timer for a busy green axis.");
    }

    private static void strategyCanBeChangedAtRuntime() {
        TrafficLight light = new TrafficLight(5, 2, 1, Axis.HORIZONTAL);
        SignalisedIntersection intersection = busyIntersection(
                light,
                SignalTimingStrategies.adaptiveQueuePressure());
        intersection.setTimingStrategy(SignalTimingStrategies.fixedTime());

        intersection.update();

        require(light.getTimer() == 1,
                "Changing to fixed-time should immediately change the intersection behaviour.");
    }

    private static SignalisedIntersection busyIntersection(
            TrafficLight light,
            SignalTimingStrategy strategy) {
        SignalisedIntersection intersection = new SignalisedIntersection(
                100,
                100,
                light,
                strategy);
        Road road = new Road(
                0,
                100,
                200,
                100,
                List.of(Direction.EAST));

        for (int index = 0; index < 4; index++) {
            road.getLanes().get(0).addVehicle(
                    new Car(55 + index * 10, 91, Direction.EAST));
        }
        intersection.connect(road);
        return intersection;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
