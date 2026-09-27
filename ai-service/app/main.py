"""FastAPI 应用入口。"""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Depends
from fastapi.responses import JSONResponse

from app.config import settings
from app.security import verify_internal_token

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info(
        "AI 服务启动, model=%s, base_url=%s, port=%s",
        settings.llm_model,
        settings.llm_base_url,
        settings.ai_service_port,
    )
    if not settings.llm_api_key:
        logger.warning("LLM_API_KEY 未设置, LLM 调用将失败")
    if not settings.ai_service_token:
        logger.warning("AI_SERVICE_TOKEN 未设置, 内部认证已关闭 (应确保服务仅绑定 localhost)")
    yield


app = FastAPI(title="Sky Take-Out AI Service", version="1.0.0", lifespan=lifespan)


@app.get("/api/v1/health")
async def health():
    """健康检查 (无需认证)。"""
    return {"status": "ok", "llm_configured": bool(settings.llm_api_key)}


# 路由将在后续步骤注册
# from app.routers import chat, analysis
# app.include_router(chat.router, prefix="/api/v1", dependencies=[Depends(verify_internal_token)])
# app.include_router(analysis.router, prefix="/api/v1", dependencies=[Depends(verify_internal_token)])
