# JMeter 多用户秒杀压测

## 一、导入测试计划

1. 打开 JMeter
2. **文件 → 打开** → 选择 `seckill-loadtest.jmx`
3. 点击 **测试计划**，修改「用户定义的变量」：

| 变量 | 说明 | 示例 |
|------|------|------|
| LOGIN_USER | 登录账号 | admin |
| LOGIN_PASS | 登录密码 | 123456Aa! |
| SECKILL_ID | 秒杀活动 ID | 1 |
| COURSE_ID | 课程 ID | 1 |
| PRICE | 原价 | 99 |
| SK_PRICE | 秒杀价 | 9.9 |
| STOCK | 重置库存 | 100 |

## 二、测试计划结构

```
测试计划
├── HTTP请求默认值          localhost:24101
├── setUp - 登录并准备秒杀   （只跑 1 次）
│   ├── 01-登录             → 提取 token
│   └── 02-准备秒杀         → 开启活动 + 重置库存
└── 并发秒杀用户            （100 线程，5 秒 ramp-up）
    └── 03-秒杀kill         → 每个线程不同 fkUserId
├── 聚合报告
└── 查看结果树
```

## 三、并发参数（线程组）

在 **并发秒杀用户** 中调整：

| 参数 | 含义 | 建议 |
|------|------|------|
| 线程数 | 模拟用户数 | 100~500 |
| Ramp-Up | 多少秒内全部启动 | 5~10 |
| 循环次数 | 每用户请求次数 | 1 |

`fkUserId` 使用 `${__threadNum}`，100 个线程对应用户 ID 1~100，避免同一用户重复待付款订单。

## 四、运行

1. 确认 `ml-gateway`、`ml-sale`、`ml-order`、Redis、RocketMQ 已启动
2. 确认 `ml-sale` 已包含 `prepareLoadTest` 接口（需重启过服务）
3. 点击绿色 **启动** 按钮
4. 查看 **聚合报告**：
   - 样本数、错误率、平均/90%/95%/99% 响应时间、吞吐量

## 五、预期结果（库存 100，线程 200）

- 成功约 **100** 次（抢到库存）
- 其余失败原因多为「库存不足」
- 成功响应示例：`{"code":1000,"data":"1987654321098765432",...}`

## 六、手动搭建（不用 jmx 文件）

### 1. 测试计划 → 添加 HTTP请求默认值
- 服务器：`localhost`
- 端口：`24101`

### 2. 添加 setUp 线程组
**HTTP请求 - 登录**
- 方法：POST
- 路径：`/user-server/api/v1/user/loginByAccount`
- Body：
```json
{"username":"你的账号","password":"你的密码"}
```
- 后置处理器 → **JSON提取器**：变量名 `TOKEN`，表达式 `$.data.token`
- 后置处理器 → **BeanShell**：
```java
props.put("TOKEN", vars.get("TOKEN"));
```

**HTTP请求 - 准备秒杀**
- 方法：POST
- 路径：`/sale-server/api/v1/seckill/prepareLoadTest?seckillId=1&stock=100`
- HTTP信息头：`token` = `${TOKEN}`

### 3. 添加线程组（100 线程，Ramp-Up 5 秒）
**HTTP信息头管理器**
- `Content-Type`: application/json
- `token`: `${__P(TOKEN)}`

**HTTP请求 - kill**
- 方法：POST
- 路径：`/sale-server/api/v1/seckill/kill`
- Body：
```json
{
  "fkSeckillId": 1,
  "fkUserId": ${__threadNum},
  "fkCourseId": 1,
  "price": 99,
  "skPrice": 9.9
}
```

### 4. 添加监听器
- 聚合报告
- 查看结果树（调试时开启，压测时可禁用）

## 七、使用 CSV 多账号（可选）

若需要每个线程用不同账号登录，可在线程组下添加 **CSV 数据文件设置**，引用 `users.csv`，并在 kill 请求中使用 `${fkUserId}`。

## 八、注意事项

- JMeter 需安装 **JSON Plugins** 或使用 JMeter 5.x 内置 JSON 提取器
- 压测前建议关闭「查看结果树」以节省内存
- 网关 token 有效期 30 分钟，长时间压测需重新登录
