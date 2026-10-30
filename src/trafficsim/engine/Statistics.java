package trafficsim.engine;

import trafficsim.model.vehicle.Vehicle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;

public final class Statistics {

    private Statistics() {}

    /**
     * Calculate all dashboard measures in one traversal. A custom collector is
     * preferable to separate stream pipelines because the accumulation rules live
     * in one reusable unit and only one pass over the vehicles is required.
     *
     * <p>The collector includes a combiner and is therefore structurally ready for
     * a parallel stream. The application deliberately uses a sequential stream:
     * the live {@link Vehicle} objects are mutable, protected by the network lock,
     * and the normal population is too small to repay parallel-stream overhead.</p>
     */
    public static TrafficSummary summarize(List<Vehicle> vehicles) {
        return vehicles.stream().collect(summaryCollector());
    }

    private static Collector<Vehicle, MutableSummary, TrafficSummary> summaryCollector() {
        return Collector.of(
                MutableSummary::new,
                MutableSummary::accept,
                MutableSummary::combine,
                MutableSummary::finish);
    }

    private static final class MutableSummary {
        private int count;
        private long stopped;
        private double speedTotal;
        private final Map<String, Long> vehicleTypes = new HashMap<>();

        private void accept(Vehicle vehicle) {
            count++;
            speedTotal += vehicle.getSpeed();
            if (vehicle.getSpeed() <= 0.01) stopped++;
            vehicleTypes.merge(vehicle.getClass().getSimpleName(), 1L, Long::sum);
        }

        private MutableSummary combine(MutableSummary other) {
            count += other.count;
            stopped += other.stopped;
            speedTotal += other.speedTotal;
            other.vehicleTypes.forEach(
                    (type, amount) -> vehicleTypes.merge(type, amount, Long::sum));
            return this;
        }

        private TrafficSummary finish() {
            double averageSpeed = count == 0 ? 0.0 : speedTotal / count;
            double stoppedPercentage = count == 0 ? 0.0 : stopped * 100.0 / count;
            TrafficSummary.CongestionLevel level = stoppedPercentage >= 65.0
                    ? TrafficSummary.CongestionLevel.GRIDLOCK
                    : stoppedPercentage >= 35.0
                    ? TrafficSummary.CongestionLevel.BUSY
                    : TrafficSummary.CongestionLevel.FLOWING;
            return new TrafficSummary(
                    count, stopped, averageSpeed, stoppedPercentage, vehicleTypes, level);
        }
    }
}
