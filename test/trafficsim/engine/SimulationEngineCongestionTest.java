package trafficsim.engine;

import trafficsim.analysis.CongestionReport;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.vehicle.Car;
import trafficsim.util.Direction;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/** Integration checks for congestion reporting in the simulation engine. */
public final class SimulationEngineCongestionTest {

    private SimulationEngineCongestionTest() {}

    public static void main(String[] args) {
        RoadNetwork network = networkWithOneCar();
        VehicleSpawner spawner = new VehicleSpawner(new Random(7), 0.0);
        SimulationEngine engine = new SimulationEngine(network, 30, spawner, 7L);

        CongestionReport initial = engine.getLatestCongestionReport();
        require(initial.tick() == 0, "The initial report should represent tick zero.");
        require(initial.networkStatistics().vehicleCount() == 1,
                "The initial report should include the existing car.");

        AtomicReference<CongestionReport> observedReport = new AtomicReference<>();
        engine.addObserver(() -> observedReport.set(engine.getLatestCongestionReport()));
        engine.step();

        CongestionReport updated = engine.getLatestCongestionReport();
        require(updated.tick() == 1, "The report should advance with the engine tick.");
        require(updated.networkStatistics().vehicleCount() == 1,
                "The updated report should retain the car.");
        require(updated.networkStatistics().averageSpeed() > 0.0,
                "The updated report should see the car accelerate.");
        require(observedReport.get() == updated,
                "Observers should receive the completed report for the current tick.");

        System.out.println("SimulationEngineCongestionTest passed");
    }

    private static RoadNetwork networkWithOneCar() {
        RoadNetwork network = new RoadNetwork();
        Road horizontal = new Road(
                0, 50, 100, 50, List.of(Direction.EAST));
        Road vertical = new Road(
                50, 0, 50, 100, List.of(Direction.SOUTH));
        horizontal.getLanes().get(0).addVehicle(
                new Car(10, 50, Direction.EAST));
        network.addRoad(horizontal);
        network.addRoad(vertical);
        return network;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
