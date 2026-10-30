package trafficsim.view;

import trafficsim.analysis.CongestionLevel;
import trafficsim.engine.SimulationEngine;
import trafficsim.engine.VehicleSpawner;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.vehicle.Car;
import trafficsim.util.Direction;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/** Off-screen rendering checks for the congestion heatmap. */
public final class SimulationDisplayCongestionTest {

    private SimulationDisplayCongestionTest() {}

    public static void main(String[] args) throws IOException {
        RoadNetwork network = new RoadNetwork();
        Road road = new Road(
                200, 350, 800, 350, List.of(Direction.EAST));
        road.getLanes().get(0).addVehicle(
                new Car(250, 341, Direction.EAST));
        network.addRoad(road);

        SimulationEngine engine = new SimulationEngine(
                network,
                30,
                new VehicleSpawner(new Random(11), 0.0),
                11L);
        SimulationDisplay display = new SimulationDisplay(engine);
        display.setSize(1100, 700);

        BufferedImage image = new BufferedImage(
                1100, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        display.paint(graphics);
        graphics.dispose();

        if (args.length == 1) {
            ImageIO.write(image, "png", new File(args[0]));
        }

        Color renderedRoad = new Color(image.getRGB(500, 358), true);
        Color expectedRoad = SimulationDisplay.roadColour(
                CongestionLevel.MODERATE);
        require(renderedRoad.equals(expectedRoad),
                "One queued vehicle should render the road in the moderate colour.");

        System.out.println("SimulationDisplayCongestionTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
