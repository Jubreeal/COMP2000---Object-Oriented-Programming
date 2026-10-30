# COMP2000 Worksheet 2 - Final Submission

**Student name:** [ADD YOUR FULL NAME]

**GitHub repo URL:** https://github.com/Jubreeal/COMP2000---Object-Oriented-Programming

---

## 1. Design Patterns

The accompanying `assignment-2-pattern-diagram.pdf` shows the participants and
relationships for the three patterns discussed below.

### 1.1. Which design pattern(s) did you implement? Name them and state where each appears in your codebase.

I implemented and integrated three behavioural patterns:

1. **Strategy** controls traffic-signal timing. `SignalTimingStrategy` is the
   strategy interface. `FixedTimeSignalStrategy` and
   `QueueResponsiveSignalStrategy` are interchangeable concrete strategies.
   `SignalisedIntersection` is the context that creates a `SignalSnapshot`, asks
   its strategy for a `SignalDecision`, and applies any extension to its
   `TrafficLight`. `SimulationEngine` and `SignalControlMode` allow the policy to
   be changed atomically at runtime. The user can press `T`, or launch with
   `--signals=fixed` or `--signals=adaptive`, to run a visible comparison.

2. **State** controls the traffic-light lifecycle. `TrafficLight` is the context,
   `LightState` is the state interface, and `GreenState`, `YellowState`, and
   `RedState` are concrete states. Each state owns the rule for leaving that
   phase. The all-red state also swaps the active axis before returning to green.

3. **Observer** separates simulation updates from rendering. `SimulationEngine`
   is the subject, `SimulationObserver` is the observer interface, and
   `SimulationDisplay` is the concrete observer. After an atomic simulation tick,
   the engine notifies its observers and the display requests a Swing repaint.

### 1.2. For each pattern, paste a code snippet showing the key interaction between participants.

**Strategy - `SignalisedIntersection.maybeExtendGreen()`**

```java
SignalSnapshot snapshot = new SignalSnapshot(
        light.getPhase(),
        green,
        remaining,
        queuedOnAxis(green),
        queuedOnAxis(green.other()),
        extensionsThisPhase);
SignalDecision decision = timingStrategy.decide(snapshot);
if (decision.extensionTicks() > 0) {
    light.extendCurrentPhase(decision.extensionTicks());
    extensionsThisPhase++;
}
```

The context knows how to observe and update an intersection, but the selected
strategy owns the decision rule. For example, the adaptive strategy extends a
nearly completed green only when its queue reaches the threshold, is not smaller
than the red queue, and has not reached the fairness cap.

**State - `TrafficLight.update()` and `GreenState.update()`**

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

`TrafficLight` delegates phase-specific behaviour to its current state instead
of containing one growing conditional over every phase.

**Observer - `SimulationEngine` and `SimulationDisplay`**

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

The engine publishes only a completed tick. It does not import Swing or call
painting methods directly.

### 1.3. For each pattern, explain why you chose it over an alternative. What trade-off did you make?

**Strategy:** The initial adaptive rule was embedded directly in
`SignalisedIntersection`. I could have added a boolean and an `if/else` for fixed
versus adaptive behaviour, but every new controller would have enlarged that
conditional and mixed policy with intersection mechanics. Strategy gives each
policy one testable class and supports runtime substitution without changing the
intersection. I considered subclassing `SignalisedIntersection`, but subclasses
would duplicate the intersection role and make live switching awkward. The
trade-off is additional small classes and indirection. I accepted that cost
because the policies are meaningful experimental variables rather than cosmetic
variations.

**State:** An enum plus a `switch` in `TrafficLight.update()` would require fewer
files and is reasonable for three simple phases. State was chosen because the
red phase also swaps axes and future phases, such as pedestrian priority, can
gain their own transition behaviour without modifying a central switch. The
trade-off is one object per state transition and more navigation between files.
For this small simulation the allocation cost is negligible, while the transition
rules remain cohesive and independently understandable.

**Observer:** The display could poll the engine using a Swing timer, or the engine
could hold a direct `SimulationDisplay` reference. Polling would duplicate timing
and might repaint half-updated data; a direct reference would couple the model to
Swing. Observer allows other consumers, such as a recorder, without changing the
engine. Its trade-off is lifecycle and threading discipline: notifications happen
on the simulation thread, so the observer performs only Swing's thread-safe
`repaint()` request. Painting and simulation share the network lock so a frame
never observes a partial tick.

---

## 2. Streams / Lambdas / Parallelism

### 2.1. List every file in your project that uses lambda expressions or the Streams API.

- `src/trafficsim/Main.java` - lambda passed to `SwingUtilities.invokeLater`.
- `src/trafficsim/engine/SimulationEngine.java` - simulation-loop `Thread` lambda.
- `src/trafficsim/engine/Statistics.java` - stream pipelines, a custom collector,
  method references, `Map.merge`, and a combining lambda.
