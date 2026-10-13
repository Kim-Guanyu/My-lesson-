# 秒杀链路压测报告：一个补偿竞态的发现与修复

> 压测工具：Apache JMeter 5.6.3（Windows 宿主机）
> 被压环境：单台虚拟机 192.168.211.132
> 两轮压测：2026-09-18 00:38（v2.0，发现问题）｜2026-09-21 00:1x（v2.1，验证修复）

相关文档：[秒杀高并发方案](seckill-concurrency.md)｜[中间件复习手册](middleware-review.md)

---

## TL;DR

1000 并发打 100 库存，压出了一个「MQ 发送超时被误判为发送失败」的补偿竞态：

```
Lua 扣减成功 96 次
  ├─ 71 次 convertAndSend 正常          → 返回订单号
  └─ 25 次超时 → catch 里回滚库存        ← 但消息其实已投递，消费端已建单
终态：DB 订单 96 张、Redis 占位 71 个、库存 29 个
     理论最大成交 96 + 29 = 125 > 初始库存 100
```

**根因**：网络超时的语义是三态（成功 / 失败 / **未知**），而 `try-catch` 只有两态。把「未知」当成「失败」去补偿，就造出了「订单有效但库存已释放」的悬空单。

**修复**：改用 RocketMQ 事务消息，让「扣库存」和「消息可投递」成为一个原子单元；提交 ack 丢失时不再靠 catch 猜，交给 broker 回查裁决。

**复测**：DB 订单 100 = Redis 占位 100，订单数 + 库存 = 100，五口径全部闭合。

| 口径 | v2.0 | v2.1 |
|------|------|------|
| DB 订单数 | 96 | **100** |
| Redis 占位 | 71 ❌ | **100** ✅ |
| Redis 库存 | 29 | **0** |
| 订单数 + 库存 | **125** ❌ | **100** ✅ |
| 「发送失败回滚」日志 | 25 次 | **0 次** |

---

## 目录

