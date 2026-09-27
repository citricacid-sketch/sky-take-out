# Step 7: 灰度切流 — 启动与监控

## 启动顺序

### 1. 配置环境变量

```bash
cd ai-service
cp .env.example .env
# 编辑 .env，填写:
#   LLM_API_KEY=your_key
#   MYSQL_PASSWORD=XS971818
#   REDIS_PASSWORD=XS971818
#   AI_SERVICE_TOKEN=shared_secret_with_java
```

### 2. 启动 Python AI 服务

```bash
./start.sh
# 或: python run.py
```

验证：
```bash
curl http://127.0.0.1:8000/api/v1/health
# → {"status":"ok","service":"ai-service"}

curl http://127.0.0.1:8000/api/v1/ready
# → {"status":"ready","llm_configured":true}
```

### 3. 启动 Java 后端 (切到 Python  provider)

```bash
cd backend
AI_PROVIDER=python \
AI_SERVICE_TOKEN=shared_secret_with_java \
mvn spring-boot:run -pl sky-server
```

或修改 `application-dev.yml`:
```yaml
sky:
  ai:
    service:
      provider: python
      token: shared_secret_with_java
```

### 4. 验证双跑

```bash
# 客服对话
curl -X POST http://127.0.0.1:8080/user/chat \
  -H "Content-Type: application/json" \
  -H "authentication: <token>" \
  -d '{"message":"今天营业额多少"}'

# 数据分析
curl -X POST http://127.0.0.1:8080/admin/ai/ask \
  -H "Content-Type: application/json" \
  -H "token: <admin_token>" \
  -d '{"question":"最近7天订单量"}'
```

## 监控要点

### 观察指标
- **响应时间**: Python 路径比 Java 路径多 ~50-100ms (HTTP 开销)，可接受
- **错误率**: 5xx 应 < 1%
- **降级次数**: 日志搜 "降级到 Java 实现"，应极少

### 日志关键字
| 关键字 | 含义 | 动作 |
|---|---|---|
| `AiServiceClient 调用` | 正常调用 Python | 无 |
| `Python AI 服务不可用，降级` | Python 挂了 | 检查 Python 服务 |
| `内部令牌校验失败` | token 不匹配 | 检查 AI_SERVICE_TOKEN |
| `查询被拒绝` | SQL 安全拦截 | 正常 |

### 回滚

若 Python 服务有问题，立即回滚：
```bash
# 方式 1: 改配置重启
AI_PROVIDER=java mvn spring-boot:run -pl sky-server

# 方式 2: 改 application-dev.yml
sky.ai.service.provider: java
```

## 灰度策略

1. **第 1 天**: `provider=python`，观察日志，不面向用户
2. **第 2-3 天**: 内部试用，收集反馈
3. **第 4-7 天**: 全量切流，持续监控
4. **无异常**: 进入 Step 8 删除旧代码
