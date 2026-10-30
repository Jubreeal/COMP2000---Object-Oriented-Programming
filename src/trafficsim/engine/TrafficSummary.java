package trafficsim.engine;

import java.util.Map;

/** Immutable result produced by the traffic statistics custom collector. */
public record TrafficSummary(
        int vehicleCount,
        long stoppedCount,
        double averageSpeed,
        double stoppedPercentage,
        Map<String, Long> vehicleTypes,
        CongestionLevel congestionLevel) {

    public enum CongestionLevel {
        FLOWING, BUSY, GRIDLOCK
    }

    public TrafficSummary {
        vehicleTypes = Map.copyOf(vehicleTypes);
    }
}
