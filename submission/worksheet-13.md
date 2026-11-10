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

My principal extension was a live, directional congestion-analysis system inspired by the coloured traffic layer used in Google Maps. Rather than colouring an entire road whenever vehicles stop at a traffic light, the system independently analyses the 120-unit lane segment approaching each intersection. At the end of every simulation tick, the engine captures an immutable network snapshot. The analyser then counts queued vehicles, calculates congestion scores, and classifies each directional approach as low, moderate, or high congestion. Only the affected approach is rendered green, amber, or red, while unmeasured road sections retain their normal dark colour. The dashboard supplements the heatmap with average speed, queued-vehicle percentage, and the current busiest approach.

A related extension is the switchable signal-control system. Traffic lights can use either fixed-time timing or an adaptive queue-pressure strategy, and the active mode is displayed within the interface. Users can change the policy during a running simulation using the `T` key or the timing-control button. Combined with a reproducible run seed, this allows the program to operate as a controlled traffic experiment rather than only as a visual animation. The feature was inspired by real actuated traffic signals, which modify their timing in response to measured demand.

The congestion thresholds were calibrated using observed simulation behaviour rather than selected solely for visual effect. During a 3,410-tick run with seed `0x19BA0744D40DDE4E`, the maximum directional queue was zero for 13 ticks, one for 843 ticks, two for 2,037 ticks, and three for 517 ticks; it never reached four. Based on this distribution, zero or one queued vehicle is classified as low congestion, two as moderate, and three or more as high. This produced a heatmap that remains responsive while distinguishing ordinary signal delays from unusually heavy queues.

### 3.2. Paste a code snippet or describe a design decision that demonstrates emergent behaviour, non-trivial complexity, or a genuinely surprising outcome.

A significant design decision was to separate the mutable simulation from its analytical processing. The engine completes all model updates and captures one consistent snapshot while holding the network lock. Congestion analysis then operates on that immutable snapshot after the lock has been released:

```java
NetworkSnapshot snapshot;

synchronized (network) {
    tickCount++;
    updateLights();

    List<Vehicle> vehicles = collectVehicles();
    Map<Vehicle, SensorReading> readings = readSensors(vehicles);
    moveLaneVehicles(readings);

    spawner.despawnOffMap(network);
    if (vehicles.size() < SimConstants.SPAWN_CAP) {
        spawner.tick(network);
    }

    snapshot = NetworkSnapshot.capture(network, tickCount);
}

// Analysis reads only immutable copies and does not hold the network lock.
latestCongestionReport = congestionAnalyser.analyse(snapshot);
```

The congestion displayed by the heatmap is not scripted. Vehicle spawning, differences in vehicle behaviour, bus dwell times, traffic-light phases, collision avoidance, and local queue formation interact to produce changing conditions. The analyser observes the resulting state rather than instructing the display when or where congestion should occur. Consequently, a queue can develop on one directional approach, raise its classification from green to amber or red, and later disperse. These changing spatial patterns emerge from the interaction of independent local rules.

A genuinely surprising outcome occurred during evaluation of the adaptive timing strategy. In a reproducible 5,000-tick comparison using the same seed, both fixed and adaptive modes produced an average speed of approximately `1.8183` and a queued-vehicle rate of approximately `30.92%`. Investigation showed that the adaptive rule rarely activated because vehicles on the green axis normally began moving and therefore no longer satisfied the queued-speed threshold.

I retained the Strategy architecture because it correctly supports interchangeable and independently tested policies, but I do not claim that the current adaptive policy improves the default simulation. This result identified a limitation in the demand measurement: a future strategy should consider approaching traffic and predicted arrivals, rather than only vehicles already stopped on the green axis. Reporting this outcome demonstrates evaluation of the system’s actual behaviour rather than assuming that a more complex controller must perform better.

### 3.3. If you had another six weeks, what would you add or change?

