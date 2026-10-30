package trafficsim.strategy.signal;

/**
 * Strategy for changing the duration of a traffic signal's active phase.
 * Implementations are deliberately stateless; per-intersection state remains in
 * the intersection context that owns the light.
 */
public interface SignalTimingStrategy {
    SignalDecision decide(SignalSnapshot snapshot);
    String displayName();
}
