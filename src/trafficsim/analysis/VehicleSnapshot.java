package trafficsim.analysis;

import trafficsim.model.vehicle.Vehicle;
import trafficsim.util.Direction;

import java.util.Objects;

/**
 * Immutable copy of the vehicle data needed for traffic-flow analysis.
 * The live {@link Vehicle} can continue moving without changing this record.
 */
public record VehicleSnapshot(
        double x,
        double y,
        double speed,
        Direction direction) {

    public static VehicleSnapshot from(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle cannot be null.");
        return new VehicleSnapshot(
                vehicle.getX(),
                vehicle.getY(),
                vehicle.getSpeed(),
                vehicle.getDirection());
    }
}
