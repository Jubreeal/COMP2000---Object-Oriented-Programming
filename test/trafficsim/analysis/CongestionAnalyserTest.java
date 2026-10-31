package trafficsim.analysis;

import trafficsim.util.Direction;

import java.util.Arrays;
import java.util.List;

/** Simple standard-Java checks that require no external test library. */
public final class CongestionAnalyserTest {

    private static final double TOLERANCE = 0.000_000_001;

    private CongestionAnalyserTest() {}

    public static void main(String[] args) {
        classifiesRoadCongestionUsingTheAgreedScore();
        classifiesApproachesByVisibleQueueLength();
        ordersRoadsFromHighestToLowestCongestion();
        handlesAnEmptyNetwork();
        producesTheSameSequentialAndParallelReport();
        System.out.println("CongestionAnalyserTest passed");
    }

    private static void classifiesApproachesByVisibleQueueLength() {
        CongestionReport report = new CongestionAnalyser().analyse(
                new NetworkSnapshot(
                        42,
                        List.of(),
                        List.of(
                                approach(0, speeds(0.0)),
                                approach(100, speeds(0.0, 0.0)),
                                approach(200, speeds(0.0, 0.0, 0.0, 0.0)))));

        ApproachCongestion high = report.approaches().get(0);
        ApproachCongestion moderate = report.approaches().get(1);
        ApproachCongestion low = report.approaches().get(2);

        require(high.level() == CongestionLevel.HIGH,
                "Four queued vehicles should be high congestion.");
        requireClose(high.score(), 100.0,
                "Four queued vehicles should fill the queue reference.");
        require(moderate.level() == CongestionLevel.MODERATE,
                "Two queued vehicles should be moderate congestion.");
        requireClose(moderate.score(), 50.0,
                "Two queued vehicles should score 50.");
        require(low.level() == CongestionLevel.LOW,
                "One newly stopped vehicle should remain low congestion.");
        requireClose(low.score(), 25.0,
                "One queued vehicle should score 25.");
    }

    private static void classifiesRoadCongestionUsingTheAgreedScore() {
        CongestionReport report = new CongestionAnalyser().analyse(
                networkWith(
                        road(0, speeds(0.0, 1.0, 1.0, 1.0)),
                        road(100, speeds(0.0)),
                        road(200, speeds(0.0, 0.0))));

        RoadCongestion high = report.roads().get(0);
        RoadCongestion moderate = report.roads().get(1);
        RoadCongestion low = report.roads().get(2);

        require(high.level() == CongestionLevel.HIGH,
                "Two queued vehicles out of two should be high congestion.");
        requireClose(high.score(), 70.0,
                "Two queued vehicles out of two should score 70.");
        require(moderate.level() == CongestionLevel.MODERATE,
                "One queued vehicle out of one should be moderate congestion.");
        requireClose(moderate.score(), 55.0,
                "One queued vehicle out of one should score 55.");
        require(low.level() == CongestionLevel.LOW,
                "One queued vehicle out of four should be low congestion.");
        requireClose(low.score(), 25.0,
                "One queued vehicle out of four should score 25.");
    }

    private static void ordersRoadsFromHighestToLowestCongestion() {
        CongestionReport report = new CongestionAnalyser().analyse(
                networkWith(
                        road(0, speeds(0.0, 1.0, 1.0, 1.0)),
                        road(100, speeds(0.0)),
                        road(200, speeds(0.0, 0.0))));

        require(report.roads().get(0).x1() == 200,
                "The high-congestion road should be first.");
        require(report.roads().get(1).x1() == 100,
                "The moderate-congestion road should be second.");
        require(report.roads().get(2).x1() == 0,
                "The low-congestion road should be last.");
        require(report.mostCongestedRoad().orElseThrow().x1() == 200,
                "The report should identify its most congested road.");
        require(report.networkStatistics().vehicleCount() == 7,
                "Network statistics should include vehicles from every road.");
    }

