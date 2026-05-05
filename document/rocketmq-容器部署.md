# RocketMQ 容器部署（修复 connect failed）

## 1. 配置文件位置

- Compose 文件：`docker-compose.rocketmq.yml`
- Broker 配置：`docker/rocketmq/broker.conf`

## 2. 启动步骤

在项目根目录执行：

```bash
docker compose -f docker-compose.rocketmq.yml up -d
```

查看容器状态：

```bash
docker compose -f docker-compose.rocketmq.yml ps
```

## 3. 为什么这个配置能解决连接失败

核心是 `broker.conf` 里的：

- `namesrvAddr=192.168.211.132:9876`
- `brokerIP1=192.168.211.132`

说明：

1. `namesrvAddr` 保证 Broker 正确注册到 NameServer。
2. `brokerIP1` 强制 Broker 对外公布宿主机 IP，而不是容器内网 IP（172.x.x.x）。
3. Java 服务与 Console 拿到路由后，能访问到真实可达地址，避免 `connect to failed`。

## 4. 访问地址

- Console: `http://192.168.211.132:12581`
- NameServer: `192.168.211.132:9876`
- Broker: `192.168.211.132:10911`

## 5. 你的微服务配置建议

在 Nacos 中（`ml-order-dev.yaml`、`ml-sale-dev.yaml`）统一配置：

```yaml
rocketmq:
  name-server: 192.168.211.132:9876
```

## 6. 快速排查命令

查看 Broker 启动日志：

```bash
docker logs -f mqbroker
```

查看 Console 启动日志：

```bash
docker logs -f rocket-console
```

端口连通性检查（在微服务所在机器执行）：

```bash
telnet 192.168.211.132 9876
telnet 192.168.211.132 10911
```

## 7. 重启命令

```bash
docker compose -f docker-compose.rocketmq.yml restart
```

停止并删除容器：

```bash
docker compose -f docker-compose.rocketmq.yml down
```
