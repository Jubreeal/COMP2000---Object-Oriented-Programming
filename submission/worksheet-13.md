# COMP2000 Worksheet 2 — Final Submission

**Student name:** Jubril Olaboopo

**GitHub repo URL:** https://github.com/Jubreeal/COMP2000---Object-Oriented-Programming

---

## 1. Design Patterns

The final simulation applies three behavioural design patterns: Strategy, State, and Observer. Strategy was the principal pattern introduced for Assignment 2, enabling traffic-signal timing policies to be selected and changed at runtime without modifying intersection logic. State encapsulates the transitions between green, yellow, and red signal phases, while Observer decouples simulation updates from the Swing display. Together, these patterns improve separation of responsibilities, extensibility, and testability.

The accompanying `assignment-2-pattern-diagram.pdf` identifies the participants in each pattern and illustrates their relationships within the simulation.

### 1.1. Which design pattern(s) did you implement? Name them and state where each appears in your codebase.

1. **Strategy Pattern — traffic-signal timing:** The strategy interface is defined in `src/trafficsim/strategy/SignalTimingStrategy.java`. Its fixed-time and adaptive queue-pressure implementations are provided as lambda expressions in `SignalTimingStrategies.java`. `SignalisedIntersection.java` acts as the context: it collects the current signal phase, remaining green time, previous extensions, and queue lengths in an immutable `SignalTimingContext`, then delegates the extension decision to the selected strategy. `SignalTimingMode.java` associates each available mode with its strategy, while `SimulationEngine.java` applies the selected strategy consistently across all signalised intersections. Users can switch strategies at runtime using the `T` key or the timing-control button.

2. **State Pattern — traffic-light lifecycle:** `TrafficLight.java` acts as the context and delegates phase-specific behaviour to the current `LightState`. The state interface is defined in `LightState.java`, with `GreenState.java`, `YellowState.java`, and `RedState.java` providing the concrete implementations. Each state determines when and how the light transitions to the next phase: green changes to yellow, yellow changes to red, and red changes back to green after swapping the active road axis. This keeps transition logic within the relevant state instead of placing all phase behaviour in one large conditional inside `TrafficLight`.

3. **Observer Pattern — simulation and display communication:** `SimulationEngine.java` acts as the subject and maintains a collection of observers implementing the SimulationObserver` interface. `SimulationDisplay.java` is the concrete observer and registers with the engine to receive update notifications. After completing a simulation tick and producing the latest congestion report, the engine calls `onSimulationStep()` on each observer. The display responds by requesting a repaint, allowing the interface to reflect the latest state without coupling the simulation engine directly to Swing rendering code.
   
### 1.2. For each pattern, paste a code snippet showing the key interaction between participants.

#### Strategy — policy delegation

`SignalTimingStrategy` defines the decision that may vary, while `SignalTimingStrategies` provides the fixed-time and adaptive implementations:

```java
@FunctionalInterface
public interface SignalTimingStrategy {
    boolean shouldExtendGreen(SignalTimingContext context);
}

private static final SignalTimingStrategy FIXED_TIME = context -> false;

private static final SignalTimingStrategy ADAPTIVE_QUEUE_PRESSURE = context -> {
    Objects.requireNonNull(context, "Signal timing context cannot be null.");
    return context.phase() == LightPhase.GREEN
            && context.remainingGreenTicks()
                    <= SimConstants.ADAPTIVE_EXTENSION_WINDOW
            && context.extensionsThisPhase()
                    < SimConstants.ADAPTIVE_MAX_EXTENSIONS
            && context.queuedOnGreenAxis()
                    >= SimConstants.ADAPTIVE_QUEUE_HIGH
            && context.queuedOnGreenAxis() > context.queuedOnRedAxis();
};
```

`SignalisedIntersection` creates an immutable context from the current simulation state and delegates the decision to its selected strategy:

```java
SignalTimingContext context = new SignalTimingContext(
        light.getPhase(),
        remaining,
        extensionsThisPhase,
        queuedOnAxis(greenAxis),
        queuedOnAxis(greenAxis.other()));

if (!timingStrategy.shouldExtendGreen(context)) {
    return;
}

