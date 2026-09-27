"""苍穹外卖智能客服 Agent。

基于 langchain-core 原语实现的工具调用循环：
- 对话记忆：Redis (key ``ai:chat:{userId}:{sessionId}``，TTL 30 分钟)
- 工具：查订单状态 / 查配送时间 / 查菜品信息 (均来自只读 SQL 执行器)
- 降级策略：Redis 故障 → 无记忆模式；LLM 故障 → 友好提示
"""

from __future__ import annotations

import json
import logging
import re
import uuid
from typing import Any, Optional

from langchain_core.messages import (
    AIMessage,
    BaseMessage,
    HumanMessage,
    SystemMessage,
    ToolMessage,
)
from langchain_core.tools import tool
from redis import Redis

from app.config import settings
from app.db import execute_readonly
from app.llm import get_chat_llm

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# 工具定义 (@tool 装饰器)
# ---------------------------------------------------------------------------


@tool
def get_order_status(orderId: str) -> str:
    """查询订单的当前状态。

    Args:
        orderId: 订单编号 (id 字段)。
    """
    if not orderId or not orderId.isdigit():
        return json.dumps({"error": "orderId 必须是数字"}, ensure_ascii=False)
    try:
        res = execute_readonly(
            f"SELECT id, status, amount, order_time FROM orders WHERE id = {int(orderId)} LIMIT 1"
        )
        rows = res["rows"]
        if not rows:
            return json.dumps({"error": f"未找到订单 {orderId}"}, ensure_ascii=False)
        return json.dumps(rows[0], ensure_ascii=False, default=str)
    except Exception as e:  # noqa: BLE001
        logger.warning("get_order_status 失败: %s", e)
        return json.dumps({"error": f"查询订单失败: {e}"}, ensure_ascii=False)


@tool
def get_delivery_time(orderId: str) -> str:
    """查询订单的预计配送/送达时间。

    Args:
        orderId: 订单编号 (id 字段)。
    """
    if not orderId or not orderId.isdigit():
        return json.dumps({"error": "orderId 必须是数字"}, ensure_ascii=False)
    try:
        res = execute_readonly(
            f"SELECT id, estimated_delivery_time, delivery_status, order_time "
            f"FROM orders WHERE id = {int(orderId)} LIMIT 1"
        )
        rows = res["rows"]
        if not rows:
            return json.dumps({"error": f"未找到订单 {orderId}"}, ensure_ascii=False)
        return json.dumps(rows[0], ensure_ascii=False, default=str)
    except Exception as e:  # noqa: BLE001
        logger.warning("get_delivery_time 失败: %s", e)
        return json.dumps({"error": f"查询配送时间失败: {e}"}, ensure_ascii=False)


@tool
def search_dish(keyword: str) -> str:
    """按关键字搜索菜品信息 (名称、价格、描述)。

    Args:
        keyword: 搜索关键词，匹配菜品名称。
    """
    if not keyword or not keyword.strip():
        return json.dumps({"error": "keyword 不能为空"}, ensure_ascii=False)
    # 安全清洗：仅保留字母、数字、中文、空格，剥离所有 SQL 注入危险字符
    safe_kw = re.sub(r"[^\w\s一-鿿]", "", keyword, flags=re.UNICODE)[:20]
    safe_kw = safe_kw.strip()
    if not safe_kw:
        return json.dumps({"error": "keyword 无效"}, ensure_ascii=False)
    try:
        res = execute_readonly(
            f"SELECT id, name, price, description, category_id "
            f"FROM dish WHERE name LIKE '%{safe_kw}%' LIMIT 10"
        )
        return json.dumps(res["rows"], ensure_ascii=False, default=str)
    except Exception as e:  # noqa: BLE001
        logger.warning("search_dish 失败: %s", e)
        return json.dumps({"error": f"搜索菜品失败: {e}"}, ensure_ascii=False)


# Agent 可调用的工具列表
CHAT_TOOLS = [get_order_status, get_delivery_time, search_dish]

# ---------------------------------------------------------------------------
# Redis 记忆助手
# ---------------------------------------------------------------------------

_CHAT_KEY_PREFIX = "ai:chat:"


def _make_redis() -> Optional[Redis]:
    """创建 Redis 连接；失败返回 None (降级)。"""
    try:
        r = Redis(
            host=settings.redis_host,
            port=settings.redis_port,
            password=settings.redis_password or None,
            db=settings.redis_db,
            decode_responses=True,
            socket_connect_timeout=2,
            socket_timeout=2,
        )
        r.ping()
        return r
    except Exception as e:  # noqa: BLE001
        logger.warning("Redis 连接失败, 降级为无记忆模式: %s", e)
        return None


def _history_key(user_id: str, session_id: str) -> str:
    return f"{_CHAT_KEY_PREFIX}{user_id}:{session_id}"


