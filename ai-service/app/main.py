"""FastAPI 应用入口。"""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.responses import JSONResponse

from app.config import settings

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info(
        "AI 服务启动, model=%s, base_url=%s, port=%s",
        settings.llm_model,
        settings.llm_base_url,
        settings.ai_service_port,
    )
    # 启动校验
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


# 路由由 routers.py 注册
from app.routers import router  # noqa: E402
app.include_router(router)
