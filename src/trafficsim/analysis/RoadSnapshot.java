package trafficsim.analysis;

import trafficsim.model.road.Road;

import java.util.List;
import java.util.Objects;

/**
 * Immutable traffic data for one road at a particular simulation tick.
 */
public record RoadSnapshot(
        int x1,
        int y1,
        int x2,
        int y2,
        List<VehicleSnapshot> vehicles) {

    public RoadSnapshot {
        vehicles = List.copyOf(
                Objects.requireNonNull(vehicles, "Vehicles cannot be null."));
    }

    public static RoadSnapshot from(Road road) {
        Objects.requireNonNull(road, "Road cannot be null.");

        List<VehicleSnapshot> vehicles = road.getLanes().stream()
                .flatMap(lane -> lane.getVehicles().stream())
                .map(VehicleSnapshot::from)
                .toList();

        return new RoadSnapshot(
                road.getX1(),
                road.getY1(),
                road.getX2(),
                road.getY2(),
                vehicles);
    }
}
