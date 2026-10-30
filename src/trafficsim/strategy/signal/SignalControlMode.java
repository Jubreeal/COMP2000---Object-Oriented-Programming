package trafficsim.strategy.signal;

import trafficsim.SimConstants;

/** User-selectable signal policies used by the live comparison experiment. */
public enum SignalControlMode {
    FIXED {
        @Override
        public SignalTimingStrategy createStrategy() {
            return new FixedTimeSignalStrategy();
        }
    },
    ADAPTIVE {
        @Override
        public SignalTimingStrategy createStrategy() {
            return new QueueResponsiveSignalStrategy(
                    SimConstants.ADAPTIVE_DECISION_WINDOW,
                    SimConstants.ADAPTIVE_QUEUE_HIGH,
                    SimConstants.ADAPTIVE_EXTEND_TICKS,
                    SimConstants.ADAPTIVE_MAX_EXTENSIONS);
        }
    };

    public abstract SignalTimingStrategy createStrategy();

    public SignalControlMode other() {
        return this == FIXED ? ADAPTIVE : FIXED;
    }
}