    private static void handlesAnEmptyNetwork() {
        CongestionReport report = new CongestionAnalyser().analyse(
                new NetworkSnapshot(7, List.of()));

        require(report.tick() == 7, "The report should retain the snapshot tick.");
        require(report.roads().isEmpty(), "An empty network should have no road results.");
        require(report.mostCongestedRoad().isEmpty(),
                "An empty network should not have a most-congested road.");
        require(report.networkStatistics().vehicleCount() == 0,
                "An empty network should have no vehicles.");
    }

    private static void producesTheSameSequentialAndParallelReport() {
        CongestionAnalyser analyser = new CongestionAnalyser();
        NetworkSnapshot snapshot = new NetworkSnapshot(
                42,
                List.of(
                        road(0, speeds(0.0, 0.3, 1.2, 2.0)),
                        road(100, speeds(0.0, 0.0, 0.4)),
                        road(200, speeds(0.8, 1.1, 1.7, 2.2, 2.7))),
                List.of(
                        approach(300, speeds(0.0)),
                        approach(400, speeds(0.0, 0.0, 0.2)),
                        approach(500, speeds(1.0, 1.5))));

        CongestionReport sequential = analyser.analyse(snapshot);
        CongestionReport parallel = analyser.analyseParallel(snapshot);

        require(sequential.tick() == parallel.tick(),
                "Sequential and parallel reports should retain the same tick.");
        require(sequential.networkStatistics().vehicleCount()
                        == parallel.networkStatistics().vehicleCount(),
                "Sequential and parallel network counts should match.");
        require(sequential.networkStatistics().queuedVehicleCount()
                        == parallel.networkStatistics().queuedVehicleCount(),
                "Sequential and parallel network queue counts should match.");
        requireClose(
                sequential.networkStatistics().averageSpeed(),
                parallel.networkStatistics().averageSpeed(),
                "Sequential and parallel network average speeds should match.");
        require(sequential.roads().size() == parallel.roads().size(),
                "Sequential and parallel reports should contain the same roads.");

        for (int index = 0; index < sequential.roads().size(); index++) {
            RoadCongestion sequentialRoad = sequential.roads().get(index);
            RoadCongestion parallelRoad = parallel.roads().get(index);
            require(sequentialRoad.level() == parallelRoad.level(),
                    "Sequential and parallel congestion levels should match.");
            requireClose(
                    sequentialRoad.score(),
                    parallelRoad.score(),
                    "Sequential and parallel congestion scores should match.");
        }

        require(sequential.approaches().size()
                        == parallel.approaches().size(),
                "Sequential and parallel reports should contain the same approaches.");
        for (int index = 0; index < sequential.approaches().size(); index++) {
            ApproachCongestion sequentialApproach =
                    sequential.approaches().get(index);
            ApproachCongestion parallelApproach =
                    parallel.approaches().get(index);
            require(sequentialApproach.level() == parallelApproach.level(),
                    "Sequential and parallel approach levels should match.");
            requireClose(
                    sequentialApproach.score(),
                    parallelApproach.score(),
                    "Sequential and parallel approach scores should match.");
        }
    }

    private static NetworkSnapshot networkWith(RoadSnapshot... roads) {
        return new NetworkSnapshot(42, List.of(roads));
    }

    private static RoadSnapshot road(int x, List<VehicleSnapshot> vehicles) {
        return new RoadSnapshot(x, 0, x + 80, 0, vehicles);
    }

    private static ApproachSnapshot approach(
            int x,
            List<VehicleSnapshot> vehicles) {
        return new ApproachSnapshot(
                x,
                0,
                x + 80,
                0,
                x + 100,
                0,
                Direction.EAST,
                vehicles);
    }

    private static List<VehicleSnapshot> speeds(double... speeds) {
        return Arrays.stream(speeds)
                .mapToObj(speed -> new VehicleSnapshot(
                        0.0,
                        0.0,
                        speed,
                        Direction.EAST))
                .toList();
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
