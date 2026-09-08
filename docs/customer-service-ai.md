# 智能客服（Spring AI）

> 模块：`ml-sale`  
> 接口前缀：`/sale-server/api/v1/customerService`

---

## 1. 架构

```
小程序 chat 页
    │ POST /ask
    ▼
CustomerServiceController
    ▼
CustomerServiceImpl
    ├─ Spring AI ChatClient（大模型，优先）
    │     ├─ 系统提示词（角色 + 业务规则）
    │     ├─ FAQ 知识库注入
    │     └─ 用户上下文（课程、订单统计）
    └─ 规则引擎兜底（原关键词匹配）
```

---

## 2. 依赖

父 POM 已引入 `spring-ai-bom 1.0.0`，`ml-sale` 使用：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

兼容所有 **OpenAI 协议** 的模型服务（OpenAI、DeepSeek、Moonshot、阿里云百炼兼容模式等）。

---

## 3. 配置（Nacos）

参考 [`ml-sale/src/main/resources/customer-service-ai.example.yaml`](../ml-sale/src/main/resources/customer-service-ai.example.yaml)，在 Nacos `ml-sale-dev.yaml` 中添加：

```yaml
customer-service:
  ai:
    enabled: true
    fallback-to-rules: true

spring:
  ai:
    openai:
      api-key: sk-xxx
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
```

| 配置项 | 说明 |
|--------|------|
| `customer-service.ai.enabled` | 是否启用大模型（默认 false，未配 Key 时不启用） |
| `customer-service.ai.fallback-to-rules` | 模型失败时是否回退规则引擎 |
| `spring.ai.openai.api-key` | 模型 API Key |
| `spring.ai.openai.base-url` | 兼容接口地址 |
| `spring.ai.openai.chat.options.model` | 模型名称 |

### 环境变量方式（推荐生产）

```bash
export OPENAI_API_KEY=sk-xxx
export OPENAI_BASE_URL=https://api.deepseek.com
export OPENAI_MODEL=deepseek-chat
```

Nacos 中写 `${OPENAI_API_KEY}` 引用。

---

## 4. 接口说明

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/customerService/faq` | 常见问题列表（不变） |
| POST | `/customerService/ask` | 智能问答（Spring AI + 兜底） |

请求体示例：

```json
{
  "question": "怎么查看我的订单？",
  "fkUserId": 3,
  "courseId": 1,
  "courseTitle": "JB1-1-新手村"
}
```

---

## 5. 核心代码

| 类 | 职责 |
|----|------|
| `CustomerServiceKnowledge` | FAQ 与系统提示词模板 |
| `CustomerServiceAiConfig` | 注册 `ChatClient` Bean |
| `CustomerServiceAiProperties` | 开关与模型参数 |
| `CustomerServiceImpl` | AI 调用 + 规则兜底 |

---

## 6. 本地验证

1. 在 Nacos 配置 API Key 并设置 `customer-service.ai.enabled=true`
2. 重启 `ml-sale`
3. 小程序进入课程详情 → 客服聊天，或 Swagger 调用 `/ask`

未配置 Key 时自动使用规则引擎，不影响现有功能。

---

## 7. 后续扩展

- [ ] 接入 RAG（向量库存 FAQ / 课程文档）
- [ ] 多轮对话记忆（`ChatMemory`）
- [ ] 流式输出 SSE（小程序需改造）
