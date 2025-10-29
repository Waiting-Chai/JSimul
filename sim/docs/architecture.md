# SimPy(Java) 架构设计概述

本实现遵循 SimPy 的事件驱动离散事件仿真架构，将 "事件"、"环境"、"进程" 和 "共享资源" 作为核心抽象。

## 核心组件
- `Environment`：基于 `(time, priority, id)` 的最小堆调度队列，驱动事件执行，提供 `step()` 与 `run(until)`。
- `Event`：基础事件，包含 `ok/value/defused/callbacks` 状态与 `succeed()/fail()/trigger()` 行为。
- `Timeout`：创建即按 `NORMAL` 优先级调度的延时事件。
- `Condition / AllOf / AnyOf`：复合事件，监听一组事件并在条件满足时触发，返回 `ConditionValue`（事件到值的映射）。
- `Process`：基于 Java 21 虚拟线程实现的进程语义，用户逻辑通过 `ProcessContext.await(Event)` 阻塞等待事件完成；支持 `interrupt(cause)` 中断，抛出 `Interrupt` 异常。
- `BaseResource`：资源基类，维护 `putQueue/getQueue` 并通过 `_doPut/_doGet` 决定触发。
- `Resource`：容量控制的资源，请求/释放使用权。
- `Store / PriorityStore / FilterStore`：对象存储（FIFO/优先级/过滤）。
- `Container`：连续量容器，支持按数量 `put/get`。

## 事件流程
1. 创建事件（如 `Timeout(env, d)`）时，事件被放入环境调度队列（带时间与优先级）。
2. `Environment.step()` 弹出队首事件并推进 `now`，随后分发该事件的所有回调。
3. 若事件失败且未 defuse（`defused=false`），环境抛出异常并终止当前步（与原版一致）。
4. `run(until)` 按条件循环 `step()`，当 `until` 事件到达或时间到达时抛出 `StopSimulation`，返回其值。

## 进程语义
- `Process` 通过 `Initialize` 事件在 `URGENT` 优先级启动虚拟线程；`ProcessContext.await(Event)` 为阻塞等待封装。
- `interrupt(cause)` 通过 `Interruption` 事件在 `URGENT` 优先级完成，若进程正在等待事件，则立即令等待失败并抛出 `Interrupt(cause)`。
- 该模型在行为/时序上对齐原版：中断与事件触发统一由环境按优先级驱动。

## 资源与队列不变量
- 每次触发后，`putQueue/getQueue` 从队列移除已触发事件，保持队列中均为“未触发”事件（与原版一致）。
- `Resource` 在容量未满时立即成功请求；`Release` 立即成功并移除对应使用者。
- `Store` 系列按各自策略触发 `get`：FIFO/堆顶/过滤器。

## 类关系（文本版）
- Environment
  - has PriorityQueue<Scheduled(time, priority, id, event)>
  - schedules Event
  - drives Process (setActiveProcess)
- Event
  - has callbacks: List<Callback>
  - subclasses: Timeout, Condition, Process.Initialize, Process.Interruption
- Condition
  - has events: List<Event>
  - value: ConditionValue (Map<Event,Object>)
- Process
  - has ProcessContext (await)
  - uses Initialize/Interruption
- BaseResource
  - has putQueue/getQueue: List<Event>
  - subclasses: Resource, Store, PriorityStore, FilterStore, Container

## 设计要点
- 单线程事件调度与虚拟线程进程执行分离，保持仿真时序可控与确定性。
- 回调分发时立即将 `callbacks` 置空，避免并发修改（与原版策略一致）。
- 失败事件传播遵循“未 defuse 则抛出”，保证异常可见性。