light.extendCurrentPhase(SimConstants.ADAPTIVE_EXTEND_TICKS);
extensionsThisPhase++;
```

The intersection remains responsible for observing and updating the traffic light, while the selected strategy determines whether an extension is appropriate. This is the central Strategy-pattern interaction.

#### State — phase-specific transition behaviour

`TrafficLight` delegates each update to its current `LightState`. The concrete state then determines whether a transition is required:

```java
public interface LightState {
    void update(TrafficLight light);
    LightPhase getPhase();
}

// TrafficLight context
public void update() {
    timer++;
    state.update(this);
}

// GreenState
@Override
public void update(TrafficLight light) {
    if (light.getTimer() >= light.getGreenDuration()) {
        light.setState(new YellowState());
    }
}
```

`TrafficLight` manages shared signal data such as the timer and active axis, but it does not decide the transition rules itself. Those rules belong to the current state object.

#### Observer — completed-tick notification

`SimulationObserver` defines the notification contract. After completing a simulation tick and updating its congestion report, `SimulationEngine` notifies every registered observer:

```java
public interface SimulationObserver {
    void onSimulationStep();
}

// SimulationEngine subject
private void notifyObservers() {
    for (SimulationObserver observer : observers) {
        observer.onSimulationStep();
    }
}

// SimulationDisplay concrete observer
@Override
public void onSimulationStep() {
    repaint();
}
```

The display reacts by requesting a repaint but does not control the simulation loop. This allows the engine to publish completed updates without depending directly on Swing or a specific display implementation.

### 1.3. For each pattern, explain why you chose it over an alternative. What trade-off did you make?

**Strategy:** The simplest alternative was to store a mode flag in `SignalisedIntersection` and use an `if/else` statement to choose between fixed and adaptive timing. Although shorter for two policies, this approach would mix signal-control policy with intersection mechanics and require the class to be modified whenever another policy was introduced. Subclassing `SignalisedIntersection` was also considered, but it would make runtime switching difficult and could duplicate common intersection behaviour. Strategy keeps each policy replaceable and independently testable, while the functional interface supports concise lambda implementations. The trade-off is additional abstractions and less immediately visible control flow. This was accepted because timing policies represent meaningful, extensible simulation behaviours.

**State:** An enum combined with a `switch` inside `TrafficLight.update()` could implement the current three phases using fewer classes. However, the conditional would grow as phase-specific behaviour became more complex. State places each transition rule within the relevant phase and allows new behaviours, such as pedestrian or emergency-priority phases, to be introduced without expanding one central conditional. The trade-off is a larger number of small classes and the creation of a new state object during each transition. In this simulation, that overhead is negligible compared with the clearer separation of phase responsibilities.

**Observer:** The engine could hold a direct reference to `SimulationDisplay`, but this would couple simulation logic to Swing and make the engine harder to reuse or test independently. Alternatively, the display could poll the engine using a separate timer, but this would duplicate scheduling and could request data during an incomplete tick. Observer allows the engine to publish completed updates through a small interface and supports additional consumers, such as a statistics recorder, without changing the engine. The trade-off is indirect notification flow and the need for thread awareness. Because notifications originate on the simulation thread, the display performs only the thread-safe `repaint()` request and leaves actual painting to Swing’s Event Dispatch Thread.

---
## 2. Streams, Lambdas and Parallelism

### 2.1. List every file in your project that uses lambda expressions or the Streams API.

The following production files use lambda expressions, stream pipelines, or the Streams API’s `Collector` abstraction:

- `src/trafficsim/Main.java` — uses a lambda with `SwingUtilities.invokeLater` to create the interface on Swing’s Event Dispatch Thread.
- `src/trafficsim/analysis/ApproachSnapshot.java` — filters and maps vehicles located within a directional intersection approach.
- `src/trafficsim/analysis/CongestionAnalyser.java` — selects sequential or parallel streams, maps snapshots to congestion results, sorts them deterministically, and applies the custom collector.
- `src/trafficsim/analysis/CongestionCollector.java` — implements a custom `Collector` using a supplier, accumulator, combiner, and finisher.
- `src/trafficsim/analysis/CongestionReport.java` — uses streams to retrieve the busiest road and directional approach.
- `src/trafficsim/analysis/NetworkSnapshot.java` — uses nested pipelines to capture immutable road, approach, and vehicle data.
- `src/trafficsim/analysis/RoadSnapshot.java` — flattens the vehicles from multiple lanes into one immutable road snapshot.
- `src/trafficsim/engine/SimulationEngine.java` — uses a lambda for the simulation thread and a stream pipeline to apply a selected timing strategy to every signalised intersection.
- `src/trafficsim/engine/Statistics.java` — calculates average speed and stopped-vehicle percentages using streams.
- `src/trafficsim/engine/VehicleSpawner.java` — uses lambdas as vehicle factories and as a predicate for removing vehicles outside the network.
- `src/trafficsim/factory/NetworkLoader.java` — uses streams for direction parsing and network validation.
- `src/trafficsim/model/road/SignalisedIntersection.java` — uses nested streams to count queued vehicles travelling on a selected road axis.
- `src/trafficsim/strategy/SignalTimingStrategies.java` — implements fixed-time and adaptive timing strategies as lambda expressions.
- `src/trafficsim/view/SimulationDisplay.java` — uses lambdas for event handling and congestion-information formatting.

Streams and lambdas also appear in the following test files:

- `test/trafficsim/analysis/CongestionAnalyserTest.java` — compares sequential and parallel analysis results.
- `test/trafficsim/analysis/CongestionCollectorTest.java` — verifies sequential and parallel collection produce equivalent statistics.
- `test/trafficsim/engine/SimulationEngineCongestionTest.java` — uses an observer lambda to verify report publication.
- `test/trafficsim/view/SimulationDisplaySignalTimingTest.java` — uses a stream to locate and verify the timing-control component.

The arrow syntax used in enum `switch` expressions is not a lambda expression. Files containing only switch-expression arrows have therefore not been included.
### 2.2. For the most sophisticated use, paste the code and explain: what does it do, and why was a stream/lambda better than a loop?

The most sophisticated use is the congestion-analysis pipeline, which combines immutable snapshots, optional parallel processing, deterministic sorting, and a custom collector. `CongestionAnalyser` converts each road snapshot into a congestion result and then aggregates all vehicle snapshots into network-wide statistics:

```java
Stream<RoadSnapshot> roadStream = parallel
        ? snapshot.roads().parallelStream()
        : snapshot.roads().stream();

