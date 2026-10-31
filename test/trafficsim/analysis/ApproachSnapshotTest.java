package trafficsim.analysis;

import trafficsim.model.light.TrafficLight;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.road.SignalisedIntersection;
import trafficsim.model.vehicle.Car;
import trafficsim.util.Axis;
import trafficsim.util.Direction;

import java.util.List;

/** Checks that snapshots contain only vehicles near an intersection approach. */
public final class ApproachSnapshotTest {

    private ApproachSnapshotTest() {}

    public static void main(String[] args) {
        RoadNetwork network = new RoadNetwork();
        Road road = new Road(
                0, 100, 500, 100, List.of(Direction.EAST));
        road.getLanes().get(0).addVehicle(
                new Car(10, 91, Direction.EAST));
        road.getLanes().get(0).addVehicle(
                new Car(150, 91, Direction.EAST));

        SignalisedIntersection intersection = new SignalisedIntersection(
                250,
                100,
                new TrafficLight(50, 30, 3, Axis.HORIZONTAL));
        intersection.connect(road);
        network.addRoad(road);
        network.addIntersection(intersection);

        NetworkSnapshot snapshot = NetworkSnapshot.capture(network, 0);
        ApproachSnapshot approach = snapshot.approaches().get(0);

        require(snapshot.approaches().size() == 1,
                "One lane and intersection should produce one approach.");
        require(approach.vehicles().size() == 1,
                "A vehicle far from the intersection should not affect its approach.");
        require(approach.vehicles().get(0).x() == 150.0,
                "The vehicle within 120 units should be captured.");
        require(approach.x1() == 130 && approach.x2() == 226,
                "The coloured segment should stop before the intersection.");

        System.out.println("ApproachSnapshotTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
