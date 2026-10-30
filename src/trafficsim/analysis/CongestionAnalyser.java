package trafficsim.analysis;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

/** Converts immutable network snapshots into ordered congestion reports. */
public final class CongestionAnalyser {

    private static final double HIGH_QUEUE_REFERENCE = 4.0;
    private static final double QUEUE_PRESSURE_WEIGHT = 0.60;
    private static final double QUEUED_FRACTION_WEIGHT = 0.40;
    private static final double MODERATE_SCORE = 30.0;
    private static final double HIGH_SCORE = 60.0;

    private static final Comparator<RoadCongestion> MOST_CONGESTED_FIRST =
            Comparator.comparingDouble(RoadCongestion::score)
                    .reversed()
                    .thenComparingInt(RoadCongestion::x1)
                    .thenComparingInt(RoadCongestion::y1)
                    .thenComparingInt(RoadCongestion::x2)
                    .thenComparingInt(RoadCongestion::y2);

    public CongestionReport analyse(NetworkSnapshot snapshot) {
        return analyse(snapshot, false);
    }

    public CongestionReport analyseParallel(NetworkSnapshot snapshot) {
        return analyse(snapshot, true);
    }

    private CongestionReport analyse(NetworkSnapshot snapshot, boolean parallel) {
        Objects.requireNonNull(snapshot, "Network snapshot cannot be null.");

        Stream<RoadSnapshot> roadStream = parallel
                ? snapshot.roads().parallelStream()
                : snapshot.roads().stream();

        var roadResults = roadStream
                .map(this::analyseRoad)
                .sorted(MOST_CONGESTED_FIRST)
                .toList();

        Stream<VehicleSnapshot> vehicleStream = snapshot.vehicles();
        if (parallel) {
            vehicleStream = vehicleStream.parallel();
        }
        TrafficStatistics networkStatistics = vehicleStream
                .collect(CongestionCollector.toTrafficStatistics());

        return new CongestionReport(
                snapshot.tick(),
                networkStatistics,
                roadResults);
    }

    private RoadCongestion analyseRoad(RoadSnapshot road) {
        TrafficStatistics statistics = road.vehicles().stream()
                .collect(CongestionCollector.toTrafficStatistics());

        double queuePressure = Math.min(
                1.0,
                statistics.queuedVehicleCount() / HIGH_QUEUE_REFERENCE);
        double score = 100.0 * (
                QUEUE_PRESSURE_WEIGHT * queuePressure
                        + QUEUED_FRACTION_WEIGHT * statistics.queuedFraction());

        return new RoadCongestion(
                road.x1(),
                road.y1(),
                road.x2(),
                road.y2(),
                statistics,
                score,
                levelFor(score));
    }

    private CongestionLevel levelFor(double score) {
        if (score >= HIGH_SCORE) {
            return CongestionLevel.HIGH;
        }
        if (score >= MODERATE_SCORE) {
            return CongestionLevel.MODERATE;
        }
        return CongestionLevel.LOW;
    }
}
