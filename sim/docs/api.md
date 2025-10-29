# API 参考手册（Java 版本）

## 核心包 `com.jsimul.simpy.core`
- `Environment`
  - 构造：`Environment()`, `Environment(double initialTime)`
  - 属性：`double now()`, `Process activeProcess()`
  - 方法：`void schedule(Event e, int priority, double delay)`, `void step()`, `Object run(Object until)`
- `Event`
  - 常量：`PENDING`, `URGENT`, `NORMAL`
  - 状态：`triggered()`, `isProcessed()`, `ok()`, `isDefused()`, `value()`
  - 行为：`addCallback(Callback)`, `succeed(Object)`, `fail(Throwable)`, `trigger(Event)`
- `Timeout(Event)`
  - 构造：`Timeout(Environment, double delay[, Object value])`
- `Condition`
  - 构造：`Condition(Environment, BiPredicate<List<Event>,Integer>, List<Event>)`
  - 值：`ConditionValue`（事件到值映射）
- `AllOf`, `AnyOf`
  - 构造：`AllOf(Environment, List<Event>|Event...)`, `AnyOf(Environment, List<Event>|Event...)`
- `Process`
  - 构造：`Process(Environment, ProcessFunction)`
  - 行为：`boolean isAlive()`, `Event target()`, `void interrupt(Object cause)`
  - `ProcessFunction`：`Object run(ProcessContext ctx) throws Exception`
  - `ProcessContext`：`Environment env()`, `Object await(Event e)`
- `Interrupt`
  - 异常：`new Interrupt(Object cause)`

## 资源包 `com.jsimul.simpy.resources`
- `BaseResource`
  - 队列：`putQueue/getQueue`
  - 事件：`Put`, `Get`
- `Resource`
  - 行为：`Event request()`, `Event release(Request req)`
  - 状态：`int count()`
- `Store`
  - 行为：`Event put(Object item)`, `Event get()`
- `PriorityStore`
  - 行为：与 `Store` 一致，但按优先级出队
- `FilterStore`
  - 行为：`Event get(Predicate<Object> filter)`
- `Container`
  - 行为：`Event put(double amount)`, `Event get(double amount)`
  - 状态：`double level()`

## 异常
- `StopSimulation`：内部控制 `run(until)` 退出
- `EmptySchedule`：环境无可执行事件