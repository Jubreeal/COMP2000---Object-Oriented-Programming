package trafficsim.engine;

import trafficsim.model.light.TrafficLight;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.road.SignalisedIntersection;
import trafficsim.model.vehicle.Car;
import trafficsim.strategy.SignalTimingMode;
import trafficsim.util.Axis;
import trafficsim.util.Direction;

import java.util.List;
import java.util.Random;

/** Integration checks for changing signal timing across a running network. */
public final class SimulationEngineSignalTimingTest {

    private SimulationEngineSignalTimingTest() {}

    public static void main(String[] args) {
        RoadNetwork network = new RoadNetwork();
        TrafficLight light = new TrafficLight(5, 2, 1, Axis.HORIZONTAL);
        SignalisedIntersection intersection = new SignalisedIntersection(
                100, 100, light);
        Road road = new Road(
                0, 100, 200, 100, List.of(Direction.EAST));
        for (int index = 0; index < 4; index++) {
            road.getLanes().get(0).addVehicle(
                    new Car(55 + index * 10, 91, Direction.EAST));
        }
        intersection.connect(road);
        network.addRoad(road);
        network.addIntersection(intersection);

        SimulationEngine engine = new SimulationEngine(
                network,
                30,
                new VehicleSpawner(new Random(17), 0.0),
                17L,
                SignalTimingMode.FIXED_TIME);

        intersection.update();
        require(light.getTimer() == 1,
                "The engine should apply fixed timing to its intersections.");

        engine.setSignalTimingMode(
                SignalTimingMode.ADAPTIVE_QUEUE_PRESSURE);
        intersection.update();
        require(light.getTimer() == 0,
                "Changing engine mode should immediately enable adaptive timing.");
        require(engine.getSignalTimingMode()
                        == SignalTimingMode.ADAPTIVE_QUEUE_PRESSURE,
                "The engine should report the active timing mode.");

        System.out.println("SimulationEngineSignalTimingTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
