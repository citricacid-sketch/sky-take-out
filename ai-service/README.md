# Sky Take-Out AI Service

Python LangChain 微服务，为苍穹外卖提供 AI 能力（客服对话、NL2SQL 数据分析、点餐推荐）。

## 启动

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env，填写 LLM_API_KEY、MySQL、Redis 等

# 2. 安装依赖
pip install -r requirements.txt

# 3. 启动
python run.py
# 或: ./start.sh
```

默认端口: `8000`

## API

| 端点 | 说明 | 认证 |
|---|---|---|
| `GET /api/v1/health` | 健康检查 | 无 |
| `GET /api/v1/ready` | 就绪探针 | 无 |
| `POST /api/v1/chat` | 客服对话 | X-Internal-Token |
| `POST /api/v1/analysis/ask` | NL2SQL 数据分析 | X-Internal-Token |
| `POST /api/v1/order/plan` | 点餐推荐 | X-Internal-Token |

## 配置（.env）

```env
LLM_API_KEY=your_key
LLM_BASE_URL=https://longcat.chat/v1
LLM_MODEL=Longcat-Flash-Chat

MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=sky_take_out
MYSQL_USER=sky_ai_ro
MYSQL_PASSWORD=xxx

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=xxx

AI_SERVICE_TOKEN=shared_secret_with_java
```

## 安全

- **SQL 多层防线**: sqlglot AST 解析 + 表名白名单 + 字段黑名单 + 强制 LIMIT + 超时
- **只读数据库账号**: `sky_ai_ro`
- **内部认证**: HMAC X-Internal-Token

## 测试

```bash
pip install -r requirements-dev.txt
python -m pytest tests/ -v
```
