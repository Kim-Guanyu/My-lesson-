# 秒杀高并发方案说明

> 版本：v2.1（扣减与建单消息合为事务消息）  
> 涉及模块：`ml-sale`、`ml-order`、`ml-common`、小程序 `ml-miniapp`

---

## 1. 改造背景

旧版秒杀使用 **全局 Redisson 锁 `skLock`** + **Redis get/incr 非原子扣减**，存在：

- 全活动串行，万级并发下 QPS 低、延迟高
- `findUnpaidSn` 在锁外，用户可重复下单
- 客户端传入 `fkUserId`、价格，存在篡改风险
- 库存 Key 仅按课程维度，多活动互相影响

---

## 2. 架构概览

```
客户端 (token)
    │
    ▼
ml-gateway（Token 校验）
    │
    ▼
ml-sale /seckill/kill
    ├─ TokenUserResolver：从 Redis 解析真实 userId
    ├─ 用户限流（Redis 秒级计数）
    ├─ 活动状态缓存 / 商品明细缓存（价格以服务端为准）
    └─ RocketMQ 事务消息
         ① 发半消息（消费端不可见）
         ② 本地事务 = Lua 原子扣库存 + 写用户占位 + 预支付快照
         ③ 按本地事务结果提交或丢弃半消息
         ④ ③ 的网络往返丢失时，broker 回查占位 Key 裁决
            │
            ▼（提交后消息才可见）
ml-order OrderMessageListener
    └─ createSeckillOrder（sn 幂等 + 占位归属校验 + 唯一索引兜底）
```

---

## 3. Redis Key 规范

| Key | 格式 | 说明 |
|-----|------|------|
| 库存 | `seckill:stock:{seckillId}:{courseId}` | 活动维度隔离 |
| 用户占位 | `seckill:user:{seckillId}:{courseId}:{userId}` | 值为订单号 `sn`，TTL 960s；同时充当事务回查的状态表，见 §6 |
| 用户限流 | `seckill:rate:{userId}:{epochSecond}` | 每秒请求计数 |

工具类：`com.mdkj.util.SeckillRedisKeys`

> 旧 Key `seckill:course_count:{courseId}` 已废弃，超时回滚等逻辑仍兼容旧消息（无 `fkSeckillId` 时）。

---

## 4. 核心流程（kill 接口）

1. **鉴权**：请求头 `token` → `TokenUserResolver` 解析用户，**不再信任** Body 中的 `fkUserId`
2. **限流**：单用户默认 **10 次/秒**（`ML.Seckill.USER_KILL_RATE_PER_SECOND`）
3. **校验**：活动状态、活动明细中存在该课程
4. **重复请求**：若用户占位 Key 已有 `sn`，直接返回（幂等）
5. **事务消息**：先发半消息，再在本地事务里做 Lua 原子操作
   - 库存 > 0 且用户未占位 → `DECR` 库存 + `SET` 用户占位
   - 返回 `1` 成功 / `0` 重复 / `-1` 无库存
6. **提交或丢弃**：Lua 返回 `1` 才提交半消息；`0` / `-1` 直接丢弃（库存本就没动，无需补偿）。
   提交/回滚的网络往返若失败，不再回滚库存，改由 broker 回查裁决，详见 §6
7. **价格**：从 `seckill_detail` 读取 `course_price`、`sk_price`，**不信任客户端**

### Kill 请求体（v2）

```json
{
  "fkSeckillId": 1,
  "fkCourseId": 1
}
```

---

## 5. Lua 脚本语义

```lua
-- KEYS[1] 库存  KEYS[2] 用户占位
-- ARGV[1] 订单号 sn   ARGV[2] 占位 TTL（秒）
若用户 Key 已存在 → 返回 0
若库存 <= 0       → 返回 -1
DECR 库存 + SET 用户 Key → 返回 1
```

实现类：`ml-sale` → `SeckillStockService`

---

## 6. 事务消息：扣库存与建单消息的原子绑定

### 6.1 为什么需要

v2.0 的写法是「先 Lua 扣减 → 再 `convertAndSend` → catch 到异常就 `rollbackKill` 回滚库存」。