- `src/trafficsim/engine/VehicleSpawner.java` - lambda-based vehicle factories and
  a predicate for removing off-map vehicles.
- `src/trafficsim/factory/NetworkLoader.java` - parsing with `Arrays.stream`, plus
  `flatMap` and `noneMatch` validation.
- `src/trafficsim/model/road/RoadNetwork.java` - filtering, casting, and applying a
  signal strategy across signalised intersections.
- `tests/trafficsim/Assignment2SelfTest.java` - stream-based verification that all
  intersections receive the selected strategy.
- `tests/trafficsim/SignalStrategyExperiment.java` - stream flattening creates a
  vehicle view for the repeatable fixed-versus-adaptive experiment.

The `->` tokens in enum `switch` expressions, such as `Direction.java`, are switch
syntax rather than lambda expressions, so I have not incorrectly counted them.

### 2.2. For the most sophisticated use, paste the code and explain: what does it do, and why was a stream/lambda better than a loop?

The most sophisticated use is the custom collector in `Statistics`. It calculates
vehicle count, stopped count, total speed, vehicle-type distribution, average
speed, stopped percentage, and congestion level in one traversal.

```java
private static Collector<Vehicle, MutableSummary, TrafficSummary> summaryCollector() {
    return Collector.of(
            MutableSummary::new,
            MutableSummary::accept,
            MutableSummary::combine,
            MutableSummary::finish);
}

private MutableSummary combine(MutableSummary other) {
    count += other.count;
    stopped += other.stopped;
    speedTotal += other.speedTotal;
    other.vehicleTypes.forEach(
            (type, amount) -> vehicleTypes.merge(type, amount, Long::sum));
    return this;
}
```

A single ordinary loop could calculate the same values, but the collector is a
better design here because it packages the complete reduction contract: creation,
accumulation, combination, and conversion to an immutable `TrafficSummary`. The
dashboard needs only `vehicles.stream().collect(summaryCollector())`, rather than
several unrelated loops or stream traversals. The combiner also documents how
partial results can be merged, which makes the reduction reusable for an immutable
parallel source later. The trade-off is that a custom collector is less familiar
than a simple loop, so it is justified only because several related statistics are
produced together.

### 2.3. Did you use parallel streams, custom collectors, or the Fork/Join framework? If yes, how did you ensure thread safety? If no, where could parallelism be introduced?

I used a **custom collector**, but deliberately did not call `parallelStream()`.
Each collector partition would receive its own `MutableSummary` and the combiner
merges partitions without shared accumulator state, so the reduction itself is
parallel-compatible. The source objects are different: `Vehicle` instances are
mutable and are updated by the simulation thread. The dashboard therefore runs
the sequential collector while holding the same network monitor used for an
atomic simulation tick.

The simulation does use two real threads: a background `sim-loop` advances the
model while Swing's Event Dispatch Thread renders it. `running`, `paused`, and the
selected mode are `volatile`, and the complete model update, strategy replacement,
and rendering snapshot are coordinated with `synchronized (network)`.

Using a parallel stream for at most 40 vehicles would probably be slower because
task creation and merging would dominate the work. If the network were expanded
to thousands of vehicles, I would capture immutable `VehicleSnapshot` records
under the network lock, release the lock, and then run this collector using
`parallelStream()` over the snapshots. That design would prevent workers from
reading vehicles while they are being mutated.

---

## 3. Uniqueness and Creativity

### 3.1. What did you add to your simulation that went beyond the in-class activities? Describe the feature and what inspired it.

I turned the signal logic into a live traffic-control experiment. The same seeded
traffic run can switch between a fixed-time baseline and a queue-responsive
controller by pressing `T`. The starting mode can also be selected from the
command line, which makes repeatable comparisons possible. The dashboard reports
average speed, stopped percentage, congestion state, and the active strategy.

The headless experiment runner gives both policies the same seed. With seed
`20261030` over 6,000 ticks (after a 500-tick warm-up), the fixed policy produced
mean speed `1.731` and mean stopped traffic `31.78%`; the adaptive policy produced
mean speed `1.740` and mean stopped traffic `31.59%`. That is a small improvement
of `0.51%` in speed and `0.19` percentage points in stopped traffic for this run.
I do not treat one seed as proof that adaptive control always wins; it is a
repeatable observation and a basis for the multi-seed evaluation proposed below.

This was inspired by real actuated traffic signals, which respond to demand rather
than blindly following one timing plan. I deliberately retained the fixed policy
as a baseline: without a baseline, an "adaptive" feature is difficult to evaluate.
The adaptive policy also compares the green and red queues and limits extensions,
so it cannot continuously favour one busy direction and starve the other.

