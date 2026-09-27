"""FastAPI 应用入口。

启动流程：
1. 配置日志
2. lifespan: 打印启动信息 + 配置校验
3. 注册路由（routers.py）

端点：
- GET  /api/v1/health    — 健康检查（无需认证）
- GET  /api/v1/ready      — 就绪探针（检查 LLM 配置）
- POST /api/v1/chat       — 客服对话（需认证）
- POST /api/v1/analysis/ask — NL2SQL 数据分析（需认证）
- POST /api/v1/order/plan  — 点餐推荐（需认证）
"""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.security_utils import SECURITY_HEADERS

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理。

    启动时：
    - 打印模型/base_url/port 信息
    - 校验关键配置（LLM_API_KEY、AI_SERVICE_TOKEN）
    """
    logger.info(
        "AI 服务启动, model=%s, base_url=%s, port=%s",
        settings.llm_model,
        settings.llm_base_url,
        settings.ai_service_port,
    )
    # 启动校验（缺失只警告，不阻止启动）
    warnings = []
    if not settings.llm_api_key:
        warnings.append("LLM_API_KEY 未设置")
    if not settings.ai_service_token:
        warnings.append("AI_SERVICE_TOKEN 未设置 (开发模式)")
    if warnings:
        for w in warnings:
            logger.warning(w)
    else:
        logger.info("配置校验通过")
    yield


app = FastAPI(title="Sky Take-Out AI Service", version="1.0.0", lifespan=lifespan)

# 安全响应头中间件
@app.middleware("http")
async def add_security_headers(request, call_next):
    """为所有响应添加安全头。"""
    response = await call_next(request)
    for key, value in SECURITY_HEADERS.items():
        response.headers[key] = value
    return response

# 路由注册
from app.routers import router  # noqa: E402
app.include_router(router)
