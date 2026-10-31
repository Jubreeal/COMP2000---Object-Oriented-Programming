package trafficsim.analysis;

import trafficsim.util.Direction;

import java.util.Objects;

/** Congestion measurements for one road approach to an intersection. */
public record ApproachCongestion(
        int x1,
        int y1,
        int x2,
        int y2,
        int intersectionX,
        int intersectionY,
        Direction direction,
        TrafficStatistics statistics,
        double score,
        CongestionLevel level) {

    public ApproachCongestion {
        Objects.requireNonNull(direction, "Approach direction cannot be null.");
        Objects.requireNonNull(statistics, "Traffic statistics cannot be null.");
        Objects.requireNonNull(level, "Congestion level cannot be null.");
        if (!Double.isFinite(score) || score < 0.0 || score > 100.0) {
            throw new IllegalArgumentException(
                    "Congestion score must be between 0 and 100.");
        }
    }
}
