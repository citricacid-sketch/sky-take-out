# 苍穹外卖 (Sky Take-Out)

外卖点餐系统 — Java 后端 + 原生微信小程序 + Python AI 服务。

## 架构

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  微信小程序      │────▶│  Java 后端       │────▶│  Python AI 服务  │
│  (用户端)        │     │  (Spring Boot)   │     │  (FastAPI)       │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                              │                       │
                              ▼                       ▼
                        ┌──────────┐           ┌──────────┐
                        │  MySQL   │           │  LongCat  │
                        │  Redis   │           │  LLM     │
                        └──────────┘           └──────────┘
```

## 目录

```
sky-take-out/
├── backend/                # Java 后端 (Spring Boot 2.7 + JDK 17)
│   ├── sky-common/        # 公共模块（工具、配置、常量）
│   ├── sky-pojo/          # 实体类 / DTO / VO
│   └── sky-server/        # 主服务（Controller、Service、Mapper）
├── frontend/
│   ├── sky-admin-web/     # 管理后台（Vue 3 + Element Plus + Vite）
│   └── sky-take-out-miniapp/  # 用户端（原生微信小程序）
└── ai-service/            # Python AI 服务（FastAPI + LangChain）
```

## 启动

### Java 后端

```bash
cd backend
mvn clean install
mvn spring-boot:run -pl sky-server
# 默认端口: 8080
```

### Python AI 服务

```bash
cd ai-service
pip install -r requirements.txt
cp .env.example .env  # 编辑 LLM_API_KEY 等
python run.py
# 默认端口: 8000
```

### 微信小程序

微信开发者工具 → 导入 `frontend/sky-take-out-miniapp`

### 管理后台

```bash
cd frontend/sky-admin-web
npm install
npm run dev
# 默认端口: 5173
```

## AI 功能

| 功能 | 端点 | 说明 |
|---|---|---|
| 智能客服 | `POST /user/chat` | 基于 LongCat LLM 的对话助手 |
| 数据分析 | `POST /admin/ai/ask` | NL2SQL 自然语言数据查询 |
| 点餐推荐 | `POST /api/v1/order/plan` | 规则引擎推荐菜品组合 |

## 配置

### Java (`backend/sky-server/src/main/resources/application.yml`)

```yaml
sky:
  datasource: { host, port, database, username, password }
  redis: { host, port, password }
  ai:
    service:
      provider: python
      url: http://127.0.0.1:8000
      token: sky_take_out_dev_token_2026
```

### Python (`ai-service/.env`)

```env
LLM_API_KEY=your_longcat_key
LLM_BASE_URL=https://api.longcat.chat/anthropic
LLM_MODEL=LongCat-2.0
MYSQL_HOST=127.0.0.1
AI_SERVICE_TOKEN=sky_take_out_dev_token_2026
```

## 测试

```bash
# Java
mvn test -pl sky-server

# Python
cd ai-service
pip install -r requirements-dev.txt
python -m pytest tests/ -v

# AI 评测
python -m tests.evaluation.compare
```

## 文档

- [前端开发指南](backend/docs/frontend-dev-guide.md) — 接口契约、AI 对接要点
- [ai-service README](ai-service/README.md) — AI 服务 API、配置、安全
- [灰度切流指南](ai-service/docs/rollout_guide.md) — 上线流程

---

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
