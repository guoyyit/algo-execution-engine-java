# 本地算法交易执行服务

一个完全本地运行的母单拆分执行引擎：大额母单按**时间进度**或**市场成交量参与率**拆成多笔子单，
在本地模拟的行情与撮合环境中逐 tick 执行。不依赖任何外部交易接口。

## 快速开始

```bash
mvn spring-boot:run
```

引擎由虚拟时钟驱动，HTTP 本身不会自动推进时间，便于离线复现。通过
`POST /api/algo/ticks/{count}` 推进 N 个 tick（每个 tick 长度由 `algo.engine.tick-interval-millis` 决定）。

提交一个时间拆分母单示例：

```bash
curl -X POST localhost:8080/api/algo/orders \
  -H 'Content-Type: application/json' \
  -d '{"symbol":"600000","side":"BUY","totalQty":1000,"durationMillis":600,
       "strategy":"TIME_SCHEDULE","sliceCount":6,"seed":42}'
```

## HTTP 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/algo/orders` | 提交母单（见 `OrderRequest`） |
| POST | `/api/algo/orders/{id}/pause` | 暂停 |
| POST | `/api/algo/orders/{id}/resume` | 恢复并按剩余量/剩余时间重建计划 |
| POST | `/api/algo/orders/{id}/cancel` | 取消（撤销全部在途子单） |
| GET | `/api/algo/orders/{id}` | 母单状态与进度 |
| GET | `/api/algo/orders` | 全部母单 |
| GET | `/api/algo/orders/{id}/plan` | 当前计划（版本化、确定、可查询） |
| GET | `/api/algo/orders/{id}/children` | 全部子单 |
| GET | `/api/algo/orders/{id}/deviations` | 计划与实际成交的偏离记录 |
| POST | `/api/algo/ticks/{count}` | 推进模拟时钟 |

## 拆分口径

- **时间拆分 `TIME_SCHEDULE`**：把母单剩余时间均分为 N 个切片，数量按
  `remainingQty / N` 均分，余数全部归入最后一笔。切片 `scheduledTime` 为
  `now + remainingTime * (i+1) / N`，最后一笔严格落在 `endTime`。
- **成交量参与 `VOLUME_PARTICIPATION`**：目标累计量统一为
  `min(remainingTotal, (市场累计成交量 - 计划起始市场量) × participationRate)`。
  计划同样确定地切成 N 笔，当行情显示的市场成交量里程碑达到目标累计量时才提交下一笔。
- **统一偏离口径**：偏离始终定义为
  `deviationQty = actualCumFilledQty - plannedCumQty`
  （正=超前，负=落后），附带 sliceIndex、计划累计量、实际累计量与原因。
  原因包括 `slice-submitted`、`planned-vs-filled@tickN`、`ACCOUNT_LIMIT_REJECTED`、
  `EXECUTOR_ERROR:*`。

## 进度计算

- 母单进度 = `filledQty / totalQty`（见 `ParentOrder.progress()`）。
- `filledQty` 由全部子单的已成交量汇总，不单独记账，避免口径漂移。
- 子单部分成交后未成交部分在撤单/超时/暂停时自动回到母单 `remainingQty`，
  恢复时基于 `totalQty - filledQty` 重新生成整份计划（计划版本号 +1）。

## 暂停 / 恢复 / 取消 / 到期

- 暂停生效后立即撤销全部在途子单，之后不再生成或提交新子单。
- 恢复时状态从 `PAUSED -> ACTIVE`，按**剩余任务量与剩余时间**生成新版本计划，
  与已成交进度严格一致。
- 取消先进入 `CANCELLING`，等所有在途子单撤单成交回报结束后置为 `CANCELLED`。
- 到 `endTime` 仍有剩余且无在途单时，尝试提交一笔收尾子单；若额度等原因无法提交，
  进入 `EXPIRED`。终态（`COMPLETED/CANCELLED/EXPIRED`）不可回退，也不再产生子单或成交。

## 异常链路

