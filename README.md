# JSimul

JSimul 是一个基于 Java 21 的离散事件仿真框架，面向 SimPy 的语义与 API 对齐，提供事件、进程、条件组合以及资源调度等核心能力。项目强调组合优先的设计，通过小而精的原语拼装出丰富的行为，方便在 JVM 环境中重现 SimPy 的建模体验。

## 功能概览
- **事件驱动核心**：提供 `Event`、`Timeout`、`Process` 以及 `AllOf`、`AnyOf` 等组合原语，实现基于调度队列的时间推进机制。
- **资源建模**：内置 `Resource`、`PriorityResource`、`PreemptiveResource`、`Container`、`Store` 等资源类型，支持优先级、抢占、容量和过滤存取。
- **条件与回调**：支持条件事件求值、回调移除与复制等 SimPy 兼容特性，确保回调触发顺序与短路逻辑正确。
- **实时模式**：提供 `RealtimeEnvironment`，在需要时可按真实时间步进模拟。
- **异常与中断**：支持进程中断、异常传播、主动退出（`env.exit(...)`）等语义，贴合 Python 端行为。

## 性能特点
- **优先队列调度**：使用 `PriorityQueue` 与递增事件标识减少争用，确保大批量事件插入与弹出时的可预测性能。
- **对象复用友好**：事件与组合条件采用组合式拆分，避免深层继承开销，便于 JVM JIT 进行内联与逃逸分析。
- **并发安全的调度器锁**：通过轻量级的队列锁保护调度操作，保证在多生产者场景下的线程安全。
- **性能测试护栏**：仓库内的性能单测覆盖大批量超时事件与链式回调场景，帮助在回归时监测吞吐退化。

## 设计美学与哲学
- **组合优先**：倾向组合而非继承的实现方式，减少层级，强调可重用的行为片段。
- **语义一致性**：以 SimPy 的用户体验为基准，对事件语义、资源 API 和异常传播路径保持一致，降低迁移成本。
- **可测试性**：所有核心原语均配有针对功能与 determinism 的测试，新增特性也需要覆盖全面的单元测试。
- **简洁编码**：统一使用 UTF-8 编码与英文注释，减少本地化差异带来的理解成本。

## 模块与目录
- **`sim` 模块**：核心实现与测试集。
  - `src/main/java/com/jsimul/core`：事件、进程、环境、条件等核心调度与组合逻辑。
  - `src/main/java/com/jsimul/collections`：资源、容器、商店等集合型原语。
  - `src/test/java`：与 SimPy 语义对齐的行为测试、异常传播测试、资源特性测试，以及性能验证用例。
- **`examples` 目录**：独立的示例代码，展示从“Hello World”到资源抢占、条件组合的常见建模套路，可直接以 `javac`/`java` 或 IDE 运行。

## 快速开始
1. 安装 JDK 21 与 Maven 3.9+。
2. 构建与运行测试：
   ```bash
   mvn -pl sim test
   ```
3. 浏览 `examples` 目录下的示例，按需编译运行：
   ```bash
   javac -cp sim/target/classes examples/basic/HelloEnvironment.java
   java -cp sim/target/classes:examples basic.HelloEnvironment
   ```

## 特性清单（与 SimPy 对齐）
- 事件调度：延时、任意值触发、回调管理、状态检查。
- 进程语义：生成器式进程、主动退出、异常传递与堆栈保留。
- 条件组合：`allOf` / `anyOf` 组合、值合并、失败短路与结果透传。
- 资源系统：标准资源、优先级资源、可抢占资源、存储（过滤/优先）、容器容量约束与超时语义。
- 运行模式：离散事件模拟与实时模拟切换。

## 示例导航
- `examples/basic/HelloEnvironment.java`：最小事件调度与运行示例。
- `examples/resources/PriorityCheckout.java`：演示 `PriorityResource` 的占用与释放流程。
- `examples/conditions/ConditionalAssembly.java`：组合条件触发与结果收集示例。

## 测试与性能
- 功能测试：`sim/src/test/java` 覆盖环境调度、条件组合、资源抢占、异常传播等。
- 性能测试：`PerformanceSimulationTest` 验证大批量超时事件与链式回调的执行时间，防止明显性能回退。

## 贡献指南
- 提交前确保 `mvn -pl sim test` 通过。
- 新增代码需配套单元测试与必要文档更新。
- 遵循组合优先的实现方式，避免不必要的继承层级。

