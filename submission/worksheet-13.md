# COMP2000 Worksheet 2 — Final Submission

**Student name:** [ADD YOUR FULL NAME]

**GitHub repo URL:** https://github.com/Jubreeal/COMP2000---Object-Oriented-Programming

---

## 1. Design Patterns

The accompanying `assignment-2-pattern-diagram.pdf` shows the participants and
relationships described below. Strategy was the principal pattern added for
Assignment 2. State and Observer were retained from the earlier design because
they collaborate directly with the new signal-control and analytics features.

### 1.1. Which design pattern(s) did you implement? Name them and state where each appears in your codebase.

1. **Strategy** controls signal timing. `SignalTimingStrategy` is the strategy
   interface, while `SignalTimingStrategies` supplies fixed-time and adaptive
   queue-pressure implementations as lambda expressions. `SignalisedIntersection`
   is the context: it creates a `SignalTimingContext` from current traffic and
   delegates the extension decision to its selected strategy. `SignalTimingMode`
   names the available policies, and `SimulationEngine` changes the policy for
   every signalised intersection at runtime. The display allows the user to
   switch modes with the `T` key or the timing button.

2. **State** controls each traffic light's lifecycle. `TrafficLight` is the
   context, `LightState` is the state interface, and `GreenState`, `YellowState`,
   and `RedState` are the concrete states. Each state owns its transition rule;
   `RedState` also swaps the active road axis before returning to green.

3. **Observer** separates the simulation from its Swing display.
   `SimulationEngine` is the subject, `SimulationObserver` is the observer
   interface, and `SimulationDisplay` is the concrete observer. The engine
   notifies the display only after a complete simulation tick.

### 1.2. For each pattern, paste a code snippet showing the key interaction between participants.

**Strategy — decision and context interaction**

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

`SignalisedIntersection` supplies the current traffic data but does not contain
the policy itself:

```java
SignalTimingContext context = new SignalTimingContext(
        light.getPhase(),
        remaining,
        extensionsThisPhase,
        queuedOnAxis(greenAxis),
        queuedOnAxis(greenAxis.other()));

if (timingStrategy.shouldExtendGreen(context)) {
    light.extendCurrentPhase(SimConstants.ADAPTIVE_EXTEND_TICKS);
    extensionsThisPhase++;
}
```

**State — `TrafficLight` delegates to its current phase object**

```java
// TrafficLight (context)
public void update() {
    timer++;
    state.update(this);
}

// GreenState (concrete state)
public void update(TrafficLight light) {
    if (light.getTimer() >= light.getGreenDuration()) {
        light.setState(new YellowState());
    }
}
```

**Observer — engine notification and display response**

```java
// SimulationEngine (subject)
private void notifyObservers() {
    for (SimulationObserver observer : observers) {
        observer.onSimulationStep();
    }
}

// SimulationDisplay (concrete observer)
@Override
public void onSimulationStep() {
    repaint();
}
```

### 1.3. For each pattern, explain why you chose it over an alternative. What trade-off did you make?

**Strategy:** A boolean plus an `if/else` inside `SignalisedIntersection` would
have been shorter for two modes, but every new timing policy would enlarge the
intersection class and mix traffic-control policy with intersection mechanics.
Subclassing the intersection would also make live switching awkward. Strategy
keeps each policy replaceable and independently testable, and a functional
interface allows concise lambda implementations. The trade-off is additional
types and indirection. I accepted that cost because timing policies are genuine
experimental alternatives rather than cosmetic options.

**State:** An enum and a large `switch` could implement three phases with fewer
files. State keeps each phase's transition behaviour beside the phase and allows
future phases, such as pedestrian priority, without extending one central
conditional. The trade-off is more objects and navigation between small classes.
That overhead is minor compared with the clearer transition responsibilities.

**Observer:** The engine could directly call `SimulationDisplay`, but this would
couple the model to Swing. The display could instead poll the engine, but polling
could duplicate timing and observe an incomplete update. Observer keeps the
engine independent of the user interface and permits future observers such as a
statistics recorder. The trade-off is threading discipline: notification occurs
on the simulation thread, so the observer requests the thread-safe `repaint()`
rather than painting directly.

---

## 2. Streams / Lambdas / Parallelism

### 2.1. List every file in your project that uses lambda expressions or the Streams API.

Production files:

- `src/trafficsim/Main.java` — lambda passed to `SwingUtilities.invokeLater`.
- `src/trafficsim/analysis/ApproachSnapshot.java` — filters and maps vehicles
  belonging to one directional approach.
- `src/trafficsim/analysis/CongestionAnalyser.java` — sequential/parallel stream
  selection, mapping, deterministic sorting, and custom collection.
