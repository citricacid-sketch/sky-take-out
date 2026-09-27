"""苍穹外卖智能客服 Agent 模块。

本模块基于 langchain-core 原语实现了一个具备工具调用能力的智能客服循环，
主要服务于苍穹外卖系统的用户咨询场景（查询订单状态、配送时间、搜索菜品）。

核心组件：
- **工具层**：通过 ``@tool`` 装饰器定义三个只读查询工具
  （``get_order_status`` / ``get_delivery_time`` / ``search_dish``），
  底层依赖 ``execute_readonly`` 执行 SQL，避免写入风险。
- **记忆层**：使用 Redis 持久化对话历史，key 格式为
  ``ai:chat:{userId}:{sessionId}``，TTL 由 ``settings.chat_session_ttl`` 控制。
- **循环层**：``ChatAgent._tool_loop`` 实现 LLM↔工具的多轮调用，
  最大轮次由 ``max_tool_iters`` 限制（默认 5），防止无限循环。
- **降级策略**：Redis 故障时自动退化为无记忆模式；LLM 调用异常时返回友好提示。

使用方式::

    from app.agents.chat import chat_agent

    result = await chat_agent.chat(
        user_id="user_1",
        message="我的订单 1001 到哪了？",
        context={"recentOrders": [...], "shopStatus": "营业中"},
    )
    print(result["reply"], result["session_id"])
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

    通过只读 SQL 查询 ``orders`` 表，返回订单的状态码、金额和下单时间；
    调用方需自行将状态码解析为中文（可借助 ``_resolve_status``）。

    Args:
        orderId: 订单编号（对应 ``orders.id`` 字段），必须为纯数字字符串。

    Returns:
        JSON 字符串。成功时包含 ``id / status / amount / order_time`` 字段；
        失败时返回 ``{"error": "..."}`` 格式。

    Raises:
        本函数不抛出异常，内部捕获后统一返回错误 JSON。
    """
    if not orderId or not orderId.isdigit():
        # 参数校验：非纯数字直接返回错误，避免 SQL 注入风险
        return json.dumps({"error": "orderId 必须是数字"}, ensure_ascii=False)
    try:
        # 使用 int() 强转进一步确保安全，配合 execute_readonly 只读执行器
        res = execute_readonly(
            f"SELECT id, status, amount, order_time FROM orders WHERE id = {int(orderId)} LIMIT 1"
        )
        rows = res["rows"]
        if not rows:
            return json.dumps({"error": f"未找到订单 {orderId}"}, ensure_ascii=False)
        # default=str 处理 datetime 等不可序列化类型
        return json.dumps(rows[0], ensure_ascii=False, default=str)
    except Exception as e:  # noqa: BLE001
        logger.warning("get_order_status 失败: %s", e)
        return json.dumps({"error": f"查询订单失败: {e}"}, ensure_ascii=False)


