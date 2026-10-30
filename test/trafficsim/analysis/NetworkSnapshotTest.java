package trafficsim.analysis;

import trafficsim.engine.SensorReading;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.vehicle.Car;
import trafficsim.util.Direction;

import java.util.List;

/** Simple standard-Java checks that require no external test library. */
public final class NetworkSnapshotTest {

    private NetworkSnapshotTest() {}

    public static void main(String[] args) {
        RoadNetwork network = new RoadNetwork();
        Road road = new Road(0, 0, 100, 0, List.of(Direction.EAST));
        Car car = new Car(10, 0, Direction.EAST);
        road.getLanes().get(0).addVehicle(car);
        network.addRoad(road);

        NetworkSnapshot beforeMove = NetworkSnapshot.capture(network, 0);
        car.move(SensorReading.clear());
        NetworkSnapshot afterMove = NetworkSnapshot.capture(network, 1);

        VehicleSnapshot original = beforeMove.vehicles().findFirst().orElseThrow();
        VehicleSnapshot moved = afterMove.vehicles().findFirst().orElseThrow();

        require(original.x() == 10.0, "Original position should be preserved.");
        require(original.speed() == 0.0, "Original speed should be preserved.");
        require(moved.x() > original.x(), "A later snapshot should see movement.");
        require(moved.speed() > original.speed(), "A later snapshot should see acceleration.");
        require(beforeMove.roads().size() == 1, "Snapshot should contain the road.");

        boolean unmodifiable = false;
        try {
            beforeMove.roads().clear();
        } catch (UnsupportedOperationException expected) {
            unmodifiable = true;
        }
        require(unmodifiable, "Snapshot road list should be unmodifiable.");

        System.out.println("NetworkSnapshotTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