class ChatAgent:
    """苍穹外卖智能客服。

    用法::

        agent = ChatAgent()
        result = await agent.chat("user_1", "我的订单 1001 到哪了？")
        print(result["reply"], result["session_id"])
    """

    def __init__(
        self,
        llm: Any = None,
        redis_client: Optional[Redis] = None,
        tools: list[Any] | None = None,
        max_tool_iters: int = 5,
    ) -> None:
        self.llm = (llm or get_chat_llm()).bind_tools(tools or CHAT_TOOLS)
        self.redis = redis_client if redis_client is not None else _make_redis()
        self.tools = {t.name: t for t in (tools or CHAT_TOOLS)}
        self.max_tool_iters = max_tool_iters
        self.system_prompt = (
            "你是苍穹外卖的智能客服小助手，名字叫'小苍穹'。"
            "你的职责是帮助用户查询订单状态、配送时间，以及推荐和搜索菜品。"
            "请用友好、专业、简洁的中文回答。"
            "如果用户的问题超出你的能力范围，请礼貌地说明。"
            "当需要查询数据时，请使用提供的工具。"
        )

    # -- 记忆读写 ----------------------------------------------------------

    def _load_history(self, user_id: str, session_id: str) -> list[BaseMessage]:
        if not self.redis:
            return []
        try:
            raw = self.redis.get(_history_key(user_id, session_id))
            if not raw:
                return []
            items = json.loads(raw)
            return [_dict_to_message(item) for item in items]
        except Exception as e:  # noqa: BLE001
            logger.warning("读取对话历史失败: %s", e)
            return []

    def _save_history(
        self, user_id: str, session_id: str, history: list[BaseMessage]
    ) -> None:
        if not self.redis:
            return
        try:
            payload = [_message_to_dict(m) for m in history]
            key = _history_key(user_id, session_id)
            self.redis.setex(key, settings.chat_session_ttl, json.dumps(payload, ensure_ascii=False))
        except Exception as e:  # noqa: BLE001
            logger.warning("保存对话历史失败: %s", e)

    # -- 工具执行 ----------------------------------------------------------

    def _run_tool(self, name: str, args: dict[str, Any]) -> str:
        fn = self.tools.get(name)
        if fn is None:
            return json.dumps({"error": f"未知工具: {name}"}, ensure_ascii=False)
        try:
            result = fn.invoke(args)
            return result if isinstance(result, str) else json.dumps(result, ensure_ascii=False)
        except Exception as e:  # noqa: BLE001
            logger.exception("工具 %s 执行失败", name)
            return json.dumps({"error": f"工具执行失败: {e}"}, ensure_ascii=False)

    # -- 核心对话 ----------------------------------------------------------

    async def chat(
        self, user_id: str, message: str, session_id: Optional[str] = None
    ) -> dict[str, str]:
        """执行一轮对话。

        Returns:
            ``{"reply": ..., "session_id": ...}``
        """
        # 1. session_id 生成 / 复用
        if not session_id:
            session_id = uuid.uuid4().hex[:16]

        # 2. 装配消息
        history = self._load_history(user_id, session_id)
        messages: list[BaseMessage] = [SystemMessage(content=self.system_prompt)]
        messages.extend(history)
        messages.append(HumanMessage(content=message))

        # 3. 工具调用循环
        try:
            reply = await self._tool_loop(messages)
        except Exception as e:  # noqa: BLE001
            logger.exception("LLM 调用失败: %s", e)
            reply = f"抱歉，AI 服务暂时不可用 ({type(e).__name__})"
            messages.append(AIMessage(content=reply))

        # 4. 更新记忆
        self._save_history(user_id, session_id, messages[1:])  # 不存 system

        return {"reply": reply, "session_id": session_id}

    async def _tool_loop(self, messages: list[BaseMessage]) -> str:
        for _ in range(self.max_tool_iters):
            ai_msg: AIMessage = await self.llm.ainvoke(messages)
            messages.append(ai_msg)

            if not ai_msg.tool_calls:
                return ai_msg.content if isinstance(ai_msg.content, str) else str(ai_msg.content)

            for call in ai_msg.tool_calls:
                name = call["name"]
                args = call.get("args", {})
                call_id = call["id"]
                tool_result = self._run_tool(name, args)
                messages.append(ToolMessage(content=tool_result, tool_call_id=call_id))

        # 超过最大轮次，强制结束
        logger.warning("工具调用超过最大轮次 %d", self.max_tool_iters)
        return "抱歉，处理时间较长，请稍后再试。"


# ---------------------------------------------------------------------------
# 消息序列化 helpers
# ---------------------------------------------------------------------------


def _dict_to_message(item: dict[str, Any]) -> BaseMessage:
    role = item.get("role", "user")
    content = item.get("content", "")
    if role == "system":
        return SystemMessage(content=content)
    if role == "assistant":
        return AIMessage(content=content)
    return HumanMessage(content=content)


def _message_to_dict(msg: BaseMessage) -> dict[str, str]:
    if isinstance(msg, HumanMessage):
        role = "user"
    elif isinstance(msg, AIMessage):
        role = "assistant"
    elif isinstance(msg, ToolMessage):
        role = "tool"
    elif isinstance(msg, SystemMessage):
        role = "system"
    else:
        # 兜底：使用消息自身的 type 属性 (BaseMessage 子类都有)
        role = getattr(msg, "type", "assistant")
    return {"role": role, "content": str(msg.content)}


class _LazyChatAgent:
    """延迟创建的 ChatAgent 代理。

    模块加载时不构造真实实例 (避免 ChatOpenAI 在空 api_key 下校验失败)；
    首次调用 ``chat`` 等方法时才真正初始化。这样路由可以直接
    ``chat_agent.chat(...)``，而测试构造 ``ChatAgent(...)`` 不会受影响。
    """

    def __init__(self) -> None:
        self._instance: ChatAgent | None = None

    def _get(self) -> ChatAgent:
        if self._instance is None:
            self._instance = ChatAgent()
        return self._instance

    def __getattr__(self, name: str) -> Any:
        return getattr(self._get(), name)


# 全局单例 (路由直接使用)
chat_agent = _LazyChatAgent()
