package trafficsim;

import trafficsim.engine.SensorReading;
import trafficsim.engine.Statistics;
import trafficsim.engine.TrafficSummary;
import trafficsim.engine.SimulationEngine;
import trafficsim.engine.VehicleSpawner;
import trafficsim.factory.NetworkLoader;
import trafficsim.model.road.SignalisedIntersection;
import trafficsim.model.vehicle.Car;
import trafficsim.model.vehicle.Truck;
import trafficsim.model.vehicle.Vehicle;
import trafficsim.strategy.signal.FixedTimeSignalStrategy;
import trafficsim.strategy.signal.QueueResponsiveSignalStrategy;
import trafficsim.strategy.signal.SignalDecision;
import trafficsim.strategy.signal.SignalSnapshot;
import trafficsim.strategy.signal.SignalControlMode;
import trafficsim.util.Axis;
import trafficsim.util.Direction;
import trafficsim.util.LightPhase;

import java.util.List;
import java.util.Random;

/** Minimal standard-JRE regression tests; no external testing library is required. */
public final class Assignment2SelfTest {
    private Assignment2SelfTest() {}

    public static void main(String[] args) {
        testFixedStrategyNeverExtends();
        testAdaptiveStrategyBalancesDemandAndFairness();
        testEngineChangesEveryIntersectionStrategy();
        testCustomStatisticsCollector();
        System.out.println("Assignment 2 self-tests passed");
    }

    private static void testFixedStrategyNeverExtends() {
        SignalDecision decision = new FixedTimeSignalStrategy().decide(
                new SignalSnapshot(LightPhase.GREEN, Axis.HORIZONTAL, 1, 20, 0, 0));
        check(decision.extensionTicks() == 0, "Fixed strategy extended a phase");
    }

    private static void testAdaptiveStrategyBalancesDemandAndFairness() {
        QueueResponsiveSignalStrategy strategy =
                new QueueResponsiveSignalStrategy(4, 4, 12, 3);

        SignalDecision busyGreen = strategy.decide(
                new SignalSnapshot(LightPhase.GREEN, Axis.HORIZONTAL, 2, 7, 4, 0));
        check(busyGreen.extensionTicks() == 12, "Busy green approach was not extended");

        SignalDecision busierRed = strategy.decide(
                new SignalSnapshot(LightPhase.GREEN, Axis.HORIZONTAL, 2, 4, 8, 0));
        check(busierRed.extensionTicks() == 0, "Strategy starved a busier red approach");

        SignalDecision capped = strategy.decide(
                new SignalSnapshot(LightPhase.GREEN, Axis.HORIZONTAL, 1, 12, 0, 3));
        check(capped.extensionTicks() == 0, "Extension cap was ignored");

        SignalDecision tooEarly = strategy.decide(
                new SignalSnapshot(LightPhase.GREEN, Axis.HORIZONTAL, 8, 12, 0, 0));
        check(tooEarly.extensionTicks() == 0, "Strategy extended too early in the phase");
    }

    private static void testCustomStatisticsCollector() {
        Vehicle.seedNoise(1234L);
        Car stoppedCar = new Car(0, 0, Direction.EAST);
        Truck movingTruck = new Truck(0, 0, Direction.EAST, 2_500);
        movingTruck.move(SensorReading.clear());

        TrafficSummary summary = Statistics.summarize(List.of(stoppedCar, movingTruck));
        check(summary.vehicleCount() == 2, "Collector returned the wrong vehicle count");
        check(summary.stoppedCount() == 1, "Collector returned the wrong stopped count");
        check(summary.averageSpeed() > 0.0, "Collector lost the moving vehicle's speed");
        check(summary.vehicleTypes().getOrDefault("Car", 0L) == 1L,
                "Collector did not group the car");
        check(summary.vehicleTypes().getOrDefault("Truck", 0L) == 1L,
                "Collector did not group the truck");
    }

    private static void testEngineChangesEveryIntersectionStrategy() {
        var network = NetworkLoader.buildDefault();
        var engine = new SimulationEngine(
                network, 30, new VehicleSpawner(new Random(1L), 0.0), 1L);
        engine.setSignalControlMode(SignalControlMode.FIXED);

        boolean everySignalIsFixed = network.getIntersections().stream()
                .map(SignalisedIntersection.class::cast)
                .allMatch(intersection -> intersection.getTimingStrategy()
                        instanceof FixedTimeSignalStrategy);
        check(everySignalIsFixed, "Engine did not change every signal strategy atomically");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
