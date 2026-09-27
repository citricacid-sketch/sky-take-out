"""ChatAgent 单元测试。

覆盖：
- session_id 生成与复用
- 工具调用循环 (mock LLM 返回 tool_calls)
- Redis 记忆读写 (mock Redis)
- LLM 故障降级
- 参数校验
"""

from __future__ import annotations

import json
import sys
import types
from typing import Any
from unittest.mock import MagicMock, patch

import pytest

# ---------------------------------------------------------------------------
# 构造一个最小化的 fake LLM (BaseChatModel 子类)，无需真实 API
# ---------------------------------------------------------------------------


class _FakeToolCall:
    def __init__(self, name: str, args: dict[str, Any], call_id: str) -> None:
        self.name = name
        self.args = args
        self.id = call_id

    def __getitem__(self, key: str) -> Any:
        # 让 AIMessage.tool_calls 的 dict 访问兼容
        return {"name": self.name, "args": self.args, "id": self.id}[key]


class _FakeAIMessage:
    """模拟 langchain_core AIMessage 的最小结构。"""

    def __init__(self, content: str = "", tool_calls: list[dict[str, Any]] | None = None) -> None:
        self.content = content
        self.tool_calls = tool_calls or []


class _FakeLLM:
    """可编排响应序列的伪 LLM。

    每次 ainvoke 取出下一个预设响应。
    """

    def __init__(self, responses: list[_FakeAIMessage]) -> None:
        self._responses = list(responses)
        self._idx = 0
        self.calls: list[list[Any]] = []

    def bind_tools(self, tools: list[Any]) -> "_FakeLLM":
        self.tools = tools
        return self

    async def ainvoke(self, messages: list[Any]) -> _FakeAIMessage:
        self.calls.append(list(messages))
        if self._idx < len(self._responses):
            resp = self._responses[self._idx]
            self._idx += 1
            return resp
        return _FakeAIMessage(content="done")


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------


@pytest.fixture
def mock_redis() -> MagicMock:
    """内存型 fake Redis。"""
    store: dict[str, tuple[str, int | None]] = {}  # key -> (value, ttl)

    r = MagicMock()

    def _get(k: str) -> str | None:
        return store.get(k, (None, None))[0]

    def _setex(k: str, ttl: int, v: str) -> None:
        store[k] = (v, ttl)

    def _set(k: str, v: str) -> None:
        store[k] = (v, None)

    def _delete(k: str) -> None:
        store.pop(k, None)

    r.get.side_effect = _get
    r.setex.side_effect = _setex
    r.set.side_effect = _set
    r.delete.side_effect = _delete
    r.ping.return_value = True
    r._store = store  # 测试断言用
    return r


# ---------------------------------------------------------------------------
# 测试
# ---------------------------------------------------------------------------


class TestChatAgentSession:
    """session_id 生成与复用。"""

    @pytest.mark.asyncio
    async def test_generates_session_id_when_none(self, mock_redis: MagicMock) -> None:
        from app.agents.chat import ChatAgent

        llm = _FakeLLM([_FakeAIMessage(content="你好呀！")])
        agent = ChatAgent(llm=llm, redis_client=mock_redis)
        result = await agent.chat("user_1", "你好")
        assert len(result["session_id"]) == 16
        assert result["reply"] == "你好呀！"

    @pytest.mark.asyncio
    async def test_reuses_session_id(self, mock_redis: MagicMock) -> None:
        from app.agents.chat import ChatAgent

        llm = _FakeLLM([
            _FakeAIMessage(content="第一次回复"),
            _FakeAIMessage(content="第二次回复"),
        ])
        agent = ChatAgent(llm=llm, redis_client=mock_redis)
        r1 = await agent.chat("user_1", "你好")
        sid = r1["session_id"]
        r2 = await agent.chat("user_1", "再问一次", session_id=sid)
        assert r2["session_id"] == sid


class TestChatAgentMemory:
    """Redis 记忆读写。"""

    @pytest.mark.asyncio
    async def test_history_persisted_to_redis(self, mock_redis: MagicMock) -> None:
        from app.agents.chat import ChatAgent, _history_key

        llm = _FakeLLM([_FakeAIMessage(content="收到")])
        agent = ChatAgent(llm=llm, redis_client=mock_redis)
        result = await agent.chat("user_1", "你好")
        key = _history_key("user_1", result["session_id"])
        assert key in mock_redis._store
        raw = mock_redis._store[key][0]
        items = json.loads(raw)
        assert len(items) == 2  # user + assistant
        assert items[0]["role"] == "user"
        assert items[0]["content"] == "你好"
        assert items[1]["role"] == "assistant"

    @pytest.mark.asyncio
    async def test_history_loaded_on_followup(self, mock_redis: MagicMock) -> None:
        from app.agents.chat import ChatAgent

        call_count = 0

        class _TrackingLLM(_FakeLLM):
            async def ainvoke(self, messages: list[Any]) -> _FakeAIMessage:
                nonlocal call_count
                call_count += 1
                # 第二轮应能看到历史消息
                if call_count == 2:
                    # messages 包含 system + 历史 user/assistant + 新 user
                    assert len(messages) >= 4
                return _FakeAIMessage(content=f"reply_{call_count}")

        llm = _TrackingLLM([])
        agent = ChatAgent(llm=llm, redis_client=mock_redis)
        r1 = await agent.chat("user_1", "第一条")
        await agent.chat("user_1", "第二条", session_id=r1["session_id"])
        assert call_count == 2

    @pytest.mark.asyncio
    async def test_redis_down_degrades_gracefully(self) -> None:
        """Redis 挂了 → 无记忆模式，不抛异常。"""
        from app.agents.chat import ChatAgent

        broken_redis = MagicMock()
        broken_redis.get.side_effect = Exception("connection refused")
        broken_redis.setex.side_effect = Exception("connection refused")

        llm = _FakeLLM([_FakeAIMessage(content="ok")])
        agent = ChatAgent(llm=llm, redis_client=broken_redis)
        result = await agent.chat("user_1", "你好")
        assert result["reply"] == "ok"


