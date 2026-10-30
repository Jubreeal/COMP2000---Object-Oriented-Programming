package trafficsim.analysis;

/**
 * Immutable traffic measurements produced from a group of vehicle snapshots.
 */
public record TrafficStatistics(
        long vehicleCount,
        long queuedVehicleCount,
        double averageSpeed) {

    public TrafficStatistics {
        if (vehicleCount < 0) {
            throw new IllegalArgumentException("Vehicle count cannot be negative.");
        }
        if (queuedVehicleCount < 0 || queuedVehicleCount > vehicleCount) {
            throw new IllegalArgumentException(
                    "Queued vehicle count must be between zero and vehicle count.");
        }
        if (!Double.isFinite(averageSpeed) || averageSpeed < 0.0) {
            throw new IllegalArgumentException(
                    "Average speed must be a finite, non-negative value.");
        }
    }

    public double queuedFraction() {
        return vehicleCount == 0
                ? 0.0
                : (double) queuedVehicleCount / vehicleCount;
    }
}
