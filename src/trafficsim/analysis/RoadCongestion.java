package trafficsim.analysis;

import java.util.Objects;

/** Congestion measurements and classification for one road. */
public record RoadCongestion(
        int x1,
        int y1,
        int x2,
        int y2,
        TrafficStatistics statistics,
        double score,
        CongestionLevel level) {

    public RoadCongestion {
        Objects.requireNonNull(statistics, "Traffic statistics cannot be null.");
        Objects.requireNonNull(level, "Congestion level cannot be null.");
        if (!Double.isFinite(score) || score < 0.0 || score > 100.0) {
            throw new IllegalArgumentException(
                    "Congestion score must be between 0 and 100.");
        }
    }
}
