# 中间件复习手册：Redis / RocketMQ / Elasticsearch

> 目的：从「这东西是干什么的」到「代码怎么写」，用本项目的真实代码当例句。
> 适用：Spring Boot 3.2.5 / Java 17 / Redis / RocketMQ 5.2 / Elasticsearch 8
> 整理时间：2026-08-19

## 怎么用这份文档

**不需要能跑起来环境**——中间件的「调用代码」就是 Java 代码，读懂 + 会写就行，跑起来只是验证。
本项目三个中间件的调用代码加起来不到 300 行，本文把它们全部拆开逐行讲。

每个中间件按同一个结构讲，建议按顺序读：

1. **它是干什么的** —— 不讲原理，只讲「不用它会出什么问题」
2. **核心概念** —— 最少必要的几个名词
3. **怎么接入** —— pom + yml + 注入，三步模板
4. **API 速查表** —— 查着写代码
5. **项目真实代码逐行读** —— 从易到难
6. **背诵清单** —— 合上文档能默写出来才算过
7. **自检问题** —— 答不上来就回到对应小节

---

## 目录

- [〇、配置写法勘误（先看这个）](#〇配置写法勘误先看这个)
- [一、Redis](#一redis)
- [二、RocketMQ](#二rocketmq)
- [三、Elasticsearch](#三elasticsearch)
- [四、读代码顺序清单](#四读代码顺序清单)
- [五、等有环境之后](#五等有环境之后)
- [六、进阶方向（本文不展开）](#六进阶方向本文不展开)

---

## 〇、配置写法勘误（先看这个）

Spring Boot 2.x → 3.x 改过配置前缀，抄错了会出「连的是 localhost、报连接拒绝」这种莫名其妙的问题。

| 中间件 | Spring Boot 3 正确写法 | 本项目现状 |
|---|---|---|
| Redis | `spring.data.redis.host` / `port` | 正确（Boot 2.x 才是 `spring.redis.*`） |
| Elasticsearch | `spring.elasticsearch.uris` | `ml-barrage/src/main/resources/application.yml:6` 写对了；但 `README.md:305` 的 Nacos 配置骨架写成了 `spring.data.elasticsearch.uris`，**这个属性不存在**，会被静默忽略然后默认连 `localhost:9200` |
| RocketMQ | `rocketmq.name-server` + `rocketmq.producer.group` | 正确 |

> `spring.data.elasticsearch.*` 下只有 Repository 相关配置（如 `repositories.enabled`），**连接地址不在这个前缀下**。

---

## 一、Redis

### 1. 它是干什么的

一个**跑在独立进程里的内存 Key-Value 数据库**。

理解它最好的角度：**它就是一个所有微服务都能共享的 HashMap**。

本项目是微服务多实例部署（`ml-user`、`ml-order`… 各是独立 JVM）。假设 Token 存在 Java 的 `HashMap` 里：

- 用户在 `ml-user` 实例 A 登录，下一个请求打到 `ml-gateway`，网关拿不到这个 Token → 登录态丢了
- 服务重启 → 全部用户掉线
- 验证码想 5 分钟自动失效 → 得自己写定时清理

Redis 解决的就是这三件事：**跨进程共享、独立于应用的生命周期、原生支持过期时间**。

又因为它是内存 + 单线程，读写在十万 QPS 量级，所以顺带被当**缓存**（挡在 MySQL 前面）和**计数器**（秒杀库存、限流）用。

### 2. 五种数据结构

| 结构 | 是什么 | Redis 命令 | 本项目用在哪 |
|---|---|---|---|
| **String** | 一个 key 一个值（文本 / 数字 / JSON） | `SET` `GET` `DEL` `INCR` `SETEX` | Token、验证码、秒杀库存、统计缓存 |
| **Hash** | 一个 key 下挂多个 field-value（像个小对象） | `HSET` `HGET` `HGETALL` | 秒杀商品明细 `seckill:detail:1:1` |
| **List** | 有序可重复，两头能进出 | `LPUSH` `RPOP` `LRANGE` | 项目封装了未使用（可做最新列表） |
| **Set** | 无序不重复集合 | `SADD` `SISMEMBER` `SINTER` | 项目封装了未使用（可做点赞 / 共同关注） |
| **ZSet** | 带分数的有序集合 | `ZADD` `ZRANGE` `ZINCRBY` | 项目封装了未使用（可做排行榜） |

**现阶段只需熟练 String 和 Hash**，本项目 90% 都在用这两个。其余三个知道用途即可。

### 3. 怎么接入（三步模板）

**① pom.xml**（见 `ml-common/pom.xml:71`、`ml-course/pom.xml:88`、`ml-sale/pom.xml:89` 等）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**② yml**

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      # password: xxx
      # database: 0
```

**③ 注入**（Spring Boot 已自动装配，直接用）

```java
@Resource
private StringRedisTemplate stringRedisTemplate;
```

#### `StringRedisTemplate` vs `RedisTemplate`（最容易懵的点）

| | `RedisTemplate<Object,Object>` | `StringRedisTemplate` |
|---|---|---|
| 序列化 | 默认 JDK 序列化 | key / value 都按字符串处理 |
| redis-cli 里看到的 | `\xac\xed\x00\x05...` 乱码 | 可读文本 |
| 跨语言 | 不行 | 可以 |
| 存对象 | 自动 | **必须手动转 JSON** |

**本项目全程用 `StringRedisTemplate`**（`ml-common/src/main/java/com/mdkj/util/MyRedis.java:19`）。
代价就是存对象要手动序列化，所以项目里到处是这个搭配：

```java
// 存：对象 → JSON 字符串
redis.setEx(tokenKey, JSONUtil.toJsonStr(user), 30, TimeUnit.MINUTES);   // UserServiceImpl.java:527

// 取：JSON 字符串 → 对象
String json = redis.get(key);
Course course = JSON.parseObject(json, Course.class);                     // CourseServiceImpl.java:97
```

记住这个规律，就理解了项目里为什么处处 `JSONUtil` / `JSON.toJSONString`。

### 4. API 速查表

`StringRedisTemplate` 的用法是**先拿 Operations，再调方法**：

```java
stringRedisTemplate.opsForValue()   // String → ValueOperations
stringRedisTemplate.opsForHash()    // Hash   → HashOperations
stringRedisTemplate.opsForList()    // List
stringRedisTemplate.opsForSet()     // Set
stringRedisTemplate.opsForZSet()    // ZSet
```

本项目的 `MyRedis` 就是在构造器里把这 5 个 Operations 提前取好（`MyRedis.java:28-36`），再包成简短方法：

| Redis 命令 | 原生 Spring 写法 | 项目 `MyRedis` 方法 |
|---|---|---|
| `SET k v` | `opsForValue().set(k,v)` | `redis.set(k,v)` |
| `SETEX k 300 v` | `opsForValue().set(k,v,300,TimeUnit.SECONDS)` | `redis.setEx(k,v,300,TimeUnit.SECONDS)` |
| `SETNX k v` | `opsForValue().setIfAbsent(k,v)` | `redis.setNx(k,v)` |
| `GET k` | `opsForValue().get(k)` | `redis.get(k)` |
| `DEL k` | `delete(k)` | `redis.del(k)` |
| `EXISTS k` | `hasKey(k)` | `redis.exists(k)` |
| `EXPIRE k 60` | `expire(k,60,TimeUnit.SECONDS)` | `redis.expire(k,60,TimeUnit.SECONDS)` |
| `INCR k` / `INCRBY k 5` | `opsForValue().increment(k,5)` | `redis.incr(k,5)` |
| `HSET k f v` | `opsForHash().put(k,f,v)` | `redis.hSet(k,f,v)` |
| `HGET k f` | `opsForHash().get(k,f)` | `redis.hGet(k,f)` |
| `HGETALL k` | `opsForHash().entries(k)` | `redis.hGetAll(k)` |
| `EVAL 脚本` | `execute(RedisScript, keys, args)` | `redis.lua(脚本, keys, args...)` |

> **学习方法**：记住左边的 Redis 命令名，中间那列的规律就是 `opsForXxx().命令的驼峰化`。记住命令，API 自然就会。

### 5. 项目真实代码逐行读

#### 例句 1：验证码 —— 最基础的三招 setEx / get / del

`ml-user/src/main/java/com/mdkj/service/impl/UserServiceImpl.java:390,412,420`

```java
// 【存】生成验证码，5 分钟后自动消失，不用自己写清理逻辑
redis.setEx(key, val, 5, TimeUnit.MINUTES);

// ...用户提交验证码时...

// 【取】拿出来比对
String vcodeFromRedis = redis.get(key);
if (vcodeFromRedis == null) { /* 已过期 */ }

// 【删】用过即作废，防止重复使用
redis.del(key);
```

这是 Redis 最典型的用法：**一次性、有时效的数据**。
Token 会话（`UserServiceImpl.java:527`）是同一个套路，只是 TTL 换成 30 分钟。

> 记住这个「三段式」：**setEx 写入 → get 校验 → del 作废**。
> 验证码、一次性链接、临时凭证全是这个模式。

#### 例句 2：缓存 —— Cache Aside 模式（工作中最常写的）

`ml-order/src/main/java/com/mdkj/service/impl/OrderServiceImpl.java:272-318`

```java
// ① 先查缓存，命中就直接返回，MySQL 一次都不碰
String dataFromRedis = redis.get(ML.Redis.ORDER_STATISTICS_DATA_KEY);
if (ObjectUtil.isNotNull(dataFromRedis)) {
    return JSONUtil.parseObj(dataFromRedis);
}

// ② 没命中 → 查数据库（这里是十几条 count 统计，很慢）
Map<String, Object> result = new HashMap<>();
// ...

// ③ 把结果回填缓存，2 小时后过期
redis.setEx(ML.Redis.ORDER_STATISTICS_DATA_KEY, JSONUtil.toJsonStr(result), 2, TimeUnit.HOURS);
return result;
```

**「查缓存 → 没有就查库 → 回填缓存」这个三段结构叫 Cache Aside**，是缓存的标准写法。
项目里 `UserServiceImpl.java:634-681`、`CourseServiceImpl.java:94-97` 都是同一个结构。

**背诵模板：**

```java
public T get(Long id) {
    String json = redis.get(KEY_PREFIX + id);                      // 1. 查缓存
    if (json != null) return JSON.parseObject(json, T.class);

    T data = mapper.selectOneById(id);                             // 2. 查库
    if (data == null) return null;

    redis.setEx(KEY_PREFIX + id, JSON.toJSONString(data),          // 3. 回填
                30, TimeUnit.MINUTES);
    return data;
}
```

**数据更新时要删缓存**（`redis.del(KEY_PREFIX + id)`），否则用户看到旧数据。

#### 例句 3：Hash —— 存一个「小对象」

`ml-sale/src/main/java/com/mdkj/component/SeckillStockService.java:111-127`

```java
public void cacheDetail(...) {
    String key = SeckillRedisKeys.detail(seckillId, courseId);      // "seckill:detail:1:1"
    redis.getHashOps().putAll(key, Map.of(                          // HSET 多个字段
            "courseTitle", courseTitle,
            "courseCover", courseCover,
            "coursePrice", String.valueOf(coursePrice),
            "skPrice",     String.valueOf(skPrice)));
    redis.expire(key, 12, TimeUnit.HOURS);                          // Hash 只能整体设过期
}

public Map<Object, Object> getCachedDetail(...) {
    return redis.getHashOps().entries(key);                         // HGETALL，空 Map 表示未命中
}
```

**Hash vs String 存 JSON，怎么选？**

- 只整体读写 → String + JSON（简单，项目里 Token、课程都这样）
- 想单独改 / 读某个字段，或字段多想省内存 → Hash

**Hash 的坑**：过期时间只能设在整个 key 上，**不能给单个 field 设**（所以 `:126` 是给整个 key 设的）。

#### 例句 4：INCR 做限流 —— 计数器用法

`SeckillStockService.java:82-90`

```java
public boolean tryAcquireUserRate(Long userId, int maxPerSecond) {
    long second = System.currentTimeMillis() / 1000;         // 当前秒，作为 key 的一部分
    String key = SeckillRedisKeys.userRate(userId, second);  // "seckill:rate:{userId}:{秒}"
    long count = redis.incr(key, 1);                         // INCR，key 不存在自动从 0 开始
    if (count == 1) {                                       // 第一次才设过期，避免每次刷新
        redis.expire(key, 2, TimeUnit.SECONDS);
    }
    return count <= maxPerSecond;                           // 超阈值就拒绝
}
```

两个关键点：

1. **`INCR` 对不存在的 key 当 0 处理**，所以不用先判断存在
2. **把「当前秒」拼进 key 名**，每秒天然是一个新计数器，过期后自动消失 —— 固定窗口限流的经典技巧

#### 例句 5：Lua 脚本 —— Redis 的「事务」（重点）

**为什么需要 Lua？** 先看不用会出什么事：

```java
// ❌ 错误写法（两条命令，两次网络往返）
int stock = Integer.parseInt(redis.get(stockKey));   // A、B 两个请求同时读到 stock = 1
if (stock > 0) {
    redis.incr(stockKey, -1);                        // A 扣成 0，B 也扣 → 变成 -1，超卖！
}
```

问题在于「读」和「改」之间有空隙，别的请求插进来了。
**Redis 单条命令是原子的，但两条命令之间不是。**

**Lua 脚本会被 Redis 当成一整条命令执行，中间不会被打断** —— 这就是解决方案。

`SeckillStockService.java:23-39`

```java
private static final String LUA_TRY_KILL = """
        local stockKey = KEYS[1]        -- KEYS[1] 是外部传入的第 1 个 key（Lua 下标从 1 开始）
        local userKey  = KEYS[2]        -- KEYS[2] 是第 2 个 key
        local sn  = ARGV[1]             -- ARGV[1] 是外部传入的第 1 个参数
        local ttl = tonumber(ARGV[2])   -- 传入的值都是字符串，要数字得 tonumber

        local existing = redis.call('GET', userKey)   -- redis.call('命令', 参数...) 执行 Redis 命令
        if existing and existing ~= '' then
            return 0                                  -- 这个用户已经抢过了 → 返回 0
        end
        local stock = tonumber(redis.call('GET', stockKey) or '0')  -- or '0' 处理 key 不存在
        if stock <= 0 then
            return -1                                 -- 没库存 → 返回 -1
        end
        redis.call('DECR', stockKey)                  -- 扣 1
        redis.call('SET', userKey, sn, 'EX', ttl)     -- 记下「这个用户抢到了，订单号是 sn」
        return 1                                      -- 成功 → 返回 1
        """;
```

调用侧（`SeckillStockService.java:48-57`）：

```java
public long tryKill(Long seckillId, Long courseId, Long userId, String sn) {
    String stockKey = SeckillRedisKeys.stock(seckillId, courseId);
    String userKey  = SeckillRedisKeys.userOrder(seckillId, courseId, userId);
    Long result = redis.lua(
            LUA_TRY_KILL,
            List.of(stockKey, userKey),            // → 脚本里的 KEYS[1]、KEYS[2]
            sn,                                    // → ARGV[1]
            String.valueOf(TTL_SECONDS));          // → ARGV[2]
    return result == null ? -1L : result;
}
```

**Java 侧执行 Lua 的标准写法**（`MyRedis.java:962-967`，背下来）：

```java
public Long lua(String luaScript, List<String> keys, Object... args) {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptText(luaScript);          // 脚本内容
    script.setResultType(Long.class);         // 声明返回类型（Long / Boolean / List）
    return stringRedisTemplate.execute(script, keys, args);   // keys → KEYS[]，args → ARGV[]
}
```

**写 Lua 只需记住 4 件事：**

| 要点 | 说明 |
|---|---|
| `KEYS[n]` / `ARGV[n]` | key 放 KEYS，其它参数放 ARGV。**下标从 1 开始**（Lua 特色） |
| `redis.call('CMD', ...)` | 执行 Redis 命令，参数就是命令后面跟的那些 |
| 类型转换 | 传进来全是字符串，比大小要 `tonumber(...)`；key 不存在返回 `false`/`nil`，用 `or '0'` 兜底 |
| 返回值 | 只能返回数字、字符串、表。**用数字当状态码最省事**（项目：`1` 成功 / `0` 重复 / `-1` 无库存） |

> **Lua 的适用场景一句话：需要「先判断再修改」，且不能被别人插队的时候。**
> 扣库存、分布式锁释放、限流令牌桶，全是这个场景。

配套的回滚脚本 `ml-common/src/main/java/com/mdkj/component/SeckillStockCompensator.java:22-33` 多了一层「归属校验」：

```lua
local currentSn = redis.call('GET', userKey)
if not currentSn or currentSn ~= expectedSn then
    return 0                          -- 占位不是这一单的，说明已经补偿过了 → 什么都不做
end
redis.call('INCR', stockKey)          -- 库存加回来
redis.call('DEL', userKey)            -- 清掉占位
return 1
```

这叫**幂等补偿**：同一条消息重复消费，最多只补一次库存。这个思路比脚本本身值钱。

### 6. Redis 背诵清单

```java
// 注入
@Resource private StringRedisTemplate redisTemplate;

// ===== String =====
redisTemplate.opsForValue().set("k", "v");
redisTemplate.opsForValue().set("k", "v", 30, TimeUnit.MINUTES);
String v   = redisTemplate.opsForValue().get("k");
Long   n   = redisTemplate.opsForValue().increment("counter", 1);
Boolean ok = redisTemplate.opsForValue().setIfAbsent("lock", "uuid");   // SETNX

// ===== Hash =====
redisTemplate.opsForHash().put("user:1", "name", "Kim");
Object name              = redisTemplate.opsForHash().get("user:1", "name");
Map<Object,Object> all   = redisTemplate.opsForHash().entries("user:1");

// ===== 通用 =====
redisTemplate.delete("k");
redisTemplate.hasKey("k");
redisTemplate.expire("k", 60, TimeUnit.SECONDS);

// ===== Lua =====
DefaultRedisScript<Long> script = new DefaultRedisScript<>();
script.setScriptText("return redis.call('INCR', KEYS[1])");
script.setResultType(Long.class);
redisTemplate.execute(script, List.of("k"), "arg1");
```

### 7. Redis 自检问题

1. `StringRedisTemplate` 和 `RedisTemplate` 差在哪？为什么本项目存对象要手动 `JSONUtil.toJsonStr`？
2. 默写 Cache Aside 的三段式；数据更新时该做什么？
3. 为什么扣库存不能写成「先 GET 判断、再 DECR」？Lua 解决的是什么问题？
4. Lua 里 `KEYS` 和 `ARGV` 分别放什么？下标从几开始？
5. 用户占位 Key 的值为什么存订单号 `sn` 而不是简单存 `1`？（提示：看 `SeckillStockCompensator` 的归属校验）
6. Hash 能给单个 field 设过期时间吗？

---

## 二、RocketMQ

### 1. 它是干什么的

一个**跑在独立进程里的「消息中转站」**。

理解它最好的角度：**把方法调用改成「留言」**。

原来的写法（同步调用）：

```java
// 用户点秒杀 → 一路走完才返回，用户干等着
orderService.createOrder(...);   // 查用户、查课程、写订单、写明细... 200ms
return "下单成功";
```

改成 MQ（异步）：

```java
// 用户点秒杀 → 扔一条消息就返回，2ms
rocketmqTemplate.convertAndSend("ml-topic:ml-tag", orderMessage);
return sn;                       // 立刻返回订单号，建单在另一个服务里慢慢做
```

MQ 带来三个能力，**本项目三个都用到了**：

| 能力 | 含义 | 项目用在哪 |
|---|---|---|
| **异步 / 削峰** | 一万人同时秒杀，消息堆在 MQ 里排队，`ml-order` 按自己的速度慢慢建单，数据库不会被打崩 | 秒杀建单 |
| **解耦** | `ml-sale` 不需要知道 `ml-order` 的存在，只管发消息 | 秒杀建单 |
| **延迟消息** | 「10 分钟后再把这条消息投给我」—— 天然的定时任务，不用扫表 | 订单超时取消 |

### 2. 五个核心概念

```
Producer（生产者，ml-sale）
    │  发消息
    ▼
NameServer（注册中心，记录哪些 Broker 活着）  ← Broker 定时上报
    │
Broker（真正存消息的服务）
    │  存着 Topic「ml-topic」
    ▼
Consumer（消费者，ml-order）拉消息来消费
```

| 概念 | 一句话 | 本项目的值 |
|---|---|---|
| **NameServer** | 服务发现。客户端先问它「Broker 在哪」，配置里填的就是它 | `9876` 端口 |
| **Broker** | 真正存消息的服务 | 由 `docker-compose.rocketmq.yml` 启动 |
| **Topic** | 消息的大分类，像「数据库的表」 | `ml-topic`（全项目只用了 1 个） |
| **Tag** | Topic 内部的小分类，消费者可只订阅某个 tag | `ml-tag`（建单）、`order-timeout-tag`（超时） |
| **ConsumerGroup** | 消费者分组。**同组内消息只被一个实例消费；不同组各自都收到全量消息** | `ml-consumer-group`、`ml-order-timeout-group` |

**ConsumerGroup 是最容易搞错的概念**，用本项目举例：

```
ml-topic
  ├─ tag = ml-tag ─────────────→ ml-consumer-group      (OrderMessageListener) → 建单
  └─ tag = order-timeout-tag ──→ ml-order-timeout-group (OrderTimeoutListener) → 取消
```

- 部署 3 个 `ml-order` 实例，它们都属于 `ml-consumer-group`，那么一条建单消息**只会被其中 1 个实例处理**（不会重复建 3 单）→ 这叫**集群模式 CLUSTERING**
- 如果两个 listener 用同一个 `consumerGroup` 却订阅不同 tag，**会出 bug**（同组订阅关系必须一致）—— 这是 RocketMQ 的硬规则，本项目分成两个组是对的

### 3. 怎么接入（三步模板）

**① pom.xml**（`ml-sale/pom.xml:164`、`ml-order/pom.xml:162`）

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-client</artifactId>
</dependency>
```

> 根 `pom.xml:88-104` 里做了版本对齐：starter 2.3.x 会调用 `DefaultMQPushConsumer#setNamespaceV2`，所以必须把 client 排除后单独指定 5.2.0。遇到 `NoSuchMethodError` 就是这类版本不匹配。

**② yml**

```yaml
rocketmq:
  name-server: 127.0.0.1:9876   # 填 NameServer 地址，不是 Broker
  producer:
    group: ml-group             # 生产者组名，随便起但不能不填
```

**③ 注入**

```java
@Resource
private RocketMQTemplate rocketmqTemplate;
```

### 4. 发送 API 速查

**destination 语法是 `"topic:tag"`**（冒号分隔，tag 可省略）。这是 rocketmq-spring 的约定。

| 方法 | 行为 | 什么时候用 |
|---|---|---|
| `convertAndSend(dest, 对象)` | 自动把对象转 JSON 发出，**不返回结果** | 最常用 |
| `syncSend(dest, msg)` | 同步发送，返回 `SendResult`（含 msgId、状态） | 需要确认是否发成功 |
| `syncSend(dest, msg, timeout, delayLevel)` | 同步 + **延迟消息** | 延迟任务 |
| `asyncSend(dest, msg, callback)` | 异步发送，回调里拿结果 | 追求发送吞吐 |
| `sendOneWay(dest, msg)` | 发了不管结果 | 日志类，允许丢 |
| `syncSendOrderly(dest, msg, hashKey)` | 顺序消息，相同 hashKey 进同一队列 | 需要保证顺序 |

**延迟级别表**（RocketMQ 4.x 固定 18 级；5.x 支持任意时间）：

| level | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | **14** | 15 | 16 | 17 | 18 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 时间 | 1s | 5s | 10s | 30s | 1m | 2m | 3m | 4m | 5m | 6m | 7m | 8m | 9m | **10m** | 20m | 30m | 1h | 2h |

本项目 `ml-order/.../OrderServiceImpl.java:88-89` 的 `ORDER_TIMEOUT_DELAY_LEVEL = 14` 就是查这张表得来的 10 分钟。

### 5. 接收 API：`@RocketMQMessageListener` 逐参数拆解

**把这个注解的 6 个参数搞懂，MQ 消费就会了。**

`ml-order/src/main/java/com/mdkj/component/OrderMessageListener.java:14-22`

```java
@Component                                        // 必须是 Spring Bean
@RocketMQMessageListener(
        consumerGroup = "ml-consumer-group",      // ① 消费者组名，必填
        topic = "ml-topic",                       // ② 订阅哪个 topic，必填
        nameServer = "${rocketmq.name-server:...}",  // ③ 可省略，默认读全局配置
        selectorExpression = "ml-tag",            // ④ 只要这个 tag，默认 "*" 全要
        consumeMode = ConsumeMode.CONCURRENTLY,   // ⑤ 并发消费（默认）/ ORDERLY 顺序消费
        messageModel = MessageModel.CLUSTERING)   // ⑥ 集群（默认，组内只消费一次）/ BROADCASTING 广播
public class OrderMessageListener implements RocketMQListener<OrderMessage> {
                                          //   ↑ 泛型 = 消息体类型，框架自动 JSON 反序列化
    @Override
    public void onMessage(OrderMessage orderMessage) {   // 收到一条消息就调一次
        orderService.createSeckillOrder(orderMessage);
    }
}
```

**记住这个组合就够了：**

```
implements RocketMQListener<消息类型>   +   重写 onMessage(消息类型 msg)
```

框架帮你做的事：拉消息 → JSON 反序列化成泛型类 → 调 `onMessage` → **方法正常返回就 ack（确认消费成功）**。

#### 消费端最重要的两条规则

**规则一：`onMessage` 抛异常 = 告诉 MQ「我没处理成功，请重投」**

RocketMQ 会重试 **16 次**，间隔递增（10s、30s、1m…2h），16 次都失败进**死信队列** `%DLQ%_你的consumerGroup`（消息不丢，但需人工处理）。

有了这条规则，`OrderMessageListener.java:32-59` 的设计意图就清楚了：

```java
try {
    orderService.createSeckillOrder(orderMessage);
} catch (Exception e) {
    if (prepayStore.isPaid(sn)) {
        // 用户已经付钱了！建单必须成功，否则用户白花钱
        throw new IllegalStateException(...);   // ← 主动抛异常，让 MQ 一直重试
    }
    // 没付钱 → 把库存还回去，这单作废
    stockCompensator.rollbackIfOwned(...);
    // ← 这里故意不抛异常：库存已经补过了，再重试语义上就该结束
    //   （Lua 的归属校验能兜住重复补偿，但不该依赖兜底）
}
```

> **「抛异常 → 重试」/「不抛 → 结束」，这一个知识点就能解释这段代码所有分支。**

**规则二：消息一定会重复，幂等必须自己做**

网络抖动、消费超时、Broker 重投…… 同一条消息被消费两次是**必然**的，不是 bug。
所以消费端永远要先问一句「这条消息处理过了吗」。

本项目的幂等手段（`OrderServiceImpl.createSeckillOrder`）：

1. 用 `sn`（订单号）查一下，已存在直接返回
2. 校验 Redis 占位还属不属于这个 `sn`（`OrderServiceImpl.java:444-448`），不属于说明是过期消息，扔掉
3. 数据库 `sn` 唯一索引兜底，catch `DuplicateKeyException`

> **幂等三板斧：查一遍 + 唯一索引 + 状态机判断。**

### 6. 项目真实代码逐行读

#### 发送端：秒杀建单（`ml-sale/.../SeckillServiceImpl.java:306-328`）

```java
// ① 组装消息对象（普通 POJO，会被自动转成 JSON）
OrderMessage orderMessage = new OrderMessage();
orderMessage.setSn(sn);
orderMessage.setFkUserId(fkUserId);
orderMessage.setCourseTitle(courseTitle);   // 带上快照，消费端就不用再远程调用查课程
// ...

try {
    // ② 发送："topic:tag" 格式
    rocketmqTemplate.convertAndSend("ml-topic:ml-tag", orderMessage);
} catch (Exception e) {
    // ③ 发送失败 → 库存已经扣了，必须还回去（否则库存白扣，商品卖不出去）
    seckillStockService.rollbackKill(fkSeckillId, fkCourseId, fkUserId, sn);
    throw new ServiceException(...);
}
return sn;
```

工程细节：**扣库存(Redis) 和 发消息(MQ) 是两个系统，无法保证同时成功**。
项目的处理是「扣了库存但发送失败 → 手动回滚库存」。
（更严格的方案是 MQ 事务消息，本项目没用，知道有这个东西即可。）

#### 发送端：延迟消息做超时取消（`ml-order/.../OrderServiceImpl.java:558-568`）

```java
OrderTimeoutMessage timeoutMessage = new OrderTimeoutMessage();
timeoutMessage.setSn(sn);
// ...

rocketmqTemplate.syncSend(
        "ml-topic:order-timeout-tag",                        // 换了 tag，会被另一个 listener 收到
        MessageBuilder.withPayload(timeoutMessage).build(),   // 延迟消息必须用 Message 对象，不能直接传 POJO
        3000,                                                // 发送超时 3 秒
        ORDER_TIMEOUT_DELAY_LEVEL);                          // = 14 → 10 分钟后才投递
```

10 分钟后 `OrderTimeoutListener` 收到消息 → 调 `handleSeckillOrderTimeout` → 若仍未付款则取消订单 + 回补库存。

**这是「延迟任务」的标准写法。** 相比自己写定时任务扫表
（`select * from order where status=未付款 and created < now()-10min`）的好处：不用轮询、时间精准、数据量大了也不慢。

> **下单 → 发一条 10 分钟延迟消息 → 到点检查是否已付款。这个模式在电商里到处都是。**

#### 消费端对比：两个 listener 的差异

| | `OrderMessageListener` | `OrderTimeoutListener` |
|---|---|---|
| consumerGroup | `ml-consumer-group` | `ml-order-timeout-group` |
| topic | `ml-topic` | `ml-topic`（同一个） |
| selectorExpression | `ml-tag` | `order-timeout-tag` |
| 泛型 | `RocketMQListener<OrderMessage>` | `RocketMQListener<OrderTimeoutMessage>` |
| 干什么 | 建单 | 超时取消 + 回补库存 |

**同 topic、不同 tag、不同 group** —— 这是一个 topic 承载多种业务消息的常见做法。

### 7. RocketMQ 背诵清单

```java
// ===== 发送 =====
@Resource private RocketMQTemplate rocketmqTemplate;

rocketmqTemplate.convertAndSend("my-topic:my-tag", myPojo);              // 普通
rocketmqTemplate.syncSend("my-topic:my-tag",                             // 延迟
        MessageBuilder.withPayload(myPojo).build(), 3000, 14);

// ===== 接收 =====
@Component
@RocketMQMessageListener(consumerGroup = "my-group",
                         topic = "my-topic",
                         selectorExpression = "my-tag")
public class MyListener implements RocketMQListener<MyPojo> {
    @Override
    public void onMessage(MyPojo msg) {
        // 1. 幂等检查：这条处理过了吗？
        // 2. 业务处理
        // 3. 抛异常 = 让 MQ 重试；正常返回 = 消费成功
    }
}
```

### 8. RocketMQ 自检问题

1. NameServer 和 Broker 分别干什么？配置里的 `name-server` 填的是哪个？
2. ConsumerGroup 是什么？部署 3 个 `ml-order` 实例，一条建单消息会被建成几单？为什么？
3. destination 参数 `"ml-topic:ml-tag"` 的冒号前后分别是什么？
4. `onMessage` 里抛异常会发生什么？重试几次？最后去哪？
5. `OrderMessageListener` 里为什么「已支付」要抛异常、「未支付」反而吞掉异常？两个分支反过来会分别出什么事故？
6. 为什么消费端一定要做幂等？本项目用了哪三层？
7. 延迟消息为什么必须用 `MessageBuilder.withPayload(...).build()` 而不能直传 POJO？

---

## 三、Elasticsearch

### 1. 它是干什么的

一个**专门做全文搜索的数据库**。

理解它最好的角度：**MySQL 的 `LIKE '%关键字%'` 有两个致命问题，ES 就是来解决这两个问题的**。

```sql
SELECT * FROM course WHERE title LIKE '%java编程%'
```

1. **用不上索引** → 全表扫描，百万数据要几秒
2. **不理解语义** → 用户搜「编程 java」「Java 入门」，一条都匹配不出来

ES 的做法叫**倒排索引**：入库时先把文本**分词**，再建立「词 → 文档 ID」的映射。

```
课程1 "Java编程入门"   →  分词  →  [java, 编程, 入门]
课程2 "Python编程实战"  →  分词  →  [python, 编程, 实战]

倒排索引：
  java   → [课程1]
  编程   → [课程1, 课程2]
  入门   → [课程1]
```

搜「编程」→ 直接查表拿到 [课程1, 课程2]，还能按相关度排序。

> **需要「按内容模糊搜索」就上 ES，其它情况用 MySQL。**
> 本项目正是这样分工：课程的增删改查在 MySQL，只有搜索走 ES。

### 2. 概念对照表（用 MySQL 类比记）

| ES | MySQL | 本项目的值 |
|---|---|---|
| Index（索引） | 表 | `ml-course`、`ml-barrage` |
| Document（文档） | 一行记录 | 一门课程 / 一条弹幕 |
| Field（字段） | 列 | title、author、price |
| Mapping | 表结构定义 | `CourseDoc` 上的注解 |
| DSL（JSON 查询语句） | SQL | Repository 帮你生成了 |

### 3. 怎么接入（三步模板）

**① pom.xml**（`ml-course/pom.xml:149`、`ml-barrage/pom.xml:34`）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

**② yml**（注意前缀，见 [〇、配置写法勘误](#〇配置写法勘误先看这个)）

```yaml
spring:
  elasticsearch:
    uris: http://127.0.0.1:9200
```

**③ 写两个东西：一个实体类 + 一个接口**
（Spring Data 的套路，和 MyBatis 的 Entity + Mapper 一模一样）

### 4. 实体类：用注解描述 Mapping

`ml-course/src/main/java/com/mdkj/es/CourseDoc.java` 逐行：

```java
@Data
@Document(indexName = "ml-course")            // ← 对应 ES 索引名（相当于 @TableName）
public class CourseDoc {

    @Id                                        // ← 文档 ID（相当于主键）
    private Long id;

    @Field(type = FieldType.Text,              // ← Text：会分词，能全文搜索
           analyzer = "ik_max_word",           //    建索引时的分词器（细粒度，切得多）
           searchAnalyzer = "ik_smart")        //    搜索时的分词器（粗粒度，切得少）
    private String title;

    @Field(type = FieldType.Keyword)           // ← Keyword：不分词，整体作为一个词
    private String cover;

    @Field(value = "category_title",           // ← value 指定 ES 里的真实字段名（驼峰 → 下划线）
           type = FieldType.Keyword)
    private String categoryTitle;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updated;
}
```

#### `Text` vs `Keyword` —— ES 最重要的一个选择

| | Text | Keyword |
|---|---|---|
| 会不会分词 | **会** | **不会**，整个字符串是一个词 |
| 能做什么 | 全文搜索（`match`） | 精确匹配、排序、聚合、去重（`term`） |
| 例子 | 课程标题、正文、作者名 | 状态、ID、标签、封面 URL |

判断方法：**「用户会拿这个字段的一部分内容来搜吗？」** 会 → Text，不会 → Keyword。

> 顺带记一笔：`ml-barrage/src/main/java/com/mdkj/es/BarrageDoc.java:18` 把弹幕内容 `text` 标成了 `Keyword`，
> 意味着弹幕内容无法做关键词搜索。项目只用它做「按课时 ID 拉全部弹幕回放」所以够用，
> 但这是个可改进点（改 mapping 需要 reindex）。

#### ik 分词器的两个模式

```
"Java编程入门"
  ik_max_word（建索引用）→ java / 编程 / 入门 / 编 / 程 / 入 / 门   ← 切得碎，召回率高
  ik_smart  （搜索用）  → java / 编程 / 入门                        ← 切得整，精度高
```

**「建索引用 max_word，搜索用 smart」是标配写法**，直接抄。
注意 ik 是需要单独装到 ES 里的**插件**，不是 ES 自带的 —— 没装的话 `analyzer = "ik_max_word"` 会导致索引创建失败。

### 5. Repository：方法名派生查询

`ml-course/src/main/java/com/mdkj/dao/CourseRepository.java:8-17`

```java
public interface CourseRepository extends ElasticsearchRepository<CourseDoc, Long> {
                                                              //  ↑文档类型   ↑ID类型
    // 不用写实现！Spring 根据方法名自动生成 ES 查询
    Page<CourseDoc> searchByTitleOrAuthorOrderByIdx(String title, String author, Pageable pageable);
}
```

**继承 `ElasticsearchRepository` 白送的方法：**

| 方法 | 作用 |
|---|---|
| `save(doc)` / `saveAll(list)` | 新增或覆盖（ID 相同就覆盖） |
| `findById(id)` | 返回 `Optional<T>` |
| `findAll(Pageable)` | 分页查全部 |
| `deleteById(id)` / `delete(doc)` | 删除 |
| `count()` / `existsById(id)` | 计数、判存在 |

**方法名派生规则**（和 Spring Data JPA 一致）：

| 关键字 | 写法 | 含义 |
|---|---|---|
| 前缀 | `findBy...` / `searchBy...` | 两个都行 |
| 条件 | `ByTitle` | title 字段匹配 |
| 与 / 或 | `ByTitleAndAuthor` / `ByTitleOrAuthor` | AND / OR |
| 排序 | `OrderByIdxAsc` / `OrderByIdxDesc` | 按字段排序 |
| 集合 | `ByEpisodeIdIn(Collection ids)` | IN 查询 |
| 范围 | `ByPriceBetween(a, b)` | 区间 |

本项目的例子：

```java
// "搜索 title 或 author，按 idx 排序，分页"
Page<CourseDoc> searchByTitleOrAuthorOrderByIdx(String title, String author, Pageable pageable);

// ml-course/src/main/java/com/mdkj/dao/BarrageRepository.java
List<BarrageDoc> findByEpisodeIdOrderByTime(String episodeId);
List<BarrageDoc> findByEpisodeIdIn(Collection<String> episodeIds);
```

### 6. 项目真实代码逐行读

#### 写入（`ml-barrage/.../BarrageServer.java:92-105`）

```java
ASYNC_EXECUTOR.execute(() -> {                              // 丢线程池异步做，不阻塞弹幕广播
    BarrageDoc barrageDoc = JSONUtil.toBean(msg, BarrageDoc.class);
    barrageDoc.setId(SNOWFLAKE.nextIdStr());                // ES 的 @Id 要自己生成（没有自增）
    BarrageDoc result = SpringUtil.getBean(BarrageRepository.class).save(barrageDoc);
});                                                          //  ↑ 就一个 save，ES 写入就这么简单
```

> 两个可改进点（不影响学习，知道就好）：这个线程池队列无界且服务重启会丢消息，
> 更稳的做法是发 MQ、消费端批量 `bulk` 写入。

#### 查询（`ml-course/.../CourseServiceImpl.java:359-391`）

```java
public PageVO<CourseDoc> search(CoursePageDTO dto) {
    int pageNum  = dto.getPageNum();
    int pageSize = dto.getPageSize();

    // ⚠️ ES 分页页码从 0 开始（MySQL 习惯从 1 开始），所以要减 1
    pageNum = pageNum - 1;
    if (pageNum < 0) pageNum = 0;
    Pageable pageable = PageRequest.of(pageNum, pageSize);       // 构造分页参数的标准写法

    org.springframework.data.domain.Page<CourseDoc> esPage;
    if (StrUtil.isEmpty(dto.getKeyword())) {
        esPage = courseRepository.findAll(pageable);                          // 没关键字 → 查全部
    } else {
        esPage = courseRepository.searchByTitleOrAuthorOrderByIdx(
                dto.getKeyword(), dto.getKeyword(), pageable);                // 有关键字 → 搜索
    }

    // Page 对象里能拿到的东西（这几个 getter 要记住）
    PageVO<CourseDoc> result = new PageVO<>();
    result.setPageNum(pageNum + 1L);                 // 返回给前端时再 +1 转回来
    result.setTotalRow(esPage.getTotalElements());   // 总条数
    result.setTotalPage(esPage.getTotalPages());     // 总页数
    result.setRecords(esPage.getContent());          // 当前页数据 List
    return result;
}
```

> **`PageRequest.of(页码从0开始, 每页条数)` + `Page.getContent()/getTotalElements()/getTotalPages()`**
> 这套是所有 Spring Data 通用的，记一次到处能用。

#### 本项目的一个空缺（值得留意）

全项目**没有任何往 `ml-course` 索引写入的代码** —— 只有 `findAll` 和 `search`。
也就是说课程数据是靠外部手工导入 ES 的，MySQL 里增删改课程后搜索结果不会同步。

补这个链路是最好的 ES 练手项，三种常规做法：

1. **应用双写**：`CourseServiceImpl` 的 insert/update/delete 里同时 `courseRepository.save/delete`（最简单，但两边可能不一致）
2. **MQ 异步**：课程变更发消息，消费端写 ES（解耦，最终一致）
3. **canal 订阅 binlog**：业务代码零改动（最干净，但要多部署一个组件）

### 7. Repository 不够用时：`NativeQuery`

方法名派生只能做简单查询。多条件组合、字段权重、高亮就得写原生查询。
本项目没用到，但这是 ES 的正常用法，骨架长这样：

```java
@Resource private ElasticsearchOperations operations;

Query query = NativeQuery.builder()
        .withQuery(q -> q.bool(b -> b
                .should(s -> s.match(m -> m.field("title").query(keyword).boost(2.0f)))  // 标题权重×2
                .should(s -> s.match(m -> m.field("author").query(keyword)))
                .filter(f -> f.range(r -> r.field("price").lte(JsonData.of(100))))       // filter 不算分
        ))
        .withPageable(PageRequest.of(0, 10))
        .build();

SearchHits<CourseDoc> hits = operations.search(query, CourseDoc.class);
List<CourseDoc> list = hits.getSearchHits().stream().map(SearchHit::getContent).toList();
```

> 记住这个层次就好：**简单查询用 Repository 方法名，复杂查询用 NativeQuery + ElasticsearchOperations。**

### 8. ES 背诵清单

```java
// ① 实体
@Document(indexName = "my-index")
public class MyDoc {
    @Id private String id;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;
    @Field(type = FieldType.Keyword) private String status;
}

// ② 接口
public interface MyRepo extends ElasticsearchRepository<MyDoc, String> {
    Page<MyDoc> findByTitle(String title, Pageable pageable);
}

// ③ 用
@Resource private MyRepo repo;
repo.save(doc);
Page<MyDoc> page = repo.findByTitle("java", PageRequest.of(0, 10));   // 页码从 0 开始
page.getContent();
page.getTotalElements();
```

### 9. ES 自检问题

1. 为什么课程搜索不用 MySQL 的 `LIKE '%xx%'`？倒排索引解决了什么？
2. `Text` 和 `Keyword` 怎么选？判断标准是什么？
3. `ik_max_word` 和 `ik_smart` 各用在什么时候？为什么这样搭配？
4. `@Field(value = "category_title")` 的 `value` 是干什么的？
5. `PageRequest.of(0, 10)` 里的 0 是第几页？为什么 `CourseServiceImpl` 要 `pageNum - 1`？
6. ES 的 `@Id` 会自增吗？本项目弹幕的 ID 怎么来的？
7. 简单查询和复杂查询分别该用什么 API？

---

## 四、读代码顺序清单

按「从简单到复杂」排好，一天读一两个，读时对照上面对应小节。

### Redis（5 个文件）

1. `ml-common/src/main/java/com/mdkj/util/MyRedis.java` —— 只看前 200 行（String 和 Hash 部分），确认每个方法对应哪个 Redis 命令
2. `ml-user/src/main/java/com/mdkj/service/impl/UserServiceImpl.java:380-430` —— 验证码：setEx/get/del 三段式
3. `ml-order/src/main/java/com/mdkj/service/impl/OrderServiceImpl.java:269-320` —— Cache Aside
4. `ml-sale/src/main/java/com/mdkj/component/SeckillStockService.java` —— 全文 128 行，Lua + Hash + INCR 限流全在这
5. `ml-common/src/main/java/com/mdkj/component/SeckillStockCompensator.java` —— Lua 幂等补偿

### RocketMQ（4 个文件，都很短）

1. `ml-order/src/main/java/com/mdkj/component/OrderMessageListener.java` —— 61 行，消费端全貌
2. `ml-order/src/main/java/com/mdkj/component/OrderTimeoutListener.java` —— 36 行，对照看两个 listener 的 tag / group 区别
3. `ml-sale/src/main/java/com/mdkj/service/impl/SeckillServiceImpl.java:306-328` —— 发送端
4. `ml-order/src/main/java/com/mdkj/service/impl/OrderServiceImpl.java:556-570` —— 延迟消息

### Elasticsearch（4 个文件，都很短）

1. `ml-course/src/main/java/com/mdkj/es/CourseDoc.java` —— 30 行，注解全在这
2. `ml-course/src/main/java/com/mdkj/dao/CourseRepository.java` —— 18 行
3. `ml-course/src/main/java/com/mdkj/service/impl/CourseServiceImpl.java:359-391` —— 查询 + 分页
4. `ml-barrage/src/main/java/com/mdkj/component/BarrageServer.java:79-107` —— 写入

### 读代码的方法

每读一段，合上文件问自己：**「这几行如果让我从零写，我写得出来吗？」**
写不出来就回到对应的「背诵清单」抄一遍。**抄 API 是这个阶段最有效的动作。**

全部读完后做一次总检验：不看项目代码，在空项目里从零写出
「Redis Lua 扣库存 → 发 MQ 异步建单 → 延迟消息超时回补」这条链路。
卡住的每一处，就是自以为懂了但没懂的地方。

---

## 五、等有环境之后

三个中间件的本地启动：

- **RocketMQ**：`docker compose -f docker-compose.rocketmq.yml up -d`（编排文件已在项目根目录，Broker 配置见 `docker/rocketmq/broker.conf`）
- **Redis**：`docker run -d --name redis -p 6379:6379 redis`
- **Elasticsearch**：`docker run -d --name es -p 9200:9200 -e "discovery.type=single-node" ...`
  —— **必须装 ik 分词插件**，否则 `CourseDoc` 的 `analyzer = "ik_max_word"` 会导致索引创建失败

三个必装的观察工具（学习效率翻倍）：

| 工具 | 看什么 |
|---|---|
| `redis-cli` | `MONITOR` 看实时命令流、`OBJECT ENCODING` 看编码、`SLOWLOG` 看慢命令 |
| `rocketmq-dashboard` | topic / queue 分布、consumerGroup 堆积量、重试队列、死信队列 |
| Kibana Dev Tools | 写 DSL、`_analyze` 试分词、`_mapping` 看索引结构 |

第一批值得做的验证（每个半小时内）：

1. 开着 `redis-cli MONITOR`，点一次秒杀，看真实命令流，和 Lua 脚本对照
2. 把 `LUA_TRY_KILL` 临时改成「Java 里先 GET 判断、再 DECR」两条命令，压测看库存变负数（超卖），再改回来
3. 在 `OrderMessageListener.onMessage` 开头无条件抛异常，去 dashboard 看重试次数、间隔，最后进 `%DLQ%_ml-consumer-group`
4. 把延迟级别 14 改成 3（10 秒），完整跑一遍「下单不付款 → 自动取消 → 库存回补」，不用等 10 分钟
5. Kibana 里 `POST _analyze` 对同一句课程标题分别用 `ik_max_word` 和 `ik_smart`，看切词差异

---

## 六、进阶方向（本文不展开）

本文只覆盖「是什么 + 怎么调用」。把上面内容吃透后，再按需要往下挖：

- **Redis**：单线程模型、五种类型的底层编码、过期与淘汰策略、RDB/AOF 持久化、分布式锁（SETNX / Redisson 看门狗）、缓存穿透/击穿/雪崩、双写一致性、主从与 Cluster
- **RocketMQ**：CommitLog + ConsumeQueue 存储结构、事务消息（half message + 回查）、顺序消息、Rebalance、消息丢失的三段防护、消息堆积排查
- **Elasticsearch**：写入流程（buffer → translog → refresh → flush → merge）与近实时特性、query 与 filter 的区别、BM25 相关性、深分页 `search_after`、聚合 aggs、MySQL→ES 同步方案

本项目里可直接动手的练手缺口：

| 现状 | 练什么 |
|---|---|
| `ml-course` 索引无写入代码（见 §3.6） | ES 全量导入 + 增量同步 |
| `MyRedis.deleteByPrefix()` 用 `KEYS *`（`MyRedis.java:976`，注释自己都写了「阻塞风险高」） | 改成 `SCAN` 游标 + 批量删除 |
| `MyRedis.bitCount()` 逐位 `getBit` 循环（`MyRedis.java:927`） | 换成原生 `BITCOUNT`，理解 BitMap 用途（签到、UV） |
| `BarrageDoc.text` 是 `Keyword` | 改 mapping + reindex，体会改 mapping 的代价 |
| 弹幕线程池直写 ES，队列无界、重启即丢 | 改成发 MQ，消费端 `bulk` 批量写 |
| `redisson-spring-boot-starter` 依赖仍在（`ml-sale/pom.xml:153`）但代码已无引用 | 写一个 Redisson 分布式锁版秒杀，和 Lua 版压测对比 QPS |
| 秒杀支付成功后不删用户占位 Key，靠 TTL 过期 | 补主动清理，并思考「删早了会不会出问题」 |
| Sentinel 依赖已引入但未配规则 | 加网关全局限流，对比「应用层限流 vs Redis 限流」 |

相关文档：

- [docs/seckill-concurrency.md](seckill-concurrency.md) —— 秒杀链路的完整方案说明（含从 Redisson 锁到无锁 Lua 的演进背景）
- [document/rocketmq-容器部署.md](../document/rocketmq-容器部署.md) —— RocketMQ 容器部署步骤
