"""AI 服务 API 路由。"""

from __future__ import annotations

import logging
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Request

from app.agents.chat import chat_agent
from app.agents.order_planner import plan as order_planner_plan
from app.security import verify_internal_token

from app.agents import data_analysis

logger = logging.getLogger(__name__)
router = APIRouter()


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


# --- Step 2: ChatAgent ---

@router.post("/api/v1/chat", dependencies=[Depends(verify_internal_token)])
async def chat(request: Request, payload: dict[str, Any]) -> dict[str, Any]:
    """客服对话。"""
    user_id = payload.get("userId", "")
    message = payload.get("message", "")
    session_id = payload.get("sessionId")
    if not user_id or not message:
        raise HTTPException(status_code=400, detail="userId 和 message 必填")
    return await chat_agent.chat(user_id, message, session_id)


@router.post("/api/v1/analysis/ask", dependencies=[Depends(verify_internal_token)])
async def analysis_ask(request: Request, payload: dict[str, Any]) -> dict[str, Any]:
    """NL2SQL 数据分析 (DataAnalysisAgent)。"""
    question = payload.get("question", "")
    if not question:
        raise HTTPException(status_code=400, detail="question 必填")
    return await data_analysis.analyze(question)


# --- Step 4: OrderPlannerAgent ---

@router.post("/api/v1/order/plan", dependencies=[Depends(verify_internal_token)])
async def order_plan(request: Request, payload: dict[str, Any]) -> dict[str, Any]:
    """点餐推荐 (Step 4 实现)。"""
    people = int(payload.get("people", 1))
    budget = float(payload.get("budget", 0))
    tastes = payload.get("tastes", [])
    excludes = payload.get("excludes", [])
    return await order_planner_plan(people, budget, tastes, excludes)