Inspired by the Week 13 workshop presentations, I would add a speed-enforcement system involving a police vehicle. Individual roads would define speed limits, and a strategically positioned police vehicle would target the first vehicle detected travelling more than 3 km/h above the applicable limit. The police vehicle could use State to represent waiting, pursuing, and returning behaviours, while a Strategy could determine how a target is selected. This would introduce another autonomous agent whose local decisions could produce emergent interactions with traffic signals, congestion, and other vehicles.

I would also create a headless experimental framework that runs both signal-timing strategies across multiple identical seeds and records throughput, mean delay, maximum queue length, and directional fairness. The adaptive strategy could then be redesigned to measure approaching demand and predicted arrivals rather than only vehicles already stopped on the green axis.

Finally, I would replace the coordinate-only road model with a graph-based network. Vehicles could choose destinations, turn at intersections, and use routing strategies based on distance, predicted travel time, or congestion. This would support dynamic rerouting and would also allow the proposed police vehicle to pursue a target across multiple connected roads.

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

The most important concept I learned was that effective object-oriented design is based on assigning clear responsibilities and defining safe boundaries, rather than simply dividing a program into many classes. Earlier in the semester, I associated object-oriented programming mainly with classes, inheritance, and encapsulation. This project showed me that the more important decision is determining which object should own each behaviour and how objects should collaborate without becoming tightly coupled.

The clearest example was separating the mutable simulation model from immutable analytical data. `SimulationEngine` owns state changes, the snapshot records provide a consistent read-only representation, `CongestionAnalyser` interprets that data, and `SimulationDisplay` renders the resulting report. Strategy, State, and Observer further give timing policy, signal transitions, and update notification separate responsibilities. This structure made the program safer to analyse concurrently and easier to test, explain, and extend. It changed how I approach software design: I now consider responsibilities, dependencies, and change boundaries before deciding what classes to create.

**What design trade-off did you face, and how did you resolve it?**

The main trade-off was balancing concurrency with correctness and implementation complexity. Running a parallel stream directly over live lanes and vehicles could improve performance on a large network, but those objects are continuously modified by the simulation thread. Parallel workers could therefore observe different moments within the same tick or encounter unsafe concurrent modification. Keeping the network locked throughout analysis would protect the data, but it would also delay simulation updates and Swing rendering.

I resolved this by capturing one complete tick as immutable snapshot records while holding the network lock, then releasing the lock before analysis. This introduces additional object allocation and copying on every tick, but it creates a clear thread-safety boundary and allows the same analysis to run sequentially or in parallel. Because the current network is small, sequential analysis remains the live default, while the parallel implementation is retained and tested for larger datasets. I prioritised predictable behaviour and correctness over using parallelism where it offered no measured benefit.

A second trade-off concerned heatmap sensitivity. Classifying high congestion only after four vehicles were queued made the red state effectively unreachable during the calibrated run. However, setting the threshold too low would exaggerate routine traffic-light delays. I analysed the observed queue distribution and selected three queued vehicles as the high threshold, while two represents moderate congestion. Unmeasured road sections retain their dark base colour, preventing the heatmap from implying congestion where no directional approach is being evaluated.

**If you started over, what would you do differently?**

I would begin by modelling the road network as a graph, with intersections represented as nodes and road segments represented as edges. The existing coordinate-based model supports straight movement effectively, but turning, destination selection, dynamic routing, and congestion-aware rerouting require substantial changes. Establishing the graph structure and its invariants at the beginning would have provided a stronger foundation for later features such as adaptive routing and police-vehicle pursuit.

I would also agree on interfaces, ownership boundaries, and a shared definition of completion before dividing the group’s implementation work. Automated tests would be introduced alongside the first classes rather than mainly during later integration. From a version-control perspective, I would use clearly named feature branches, smaller commits, and regular code reviews from the beginning. Finally, I would establish repeatable performance experiments before tuning adaptive behaviour. These practices would make contributions easier to understand and integrate, while reducing uncertainty about responsibilities and whether a feature had genuinely improved the simulation.

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
