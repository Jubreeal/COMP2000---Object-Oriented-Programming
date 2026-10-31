package trafficsim.analysis;

import trafficsim.SimConstants;
import trafficsim.model.road.Intersection;
import trafficsim.model.road.Lane;
import trafficsim.model.road.Road;
import trafficsim.model.vehicle.Vehicle;
import trafficsim.util.Direction;

import java.util.List;
import java.util.Objects;

/**
 * Immutable traffic data for one lane approaching one intersection.
 */
public record ApproachSnapshot(
        int x1,
        int y1,
        int x2,
        int y2,
        int intersectionX,
        int intersectionY,
        Direction direction,
        List<VehicleSnapshot> vehicles) {

    private static final double APPROACH_LENGTH = 120.0;

    public ApproachSnapshot {
        Objects.requireNonNull(direction, "Approach direction cannot be null.");
        vehicles = List.copyOf(
                Objects.requireNonNull(vehicles, "Approach vehicles cannot be null."));
    }

    public static ApproachSnapshot from(
            Road road,
            Lane lane,
            Intersection intersection) {
        Objects.requireNonNull(road, "Road cannot be null.");
        Objects.requireNonNull(lane, "Lane cannot be null.");
        Objects.requireNonNull(intersection, "Intersection cannot be null.");

        Direction direction = lane.getDirection();
        int intersectionX = intersection.getX();
        int intersectionY = intersection.getY();
        int x1 = clamp(
                intersectionX - direction.dx() * APPROACH_LENGTH,
                road.getX1(),
                road.getX2());
        int y1 = clamp(
                intersectionY - direction.dy() * APPROACH_LENGTH,
                road.getY1(),
                road.getY2());
        int x2 = clamp(
                intersectionX - direction.dx()
                        * SimConstants.INTERSECTION_TILE_RADIUS,
                road.getX1(),
                road.getX2());
        int y2 = clamp(
                intersectionY - direction.dy()
                        * SimConstants.INTERSECTION_TILE_RADIUS,
                road.getY1(),
                road.getY2());

        List<VehicleSnapshot> approachingVehicles = lane.getVehicles().stream()
                .filter(vehicle -> isWithinApproach(
                        vehicle,
                        intersectionX,
                        intersectionY,
                        direction))
                .map(VehicleSnapshot::from)
                .toList();

        return new ApproachSnapshot(
                x1,
                y1,
                x2,
                y2,
                intersectionX,
                intersectionY,
                direction,
                approachingVehicles);
    }

    private static boolean isWithinApproach(
            Vehicle vehicle,
            int intersectionX,
            int intersectionY,
            Direction direction) {
        double distanceAhead = (intersectionX - vehicle.getX()) * direction.dx()
                + (intersectionY - vehicle.getY()) * direction.dy();
        return distanceAhead >= SimConstants.INTERSECTION_TILE_RADIUS
                && distanceAhead <= APPROACH_LENGTH;
    }

    private static int clamp(double value, int endpoint1, int endpoint2) {
        int minimum = Math.min(endpoint1, endpoint2);
        int maximum = Math.max(endpoint1, endpoint2);
        return (int) Math.round(Math.max(minimum, Math.min(maximum, value)));
    }
}
