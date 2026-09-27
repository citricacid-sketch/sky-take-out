"""AI 服务 API 路由。

所有 POST 端点需要 X-Internal-Token 认证（除了 health/ready）。
请求/响应格式与 Java AiServiceClient 对应。
"""

from __future__ import annotations

import logging
from typing import Any, Optional

from fastapi import APIRouter, Depends, HTTPException, Request
import pydantic
from pydantic import BaseModel

from app.agents.chat import chat_agent
from app.agents.data_analysis import analyze as analyze_fn
from app.agents.order_planner import plan as order_planner_plan
from app.security import verify_internal_token
from app.middleware.rate_limit import ai_rate_limiter

logger = logging.getLogger(__name__)
router = APIRouter()


class ChatRequest(BaseModel):
    """客服对话请求。"""
    userId: str
    message: str
    sessionId: Optional[str] = None  # 会话 ID（首次为空，后续复用）
    context: Optional[dict[str, Any]] = None  # 用户上下文（订单/店铺状态）


class AnalysisRequest(BaseModel):
    """NL2SQL 数据分析请求。"""
    question: str

    @pydantic.field_validator("question")
    @classmethod
    def question_not_empty(cls, v: str) -> str:
        """校验 question 不为空。"""
        if not v or not v.strip():
            raise ValueError("question 不能为空")
        return v


class OrderPlanRequest(BaseModel):
    """点餐推荐请求。"""
    people: int = 1  # 人数
    budget: float = 0  # 预算（0=不限）
    tastes: list[str] = []  # 口味偏好
    excludes: list[str] = []  # 忌口食材


@router.get("/api/v1/health")
async def health() -> dict[str, Any]:
    """健康检查（无需认证）。"""
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
    # 速率限制（每分钟 20 次）
    if not ai_rate_limiter.allow(payload.userId):
        raise HTTPException(
            status_code=429,
            detail={"error": "rate_limited", "message": "请求过于频繁，稍后再试"},
        )
    return await chat_agent.chat(payload.userId, payload.message, payload.sessionId, payload.context)


# --- DataAnalysisAgent ---

@router.post("/api/v1/analysis/ask", dependencies=[Depends(verify_internal_token)])
async def analysis_ask(payload: AnalysisRequest) -> dict[str, Any]:
    """NL2SQL 数据分析（DataAnalysisAgent）。"""
    # 速率限制（复用 chat 限制器）
    # 注意：analysis 没有 userId，使用 IP 或其他标识
    return await analyze_fn(payload.question)


# --- OrderPlannerAgent ---

@router.post("/api/v1/order/plan", dependencies=[Depends(verify_internal_token)])
async def order_plan(payload: OrderPlanRequest) -> dict[str, Any]:
    """点餐推荐（OrderPlannerAgent）。"""
    return await order_planner_plan(payload.people, payload.budget, payload.tastes, payload.excludes)