@tool
def get_delivery_time(orderId: str) -> str:
    """查询订单的预计配送/送达时间。

    从 ``orders`` 表中获取配送相关字段，辅助用户了解外卖送达进度。

    Args:
        orderId: 订单编号（对应 ``orders.id`` 字段），必须为纯数字字符串。

    Returns:
        JSON 字符串。成功时包含 ``id / estimated_delivery_time / delivery_status / order_time``；
        失败时返回 ``{"error": "..."}`` 格式。

    Raises:
        本函数不抛出异常，内部捕获后统一返回错误 JSON。
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
    """按关键字搜索菜品信息（名称、价格、描述）。

    使用 LIKE 模糊匹配 ``dish`` 表，返回最相关的最多 10 条记录。
    关键词会经过安全清洗，剥离 SQL 注入危险字符。

    Args:
        keyword: 搜索关键词，用于匹配菜品名称；空字符串会被拒绝。

    Returns:
        JSON 字符串。成功时为菜品列表数组（每项含 ``id / name / price / description / category_id``）；
        失败时返回 ``{"error": "..."}`` 格式。

    Raises:
        本函数不抛出异常，内部捕获后统一返回错误 JSON。
    """
    if not keyword or not keyword.strip():
        return json.dumps({"error": "keyword 不能为空"}, ensure_ascii=False)
    # 安全清洗：仅保留字母、数字、中文、空格，剥离所有 SQL 注入危险字符（如引号、分号）
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
# 新增工具时在此注册即可被 ChatAgent 自动绑定
CHAT_TOOLS = [get_order_status, get_delivery_time, search_dish]

# ---------------------------------------------------------------------------
# Redis 记忆助手
# ---------------------------------------------------------------------------

# Redis 中存储对话历史的 key 前缀，完整格式：ai:chat:{userId}:{sessionId}
_CHAT_KEY_PREFIX = "ai:chat:"


def _make_redis() -> Optional[Redis]:
    """创建 Redis 连接；失败返回 None（降级为无记忆模式）。

    连接参数从 ``settings`` 读取，设置 2 秒连接/读取超时避免阻塞。

    Returns:
        成功时返回可用的 ``Redis`` 实例；任何异常均返回 None。
    """
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
        # 降级：Redis 不可用时 agent 仍可工作，只是失去跨轮次记忆
        logger.warning("Redis 连接失败, 降级为无记忆模式: %s", e)
        return None


def _history_key(user_id: str, session_id: str) -> str:
    """构造 Redis 中对话历史的存储 key。

    Key 格式：``ai:chat:{userId}:{sessionId}``，用于唯一标识
    某个用户在某个会话中的对话历史。

    Args:
        user_id: 用户唯一标识。
        session_id: 会话唯一标识。

    Returns:
        形如 ``ai:chat:user_1:abc123def456`` 的 Redis key 字符串。
    """
    return f"{_CHAT_KEY_PREFIX}{user_id}:{session_id}"


def _resolve_status(status: Any) -> str:
    """将订单状态码转换为中文描述。

    状态码对应关系（苍穹外卖订单状态枚举）：
        1=待付款, 2=待接单, 3=已接单, 4=派送中, 5=已完成, 6=已取消。

    Args:
        status: 订单状态码（整数或 None）。

    Returns:
        对应的中文描述；未知状态码返回 ``"未知"``。
    """
    if status is None:
        return "未知"
    return {
        1: "待付款", 2: "待接单", 3: "已接单",
        4: "派送中", 5: "已完成", 6: "已取消",
    }.get(status, "未知")


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
        """初始化智能客服 Agent。

        Args:
            llm: 大语言模型实例；为 None 时通过 ``get_chat_llm`` 获取默认模型，
                 并自动绑定 ``tools`` 列表。
            redis_client: Redis 客户端；为 None 时尝试自动连接，失败则降级。
            tools: 工具列表；为 None 时使用模块默认 ``CHAT_TOOLS``。
            max_tool_iters: 工具调用循环的最大轮次，防止 LLM 陷入无限工具调用。
                            默认 5 轮。
        """
        # 绑定工具到 LLM，使模型能够输出 tool_calls 结构化调用请求
        self.llm = (llm or get_chat_llm()).bind_tools(tools or CHAT_TOOLS)
        # Redis 实例：优先使用外部注入（便于测试），否则自动创建
        self.redis = redis_client if redis_client is not None else _make_redis()
        # 构建工具名称 → 工具实例的映射表，便于 _run_tool 中按名查找
        self.tools = {t.name: t for t in (tools or CHAT_TOOLS)}
        self.max_tool_iters = max_tool_iters
        # System prompt 定义 agent 身份与职责，注入后贯穿整个对话
        self.system_prompt = (
            "你是苍穹外卖的智能客服小助手，名字叫'小苍穹'。"
            "你的职责是帮助用户查询订单状态、配送时间，以及推荐和搜索菜品。"
            "请用友好、专业、简洁的中文回答。"
            "如果用户的问题超出你的能力范围，请礼貌地说明。"
            "当需要查询数据时，请使用提供的工具。"
        )

    # -- 记忆读写 ----------------------------------------------------------

    def _build_system_prompt(self, context: Optional[dict[str, Any]]) -> str:
        """构造带业务上下文的 system prompt。

        将用户最近订单列表和店铺状态拼接到基础 prompt 中，
        使 LLM 在回答时无需额外查询即可感知用户上下文。

        Args:
            context: 上下文字典，可选包含：
                     - ``recentOrders``: 最近订单列表（最多取前 3 条）。
                     - ``shopStatus``: 店铺当前状态描述字符串。

        Returns:
            拼接后的完整 system prompt 字符串。
        """
        parts = [self.system_prompt]
        if context:
            orders = context.get("recentOrders")
            if orders and isinstance(orders, list):
                # 仅注入最近 3 条订单，避免 prompt 过长影响推理性能
                parts.append("\n\n该用户最近的订单信息：")
                for o in orders[:3]:
                    parts.append(
                        f"- 订单号：{o.get('number', '?')}，"
                        f"状态：{_resolve_status(o.get('status'))}，"
                        f"金额：{o.get('amount', '?')}，"
                        f"下单时间：{o.get('order_time', '?')}"
                    )
            shop_status = context.get("shopStatus")
            if shop_status:
                parts.append(f"\n当前店铺状态：{shop_status}")
        return "".join(parts)

    def _load_history(self, user_id: str, session_id: str) -> list[BaseMessage]:
        """从 Redis 加载指定会话的对话历史。

        Redis key 格式：``ai:chat:{userId}:{sessionId}``，
        存储内容为消息字典的 JSON 数组。

        Args:
            user_id: 用户唯一标识。
            session_id: 会话唯一标识。

        Returns:
            历史消息列表（``BaseMessage`` 实例）；Redis 不可用或
            无记录时返回空列表。
        """
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
        """将对话历史持久化到 Redis。

        使用 ``SETEX`` 命令写入，TTL 由 ``settings.chat_session_ttl`` 控制，
        到期自动删除，避免 Redis 内存无限增长。

        Args:
            user_id: 用户唯一标识。
            session_id: 会话唯一标识。
            history: 本轮对话的消息列表（不含 system 消息）。

        Returns:
            None。写入失败仅记录日志，不抛出异常。
        """
        if not self.redis:
            # Redis 不可用时静默跳过，保持无记忆模式运行
            return
        try:
            # 将 BaseMessage 序列化为字典列表，再整体 JSON 编码
            payload = [_message_to_dict(m) for m in history]
            key = _history_key(user_id, session_id)
            # setex 同时设置值与过期时间（单位：秒）
            self.redis.setex(key, settings.chat_session_ttl, json.dumps(payload, ensure_ascii=False))
        except Exception as e:  # noqa: BLE001
            logger.warning("保存对话历史失败: %s", e)

    # -- 工具执行 ----------------------------------------------------------

    def _run_tool(self, name: str, args: dict[str, Any]) -> str:
        """执行指定名称的工具函数。

        通过 ``self.tools`` 映射表按名称查找工具，使用 LangChain 的
        ``invoke`` 接口传入参数字典，统一返回 JSON 字符串。

        Args:
            name: 工具函数名称（对应 ``@tool`` 装饰器的函数名）。
            args: 工具参数字典，键为参数名、值为参数值。

        Returns:
            工具执行结果的 JSON 字符串；工具不存在或执行失败时
            返回 ``{"error": "..."}`` 格式。
        """
        fn = self.tools.get(name)
        if fn is None:
            # LLM 可能幻觉出不存在的工具名，需兜底
            return json.dumps({"error": f"未知工具: {name}"}, ensure_ascii=False)
        try:
            # LangChain tool.invoke 接受 dict 参数并返回结构化结果
            result = fn.invoke(args)
            return result if isinstance(result, str) else json.dumps(result, ensure_ascii=False)
        except Exception as e:  # noqa: BLE001
            logger.exception("工具 %s 执行失败", name)
            return json.dumps({"error": f"工具执行失败: {e}"}, ensure_ascii=False)

    # -- 核心对话 ----------------------------------------------------------

    async def chat(
        self, user_id: str, message: str, session_id: Optional[str] = None,
        context: Optional[dict[str, Any]] = None,
    ) -> dict[str, str]:
        """执行一轮完整的智能客服对话。

        流程：生成/复用 session_id → 加载历史 → 装配消息
        （注入 system prompt + 上下文）→ 调用 LLM 工具循环
        → 保存更新后的历史 → 返回回复。

        Args:
            user_id: 用户唯一标识，用于隔离不同用户的对话记忆。
            message: 用户本轮输入的消息文本。
            session_id: 会话标识；为 None 时自动生成 16 位 hex 新会话，
                        传入已有 session_id 则可延续历史对话。
            context: 业务上下文字典，可包含 ``recentOrders`` 和 ``shopStatus``，
                     用于在 system prompt 中注入实时业务信息。

        Returns:
            字典，包含两个键：
            - ``reply``: AI 最终回复的文本。
            - ``session_id``: 本次会话的 session_id（新生成或复用传入值）。

        Raises:
            本方法不抛出异常；LLM 调用失败时返回友好错误提示。
        """
        # 1. session_id 生成 / 复用：新会话生成 16 位 hex 随机 ID
        if not session_id:
            session_id = uuid.uuid4().hex[:16]

        # 2. 装配消息：从 Redis 加载历史 + 构造带上下文的 system prompt + 追加用户输入
        history = self._load_history(user_id, session_id)
        system_content = self._build_system_prompt(context)
        messages: list[BaseMessage] = [SystemMessage(content=system_content)]
        messages.extend(history)
        messages.append(HumanMessage(content=message))

        # 3. 工具调用循环：LLM 与工具多轮交互，直到无 tool_calls 或达到最大轮次
        try:
            reply = await self._tool_loop(messages)
        except Exception as e:  # noqa: BLE001
            logger.exception("LLM 调用失败: %s", e)
            reply = f"抱歉，AI 服务暂时不可用 ({type(e).__name__})"
            messages.append(AIMessage(content=reply))

        # 4. 更新记忆：保存本轮完整消息（跳过 index 0 的 system message）
        self._save_history(user_id, session_id, messages[1:])  # 不存 system

        return {"reply": reply, "session_id": session_id}

    async def _tool_loop(self, messages: list[BaseMessage]) -> str:
        """LLM ↔ 工具的多轮调用循环。

        循环逻辑：
        1. 调用 ``self.llm.ainvoke(messages)`` 获取 AI 回复。
        2. 若回复包含 ``tool_calls``，逐个执行工具并将结果作为
           ``ToolMessage`` 追加到消息列表，然后进入下一轮。
        3. 若回复不包含 ``tool_calls``，说明 LLM 已完成推理，
           直接返回 ``content`` 作为最终答案。

        退出条件：
        - **正常退出**：LLM 返回无 tool_calls 的 AIMessage。
        - **强制退出**：达到 ``self.max_tool_iters``（默认 5）轮后
          仍未结束，记录警告并返回超时提示，防止死循环。

        Args:
            messages: 当前对话消息列表，会在循环中被原地追加
                     AIMessage / ToolMessage。

        Returns:
            AI 最终回复的文本字符串。
        """
        # 外层循环：最多执行 max_tool_iters 轮 LLM 调用
        for _ in range(self.max_tool_iters):
            # Step 1: 异步调用 LLM（LongCatLLM 的 ainvoke 流程）
            # 输入完整消息历史，模型决定是直接回复还是发起工具调用
            ai_msg: AIMessage = await self.llm.ainvoke(messages)
            messages.append(ai_msg)

            # Step 2: 判断是否需要工具调用
            # 无 tool_calls → LLM 已生成最终答案，循环结束
            if not ai_msg.tool_calls:
                return ai_msg.content if isinstance(ai_msg.content, str) else str(ai_msg.content)

            # Step 3: 逐个执行工具调用
            # 同一轮可能包含多个并行工具调用（如同时查订单和配送）
            for call in ai_msg.tool_calls:
                name = call["name"]          # 工具函数名
                args = call.get("args", {})   # 工具参数字典
                call_id = call["id"]          # 本次调用的唯一 ID，用于关联 ToolMessage
                # 执行工具并获取 JSON 字符串结果
                tool_result = self._run_tool(name, args)
                # 将工具结果作为 ToolMessage 追加，tool_call_id 与 AIMessage 中的调用对应
                messages.append(ToolMessage(content=tool_result, tool_call_id=call_id))

        # 超过最大轮次仍未得到最终回复，强制结束并提示用户
        logger.warning("工具调用超过最大轮次 %d", self.max_tool_iters)
        return "抱歉，处理时间较长，请稍后再试。"


# ---------------------------------------------------------------------------
# 消息序列化 helpers
# ---------------------------------------------------------------------------


def _dict_to_message(item: dict[str, Any]) -> BaseMessage:
    """将 Redis 反序列化的消息字典还原为 LangChain ``BaseMessage`` 实例。

    仅根据 ``role`` 字段区分消息类型，不保留 tool_calls 等复杂结构
    （历史消息中的工具调用结果以纯文本形式存储）。

    Args:
        item: 消息字典，至少包含 ``role`` 和 ``content`` 两个键。

    Returns:
        对应类型的 ``BaseMessage`` 子类实例（``SystemMessage`` /
        ``AIMessage`` / ``HumanMessage``）。
    """
    role = item.get("role", "user")
    content = item.get("content", "")
    if role == "system":
        return SystemMessage(content=content)
    if role == "assistant":
        return AIMessage(content=content)
    return HumanMessage(content=content)


def _message_to_dict(msg: BaseMessage) -> dict[str, str]:
    """将 LangChain ``BaseMessage`` 实例序列化为可 JSON 编码的字典。

    用于在 ``_save_history`` 中将消息列表持久化到 Redis。

    Args:
        msg: 任意 ``BaseMessage`` 子类实例。

    Returns:
        包含 ``role`` 和 ``content`` 两个键的字典。
    """
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


# 全局单例：通过 _LazyChatAgent 延迟初始化，路由层可直接 import 并使用
# 避免模块加载时因 api_key 缺失导致 ChatOpenAI 构造失败
chat_agent = _LazyChatAgent()
