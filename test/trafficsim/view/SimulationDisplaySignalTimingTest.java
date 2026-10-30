package trafficsim.view;

import trafficsim.engine.SimulationEngine;
import trafficsim.engine.VehicleSpawner;
import trafficsim.model.road.RoadNetwork;
import trafficsim.strategy.SignalTimingMode;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Random;

/** Headless interaction check for the signal-timing control. */
public final class SimulationDisplaySignalTimingTest {

    private SimulationDisplaySignalTimingTest() {}

    public static void main(String[] args) {
        SimulationEngine engine = new SimulationEngine(
                new RoadNetwork(),
                30,
                new VehicleSpawner(new Random(19), 0.0),
                19L);
        SimulationDisplay display = new SimulationDisplay(engine);

        JButton timingButton = Arrays.stream(display.getComponents())
                .filter(JButton.class::isInstance)
                .filter(component -> "signal-timing-control".equals(
                        component.getName()))
                .map(JButton.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "The timing control should be visible on the display."));

        require(timingButton.getText().contains("ADAPTIVE"),
                "The control should show the initial adaptive mode.");
        timingButton.doClick();
        require(engine.getSignalTimingMode() == SignalTimingMode.FIXED_TIME,
                "Clicking the control should switch the engine to fixed timing.");
        require(timingButton.getText().contains("FIXED TIME"),
                "The control label should show the newly selected mode.");

        Object timingAction = display.getInputMap(
                        JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0));
        require(timingAction != null,
                "The T key should be bound to the timing control.");
        display.getActionMap().get(timingAction).actionPerformed(
                new ActionEvent(display, ActionEvent.ACTION_PERFORMED, "T"));
        require(engine.getSignalTimingMode()
                        == SignalTimingMode.ADAPTIVE_QUEUE_PRESSURE,
                "Pressing T should switch the engine back to adaptive timing.");

        System.out.println("SimulationDisplaySignalTimingTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
