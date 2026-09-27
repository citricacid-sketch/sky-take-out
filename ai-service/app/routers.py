"""AI 服务 API 路由。"""

from __future__ import annotations

import logging
from typing import Any, Optional

from fastapi import APIRouter, Depends, Request
import pydantic
from pydantic import BaseModel

from app.agents.chat import chat_agent
from app.agents.data_analysis import analyze as analyze_fn
from app.agents.order_planner import plan as order_planner_plan
from app.security import verify_internal_token

logger = logging.getLogger(__name__)
router = APIRouter()


class ChatRequest(BaseModel):
    userId: str
    message: str
    sessionId: Optional[str] = None
    context: Optional[dict[str, Any]] = None


class AnalysisRequest(BaseModel):
    question: str

    @pydantic.field_validator("question")
    @classmethod
    def question_not_empty(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("question 不能为空")
        return v


class OrderPlanRequest(BaseModel):
    people: int = 1
    budget: float = 0
    tastes: list[str] = []
    excludes: list[str] = []


@router.get("/api/v1/health")
async def health() -> dict[str, Any]:
    """健康检查 (无需认证)。"""
    return {"status": "ok", "service": "ai-service"}


@router.get("/api/v1/ready")
async def ready(request: Request) -> dict[str, Any]:
    """就绪探针：检查 LLM 是否配置。"""
    from app.config import settings
    return {
        "status": "ready" if settings.llm_api_key else "degraded",
        "llm_configured": bool(settings.llm_api_key),
    }


# --- ChatAgent ---

@router.post("/api/v1/chat", dependencies=[Depends(verify_internal_token)])
async def chat(payload: ChatRequest) -> dict[str, Any]:
    """客服对话。"""
    return await chat_agent.chat(payload.userId, payload.message, payload.sessionId, payload.context)


# --- DataAnalysisAgent ---

@router.post("/api/v1/analysis/ask", dependencies=[Depends(verify_internal_token)])
async def analysis_ask(payload: AnalysisRequest) -> dict[str, Any]:
    """NL2SQL 数据分析 (DataAnalysisAgent)。"""
    return await analyze_fn(payload.question)


# --- OrderPlannerAgent ---

@router.post("/api/v1/order/plan", dependencies=[Depends(verify_internal_token)])
async def order_plan(payload: OrderPlanRequest) -> dict[str, Any]:
    """点餐推荐 (OrderPlannerAgent)。"""
    return await order_planner_plan(payload.people, payload.budget, payload.tastes, payload.excludes)