var roadResults = roadStream
        .map(this::analyseRoad)
        .sorted(MOST_CONGESTED_FIRST)
        .toList();

Stream<VehicleSnapshot> vehicleStream = snapshot.vehicles();
if (parallel) {
    vehicleStream = vehicleStream.parallel();
}

TrafficStatistics networkStatistics = vehicleStream
        .collect(CongestionCollector.toTrafficStatistics());
```

`CongestionCollector` defines how each vehicle contributes to the result and how independently calculated partial results are combined:

```java
private void add(VehicleSnapshot vehicle) {
    vehicleCount++;
    totalSpeed += vehicle.speed();

    if (vehicle.speed() <= SimConstants.QUEUED_SPEED_THRESHOLD) {
        queuedVehicleCount++;
    }
}

private Accumulator combine(Accumulator other) {
    vehicleCount += other.vehicleCount;
    queuedVehicleCount += other.queuedVehicleCount;
    totalSpeed += other.totalSpeed;
    return this;
}

private TrafficStatistics finish() {
    double averageSpeed = vehicleCount == 0
            ? 0.0
            : totalSpeed / vehicleCount;

    return new TrafficStatistics(
            vehicleCount,
            queuedVehicleCount,
            averageSpeed);
}
```

The pipeline calculates congestion for independent roads, orders the results from busiest to least busy, and produces vehicle count, queued count, and average speed in one reduction. A conventional loop could calculate the same values, particularly for a small network. However, the stream-based design expresses the work as a reusable sequence of transformations and allows the same analysis algorithm to run sequentially or in parallel. The collector also keeps the related aggregation rules together and explicitly defines how partial results must be merged.

The trade-off is that a custom collector is more complex than a basic loop. It is justified here because the analysis calculates several related statistics, operates on immutable data, and supports both sequential and parallel execution without duplicating the algorithm.

### 2.3. Did you use parallel streams, custom collectors, or the Fork/Join framework? If yes, how did you ensure thread safety? If no, where could parallelism be introduced?

I implemented both **parallel streams** and a **custom collector**. `CongestionAnalyser.analyseParallel()` uses `parallelStream()` for road and approach analysis and converts the flattened vehicle stream to parallel execution. I did not use the Fork/Join framework directly; parallel-stream task scheduling is managed internally by Java’s common Fork/Join pool. Regression tests compare sequential and parallel reports to verify that both execution modes produce equivalent statistics and ordering.

Thread safety is achieved through snapshot isolation. `SimulationEngine` captures a complete `NetworkSnapshot` while holding the network lock and releases the lock before beginning analysis. `NetworkSnapshot`, `RoadSnapshot`, `ApproachSnapshot`, and `VehicleSnapshot` are immutable records, while their collections are defensively copied using `List.copyOf`. Parallel workers therefore process stable values rather than reading or modifying live `Vehicle`, `Lane`, or `RoadNetwork` objects.

`CongestionCollector` is intentionally not marked `CONCURRENT`. Each stream partition receives an independent `Accumulator`, and the combiner merges partial results only after partition-level accumulation. Consequently, no mutable accumulator is shared between worker threads. Deterministic comparators, including coordinate and direction tie-breakers, ensure stable result ordering even when elements are processed in a different order.

The live simulation uses sequential analysis by default because its network is relatively small and parallel-processing overhead may exceed any performance benefit. Parallel analysis remains available and tested for larger snapshots. The application also coordinates a background simulation thread with Swing’s Event Dispatch Thread: `volatile` fields publish control and report state, while synchronization on the network prevents rendering or snapshot capture from observing a partially completed simulation tick.

---

## 3. Uniqueness and Creativity

### 3.1. What did you add to your simulation that went beyond the in-class activities? Describe the feature and what inspired it.

I added a live, directional congestion-analysis system inspired by the coloured
traffic layer in Google Maps. It does not simply colour an entire road because a
traffic light happens to be red. At the end of each tick, the engine captures an
immutable view of every road and of the 120-unit lane segment approaching each
intersection. The analyser counts queued vehicles, calculates congestion scores,
and classifies each approach as low, moderate, or high congestion. The display
then colours only the affected approach green, amber, or red while leaving the
ordinary road surface dark. A dashboard reports average speed, the queued
percentage, and the current busiest approach.

I also made signal timing switchable between fixed-time and adaptive
queue-pressure strategies. The mode is visible on screen and can be changed
during a run with the `T` key or the timing button. This turns the simulation
into an experiment rather than a fixed animation, while the run seed makes the
traffic sequence reproducible.

The thresholds were calibrated against observed simulation behaviour instead of
chosen only to make the display colourful. In a 3,410-tick run using seed
`0x19BA0744D40DDE4E`, the largest approach queue was zero for 13 ticks, one for
843 ticks, two for 2,037 ticks, and three for 517 ticks; it never reached four.
This evidence led to the final meanings: 0–1 queued vehicles is low, 2 is
moderate, and 3 or more is high.

### 3.2. Paste a code snippet or describe a design decision that demonstrates emergent behaviour, non-trivial complexity, or a genuinely surprising outcome.

The most important design decision was to separate the mutable simulation from
analytics:

```java
NetworkSnapshot snapshot;
synchronized (network) {
    tickCount++;
    updateLights();
    List<Vehicle> vehicles = collectVehicles();
    Map<Vehicle, SensorReading> readings = readSensors(vehicles);
    moveLaneVehicles(readings);
    spawner.despawnOffMap(network);
    if (vehicles.size() < SimConstants.SPAWN_CAP) spawner.tick(network);
    snapshot = NetworkSnapshot.capture(network, tickCount);
}

