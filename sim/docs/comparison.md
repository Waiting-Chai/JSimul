# 与原版 SimPy 功能对比（阶段报告）

## 已实现
- 事件：`Event/Timeout/Condition(AllOf/AnyOf)`，失败与 defuse 语义
- 环境：`step()/run(until)`，调度规则 `(time, priority, id)`
- 进程：`ProcessContext.await(Event)` + 中断 `interrupt(cause)`
- 资源：`Resource/Store/PriorityStore/FilterStore/Container` 的基本行为
- 单元测试：覆盖核心路径，Jacoco 可生成覆盖率报告

## 待完善
- 优先级资源与抢占：`PriorityResource/PreemptiveResource` 的完整语义（含 `Preempted` 细节）
- 实时环境：`RealtimeEnvironment` 等（原版 `rt.py`）
- 工具函数：`util.start_delayed` 等
- 更细粒度异常与栈信息对齐：`_describe_frame` 的完整等价
- 更严格的 API 对齐：方法命名与包结构的完全一致性（当前为 Java 风格调整）

## 设计差异与理由
- 进程实现：原版基于生成器；Java 版本默认使用虚拟线程以简化“yield/await”语义映射，保持行为一致但存在线程调度开销。可提供无线程状态机备选实现以更贴近原版的资源消耗模型。
- 容器容量类型：Java 版本以 `double` 表示连续量并以 `int` 近似容量上界（简化实现），可根据需求调整为全 `double`。

## 计划
- 完成剩余资源类型与实时环境；
- 扩展测试用例，目标覆盖率 >90%；
- 发布设计说明与迁移指南，提供 Python→Java API 对照表与示例；