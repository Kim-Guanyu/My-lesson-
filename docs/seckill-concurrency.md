# 秒杀高并发方案说明

> 版本：v2.0（企业级改造）  
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
    ├─ 读取 SeckillDetail 校验活动商品与价格
    ├─ SeckillStockService：Lua 原子扣库存 + 用户占位
    └─ RocketMQ 异步建单
            │
            ▼
ml-order OrderMessageListener
    └─ createSeckillOrder（sn 幂等 + 待付款防重）
```

---

## 3. Redis Key 规范

| Key | 格式 | 说明 |
|-----|------|------|
| 库存 | `seckill:stock:{seckillId}:{courseId}` | 活动维度隔离 |
| 用户占位 | `seckill:user:{seckillId}:{courseId}:{userId}` | 值为订单号 `sn`，TTL 960s |
| 用户限流 | `seckill:rate:{userId}:{epochSecond}` | 每秒请求计数 |

工具类：`com.mdkj.util.SeckillRedisKeys`

> 旧 Key `seckill:course_count:{courseId}` 已废弃，超时回滚等逻辑仍兼容旧消息（无 `fkSeckillId` 时）。

---

## 4. 核心流程（kill 接口）

1. **鉴权**：请求头 `token` → `TokenUserResolver` 解析用户，**不再信任** Body 中的 `fkUserId`
2. **限流**：单用户默认 **10 次/秒**（`ML.Seckill.USER_KILL_RATE_PER_SECOND`）
3. **校验**：活动状态、活动明细中存在该课程
4. **重复请求**：若用户占位 Key 已有 `sn`，直接返回（幂等）
5. **Lua 原子操作**：
   - 库存 > 0 且用户未占位 → `DECR` 库存 + `SET` 用户占位
   - 返回 `1` 成功 / `0` 重复 / `-1` 无库存
6. **发 MQ**：仅在 Lua 成功后发送；失败则 `rollbackKill` 回滚库存与占位
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

## 6. 订单侧保障（ml-order）

| 机制 | 说明 |
|------|------|
| sn 幂等 | 同一订单号重复消费直接返回 |
| 待付款防重 | 已有其他未付款订单则拒绝建单，MQ 失败回滚 Redis |
| 超时取消 | 延迟消息携带 `fkSeckillId/fkUserId`，取消后回滚库存并删除用户占位 |
| OrderMessage | 新增 `fkSeckillId` 字段 |

---

## 7. 预热与压测

- **定时预热**：`SeckillJob.initSeckill` 写入新 Key `seckill:stock:{seckillId}:{courseId}`
- **压测准备**：`POST /seckill/prepareLoadTest?seckillId=&stock=` 重置库存并清理该活动用户占位

压测脚本 Redis 校验 Key：

```
seckill:stock:{seckillId}:{courseId}
```

---

## 8. 错误码

| Code | 含义 |
|------|------|
| 7002 | 库存不足 |
| 7003 | 操作过于频繁 |

---

## 9. 部署注意

1. **重启服务**：`ml-sale`、`ml-order`（消费逻辑变更）
2. **Redis 迁移**：活动开始前执行 XXL-JOB `initSeckill`，或调用 `prepareLoadTest` 写入新库存 Key
3. **小程序**：已改为仅传 `fkSeckillId`、`fkCourseId`，需发布新版
4. **压测脚本**：`loadtest-1000.mjs` 已同步，仍按用户 Token 区分账号

---

## 10. 后续可扩展

- 网关层 Sentinel 全局限流
- 秒杀排队页（令牌桶下发 `killToken`）
- 支付成功后删除用户占位 Key（当前依赖 TTL 自然过期）

---

## 11. 变更文件清单

| 文件 | 变更 |
|------|------|
| `ml-common/.../SeckillRedisKeys.java` | 新增 Key 规范 |
| `ml-common/.../TokenUserResolver.java` | Token 解析用户 |
| `ml-common/.../ML.java` | 秒杀常量 |
| `ml-common/.../OrderMessage.java` | +fkSeckillId |
| `ml-common/.../OrderTimeoutMessage.java` | +fkSeckillId/fkUserId |
| `ml-sale/.../SeckillStockService.java` | Lua + 限流 |
| `ml-sale/.../SeckillServiceImpl.java` | kill 重构 |
| `ml-sale/.../KillDTO.java` | 精简请求体 |
| `ml-sale/.../SeckillController.java` | +token 头 |
| `ml-sale/.../SeckillJob.java` | 新库存 Key |
| `ml-order/.../OrderServiceImpl.java` | 防重 + 回滚 |
| `ml-order/.../OrderMessageListener.java` | 新 Key 回滚 |
| `ml-miniapp/pages/index/index.js` | 请求体精简 |
