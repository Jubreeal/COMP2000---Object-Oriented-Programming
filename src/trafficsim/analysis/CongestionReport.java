package trafficsim.analysis;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable network-wide congestion analysis for one simulation tick. */
public record CongestionReport(
        long tick,
        TrafficStatistics networkStatistics,
        List<RoadCongestion> roads) {

    public CongestionReport {
        if (tick < 0) {
            throw new IllegalArgumentException("Tick cannot be negative.");
        }
        Objects.requireNonNull(
                networkStatistics,
                "Network statistics cannot be null.");
        roads = List.copyOf(
                Objects.requireNonNull(roads, "Road congestion results cannot be null."));
    }

    /** Results are ordered from highest to lowest congestion. */
    public Optional<RoadCongestion> mostCongestedRoad() {
        return roads.stream().findFirst();
    }
}