This feature operates alongside heterogeneous cars, trucks, buses with dwell
times, emergency vehicles with intersection priority, stochastic driver profiles,
collision avoidance, and irregular seeded spawning. The result is not a scripted
animation; local rules interact to create changing traffic conditions.

### 3.2. Paste a code snippet or describe a design decision that demonstrates emergent behaviour, non-trivial complexity, or a genuinely surprising outcome.

The adaptive decision is intentionally local:

```java
if (snapshot.phase() != LightPhase.GREEN
        || snapshot.ticksRemaining() > decisionWindow
        || snapshot.extensionsUsed() >= maximumExtensions
        || snapshot.greenQueue() < queueThreshold
        || snapshot.greenQueue() < snapshot.redQueue()) {
    return SignalDecision.NO_CHANGE;
}
return SignalDecision.extend(extensionTicks,
        "green queue " + snapshot.greenQueue()
                + " >= red queue " + snapshot.redQueue());
```

No class contains a global instruction to create or clear a traffic jam. Random
arrivals and driver slowdowns create uneven queues; buses temporarily obstruct a
lane; emergency vehicles alter right-of-way; and the selected controller feeds
the observed queues back into signal timing. A green extension may clear one
burst, but it also delays the perpendicular flow. The extension cap and
red-versus-green comparison create a bounded feedback loop. Congestion waves,
temporary recovery, and different outcomes under the two policies therefore
emerge from interactions among simple local rules.

### 3.3. If you had another 6 weeks, what would you add or change?

I would replace straight-through movement with a graph-based route model. Vehicles
would choose destinations and use Dijkstra or A* routing, while a routing Strategy
could compare shortest-distance, shortest-time, and congestion-aware policies. I
would then add lane changes and turning reservations at intersections.

For evaluation, I would add a headless experiment runner that executes both signal
strategies over many identical seeds and exports throughput, mean delay, queue
length, and fairness percentiles. That would distinguish a visually convincing
controller from one that improves results statistically. I would also introduce
immutable per-tick snapshots, enabling safe parallel analytics for much larger
networks, and expand the standard-JRE regression suite with deterministic
collision, emergency-priority, and starvation tests.

---

## 4. Log Book and Final Reflection

### 4.1. Attach or link your log book entries for Weeks 7-13.

**[ADD THE FILE NAME OR LINK TO YOUR WEEKS 7-13 LOGBOOK HERE.]**

Before submission, confirm that every week is present and dated. Include the
group separation, the decision to continue from the individual fork, the Strategy
refactor, collector design, testing, and the observed fixed-versus-adaptive results.

### 4.2. Reflect on the semester.

**What was the most important concept you learned?**

The most important lesson was that object-oriented design is about assigning
changing behaviour to the right collaborators, not simply creating inheritance
hierarchies. The signal experiment made this concrete. `TrafficLight` State objects
own phase transitions, a signal Strategy owns timing policy, the intersection owns
local traffic observations, and the Observer boundary keeps rendering outside the
engine. Each abstraction has a reason to change. This made the code easier to test
and explain than one large simulation class with flags and switches.

**What design trade-off did you face, and how did you resolve it?**

The most important trade-off was responsiveness versus fairness in adaptive
signals. Extending every busy green can improve immediate throughput but starve a
perpendicular queue. I resolved this by extending only near the end of green, only
when the green queue meets the threshold and is at least as large as the red queue,
and only up to a fixed number of extensions. This does not claim to be an optimal
traffic controller, but it makes the fairness constraint explicit and testable.

I faced a second trade-off with parallelism. Parallel streams would look more
advanced, but the live collection is small and mutable. I chose a custom collector
with a correct combiner while keeping execution sequential under the network lock.
This favoured correctness and honest performance reasoning over using parallelism
only to satisfy a keyword in the rubric.

**If you started over, what would you do differently?**

I would define the road network as a graph and agree on interfaces, invariants,
and tests before dividing implementation work. The current coordinate-based model
made straight movement easy but makes routing and turning expensive additions. I
would also establish small pull requests and automated standard-JRE tests from the
first week. That would reduce the late integration effort and make each person's
contribution easier to review.

**What feedback from the Week 7 submission did you act on?**

The Week 7 feedback recognised that the project used advanced code and rewarded
that ambition. I acted on it by retaining the technical ambition while making the
new work more purposeful and defensible: the Strategy refactor solves a genuine
variation problem, the fixed controller provides an experimental baseline, the
collector produces related analytics in one pass, and regression tests capture
fairness and policy behaviour. I also documented alternatives and trade-offs rather
than presenting complexity as automatically better.

**[CHECK THIS PARAGRAPH AGAINST THE MARKER'S EXACT WEEK 7 COMMENT AND ADD ANY
OTHER SPECIFIC FEEDBACK THEY PROVIDED.]**
