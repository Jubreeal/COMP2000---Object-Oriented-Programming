package trafficsim.analysis;

import trafficsim.util.Direction;

import java.util.List;
import java.util.stream.IntStream;

/** Simple standard-Java checks that require no external test library. */
public final class CongestionCollectorTest {

    private static final double TOLERANCE = 0.000_000_001;

    private CongestionCollectorTest() {}

    public static void main(String[] args) {
        calculatesTrafficStatistics();
        handlesAnEmptyStream();
        producesTheSameSequentialAndParallelResult();
        System.out.println("CongestionCollectorTest passed");
    }

    private static void calculatesTrafficStatistics() {
        List<VehicleSnapshot> vehicles = List.of(
                vehicleAtSpeed(0.0),
                vehicleAtSpeed(0.4),
                vehicleAtSpeed(1.0),
                vehicleAtSpeed(2.0));

        TrafficStatistics result = vehicles.stream()
                .collect(CongestionCollector.toTrafficStatistics());

        require(result.vehicleCount() == 4, "All vehicles should be counted.");
        require(result.queuedVehicleCount() == 2,
                "Speeds at or below the threshold should be queued.");
        requireClose(result.averageSpeed(), 0.85,
                "Average speed should be calculated.");
        requireClose(result.queuedFraction(), 0.5,
                "Queued fraction should be calculated.");
    }

    private static void handlesAnEmptyStream() {
        TrafficStatistics result = List.<VehicleSnapshot>of().stream()
                .collect(CongestionCollector.toTrafficStatistics());

        require(result.vehicleCount() == 0, "An empty stream should have no vehicles.");
        require(result.queuedVehicleCount() == 0,
                "An empty stream should have no queued vehicles.");
        requireClose(result.averageSpeed(), 0.0,
                "An empty stream should have zero average speed.");
        requireClose(result.queuedFraction(), 0.0,
                "An empty stream should have zero queued fraction.");
    }

    private static void producesTheSameSequentialAndParallelResult() {
        List<VehicleSnapshot> vehicles = IntStream.range(0, 10_000)
                .mapToObj(index -> vehicleAtSpeed((index % 31) / 10.0))
                .toList();

        TrafficStatistics sequential = vehicles.stream()
                .collect(CongestionCollector.toTrafficStatistics());
        TrafficStatistics parallel = vehicles.parallelStream()
                .collect(CongestionCollector.toTrafficStatistics());

        require(sequential.vehicleCount() == parallel.vehicleCount(),
                "Sequential and parallel vehicle counts should match.");
        require(sequential.queuedVehicleCount() == parallel.queuedVehicleCount(),
                "Sequential and parallel queue counts should match.");
        requireClose(sequential.averageSpeed(), parallel.averageSpeed(),
                "Sequential and parallel average speeds should match.");
    }

    private static VehicleSnapshot vehicleAtSpeed(double speed) {
        return new VehicleSnapshot(0.0, 0.0, speed, Direction.EAST);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireClose(double actual, double expected, String message) {
        if (Math.abs(actual - expected) > TOLERANCE) {
            throw new AssertionError(
                    message + " Expected " + expected + " but was " + actual + ".");
        }
    }
}