问题在于 **网络超时的语义是三态：成功 / 失败 / 未知**，而 `try-catch` 只有两态。把「未知」当成「失败」去补偿，会在消息其实已经进了 broker 的情况下把库存白还回去 —— 消费端照常建单，于是出现「**订单有效但库存已释放**」的悬空单。

1000 并发压测实测（见 [压测报告](seckill-loadtest-report.md)）：Lua 扣减成功 96 次，其中 25 次发送超时触发了错误回滚。终态 DB 订单 96 张、Redis 占位只剩 71、库存 29 —— 理论最大成交 96+29=125 > 初始库存 100。

### 6.2 三阶段

```
① sendMessageInTransaction 发半消息      写入 broker 但对消费端不可见
② executeLocalTransaction               Lua 扣减 + 写占位 + 预支付快照
③ 返回 COMMIT / ROLLBACK                 broker 据此投递或丢弃半消息
④ ③ 的往返丢失 → broker 主动回查          checkLocalTransaction 裁决
```

关键差别在于失败点的语义变了：

| 失败点 | 行为 | 库存 |
|--------|------|------|
| 半消息发送失败 | 抛 `MQClientException`，**本地事务根本没执行** | 分毫未动，无需补偿 |
| 本地事务返回 `ROLLBACK` | broker 丢弃半消息，消费端永远收不到 | Lua 没扣成功才会走这里 |
| 提交/回滚的往返失败 | 客户端**只记日志不抛异常** | 不再误回滚，转入回查 |

### 6.3 回查判据

用户占位 Key `seckill:user:{seckillId}:{courseId}:{userId}` 的值就是订单号，而它只在 Lua 扣减成功时写入。所以：

> **占位值 == 本消息的 sn ⇔ 本地事务执行成功**

这个 Key 天然就是一张事务状态表，不需要额外建表或落库。

### 6.4 两条不变量

- **Lua 返回 1 之后绝不能返回 `ROLLBACK`**：`ROLLBACK` 只丢弃半消息，**不会**把库存加回去，那样是永久少卖。所以预支付快照写失败时只记日志、照常提交（拉码接口是 DB 优先、快照兜底，快照缺失不影响最终支付）。
- **预支付快照必须在消息可见之前写入**：否则消费端建单后先调 `clearPrepayAfterOrderCreated` 删快照、生产端再写，会留下无人清理的脏 Key。

### 6.5 独立的 producer group

broker 回查时按 **producer group** 轮询挑 channel（`ProducerManager#getAvailableChannel`）。ml-sale 与 ml-order 共用配置中心的 `rocketmq.producer.group`，若沿用同一个组，回查有概率落到没有事务监听器的 ml-order 上被静默丢弃，只能等下一轮（默认 60s）。

因此 ml-sale 用 `SeckillTxProducerConfig` 单独建了一个 `RocketMQTemplate`，producer group 为 `ml-sale-seckill-tx-group`，与事务监听器绑定。

> 容器里因此存在两个 `RocketMQTemplate` Bean，注入点的字段名必须与 Bean 名一致，否则 `@Resource` 会退化成按类型注入而报 `NoUniqueBeanDefinitionException`。

### 6.6 已知限制

- **占位 TTL 与回查窗口余量偏紧**：默认最长回查窗口 ≈ 6s + 15×60s ≈ 15 分钟，占位 TTL 是 960s = 16 分钟，只剩 1 分钟余量。极端情况下占位先过期会被误判为回滚 → 少卖（方向安全，不会超卖）。若要加固，把 `ML.Seckill.USER_ORDER_TTL_SECONDS` 提到 1200。
- **注解的线程池参数会被静默忽略**：`TransactionMQProducer#initTransactionEnv` 在 `start()` 时就固化了 `checkExecutor`，而 `RocketMQTransactionConfiguration#setExecutorService` 在那之后才执行。这是 rocketmq-spring 的既有行为，回查实际跑在默认单线程池上，本项目回查量极小不受影响。

---

## 7. 订单侧保障（ml-order）

