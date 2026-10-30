package trafficsim;

import trafficsim.engine.SimulationEngine;
import trafficsim.engine.Statistics;
import trafficsim.engine.TrafficSummary;
import trafficsim.engine.VehicleSpawner;
import trafficsim.factory.NetworkLoader;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.vehicle.Vehicle;
import trafficsim.strategy.signal.SignalControlMode;

import java.util.List;
import java.util.Random;

/**
 * Repeatable, headless comparison of both signal-control strategies.
 * This is an experiment runner rather than a pass/fail test: either policy may
 * perform better for a particular seed because traffic demand is stochastic.
 */
public final class SignalStrategyExperiment {
    private static final int DEFAULT_TICKS = 6_000;
    private static final int WARM_UP_TICKS = 500;

    private SignalStrategyExperiment() {}

    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 20_261_030L;
        int ticks = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_TICKS;
        if (ticks <= WARM_UP_TICKS) {
            throw new IllegalArgumentException("Ticks must exceed " + WARM_UP_TICKS);
        }

        ExperimentResult fixed = run(SignalControlMode.FIXED, seed, ticks);
        ExperimentResult adaptive = run(SignalControlMode.ADAPTIVE, seed, ticks);

        System.out.printf("Same seed %,d over %,d ticks (after %,d warm-up ticks)%n",
                seed, ticks, WARM_UP_TICKS);
        print(fixed);
        print(adaptive);
        System.out.printf("Adaptive change: speed %+.2f%%, stopped %+.2f percentage points%n",
                percentageChange(fixed.meanSpeed(), adaptive.meanSpeed()),
                adaptive.meanStoppedPercentage() - fixed.meanStoppedPercentage());
    }

    private static ExperimentResult run(SignalControlMode mode, long seed, int ticks) {
        Vehicle.seedNoise(seed ^ 0x9E3779B97F4A7C15L);
        RoadNetwork network = NetworkLoader.loadFromFile("networks/grid.txt");
        VehicleSpawner spawner = new VehicleSpawner(
                new Random(seed), SimConstants.SPAWN_PROBABILITY);
        for (int i = 0; i < 6; i++) spawner.spawnOne(network);
        SimulationEngine engine = new SimulationEngine(network, 30, spawner, seed);
        engine.setSignalControlMode(mode);

        double speedTotal = 0.0;
        double stoppedPercentageTotal = 0.0;
        int samples = 0;
        for (int tick = 0; tick < ticks; tick++) {
            engine.step();
            if (tick < WARM_UP_TICKS) continue;
            TrafficSummary summary = Statistics.summarize(allVehicles(network));
            speedTotal += summary.averageSpeed();
            stoppedPercentageTotal += summary.stoppedPercentage();
            samples++;
        }
        return new ExperimentResult(
                mode, speedTotal / samples, stoppedPercentageTotal / samples);
    }

    private static List<Vehicle> allVehicles(RoadNetwork network) {
        return network.getRoads().stream()
                .flatMap(road -> road.getLanes().stream())
                .flatMap(lane -> lane.getVehicles().stream())
                .toList();
    }

    private static double percentageChange(double baseline, double result) {
        return baseline == 0.0 ? 0.0 : (result - baseline) * 100.0 / baseline;
    }

    private static void print(ExperimentResult result) {
        System.out.printf("%-8s mean speed %.3f, mean stopped %.2f%%%n",
                result.mode(), result.meanSpeed(), result.meanStoppedPercentage());
    }

    private record ExperimentResult(
            SignalControlMode mode,
            double meanSpeed,
            double meanStoppedPercentage) {}
}
