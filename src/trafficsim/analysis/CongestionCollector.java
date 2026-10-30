package trafficsim.analysis;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Collects immutable vehicle snapshots into traffic statistics.
 *
 * <p>Each stream partition receives a separate {@link Accumulator}. The
 * combiner merges partial values, so the collector does not share mutable
 * state between worker threads and is safe to use with a parallel stream.
 */
public final class CongestionCollector implements Collector<
        VehicleSnapshot,
        CongestionCollector.Accumulator,
        TrafficStatistics> {

    public static final double QUEUED_SPEED_THRESHOLD = 0.5;

    public static Collector<VehicleSnapshot, ?, TrafficStatistics> toTrafficStatistics() {
        return new CongestionCollector();
    }

    private CongestionCollector() {}

    @Override
    public Supplier<Accumulator> supplier() {
        return Accumulator::new;
    }

    @Override
    public BiConsumer<Accumulator, VehicleSnapshot> accumulator() {
        return Accumulator::add;
    }

    @Override
    public BinaryOperator<Accumulator> combiner() {
        return Accumulator::combine;
    }

    @Override
    public Function<Accumulator, TrafficStatistics> finisher() {
        return Accumulator::finish;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(Characteristics.UNORDERED);
    }

    public static final class Accumulator {
        private long vehicleCount;
        private long queuedVehicleCount;
        private double totalSpeed;

        private Accumulator() {}

        private void add(VehicleSnapshot vehicle) {
            vehicleCount++;
            totalSpeed += vehicle.speed();
            if (vehicle.speed() <= QUEUED_SPEED_THRESHOLD) {
                queuedVehicleCount++;
            }
        }

        private Accumulator combine(Accumulator other) {
            vehicleCount += other.vehicleCount;
            queuedVehicleCount += other.queuedVehicleCount;
            totalSpeed += other.totalSpeed;
            return this;
        }

        private TrafficStatistics finish() {
            double averageSpeed = vehicleCount == 0
                    ? 0.0
                    : totalSpeed / vehicleCount;
            return new TrafficStatistics(
                    vehicleCount,
                    queuedVehicleCount,
                    averageSpeed);
        }
    }
}