| 机制 | 说明 |
|------|------|
| sn 幂等 | 同一订单号重复消费直接返回 |
| 占位归属校验 | 占位值必须仍等于本消息的 `sn`，否则视为过期消息拒绝建单 |
| 唯一索引兜底 | `uk_order_sn` + 捕获 `DuplicateKeyException`，最终幂等防线 |
| 待付款防重 | 已有其他未付款订单则拒绝建单，建单失败时按占位归属幂等回补库存 |
| 超时取消 | 延迟消息携带 `fkSeckillId/fkUserId`，取消后回滚库存并删除用户占位 |
| OrderMessage | 新增 `fkSeckillId` 字段 |

---

## 8. 预热与压测

- **定时预热**：`SeckillJob.initSeckill` 写入新 Key `seckill:stock:{seckillId}:{courseId}`
- **压测准备**：`POST /seckill/prepareLoadTest?seckillId=&stock=` 重置库存并清理该活动用户占位

压测脚本 Redis 校验 Key：

```
seckill:stock:{seckillId}:{courseId}
```

---

## 9. 错误码

| Code | 含义 |
|------|------|
| 7002 | 库存不足 |
| 7003 | 操作过于频繁 |

---

## 10. 部署注意

1. **重启服务**：`ml-sale`、`ml-order`（消费逻辑变更）
2. **Redis 迁移**：活动开始前执行 XXL-JOB `initSeckill`，或调用 `prepareLoadTest` 写入新库存 Key
3. **小程序**：已改为仅传 `fkSeckillId`、`fkCourseId`，需发布新版
4. **压测脚本**：`loadtest-1000.mjs` 已同步，仍按用户 Token 区分账号
5. **v2.1 事务消息自检**（重启 ml-sale 后确认）：
   - 日志里没有 `NoUniqueBeanDefinitionException`（两个 Template Bean 按名注入成功）
   - 日志里没有 `already exists a transaction listener`（监听器绑到了独立 Template）
   - RocketMQ 控制台 → Producer 页能同时看到 `ml-group` 与 `ml-sale-seckill-tx-group`
   - 无需改 Nacos：独立 producer group 写在代码里，`rocketmq.producer.*` 的其余参数仍沿用配置中心

---

## 11. 后续可扩展

- 网关层 Sentinel 全局限流
- 秒杀排队页（令牌桶下发 `killToken`）
- 支付成功后删除用户占位 Key（当前依赖 TTL 自然过期）

---

## 12. 变更文件清单

| 文件 | 变更 |
|------|------|
| `ml-common/.../SeckillRedisKeys.java` | 新增 Key 规范 |
| `ml-common/.../TokenUserResolver.java` | Token 解析用户 |
| `ml-common/.../ML.java` | 秒杀常量 |
| `ml-common/.../OrderMessage.java` | +fkSeckillId |
| `ml-common/.../OrderTimeoutMessage.java` | +fkSeckillId/fkUserId |
| `ml-sale/.../SeckillStockService.java` | Lua + 限流；v2.1 移除 `rollbackKill`（改由事务消息裁决） |
| `ml-sale/.../SeckillServiceImpl.java` | kill 重构；v2.1 改用 `sendMessageInTransaction` |
| `ml-sale/.../config/SeckillTxProducerConfig.java` | **v2.1 新增** 事务消息专用 Template 与独立 producer group |
| `ml-sale/.../component/SeckillTxListener.java` | **v2.1 新增** 本地事务（Lua 扣减）与事务回查 |
| `ml-sale/.../component/SeckillTxContext.java` | **v2.1 新增** 本地事务上下文，回传 Lua 三态结果 |
| `ml-sale/src/test/.../SeckillTxListenerTest.java` | **v2.1 新增** 9 个用例覆盖提交/回滚/回查分支 |
| `ml-sale/.../KillDTO.java` | 精简请求体 |
| `ml-sale/.../SeckillController.java` | +token 头 |
| `ml-sale/.../SeckillJob.java` | 新库存 Key |
| `ml-order/.../OrderServiceImpl.java` | 防重 + 回滚 |
| `ml-order/.../OrderMessageListener.java` | 新 Key 回滚 |
| `ml-miniapp/pages/index/index.js` | 请求体精简 |