class TestChatAgentToolUse:
    """工具调用循环。"""

    @pytest.mark.asyncio
    async def test_tool_call_invoked_and_result_appended(self, mock_redis: MagicMock) -> None:
        from app.agents.chat import ChatAgent, get_order_status

        # 第一轮：LLM 返回 tool_call；第二轮：LLM 返回最终回复
        tool_call = {"name": "get_order_status", "args": {"orderId": "1001"}, "id": "call_1"}
        llm = _FakeLLM([
            _FakeAIMessage(content="", tool_calls=[tool_call]),
            _FakeAIMessage(content="订单 1001 正在配送中"),
        ])
        # mock execute_readonly
        with patch("app.agents.chat.execute_readonly") as mock_exec:
            mock_exec.return_value = {
                "columns": ["id", "status"],
                "rows": [{"id": 1001, "status": 3}],
            }
            agent = ChatAgent(llm=llm, redis_client=mock_redis)
            result = await agent.chat("user_1", "订单 1001 到哪了？")

        assert result["reply"] == "订单 1001 正在配送中"
        assert mock_exec.called

    @pytest.mark.asyncio
    async def test_unknown_tool_handled(self, mock_redis: MagicMock) -> None:
        from app.agents.chat import ChatAgent

        tool_call = {"name": "nonexistent_tool", "args": {}, "id": "call_x"}
        llm = _FakeLLM([
            _FakeAIMessage(content="", tool_calls=[tool_call]),
            _FakeAIMessage(content="已处理"),
        ])
        agent = ChatAgent(llm=llm, redis_client=mock_redis)
        result = await agent.chat("user_1", "随便问问")
        assert result["reply"] == "已处理"


class TestChatAgentErrors:
    """错误处理 / 降级。"""

    @pytest.mark.asyncio
    async def test_llm_failure_returns_friendly_message(self, mock_redis: MagicMock) -> None:
        from app.agents.chat import ChatAgent

        class _BrokenLLM(_FakeLLM):
            async def ainvoke(self, messages: list[Any]) -> _FakeAIMessage:
                raise RuntimeError("API 503")

        llm = _BrokenLLM([])
        agent = ChatAgent(llm=llm, redis_client=mock_redis)
        result = await agent.chat("user_1", "你好")
        assert "暂时不可用" in result["reply"]
        assert "session_id" in result

    @pytest.mark.asyncio
    async def test_tool_exception_does_not_crash(self, mock_redis: MagicMock) -> None:
        from app.agents.chat import ChatAgent

        tool_call = {"name": "get_order_status", "args": {"orderId": "abc"}, "id": "c1"}
        llm = _FakeLLM([
            _FakeAIMessage(content="", tool_calls=[tool_call]),
            _FakeAIMessage(content="查不到"),
        ])
        agent = ChatAgent(llm=llm, redis_client=mock_redis)
        # orderId 非数字 → 工具返回 error JSON，不抛异常
        result = await agent.chat("user_1", "查订单 abc")
        assert result["reply"] == "查不到"


class TestChatAgentValidation:
    """工具自身参数校验。"""

    def test_get_order_status_rejects_non_digit(self) -> None:
        from app.agents.chat import get_order_status

        out = get_order_status.invoke({"orderId": "abc-123"})
        data = json.loads(out)
        assert "error" in data

    def test_search_dish_strips_injection_chars(self) -> None:
        """SQL 注入字符 (引号/分号/百分号) 被剥离，无法突破字符串字面量。"""
        from app.agents.chat import search_dish

        with patch("app.agents.chat.execute_readonly") as mock_exec:
            mock_exec.return_value = {"columns": [], "rows": []}
            search_dish.invoke({"keyword": "宫保鸡丁'; DROP TABLE dish;--"})
        called_sql = mock_exec.call_args[0][0]
        # SQL 结构应为 LIKE '%<keyword>%'，关键词中的引号/分号/注释符必须被剥离
        # 提取关键词部分 (两个单引号之间的内容)
        import re as _re

        match = _re.search(r"LIKE\s+'%(.+)%'", called_sql)
        assert match is not None, f"SQL 结构异常: {called_sql}"
        keyword_in_sql = match.group(1)
        # 注入字符必须不存在于关键词中
        assert "'" not in keyword_in_sql, f"引号未被剥离: {keyword_in_sql}"
        assert ";" not in keyword_in_sql, f"分号未被剥离: {keyword_in_sql}"
        assert "--" not in keyword_in_sql, f"注释符未被剥离: {keyword_in_sql}"
        # 合法关键词仍保留
        assert "宫保鸡丁" in keyword_in_sql
