# 秒杀接口压测

## 前置条件

- 网关 `ml-gateway`（24101）、`ml-sale`、`ml-order`、Redis、RocketMQ 已启动
- 数据库中已有秒杀活动及明细（`ml_sms.seckill` / `seckill_detail`）
- 压测用户账号可登录（需在 Nacos 白名单外，携带 token）

## 1. 配置

### 压测账号（已预置）

数据库 **`ml_ums.user`** 中已批量创建 100 个压测用户：

| 项目 | 值 |
|------|-----|
| 账号 | `loadtest001` ~ `loadtest100` |
| 密码 | `LoadTest123` |
| 用户 ID | `3` ~ `102` |

如需重新生成或追加，执行：

```powershell
cd D:/java/my-lesson/scripts/seckill-loadtest
npm install mysql2 bcryptjs --no-save
node seed-users.mjs 100
```

JMeter 多账号 CSV：`jmeter/users.csv`（含 username / password / fkUserId）。

### 压测配置

```powershell
cd D:/java/my-lesson/scripts/seckill-loadtest
copy config.example.json config.json
```

编辑 `config.json`：

| 字段 | 说明 |
|------|------|
| gateway | 网关地址，默认 `http://localhost:24101` |
| username / password | 登录账号 |
| seckillId | 秒杀活动 ID |
| courseId | 秒杀课程 ID |
| price / skPrice | 原价 / 秒杀价 |
| stock | 重置库存数量 |
| concurrency | 并发数 |
| totalRequests | 总请求数 |
| userIdStart | 模拟用户 ID 起始值（建议不同 ID 测超卖） |

## 2. 一键开启秒杀 + 压测

```powershell
node loadtest.mjs config.json
```

脚本会自动：

1. 登录获取 token
2. 调用 `POST /sale-server/api/v1/seckill/prepareLoadTest` 开启活动并重置 Redis 库存
3. 并发调用 `POST /sale-server/api/v1/seckill/kill`
4. 输出 QPS、成功率、延迟分位数

## 3. 手动开启活动（可选）

```powershell
# 先登录拿 token，再执行（替换 TOKEN 和 SECKILL_ID）
curl.exe -X POST "http://localhost:24101/sale-server/api/v1/seckill/prepareLoadTest?seckillId=1&stock=100" -H "token: YOUR_TOKEN"
```

## 4. 单次验证

```powershell
curl.exe -X POST http://localhost:24101/sale-server/api/v1/seckill/kill ^
  -H "Content-Type: application/json" ^
  -H "token: YOUR_TOKEN" ^
  -d "{\"fkSeckillId\":1,\"fkUserId\":1,\"fkCourseId\":1,\"price\":99,\"skPrice\":9.9}"
```

成功返回：`{"code":1000,"data":"订单号19位","message":"..."}`

## 5. 压测建议

- **库存 100、请求 200、并发 50**：约一半成功，验证超卖保护
- **userIdStart 递增**：避免同一用户重复待付款订单干扰
- 压测后可在 RocketMQ Console 查看 `ml-topic` 消息堆积
- 观察 Redis 键 `seckill:stock:{seckillId}:{courseId}` 是否正确扣减

## 6. 接口说明

| 接口 | 方法 | 路径 |
|------|------|------|
| 准备压测 | POST | `/sale-server/api/v1/seckill/prepareLoadTest?seckillId=&stock=` |
| 秒杀下单 | POST | `/sale-server/api/v1/seckill/kill` |
| 今日活动 | GET | `/sale-server/api/v1/seckill/today` |
