package trafficsim.analysis;

import trafficsim.model.road.RoadNetwork;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Immutable view of the road network at the end of one simulation tick.
 * Analysis can safely process this data without reading mutable lane lists.
 */
public record NetworkSnapshot(long tick, List<RoadSnapshot> roads) {

    public NetworkSnapshot {
        if (tick < 0) {
            throw new IllegalArgumentException("Tick cannot be negative.");
        }
        roads = List.copyOf(
                Objects.requireNonNull(roads, "Roads cannot be null."));
    }

    /**
     * Copies the live model while holding the same lock used by the engine and
     * display, ensuring that all values come from one complete simulation state.
     */
    public static NetworkSnapshot capture(RoadNetwork network, long tick) {
        Objects.requireNonNull(network, "Road network cannot be null.");

        synchronized (network) {
            List<RoadSnapshot> roads = network.getRoads().stream()
                    .map(RoadSnapshot::from)
                    .toList();
            return new NetworkSnapshot(tick, roads);
        }
    }

    public Stream<VehicleSnapshot> vehicles() {
        return roads.stream().flatMap(road -> road.vehicles().stream());
    }
}