- `src/trafficsim/analysis/CongestionCollector.java` — custom Streams API
  `Collector` with supplier, accumulator, combiner, and finisher.
- `src/trafficsim/analysis/CongestionReport.java` — stream-based lookup of the
  busiest road and approach.
- `src/trafficsim/analysis/NetworkSnapshot.java` — nested stream pipelines that
  capture roads, approaches, and vehicles.
- `src/trafficsim/analysis/RoadSnapshot.java` — flattens lane vehicles into an
  immutable road snapshot.
- `src/trafficsim/engine/SimulationEngine.java` — simulation-thread lambda and a
  stream pipeline for changing every signalised intersection's strategy.
- `src/trafficsim/engine/Statistics.java` — stream-based average and stopped
  percentage calculations retained from the original simulation.
- `src/trafficsim/engine/VehicleSpawner.java` — lambda-based vehicle factories
  and a removal predicate.
- `src/trafficsim/factory/NetworkLoader.java` — stream-based parsing and network
  validation.
- `src/trafficsim/model/road/SignalisedIntersection.java` — nested streams count
  queued vehicles on a selected axis.
- `src/trafficsim/strategy/SignalTimingStrategies.java` — timing strategies
  implemented as lambdas.
- `src/trafficsim/view/SimulationDisplay.java` — event-handler and formatting
  lambdas.

Test files also using streams or lambdas:

- `test/trafficsim/analysis/CongestionAnalyserTest.java`
- `test/trafficsim/analysis/CongestionCollectorTest.java`
- `test/trafficsim/engine/SimulationEngineCongestionTest.java`
- `test/trafficsim/view/SimulationDisplaySignalTimingTest.java`

The `->` symbols in enum `switch` expressions are switch syntax, not lambda
expressions, so I have not counted files that contain only those expressions.

### 2.2. For the most sophisticated use, paste the code and explain: what does it do, and why was a stream/lambda better than a loop?

The most sophisticated use combines immutable snapshots, switchable parallel
pipelines, and a custom collector. `CongestionAnalyser` maps every road and
approach to a congestion result, orders the results deterministically, and
reduces all vehicle snapshots to network statistics:

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

The collector defines the complete reduction contract:

```java
@Override
public Supplier<Accumulator> supplier() {
    return Accumulator::new;
}

@Override
public BiConsumer<Accumulator, VehicleSnapshot> accumulator() {
    return Accumulator::add;
}

@Override
public BinaryOperator<Accumulator> combiner() {
    return Accumulator::combine;
}

@Override
public Function<Accumulator, TrafficStatistics> finisher() {
    return Accumulator::finish;
}
```

An ordinary loop could calculate these figures, but the stream design expresses
the operation as a reusable transformation: map independent snapshot objects,
sort them, then collect the results. The custom collector calculates vehicle
count, queued count, and total speed in one traversal and explicitly defines how
partial results combine. This is clearer than maintaining several loosely
related counters across several loops, and the same analysis can be executed
sequentially or in parallel without duplicating the algorithm. The trade-off is
that a custom collector is less immediately familiar than a simple loop, so it
is worthwhile here because it supports several related statistics and both
execution modes.

### 2.3. Did you use parallel streams, custom collectors, or the Fork/Join framework? If yes, how did you ensure thread safety? If no, where could parallelism be introduced?

I used both **parallel streams** and a **custom collector**. The public
`analyseParallel()` method selects `parallelStream()` for roads and approaches
and calls `parallel()` for the flattened vehicle stream. Tests compare sequential
and parallel reports to ensure they produce the same ordered result.

Thread safety is based on snapshot isolation rather than locking inside stream
operations. `SimulationEngine` captures a `NetworkSnapshot` while holding the
network lock, then releases the lock before analysis. `RoadSnapshot`,
`ApproachSnapshot`, and `VehicleSnapshot` are immutable records, and their lists
are defensively copied with `List.copyOf`. Parallel workers therefore never read
or modify live `Vehicle` or `Lane` objects.

`CongestionCollector` is not declared `CONCURRENT`. Each stream partition
receives its own `Accumulator`; the combiner merges completed partial
accumulators. There is no accumulator shared by multiple workers. The final
lists are sorted with deterministic tie-breakers, so their order is stable even
when processing order differs.

The normal live simulation currently uses sequential analysis because the
network is small and parallel overhead may exceed the benefit. Parallel analysis
is available and tested for larger snapshots. Separately, the application has a
background simulation thread and Swing's Event Dispatch Thread. `volatile`
fields publish control/report state, and the network lock prevents rendering or
snapshot capture from observing a half-completed tick.

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