- [一、为什么写这份报告](#一为什么写这份报告)
- [二、压测环境](#二压测环境)
- [三、压测方案](#三压测方案)
- [四、第一轮原始数据（v2.0）](#四第一轮原始数据v20)
- [五、五口径对账](#五五口径对账)
- [六、主要发现：发送超时被误判为发送失败](#六主要发现发送超时被误判为发送失败)
- [七、次要发现](#七次要发现)
- [八、未解问题](#八未解问题)
- [九、与历史压测对比](#九与历史压测对比)
- [十、修复方案](#十修复方案)
- [十一、第二轮复测（v2.1）](#十一第二轮复测v21)
- [十二、复现步骤](#十二复现步骤)

---

## 一、为什么写这份报告

此前的压测（2026-07-23，见 [`scripts/seckill-loadtest/`](../scripts/seckill-loadtest/) 下的 JSON 报告）结论是「四口径对账均为 100，未出现超卖」。

这次把并发从 100 提到 1000、并且改用 JMeter 的**同步定时器**制造尖峰流量后，压出了一个此前没被发现的竞态。**结论从「方案没问题」变成「方案在某个具体条件下会出问题」，这个变化本身就是压测的价值。**

---

## 二、压测环境

### 被压端（全部在同一台虚拟机上）

| 组件 | 部署方式 | 备注 |
|------|---------|------|
| ml-gateway / ml-user / ml-sale / ml-order | `java -jar`，**`-Xms256m -Xmx512m`** | 堆大小是本次问题的诱因，见[第六节](#六主要发现发送超时被误判为发送失败) |
| RocketMQ（rmqnamesrv / rmqbroker） | Docker | |
| Nacos / Elasticsearch / MinIO / XXL-JOB | Docker | |
| MySQL / Redis | 虚拟机本地 | |

JDK 17.0.12，日志统一输出到 `/opt/my-lesson/logs/*.log`。

### 压测端

JMeter 5.6.3 跑在 Windows 宿主机上，通过局域网访问虚拟机网关 `:24101`。

### 环境局限（影响数据解读）

- **所有被压组件 + MySQL + Redis + MQ 挤在一台虚拟机**，互相抢 CPU
- **JMeter 与被压端不在同一台机器**，但登录阶段的 BCrypt 运算把虚拟机 CPU 打满
- 单实例部署，无横向扩容，因此**本报告的吞吐量数字不代表方案上限**，只用于暴露正确性问题

---

## 三、压测方案

### JMeter 测试计划结构

```
测试计划
├── 用户定义的变量                SECKILL_ID=1 / COURSE_ID=1 / STOCK=100
├── HTTP请求默认值                192.168.211.132:24101
├── HTTP信息头管理器              Content-Type: application/json
│
├── setUp线程组「准备活动」  1 线程
│   ├── 00-管理员登录             → 提取 ADMIN_TOKEN
│   ├── 01-开始活动               POST /seckill/start/1
│   └── 02-重置库存               POST /seckill/prepareLoadTest?seckillId=1&stock=100
│
├── 线程组「并发秒杀」  1000 线程 / Ramp-Up 60s / 循环 1
│   ├── CSV数据文件设置           users1000.csv（loadtest001~1000）
│   ├── 03-登录                   → 提取 MY_TOKEN
│   └── 04-秒杀kill               POST /seckill/kill
│       ├── 同步定时器            模拟用户分组数量 = 1000  ★
│       └── JSON断言              $.code == 1000
│
├── 聚合报告
└── 查看结果树（已禁用）
```

计划文件：[`scripts/seckill-loadtest/jmeter/seckill-1000.jmx`](../scripts/seckill-loadtest/jmeter/seckill-1000.jmx)

### 三个关键设计

**① 一人一号，1000 个独立账号**

`fkUserId` 由服务端从 token 反解（[`KillDTO`](../ml-common/src/main/java/com/mdkj/dto/KillDTO.java) 只有 `fkSeckillId` + `fkCourseId`），请求体传不了用户标识。若多个线程共用一个 token，服务端会认为是同一个人：

- 单用户限流 `USER_KILL_RATE_PER_SECOND = 10` 挡掉大部分请求 → 7003
- 漏过限流的被 Lua 判为重复占位 → `return 0` → 回查占位返回**同一个 sn**
- 结果库存只会扣掉 1，报告却显示"成功率很高"

所以必须用 CSV 喂 1000 个账号，且「遇到文件结束符再次循环 = False」。

**② 同步定时器造尖峰**

Ramp-Up 60 秒是为了摊开登录（BCrypt 是 CPU 密集运算，1000 次并发登录会直接打满 CPU）。但那样 kill 请求就变成稀疏发出，不是并发。

同步定时器是集合点：每个线程登录完停在这里，攒齐 1000 个一起放行。**"1000 人同时点秒杀"靠的是它，不是线程数。**

**③ 压测边界画在 kill，不压支付**

- 支付宝当面付需要真人扫码，JMeter 模拟不了
- 压 `getQrCode` 等于在压支付宝沙箱，与自身架构无关
- **并发竞争在 kill 就结束了** —— 1000 人争 100 个库存；拿到 sn 之后每个人只操作自己那一单，彼此无竞争

---

## 四、第一轮原始数据（v2.0）

### 4.1 JMeter 聚合报告

| Label | #样本 | 平均值 | 中位数 | 90% | 95% | 99% | 最小值 | 最大值 | 异常% | 吞吐量 | 接收KB/s | 发送KB/s |
|-------|------:|------:|------:|----:|----:|----:|------:|------:|------:|-------:|-------:|-------:|
| 00-管理员登录 | 1 | 737 | 737 | 737 | 737 | 737 | 737 | 737 | 0.00% | 1.4/sec | 6.86 | 0.34 |
| 01-开始活动 | 1 | 3584 | 3584 | 3584 | 3584 | 3584 | 3584 | 3584 | 0.00% | 16.7/min | 0.07 | 0.07 |
| 02-重置库存 | 1 | 209 | 209 | 209 | 209 | 209 | 209 | 209 | 0.00% | 4.8/sec | 1.58 | 1.31 |
| **03-登录** | 1000 | **47499** | 51579 | 64185 | 65868 | 69777 | 4111 | **78310** | 0.00% | 8.2/sec | 3.30 | 2.11 |
| **04-秒杀kill** | 1000 | **12348** | 11998 | 22660 | 23948 | **25341** | 1338 | 25452 | **92.90%** | **39.3/sec** | 10.32 | 9.82 |
| 总体 | 2003 | 29881 | 23217 | 62030 | 64185 | 67587 | 209 | 78310 | 46.38% | 13.2/sec | 4.41 | 3.35 |

单位：毫秒。

`04-秒杀kill` 异常率 92.90% → **成功 71 次，失败 929 次**。

### 4.2 Redis 终态

```
GET  seckill:stock:1:1        →  29
KEYS seckill:user:1:1:*       →  71 个
```

### 4.3 MySQL 终态

**订单总数**（课程 1、秒杀来源、压测时段）：

```sql
SELECT COUNT(DISTINCT o.id) AS 订单数
FROM ml_oms.`order` o
JOIN ml_oms.order_detail od ON o.id = od.fk_order_id AND od.deleted = 0
WHERE od.fk_course_id = 1 AND o.info LIKE '%秒杀%' AND o.deleted = 0
  AND o.created >= '2026-09-18 00:38:00';
```

→ **96**

**建单时间分布**：

| created | 单数 |
|---------|-----:|
| 00:38:32 | 3 |
| 00:38:33 | 4 |
| 00:38:34 | 4 |
| 00:38:35 | 3 |
| 00:38:36 | 9 |
| 00:38:37 | 10 |
| 00:38:38 | 6 |
| 00:38:39 | 4 |
| 00:38:40 | 12 |
| 00:38:41 | 11 |
| 00:38:42 | 15 |
| 00:38:43 | 7 |
| 00:38:44 | 8 |
| **合计** | **96** |

96 单集中在 **13 秒**内落库 → 建单速率约 **7.4 单/秒**。

**状态分布**：

| status | 单数 | 说明 |
|-------:|-----:|------|
| 0 未付款 | 96 | 本次压测产生，延时消息尚未触发 |
| 2 已取消 | 1 | 00:05:02 的单线程调试单，10 分钟后被延时消息取消 |

**订单号重复检查**：

```sql
SELECT sn, COUNT(*) c FROM ml_oms.`order` GROUP BY sn HAVING c > 1;
```

→ 0 行（`uk_order_sn` 唯一索引生效）

### 4.4 消费端日志

```bash
grep "MQ 收到秒杀订单消息" /opt/my-lesson/logs/ml-order.log | grep -c "2026-09-18T00:3"
```

→ **96**

> 注：Spring Boot 3 默认日志时间格式是 ISO-8601（`2026-09-18T00:38:32.123+08:00`），分隔符是 `T` 不是空格。用空格过滤会得到 0。

---

## 五、五口径对账

| 口径 | 值 | 数据源 |
|------|---:|--------|
| 消费端实际收到消息 | **96** | ml-order.log |
| DB 订单数 | **96** | MySQL |
| Redis 用户占位 | **71** | `KEYS seckill:user:1:1:*` |
| Redis 剩余库存 | **29** | `GET seckill:stock:1:1` |
| JMeter API 成功数 | **71** | 聚合报告 |

### 算术闭合

```
Lua 扣减成功        96 次       库存 100 → 4
其中回滚            25 次       库存 4 → 29
净扣减              96 - 25 = 71

库存核对：100 - 96 + 25 = 29          ✅
占位核对：净扣减 71 = 占位 71          ✅
订单核对：消息 96 = 订单 96            ✅
API 核对：96 - 25 = 71                ✅
```

五个数字互相印证，误差为 0。

### 关键差值：订单 96 − 占位 71 = 25

**有 25 张订单，其名下的占位已被删除、库存已被归还。**

删占位 + `INCR` 库存这个组合动作，全项目只有 `rollbackIfOwned` 会做（[`SeckillStockCompensator.java:22-33`](../ml-common/src/main/java/com/mdkj/component/SeckillStockCompensator.java)）。占位 TTL 到期（960 秒）只会删 key，**不会加库存**。所以这 25 次补偿是真实发生的，不是推测。

### 为什么不可能是超时取消或消费端补偿

| 候选路径 | 排除依据 |
|---------|---------|
| `OrderTimeoutListener` 超时取消 | 代码是**先取消订单、取消成功才回补库存**（[`OrderServiceImpl.java:678-689`](../ml-order/src/main/java/com/mdkj/service/impl/OrderServiceImpl.java)）。而这 96 单全是 `status=0`，没有被取消过 |
| `OrderMessageListener` 消费端补偿 | `createSeckillOrder` 是 `@Transactional(rollbackFor = Exception.class)`，建单失败会整体回滚，**不会留下订单**。而 96 单确实存在 |

**只剩生产端一条路。**

---

## 六、主要发现：发送超时被误判为发送失败

### 6.1 竞态时序

```
时刻  生产端 ml-sale                          broker        消费端 ml-order
────────────────────────────────────────────────────────────────────────────
t0    Lua 扣减成功，写占位 sn=X  (库存 -1)
t1    convertAndSend(...)  ──────────────►  消息已入队
t2    ⏳ 等 ack（Full GC 停顿，超过 3s 默认超时）
t3                                          消息投递  ──►  校验占位 == X ✅
t4                                                          建单成功（status=0）
t5    ❌ 抛 RemotingTimeoutException
t6    catch → rollbackKill()   (库存 +1，占位删除)
t7    throw ServiceException("秒杀下单失败，请稍后重试")
t8    JMeter 记为失败

终态：订单存在（96 之一）、库存已归还（29 之一）、占位不存在
```

### 6.2 问题代码

[`ml-sale/src/main/java/com/mdkj/service/impl/SeckillServiceImpl.java:318-327`](../ml-sale/src/main/java/com/mdkj/service/impl/SeckillServiceImpl.java)

```java
try {
    rocketmqTemplate.convertAndSend("ml-topic:ml-tag", orderMessage);
    log.info("秒杀 MQ 发送成功，用户 {} 课程 {} 订单 {}", fkUserId, fkCourseId, sn);
} catch (Exception e) {
    log.error("秒杀 MQ 发送失败，回滚库存", e);
    boolean rolledBack = seckillStockService.rollbackKill(
            fkSeckillId, fkCourseId, fkUserId, sn);     // ← 假设了「异常 ⇒ 消息没投出去」
    throw new ServiceException(ResultCode.SERVER_ERROR, "秒杀下单失败，请稍后重试");
}
```

**根因一句话：超时是「结果未知」，不是「失败」。** 把未知当失败去补偿，就出现了订单和库存两边都算数的状态。

这是分布式系统里的经典陷阱 —— 网络超时的语义是三态（成功 / 失败 / 未知），而 `try-catch` 只有两态。

### 6.3 诱因：堆内存 512m

服务启动参数是 `-Xms256m -Xmx512m`。1000 并发下 ml-sale 要同时处理：

- token 解析（读 Redis）
- 活动状态 / 商品明细缓存读写
- 构造 `OrderMessage` 对象
- MQ producer 的发送缓冲

512m 堆在这个量级下频繁 Full GC，**单次停顿轻易超过 `convertAndSend` 的 3 秒默认发送超时**。一次长 GC 就足以让「消息已送达但客户端等不到 ack」发生 25 次。

### 6.4 后果：真实的超卖窗口

```
已成交（订单有效）        96
可再被抢（库存已归还）    29
────────────────────────────
理论最大成交             125  >  初始库存 100
```

**本次没有真超卖纯属偶然** —— 库存归还的那一刻，剩余请求已全部被拒，没有新的抢购者。

把 JMeter 的**循环次数改成 2**（或延长压测时间），让第二波请求去抢那 29 个被归还的库存，就能压出真实超卖。

---

## 七、次要发现

### 7.1 登录是本次测试的瓶颈，不是秒杀

| 请求 | 平均 | 99% | 最大 |
|------|-----:|----:|-----:|
| 03-登录 | **47.5 s** | 69.8 s | **78.3 s** |
| 04-秒杀kill | 12.3 s | 25.3 s | 25.5 s |

`loginByAccount` 里有 `BCrypt.checkpw()`（[`UserServiceImpl.java:533`](../ml-user/src/main/java/com/mdkj/service/impl/UserServiceImpl.java)），这是**故意设计成慢**的 CPU 密集运算；再加上 `buildLoginVO` 要查角色、查菜单、写 Redis。1000 次登录挤在 60 秒 Ramp-Up 里，虚拟机 CPU 直接打满。

**这是测试方法的开销，不是秒杀链路的性能。** 项目里的 Node 压测脚本把登录和秒杀彻底分离（`loginConcurrency: 120` 单独限并发），是更正确的做法。

### 7.2 kill 的 25 秒是排队，不是超时

```
吞吐量 39.3/sec  →  1000 ÷ 39.3 ≈ 25.4 秒  ≈  最大响应时间 25452ms
```

同步定时器放行后 1000 个请求排成一队，最后一个等了 25 秒。**不是任何超时配置导致的。**

### 7.3 削峰效果的量化

| 环节 | 速率 |
|------|-----:|
| kill 接口吞吐 | 39.3 /sec |
| 消费端建单 | ≈7.4 /sec |
| **缓冲倍数** | **≈5.3×** |

用户在 kill 返回时就拿到订单号去拉支付二维码了（靠 `seckill:prepay:{sn}` 预支付快照，不依赖订单落库），而 DB 在后面慢慢写。**这个倍数就是 MQ 削峰买来的时间。**

### 7.4 活动状态缓存存在击穿风险

`seckill:status:{id}` 的 TTL 只有 5 秒（`ML.Seckill.STATUS_CACHE_SECONDS = 5`）。`01-开始活动` 写入缓存，但 kill 请求发生在 60~140 秒之后 —— 缓存早已过期。

于是**大批请求同时未命中、同时回源查 MySQL**（[`SeckillServiceImpl.java:241-249`](../ml-sale/src/main/java/com/mdkj/service/impl/SeckillServiceImpl.java)）。典型的缓存击穿。

短 TTL 的设计意图是「让状态翻转能快速自愈」，但代价是挡不住集中流量。

### 7.5 运维层面的问题

| 现象 | 影响 |
|------|------|
| 同时跑着两个 `ml-order` 实例（PID 113732 / 7370） | 同一 consumer group 会 rebalance，**不会重复建单**，但白占内存、CPU，且让日志难以判读 |
| 项目没有任何 `logback-spring.xml` / `log4j2.xml` | 日志只输出到 stdout，靠 `nohup >` 重定向。**重启时若用 `>` 而非 `>>`，会直接清空历史日志**（本次 ml-sale.log 就是这样丢失的） |

---

## 八、未解问题

诚实列出，避免这份报告被过度引用。

### 8.1 929 次失败的错误码构成未知

| 可能 | 含义 |
|------|------|
| 7002 库存不足 | 正常拒绝，说明系统扛住了压力 |
| 500 / 连接超时 | 服务被打崩，请求压根没到 Lua |

这个区别很重要：如果大部分是 500，说明本次压测测出的是「系统在 1000 并发下崩了」，而不是「秒杀逻辑的真实表现」。

**为什么查不了**：本次在 JMeter GUI 里运行，没有生成 `result.jtl`；而 `ml-sale.log` 在重启服务时被 `>` 清空。

**下次怎么拿**：用命令行跑（或在 GUI 里加「简单数据写入器」输出 `result.jtl`），事后 `jmeter -g result.jtl -o report` 生成的 HTML 报告里 `Errors` 页会按错误码分类。

> 第二轮复测时这个问题**部分解决**了：虽然仍未采集错误码明细，但日志计数给出了更强的证据 —— `Lua=1` 101 次、消费端收到 101 条、DB 落库 100 单，三者自洽，说明系统是在「正常拒绝」而非「被打崩」。若是后者，`Lua=1` 的次数会远低于扣减应有的量。

### 8.2 总回滚次数可能大于 25

设：

- `D` = Lua 扣减总次数
- `R` = 回滚总次数
- `Y` = 「发送确实失败、消息未投递、回滚成功」的次数（这类不产生订单）

则 `D = 96 + Y`，`R = 25 + Y`，而

```
库存 = 100 - D + R = 100 - (96+Y) + (25+Y) = 29
```

**`Y` 在算术上被抵消了**，所以库存数字无法约束它。`Y` 可能是 0，也可能几十。

若 `Y` 较大，则 `D` 可能超过 100，库存中途触达 0，那部分 7002 就是合法拒绝。**这也是 8.1 必须补数据的原因。**

拿到 ml-sale 的 `grep -c "MQ 发送失败"` 即可直接得到 `R = 25 + Y`。

> 这个盲点在 v2.1 被结构性地消除了：事务消息把「发送」拆成半消息 + 提交两步，两步的失败分别有独立日志关键字（`秒杀半消息发送失败` / `秒杀事务回查`），不再需要靠算术反推。

---

## 九、与历史压测对比

| 项目 | 2026-07-23（Node 脚本） | 2026-07-23（Node，远程） | **2026-09-18（JMeter）** |
|------|------------------------|------------------------|------------------------|
| 报告文件 | `report-1780317907007.json` | `report-1784813987976.json` | 本报告 |
| 总请求 / 并发 | 10000 / 10000（每波 500） | 1000 / 1000（每波 250） | 1000 / 1000（同步定时器一次放行） |
| 初始库存 | 100 | 100 | 100 |
| API 成功 | 100 | 103 | **71** |
| 唯一订单号 | 100 | 103 | 71 |
| Redis 终态库存 | 0 | 0 | **29** |
| DB 订单数 | 100 | 142 | **96** |
| 脚本 oversell 判定 | false | **true** | — |
| 错误构成 | 库存不足 9900 | 库存不足 787 / **秒杀下单失败 38** / undefined 72 | 未采集（见 8.1） |

### 两点值得注意

**① 7 月的远程压测其实已经带着同一个 bug 的指纹**

`report-1784813987976.json` 里有 **38 次「秒杀下单失败，请稍后重试」** —— 这正是 MQ 发送失败分支抛出的错误文案。同时 `dbOrderCount = 142 > stock = 100`。当时只被脚本标了一个 `oversell: true` 就过去了，没有深究。

**② 脚本的超卖判据过于简单**

[`loadtest-1000.mjs:241`](../scripts/seckill-loadtest/loadtest-1000.mjs) 用的是 `apiSuccess > stock` → 超卖。但库存被合法回补后再次售出时，`apiSuccess` 超过初始库存是**正常**的。

更严谨的不变量应该是：

```
最终有效订单数（status ≠ 已取消）≤ 初始库存
且  Redis 库存 ≥ 0
且  订单数 = 占位数（无悬空订单）  ← 本次就是这一条被打破了
```

最后那条正是本次发现问题的关键判据，建议加进脚本。

---

## 十、修复方案

### 10.1 主要问题：用事务消息替代「发送失败就回滚」（**已实施**）

**正解**：RocketMQ 事务消息（half message + 本地事务 + 事务回查）。

```
① sendMessageInTransaction 发半消息      写入 broker 但对消费端不可见
② executeLocalTransaction               Lua 扣减 + 写占位 + 预支付快照
③ 返回 COMMIT / ROLLBACK                 broker 据此投递或丢弃半消息
④ ③ 的往返丢失 → broker 主动回查          checkLocalTransaction 裁决
```

这样「扣库存」和「消息可投递」成为一个原子单元，不再需要靠 catch 猜测消息到没到。

**关键在于三个失败点的语义被拆开了**（反编译 `rocketmq-client-5.2.0` 的 `DefaultMQProducerImpl#sendMessageInTransaction` 字节码确认，异常表 `78-85`、`152-313`、`382-393`）：

| 失败点 | 客户端行为 | 库存 |
|--------|-----------|------|
| `send(半消息)` 失败 | 抛 `MQClientException`，**本地事务根本没执行** | 分毫未动，无需补偿 |
| `executeLocalTransaction` 抛异常 | 捕获后状态留 `UNKNOW`，**不抛给调用方** | 转入回查 |
| `endTransaction`（提交/回滚那次往返） | **捕获后只打日志，不抛** | **这就是修复点**：提交 ack 超时不再触发回滚 |

**回查判据**：用户占位 Key 的值就是订单号，而它只在 Lua 扣减成功时写入 ——

> **占位值 == 本消息的 sn ⟺ 本地事务执行成功**

这个 Key 天然就是一张事务状态表，不需要额外建表。而且它与消费端的「占位归属校验」、补偿脚本的 `rollbackIfOwned` 用的是**同一个判据**，三处一致，不会互相打架。

### 10.2 实施清单

| 文件 | 改动 |
|------|------|
| [`SeckillTxProducerConfig.java`](../ml-sale/src/main/java/com/mdkj/config/SeckillTxProducerConfig.java) | **新增**。事务消息专用 `RocketMQTemplate`，独立 producer group |
| [`SeckillTxListener.java`](../ml-sale/src/main/java/com/mdkj/component/SeckillTxListener.java) | **新增**。本地事务（Lua 扣减）与事务回查 |
| [`SeckillTxContext.java`](../ml-sale/src/main/java/com/mdkj/component/SeckillTxContext.java) | **新增**。回传 Lua 三态结果给 `kill()` |
| [`SeckillServiceImpl.java`](../ml-sale/src/main/java/com/mdkj/service/impl/SeckillServiceImpl.java) | `convertAndSend` + catch 回滚 → `sendMessageInTransaction`；新增只读预检 |
| [`SeckillStockService.java`](../ml-sale/src/main/java/com/mdkj/component/SeckillStockService.java) | 删除 `rollbackKill`（已成死代码）；新增 `preCheckKill` 与 `isPaid` 转发 |
| [`ML.java`](../ml-common/src/main/java/com/mdkj/util/ML.java) | 给 `USER_ORDER_TTL_SECONDS` 加硬约束注释 |
| `SeckillTxListenerTest.java` | **新增** 14 个用例 |

**ml-order 零改动** —— 消息体字节、topic、tag 与改造前完全一致，三层幂等在事务消息的重复投递下同样成立。

### 10.3 三个实现细节

**① 独立 producer group**

broker 回查时按 producer group 轮询挑 channel（`ProducerManager#getAvailableChannel` 是 `counter.incrementAndGet() % size`）。ml-sale 与 ml-order 共用配置中心的 `ml-group`，回查有概率落到没有事务监听器的 ml-order 上 —— 客户端只打一行 warn 就返回，这一轮回查被静默吞掉，白白消耗一次 `transactionCheckMax` 配额。配额耗尽（默认 15 次）后 broker 丢弃半消息，变成「库存已扣、订单永不创建」的永久少卖。

因此单独建了 `ml-sale-seckill-tx-group`。**代价**：容器里存在两个 `RocketMQTemplate` Bean，注入点字段名必须与 Bean 名逐字符一致，否则 `@Resource` 会退化成按类型注入而报 `NoUniqueBeanDefinitionException`。

**② 只读预检快路径**

事务消息把「先扣减再发送」倒转成「先发半消息再扣减」。不预检的话，1000 打 100 场景下约九成注定失败的请求也要先往 broker 写一条半消息、跑 Lua、再发一条回滚 —— broker 写入量放大约 10 倍，还给最热的 7002 响应路径插进一次 broker 往返。

所以加了一个**只读、不修改任何 Key** 的预检 Lua 提前短路。它不是权威判据，正确性始终由事务内那次 Lua 保证。

**③ 两条写进代码注释的不变量**

- **Lua 返回 1 之后绝不能返回 `ROLLBACK`**：ROLLBACK 只丢弃半消息，**不会**把库存加回去，那是永久少卖。所以预支付快照写失败时只记日志、照常提交（拉码接口是 DB 优先、快照兜底，快照缺失不影响最终支付）。单测 `shouldStillCommitWhenPrepaySnapshotFails` 专门守这条。
- **回查时已支付一律提交**：优先于占位校验。否则占位若因任何原因失效，就会出现「用户付了钱却永远没有订单」—— 比原来的超卖窗口更严重。这与 `rollbackIfOwned`、`handleSeckillOrderTimeout` 的 `isPaid` 守卫保持一致。

### 10.4 被否决的过渡方案

| 手段 | 为什么不够 |
|------|-----------|
| 拉长发送超时 / 调大堆 | 降低超时概率，不解决语义问题 |
| 改用 `syncSend` 检查 `SendResult.getSendStatus()` | 能区分 `FLUSH_DISK_TIMEOUT` 等，但超时异常时仍然拿不到结果 |
| 不回滚，交给消费端幂等兜底 | 库存少卖（29 个变成永久不卖），方向安全但浪费库存 |
| 增加对账任务 | 定期扫「有订单但无占位」的记录修正，治标 |

> **取舍说明**：少卖比超卖安全。若短期内不上事务消息，宁可不回滚（接受少卖），也不要制造悬空订单。

### 10.5 次要问题

| 问题 | 建议 | 状态 |
|------|------|------|
| 堆内存 512m | 提到 `-Xms512m -Xmx1g`（虚拟机总内存 10G、swap 已用 80%，再大会互相挤） | ✅ 已调 |
| 状态缓存击穿 | 逻辑过期 + 单飞（只放一个请求回源），或活动期间用长 TTL + 主动失效 | ⬜ 待办 |
| 无日志配置 | 加 `logback-spring.xml` 配滚动策略；重启统一用 `>>`（用 `>` 会截断成含 NUL 空洞的文件，`grep` 会退化成二进制模式静默失效） | ⬜ 待办 |
| 重复进程 | 服务本就由 systemd 托管，却一直用 `nohup` 手工起，两套机制互不认识导致端口冲突。统一走 `systemctl` | ✅ 已纠正 |
| 限流非原子 | `tryAcquireUserRate` 是 `INCR` + `EXPIRE` 两条命令，可合并为一个 Lua | ⬜ 待办 |
| 压测脚本判据 | 加入「订单数 = 占位数」不变量，见[第九节](#九与历史压测对比) | ⬜ 待办 |

### 10.6 测试方法改进

| 问题 | 改进 |
|------|------|
| 登录污染压测数据 | 登录单独一轮低并发跑完、token 落盘，秒杀轮只读 token |
| GUI 模式数据失真 | 正式跑一律 `-n` 命令行 + `-e -o report` |
| 未采集错误码明细 | 保留 `result.jtl`，事后可 `-g result.jtl -o report` 重新出报告 |
| 单轮压测掩盖了超卖 | 循环次数设为 2 以上，让第二波去抢被回补的库存 |

---

## 十一、第二轮复测（v2.1）

> 复测时间：2026-09-21 00:1x
> 环境变更：ml-sale / ml-order 堆调至 `-Xms512m -Xmx1g`；停掉 ml-barrage / ml-course 腾内存；统一由 systemd 托管、每个服务只留一个实例。压测方案（1000 线程 / Ramp-Up 60 / 同步定时器 1000）与第一轮完全一致。

### 11.1 五口径对账

| 口径 | v2.0（有 bug） | **v2.1（修复后）** |
|------|---------------|-------------------|
| 消费端实际收到消息 | 96 | 101 |
| DB 订单数 | 96 | **100** |
| Redis 用户占位 | 71 ❌ | **100** ✅ |
| Redis 剩余库存 | 29 | **0** |
| JMeter API 成功数 | 71 | 101 |
| **订单数 + 库存** | **125** ❌ 超卖窗口 | **100** ✅ |
| **订单数 == 占位数** | **否** ❌ | **是** ✅ |
| 重复 sn | 0 | 0 |

**核心判据「DB 订单数 == Redis 占位数」成立，悬空订单归零。** 库存精确扣到 0，一单不多一单不少。

### 11.2 日志计数与因果链

```bash
grep -ac "秒杀 MQ 发送失败，回滚库存" ml-sale.log    # ① 0    ← 旧 bug 的代码已删除
grep -ac "Lua=1"                     ml-sale.log    # ② 101
grep -ac "秒杀半消息发送失败"          ml-sale.log    # ③ 1
grep -ac "秒杀本地事务执行异常"        ml-sale.log    # ④ 0
grep -ac "秒杀事务回查"               ml-sale.log    # ⑤ 1
grep -ac "秒杀预支付快照写入失败"      ml-sale.log    # ⑥ 0
grep -ac "MQ 收到秒杀订单消息"         ml-order.log   # ⑦ 101
grep -ac "库存实际补偿"               ml-order.log   # ⑧ 1
```

> 注意 `-a`：日志文件曾被 `>` 截断留下 NUL 空洞，不加 `-a` 时 grep 会退化成二进制模式，只报「匹配到」而不输出内容，计数全部失真。另外 Spring Boot 3 默认日志时间戳是 ISO-8601（`T` 分隔而非空格），按时间过滤时要用 `T00:3` 这种写法。

**101 扣减 vs 100 订单的差值是补偿链路正常工作的证据，不是缺陷**：

```
② Lua=1          101   ← 101 次扣减成功
⑦ 消费端收到      101   ← 101 条消息全部投递（没有丢消息）
⑧ 消费端补偿        1   ← 1 单建单被业务规则拒绝，库存幂等回补
   DB 订单        100   ← 101 - 1
```

那一单是压测账号已持有课程 1，命中 `createSeckillOrder` 的「已购买/已有待付款订单」检查，`OrderMessageListener` 按「未支付 → 退库存 → 吞掉异常」处理。库存被正确还回去、占位被删除、**没有留下悬空订单**，所以 `订单数 = 占位数` 依然成立。

对比第一轮：同样是补偿被触发，v2.0 留下 25 张悬空单，v2.1 留下 0 张。**差别在于补偿现在只在"确实该补"的时候发生。**

⑤ 那次事务回查同样值得记录 —— 说明 `checkLocalTransaction` 这条新增路径**在真实压力下被触发过且裁决正确**，这是单元测试覆盖不到的。

### 11.3 延迟对比

| 请求 | v2.0 平均 | v2.1 平均 | v2.0 P99 | v2.1 P99 | v2.1 吞吐 |
|------|----------:|----------:|---------:|---------:|----------:|
| 03-登录 | 47.5 s | **19.4 s** | 69.8 s | 33.1 s | 12.3/s |
| 04-秒杀kill | 12.3 s | 12.3 s | 25.3 s | 28.2 s | 35.3/s |

登录耗时降了一半多（堆调大 + 停掉无关服务腾内存），但**仍是本次测试的主要开销** —— `BCrypt.checkpw` 是故意设计成慢的 CPU 密集运算，1000 次挤在 60 秒 Ramp-Up 里必然打满 CPU。这是测试方法的开销，不是秒杀链路的性能。

kill 的 28 秒是**排队**不是超时：`1000 ÷ 35.3 ≈ 28.3 s`，与最大响应时间几乎相等。

事务消息给 kill 增加了一次 broker 往返（半消息 + 提交），但平均耗时没有退化 —— 只读预检把约九成注定失败的请求挡在了 broker 之前，抵消了这部分开销。

### 11.4 结论

| 项 | 判定 |
|----|------|
| 补偿竞态 | ✅ 已消除，悬空订单归零 |
| 超卖窗口 | ✅ 已关闭，订单数 + 库存严格 = 初始库存 |
| Lua 原子扣减 | ✅ 一如既往无超卖 |
| 事务回查路径 | ✅ 真实触发过且裁决正确 |
| 消息投递 | ✅ 生产端扣减次数 = 消费端收到数，零丢失 |

---

## 十二、复现步骤

### 前置条件

```bash
# 虚拟机上
docker ps                     # rmqnamesrv / rmqbroker / nacos 必须 Up
ps -ef | grep -E "ml-(gateway|user|sale|order)" | grep -v grep   # 四个服务各一个实例
```

```sql
-- 活动与明细必须存在，否则 start / prepareLoadTest 会抛异常
SELECT s.id, s.status, d.fk_course_id, d.sk_count
FROM ml_sms.seckill s
LEFT JOIN ml_sms.seckill_detail d ON d.fk_seckill_id = s.id AND d.deleted = 0
WHERE s.id = 1 AND s.deleted = 0;
```

### 每轮压测前清理

```sql
-- 清掉未付款订单，否则 createSeckillOrder 的「已有待付款订单」检查会让建单全部失败
UPDATE ml_oms.`order`
SET status = 2, updated = NOW()
WHERE status = 0 AND info LIKE '%秒杀%' AND username LIKE 'loadtest%';
```

> `02-重置库存` 调的 `prepareLoadTest` 会自动清理 Redis 用户占位（`deleteByPrefix(userOrderPrefix)`），不用手动清。

### 造压测账号（首次）

```bash
cd scripts/seckill-loadtest
npm install mysql2 bcryptjs --no-save
node seed-users.mjs 1000        # loadtest001~1000 / LoadTest123
```

```powershell
cd jmeter
"username,password" | Set-Content -Encoding UTF8 users1000.csv
1..1000 | ForEach-Object { "loadtest{0:D3},LoadTest123" -f $_ } | Add-Content -Encoding UTF8 users1000.csv
```

### 运行

```powershell
cd scripts/seckill-loadtest/jmeter
Remove-Item -Recurse -Force report, result.jtl -ErrorAction SilentlyContinue
Get-Date -Format "yyyy-MM-dd HH:mm:ss"          # 记下开始时间，对账 SQL 要用
<JMETER_HOME>/bin/jmeter.bat -n -t seckill-1000.jmx -l result.jtl -e -o report
```

JMeter 需要调堆：编辑 `bin/jmeter.bat`，`set HEAP=-Xms1g -Xmx4g`。

### 采集数据（压测后 30 秒开始，**10 分钟内完成**）

> 未付款订单会被延时级别 14（10 分钟）的消息取消并回补库存。超过这个时间点，Redis 库存会涨回 100、订单变成 `status=2`。

```bash
# 虚拟机
grep "MQ 收到秒杀订单消息" /opt/my-lesson/logs/ml-order.log | grep -c "2026-MM-DDTHH:M"
grep -c "MQ 发送失败" /opt/my-lesson/logs/ml-sale.log
grep -c "库存实际补偿" /opt/my-lesson/logs/ml-order.log
```

```
# Redis
GET  seckill:stock:1:1
KEYS seckill:user:1:1:*
```

```sql
-- 订单数 / 状态分布 / 重复 sn，SQL 见第四节
```

HTML 报告：`report/index.html` → 左侧 `Statistics`（看 `04-秒杀kill` 行）与 `Errors`（看错误码分布）。

### 可选：验证超时补偿链路

数据采集完后什么都不动，等 10 分钟再查一次：

| 口径 | 期望 |
|------|------|
| Redis 库存 | 涨回接近 100 |
| Redis 占位 | 归零 |
| DB 订单 status | 全部变成 2 |

对上说明「延时消息超时取消 + 库存回补」链路正常。

---

## 附：一句话总结

> Redis Lua 的原子扣减本身没有超卖问题；出问题的是它外面那层「MQ 发送失败就回滚库存」的补偿逻辑 —— 它把网络超时的「结果未知」当成了「确定失败」，导致 25 张订单的库存被错误释放，形成理论超卖窗口。定位靠的是「订单数 ≠ 占位数」这个不变量被打破，而不是任何单一指标。改用 RocketMQ 事务消息后，「扣库存」与「消息可投递」成为原子单元，提交 ack 丢失时由 broker 回查占位 Key 裁决而非靠 catch 猜测，复测五口径全部闭合。

### 这次压测真正的收获

不是"我的方案没有超卖"，而是这三件事：

1. **单一指标会骗人**。API 成功 71、Redis 库存 29、DB 订单 96 —— 单看任何一个都像是正常的。只有把五个口径摆在一起，「订单数 ≠ 占位数」这个矛盾才会暴露出来。
2. **`try-catch` 装不下分布式调用的语义**。超时是三态里的「未知」，而 catch 块天然把它归进「失败」。这个错位在单机代码里无害，在跨网络调用里就是数据不一致的源头。
3. **压测的价值在于压出问题，不在于跑出漂亮数字**。第一轮如果只看「没有超卖」就收工，这个 bug 会一直潜伏到线上 —— 而它在持续流量下（循环 ≥ 2）会变成真实超卖。