// Analysis reads only immutable copies and does not hold the network lock.
latestCongestionReport = congestionAnalyser.analyse(snapshot);
```

There is no script telling the heatmap when or where to turn red. Vehicle
spawning, different vehicle behaviour, buses stopping, traffic-light phases,
collision avoidance, and local queues interact to form congestion. The analyser
only observes the resulting state. A queue can grow on one side of an
intersection, produce a red approach, then disperse and return to amber or green.
This changing spatial pattern is emergent behaviour produced by local rules.

The strategy experiment also revealed a useful limitation: with the current
default traffic, the adaptive rule triggers rarely because vehicles on the green
axis usually begin moving and stop counting as queued. A 5,000-tick comparison
with the same seed produced essentially identical average speed and queued
percentage for fixed and adaptive modes. I retained the well-tested Strategy
architecture but do not claim that this simple policy is already an optimal
controller. Recognising this result is more defensible than forcing a visual
difference or claiming an unsupported improvement.

### 3.3. If you had another 6 weeks, what would you add or change?

First, I would build a multi-seed, headless experiment runner and measure
throughput, mean delay, maximum queue, and fairness for both timing strategies.
I would redesign the adaptive input to measure approaching demand, not only
vehicles that are already stopped on the green axis, and tune it against those
experiments.

Second, I would represent the network as a graph. Vehicles could choose
destinations, turn at intersections, and use interchangeable routing strategies
such as shortest distance, shortest predicted time, or congestion avoidance.
That would allow jams to change routes rather than only change colours.

Finally, I would benchmark the sequential and parallel analysers on much larger
generated networks. The current snapshot design makes safe parallelism possible,
but the small demonstration network is unlikely to benefit from its overhead.

---

## 4. Log Book and Final Reflection

### 4.1. Attach or link your log book entries for Weeks 7–13.

**[ADD OR LINK THE WEEKS 7–13 LOGBOOK HERE.]**

The Git history provides dated evidence for the Assignment 2 implementation on
30–31 October 2026, including immutable snapshots, the custom collector,
congestion reporting, the heatmap, switchable signal strategies, calibration,
tests, and interface refinement. The final logbook should also include the
earlier weekly activities that are not recoverable from commits, such as workshop
learning, planning, group discussions, and feedback review. These should be
completed from my own notes rather than invented retrospectively.

### 4.2. Reflect on the semester.

**What was the most important concept you learned?**

The most important concept was designing around responsibilities and safe
boundaries rather than treating object-oriented programming as simply creating
many classes. The clearest example was separating the mutable simulation model
from immutable analysis data. The engine owns updates, snapshot records own a
consistent read-only representation, `CongestionAnalyser` owns interpretation,
and the display only renders the result. Strategy, State, and Observer then give
each changing behaviour an explicit home. This made the program easier to test,
explain, and extend.

**What design trade-off did you face, and how did you resolve it?**

The main trade-off was concurrency versus correctness and complexity. Directly
running a parallel stream over live lanes and vehicles would look advanced, but
those objects change on the simulation thread and could produce races or an
inconsistent report. Holding the network lock throughout parallel analysis would
be safe but would freeze simulation and painting. I resolved this by copying one
complete tick into immutable records under the lock, releasing the lock, and
analysing only the copies. This adds allocation each tick, but it creates a clear
thread-safety boundary and permits sequential or parallel processing.

A second trade-off was heatmap sensitivity. A threshold of four queued vehicles
made high congestion effectively invisible in the tested run, while a threshold
that was too low would mark normal traffic as severe. I used observed queue
frequencies to select three as the high threshold and retained the dark road
colour as a neutral baseline outside measured approaches.

**If you started over, what would you do differently?**

I would define the road network as a graph and agree on interfaces, invariants,
and automated tests before dividing the implementation. The coordinate-based
model works well for straight movement but makes turning, routing, and rerouting
expensive additions. I would also establish feature branches, small commits, and
repeatable experiments from the first week. This would make individual work and
integration easier to review and reduce late uncertainty about group ownership.

**What feedback from the Week 7 submission did you act on?**

The marker's feedback recognised the advanced code used in Assignment 1 and the
result received a high mark. I responded by keeping the technical ambition but
making the new work purposeful and explainable. The Strategy pattern solves a
real runtime-variation problem; streams and the custom collector calculate live
statistics; immutable snapshots provide a defensible concurrency model; and the
tests verify sequential/parallel equivalence, strategy decisions, integration,
and display behaviour. I also documented limitations rather than presenting
complexity as automatically better.

**[CHECK THE PARAGRAPH ABOVE AGAINST THE MARKER'S EXACT COMMENT BEFORE SUBMISSION.]**
