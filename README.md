# COMP2000 — Traffic Simulation

A Java OOP group project simulating road traffic with vehicles, intersections, and traffic lights.

## Team

| Name | Pair | Responsibility |
|------|------|----------------|
| Ali | A | Core Engine + Observer pattern |
| Henry | A | Core Engine + Observer pattern |
| Oscar | A | Core Engine + Observer pattern |
| Callum | B | Road Network + Factory pattern |
| Jubril | B | Road Network + Factory pattern |
| Ben | C | Vehicles |
| Jacob | C | Vehicles |
| Addrita | D | Traffic Lights + Intersections |
| Cam | D | Traffic Lights + Intersections |

## Project Structure

```
src/trafficsim/
  Main.java                  <- entry point (loads networks/grid.txt if present)
  SimConstants.java          <- central tuning knobs
  engine/                    <- SimulationEngine, SimulationObserver, Statistics,
                                SensorReading, TrafficSensor, VehicleSpawner
  view/                      <- SimulationDisplay (JPanel renderer and keyboard controls)
  model/
    road/                    <- RoadNetwork, Road, Lane, BusStop, Intersection (sealed),
                                SignalisedIntersection
    vehicle/                 <- Vehicle (sealed abstract), Car, Truck, Bus,
                                EmergencyVehicle, DriverProfile
    light/                   <- TrafficLight, LightState, RedState, GreenState, YellowState
  factory/                   <- NetworkLoader
  strategy/                  <- WeightedRandom<T>
  util/                      <- Direction, LightPhase, Axis enums
  exception/                 <- SimulationException, InvalidNetworkException
networks/
  grid.txt                   <- default demo network (3x3 signalised grid)
```

## How to Run

From the repo root:

**PowerShell (Windows):**
```
javac -d src/out (Get-ChildItem src/trafficsim -Recurse -Filter *.java | ForEach-Object { $_.FullName })
java -cp src/out trafficsim.Main
```

**Bash / Git Bash:**
```
javac -d src/out $(find src/trafficsim -name "*.java")
java -cp src/out trafficsim.Main
```

Optional args:
- `<network-file>` — path to a network definition (defaults to `networks/grid.txt`)
- `--seed=<long>` — deterministic RNG seed for spawning and driver slowdown
- `--signals=adaptive|fixed` — choose the initial signal-control strategy

Requires Java 17 or newer. The GUI shows the original 1100×700 top-down city view. Press `space` to pause or resume the simulation and `T` to compare fixed-time and queue-responsive signal strategies during the same run.
The dashboard displays the generated run seed. Launching normally creates a new pattern; passing `--seed=<long>` deliberately repeats one.

## Design Patterns

| Concept | Where | Submission stage |
|---------|-------|--------|
| Inheritance | `Vehicle` (sealed) → `Car` / `Truck` / `Bus` / `EmergencyVehicle` | Week 7 |
| Generics | `WeightedRandom<T>` | Week 7 |
| Exceptions | `SimulationException` → `InvalidNetworkException` | Week 7 |
| Observer | `SimulationObserver` / `SimulationDisplay` | Week 13 |
| State | `LightState` / `RedState` / `GreenState` / `YellowState` | Week 13 |
| Strategy | `SignalTimingStrategy` → fixed-time / queue-responsive policies | Week 13 |
| Simple Factory | `NetworkLoader` (file + built-in default) | Week 13 |
| Streams / lambdas | custom statistics collector, network parsing, policy application, vehicle cleanup | Week 13 |
| Threading | Background simulation loop with lock-protected complete updates | Week 13 |
| Sealed hierarchies | `Vehicle`, `Intersection` | Modern Java |
| Records | `SensorReading` | Modern Java |

## Features

- **Straight-through traffic** — vehicles stay in their lane through every junction
- **Collision avoidance** — vehicles keep a safe same-lane gap and wait while conflicting traffic clears a junction
- **Emergency priority** — siren-on vehicles may pass red lights but wait for occupied crossings; conflicting traffic yields only when the emergency approach is clear
- **Bus service** — buses dwell briefly at Central Station, Museum, or University when the stop serves their lane, then continue
- **Random traffic** — production runs randomise spawn timing, boundary lane, vehicle type, driver profile, cargo weight, and slowdown behaviour; `--seed` makes it reproducible
- **Smooth updates** — the simulation advances atomically at 30 updates per second so rendering never sees a half-updated world
- **Signal-control experiment** — switch live between fixed-time and queue-responsive policies; adaptive signals balance competing queues and cap extensions to prevent starvation
- **One-pass traffic analytics** — a custom collector computes speed, stopped percentage, congestion level, and vehicle-type counts together
- **Australian lane placement** — vehicles spawn and travel on the left side of the road

## Communication

Discord — respond when possible, notify the team if you can't make a deadline.

## AI Use Acknowledgement

Generative AI tools were used to help review, explain, debug, and refine parts of the code and documentation. The submitted work was checked by the student, who remains responsible for understanding and explaining it.

## Standard-JRE self-tests

From the repository root on Bash / Git Bash:

```
javac -d out $(find src tests -name "*.java")
java -cp out trafficsim.Assignment2SelfTest
```

The tests exercise the fixed and adaptive strategy decisions, starvation protection,
the extension cap, and the custom traffic-statistics collector.

To compare both signal strategies headlessly with the same seed and tick count:

```
java -cp out trafficsim.SignalStrategyExperiment 20261030 6000
```

The first argument is the seed and the second is the number of ticks. The runner
reports mean speed and mean stopped percentage after a warm-up period.