- **部分成交**：子单可多次部分成交；暂停/取消/超时只撤销未成交部分，已成交永久保留。
- **行情缺失**：当 `now - latestQuote.timestamp >= algo.engine.stale-quote-millis` 视为缺失：
  - `PAUSE`（默认）：撤销在途子单并把母单置为 `PAUSED`，行情恢复后需手动 resume。
  - `DEGRADE`：保持母单 ACTIVE，但缺失窗口内不提交新子单；行情恢复自动退出降级。
- **账户限制**：共享账户按 symbol 校验持仓上下限与买入资金占用；占用在提交时预留，
  成交转为实际持仓，撤单/未成交释放预留。预留失败只拒绝该笔子单并记录
  `ACCOUNT_LIMIT_REJECTED`，不影响其他母单。
- **单母单故障隔离**：执行器内部异常被引擎捕获，仅把该母单转 `PAUSED` 并记录可解释的
  偏离事件，其他母单继续执行。

## 崩溃恢复

- 所有母单/子单/计划/偏离以 JSONL 事件形式追加写入
  `algo.engine.event-log-dir/events.jsonl`（每条事件 fsync）。
- 模拟时钟水位写入 `tick.watermark`，行情状态写入 `market-state.json`，
  保证恢复后时间不回退、行情序列连续。
- 启动时重放事件重建内存状态；处于 `SUBMITTED/PARTIALLY_FILLED` 的子单重新挂入撮合簿。
  不会重复提交已成交/已撤销子单，也不会丢失在途进度与未决成交。

## 确定性与回放

- 时间只随 tick 推进（`TickClock`），撮合与行情都使用**无状态确定性**伪随机：
  行情由 `(symbolSeed, 推进次数)` 派生，成交比例由 `(childId, tick, quoteSequence)` 哈希派生，
  并叠加每 tick 至少 15% 的在途成交下限保证子单必然收敛。
- 相同随机种子与相同 tick 序列下，两个独立运行的母单状态、每笔子单成交（数量/均价）、
  偏离记录逐笔一致。离线复现只需重放相同的 tick 数。

## 配置项

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `algo.engine.event-log-dir` | `data/eventlog` | 事件日志/时钟水位/行情快照目录 |
| `algo.engine.tick-interval-millis` | `100` | 一个 tick 对应的模拟毫秒数 |
| `algo.engine.default-slice-count` | `6` | 请求未指定 sliceCount 时的默认切片数 |
| `algo.engine.default-participation-rate` | `0.1` | 默认市场参与率 |
| `algo.engine.stale-quote-millis` | `500` | 行情多久无更新视为缺失 |
| `algo.engine.stale-quote-action` | `PAUSE` | 缺失策略：`PAUSE` 或 `DEGRADE` |
| `algo.engine.order-timeout-millis` | `2000` | 在途子单超时强制撤单 |
| `algo.engine.account-max-position` | `1000000` | 单 symbol 持仓绝对值上限 |
| `algo.engine.account-max-cash` | `1000000000` | 买入资金占用上限 |

母单级覆盖：`sliceCount`、`participationRate`、`seed` 可在 `OrderRequest` 中逐单指定。

## 测试

```bash
mvn test
```

覆盖场景（`src/test/java/.../tests`）：

- `TimeStrategySmokeTest` / `VolumeStrategyTest`：两种拆分跑满全量
- `PauseResumeTest`：暂停不产生新子单、恢复按剩余量重建版本化计划
- `CancelTest`：取消后在途全撤、终态后无新子单/成交、状态不回退
- `ExpiryTest`：额度阻断时到期终止
- `CrashRecoveryTest`：事件日志重建、在途挂单恢复、无重复提交并继续到完成
- `ConcurrencyIsolationTest`：多母单并发互不影响、账户上限不连累其他母单
- `DeterminismReplayTest`：两种策略同种子逐笔一致
- `ExceptionPathTest`：行情缺失 PAUSE/DEGRADE、部分成交回流、偏离统一口径
