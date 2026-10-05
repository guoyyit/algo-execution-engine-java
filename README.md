# 本地算法交易执行服务

在本地模拟的行情与撮合环境中，把大额母单（parent order）按**时间进度（TWAP）**或
**市场成交量参与率（VWAP/POV）**拆分为多笔子单（child order）逐步执行。
客户端通过服务接口提交母单、暂停/恢复、撤销，并查询执行状态与进度。
全程不依赖任何外部交易接口，行情与撮合均由本地确定性组件提供。

## 核心能力

- 两种可查询、确定的拆分计划：按时间均分、按市场成交量参与率。
- 母单暂停/恢复：暂停期间不产生、不提交新子单；恢复时按**剩余任务量与剩余时间**重新生成计划。
- 取消与到期终止：终止后所有在途子单被撤销，终态后不再产生子单、成交，状态不回退。
- 子单部分成交与行情缺失异常链路：未成交部分自动回到剩余任务量；行情长时间无更新可配置为暂停或降级。
- 崩溃恢复：母单/子单状态从 JSON 快照 + 事件日志重建，不重复提交已成交/已撤销子单。
- 多母单并发、故障隔离：共享账户额度/持仓并发安全，单母单故障不影响其它母单。
- 确定性与回放：固定随机种子 + 相同行情序列，执行过程逐笔一致，可离线复现。

## 模块结构

- `model`：领域模型与枚举（母单、子单、计划、切片、偏离、事件、进度）。
- `market`：`SimClock` 虚拟时钟、`ReplayMarketDataFeed` 行情源、`MatchingEngine` 确定性撮合、`MarketScenario`。
- `strategy`：`SlicePlanner` 及 `TimeWeightedPlanner`、`VolumeParticipationPlanner`。
- `execution`：`ExecutionEngine` 引擎门面、`ParentOrderExecutor` 单母单状态机、
  `Account` 账户额度、`FileOrderStore` 持久化、`IdGenerator`、事件发布。
- `service` / `web`：服务接口与 REST 控制器。
- `replay`：`ReplayRunner` 离线确定性回放。
- `config`：Spring 装配与配置项。

## 拆分口径

### 按时间（TIME_WEIGHTED, TWAP）

- 以当前时刻为窗口起点、母单 `endTime` 为窗口终点，均分为 `sliceCount` 个时间片。
- 每片基础量 = `剩余任务量 / sliceCount`，余数（前 `carry` 片各 +1）与尾片共同保证
  `sum(片量) == 剩余任务量`，**尾片吸收所有未分配余数**。
- 末片时间固定为 `endTime`。

### 按市场成交量（VOLUME_PARTICIPATION）

- 依据行情历史估算“每片市场成交量”：
  `volumePerSlice = (累计成交量差 / 时间差) * 每片时长`；
  历史不足时回退到常量 `FALLBACK_MARKET_VOLUME_PER_SLICE = 10000`。
- 每片目标量 = `volumePerSlice * participationRate`，且不超过剩余任务量；
  尾片吸收全部剩余量，保证计划总量恰好等于剩余任务量。

### 重新计划（恢复/暂停后恢复）

- 计划版本号 `planVersion` 单调递增；每次重新计划都基于当前
  **剩余任务量 = totalQuantity - cumulativeFilled** 与 **剩余时间 = endTime - now**。
- 因此新计划总量恒等于未成交的剩余任务量，与已成交进度口径一致。

## 进度计算（统一口径）

- `cumulativeFilled`：母单累计成交量（子单成交增量累加）。
- `remainingQuantity = totalQuantity - cumulativeFilled`（不为负）。
- `liveQuantity`：所有在途（SUBMITTED / PARTIALLY_FILLED）子单的未成交量之和。
- `plannedUnsubmittedQuantity = remainingQuantity - liveQuantity`。
- `progressRatio = cumulativeFilled / totalQuantity`。
- 计划偏离：`quantityDeviation = plannedCumQuantity(now) - cumulativeFilled`，
  `timeDeviationMillis = now - 最近到期片的计划时间`，
  统一记录在 `ParentOrder.deviations` 中，同一时刻只记一次。

部分成交的子单在其切片结束时若仍有未成交量，未成交部分**回到母单剩余任务量**参与后续计划。

## 状态机

- 母单：`CREATED -> ACTIVE -> PAUSED -> ACTIVE -> COMPLETED/CANCELLED/EXPIRED/FAILED`。
- 终态（`isTerminal`）后拒绝一切状态变更与新成交，状态不回退。
- 子单：`PENDING -> SUBMITTED -> PARTIALLY_FILLED -> FILLED`，
  或 `CANCELLED / EXPIRED / REJECTED`。
- 取消/到期时在途子单一律撤销（部分成交则记为 EXPIRED 并保留已成交部分）。

## 行情缺失策略（可配置）

当 `now - 最新行情时间 > staleQuoteTimeoutMillis`：

- `PAUSE`：自动暂停母单（事件 `STALE_QUOTE`，随后 `PAUSED`），恢复需显式调用 resume。
- `DEGRADE`：继续执行但新切片量减半（降级事件可在事件流中解释）。

行情恢复且未超阈值时自动退出降级标记。

## 持久化与崩溃恢复

- `FileOrderStore` 在 `algo.engine.store-dir` 下保存：
  - `<parentOrderId>.json`：母单聚合（含计划、子单、偏离）原子快照；
  - `events.log`：每行一个 JSON 的追加事件日志。
- 启动时 `ExecutionEngine.recover()`：读快照重建聚合，按事件序列号重放事件，
  校正子单状态/成交量、母单状态，并恢复母单级子单序列号，避免崩溃后重复提交。
- 成交事件按子单累计成交量去重应用，不重复计入。

## 确定性与回放

- 撮合随机性仅来自以母单 `randomSeed` 初始化的 `java.util.Random`；
  时间来自单调的 `SimClock`，不使用墙钟与线程时序。
- `ReplayRunner.run(request, scenario)` 使用独立内存存储跑完整过程并返回事件轨迹；
  `ReplayRunner.sameTrace(a,b)` 逐笔比较时间、类型、数量、价格、明细与终态。

## REST 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/orders` | 提交母单 |
| POST | `/api/orders/{id}/pause` | 暂停 |
| POST | `/api/orders/{id}/resume` | 恢复并重新计划 |
| POST | `/api/orders/{id}/cancel` | 取消 |
| GET  | `/api/orders/{id}` | 查询进度 |
| GET  | `/api/orders` | 全部母单进度 |
| GET  | `/api/orders/{id}/events` | 事件流 |
| POST | `/api/orders/market/ticks` | 注入本地行情 tick |
| POST | `/api/orders/replay` | 携带行情场景离线回放 |

## 配置项（`application.properties`，前缀 `algo.engine`）

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `store-dir` | `data/engine` | 快照与事件日志目录 |
| `account-cash-limit` | `1000000` | 买入共享额度上限 |
| `account-position-limit` | `1000000` | 卖出共享持仓上限 |
| `default-stale-quote-timeout-millis` | `30000` | 行情缺失判定阈值（请求未指定时） |
| `default-slice-count` | `10` | 默认切片数 |
| `default-participation-rate` | `0.1` | 默认市场参与率 |

每个母单请求还可单独指定 `staleQuotePolicy`、`staleQuoteTimeoutMillis`、
`sliceCount`、`participationRate`、`randomSeed`。

## 构建与测试

```bash
mvn test
```

测试覆盖：时间拆分口径、暂停/恢复、取消/到期、行情缺失暂停与降级、
部分成交与偏离记录、崩溃恢复（不重复提交）、多母单并发隔离、确定性回放。
