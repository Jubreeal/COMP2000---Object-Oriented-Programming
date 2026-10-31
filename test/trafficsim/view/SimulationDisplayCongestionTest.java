package trafficsim.view;

import trafficsim.analysis.CongestionLevel;
import trafficsim.engine.SimulationEngine;
import trafficsim.engine.VehicleSpawner;
import trafficsim.model.light.TrafficLight;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.road.SignalisedIntersection;
import trafficsim.model.vehicle.Car;
import trafficsim.util.Axis;
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
                new Car(205, 341, Direction.EAST));
        road.getLanes().get(0).addVehicle(
                new Car(225, 341, Direction.EAST));
        road.getLanes().get(0).addVehicle(
                new Car(245, 341, Direction.EAST));
        road.getLanes().get(0).addVehicle(
                new Car(410, 341, Direction.EAST));
        road.getLanes().get(0).addVehicle(
                new Car(440, 341, Direction.EAST));
        road.getLanes().get(0).addVehicle(
                new Car(620, 341, Direction.EAST));

        SignalisedIntersection highIntersection = intersectionAt(300, 350);
        highIntersection.connect(road);
        SignalisedIntersection moderateIntersection = intersectionAt(500, 350);
        moderateIntersection.connect(road);
        SignalisedIntersection lowIntersection = intersectionAt(700, 350);
        lowIntersection.connect(road);
        network.addRoad(road);
        network.addIntersection(highIntersection);
        network.addIntersection(moderateIntersection);
        network.addIntersection(lowIntersection);

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

        Color renderedHighApproach = new Color(
                image.getRGB(260, 341), true);
        Color expectedHighApproach = SimulationDisplay.roadColour(
                CongestionLevel.HIGH);
        require(renderedHighApproach.equals(expectedHighApproach),
                "Three queued vehicles should render their approach as high.");

        Color renderedModerateApproach = new Color(
                image.getRGB(460, 341), true);
        Color expectedModerateApproach = SimulationDisplay.roadColour(
                CongestionLevel.MODERATE);
        require(renderedModerateApproach.equals(expectedModerateApproach),
                "Two queued vehicles should render their approach as moderate.");

        Color renderedLowApproach = new Color(
                image.getRGB(650, 341), true);
        Color expectedLowApproach = SimulationDisplay.roadColour(
                CongestionLevel.LOW);
        require(renderedLowApproach.equals(expectedLowApproach),
                "One newly stopped vehicle should leave its approach low.");

        System.out.println("SimulationDisplayCongestionTest passed");
    }

    private static SignalisedIntersection intersectionAt(int x, int y) {
        return new SignalisedIntersection(
                x,
                y,
                new TrafficLight(50, 30, 3, Axis.HORIZONTAL));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
