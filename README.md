# JSimul

Java implementation of a SimPy-style discrete-event simulation core. Built with Java 21 and Maven.

## Modules
- `sim`: simulation core (events, processes, conditions) plus collections/resources (Resource, Store, Container, Priority/PreemptiveResource) and examples.
- `basic`, `plat`: other project modules (not covered here).

## Build & Test
- Prerequisite: JDK 21, Maven 3.9+.
- Build core module: `cd sim && mvn -q package`
- Run tests: `cd sim && mvn test`

## Quick Start
Key primitives live in `com.jsimul.core`:
- `Environment`: schedules events and runs simulations (`env.run(...)`, `env.timeout(...)`, `env.process(...)`).
- `Event` / `Timeout`: awaitable events with callbacks; `Timeout` triggers after a delay.
- `Process`: user logic running in a task; uses `ProcessContext.await(Event)` to block on events.
- `Condition` (`AnyOf` / `AllOf`): compose events.

Resources live in `com.jsimul.collections`:
- `Resource`, `PriorityResource`, `PreemptiveResource`: capacity-constrained shared resources with request/release.
- `Store`, `FilterStore`, `PriorityStore`: item queues with optional filtering/priority.
- `Container`: continuous quantity put/get.

## Examples
Examples are under `sim/src/main/java/com/jsimul/examples` and use logging to show steps:
- `BasicUsageExample`: two processes waiting on timeouts.
- `ResourceUsageExample`: queued jobs requesting/releasing a single machine.
- `ConditionExample`: demonstrates AnyOf/AllOf composition.
- `FlowLineScenario`: full flow-line (A->F) with vehicles moving parts; sweeps vehicle count to meet a daily target.

Run an example (from project root after build):
```bash
cd sim
mvn -q -DskipTests package
java -cp target/classes com.jsimul.examples.BasicUsageExample
```
Replace class with any example to explore.

## Logging
Examples use `java.util.logging` to emit INFO-level milestones. Adjust the global logging config (e.g., `-Djava.util.logging.config.file=...`) to control verbosity when running examples. Developers can add similar logging to processes and callbacks to aid troubleshooting.
