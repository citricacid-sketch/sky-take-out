"""ChatAgent 上下文注入测试。"""

import pytest
from unittest.mock import MagicMock

from app.agents.chat import ChatAgent
from langchain_core.messages import AIMessage


@pytest.fixture
def mock_redis():
    """模拟 Redis（返回空历史）。"""
    redis = MagicMock()
    redis.get.return_value = None
    return redis


class TestChatContext:
    """测试上下文注入到 system prompt。"""

    def test_context_with_orders(self, mock_redis):
        """上下文中包含订单时应注入到 system prompt。"""
        agent = ChatAgent(redis_client=mock_redis)

        # 模拟 LLM 返回
        async def fake_invoke(messages):
            # 检查 system prompt 是否包含订单信息
            system_msg = messages[0]
            assert "最近的订单信息" in system_msg.content
            assert "订单号" in system_msg.content
            return AIMessage(content="回复")

        agent.llm = MagicMock()
        agent.llm.ainvoke = fake_invoke
        agent.llm.bind_tools = lambda tools: agent.llm

        # 构造上下文
        context = {
            "recentOrders": [
                {"number": "DD001", "status": 5, "amount": 58.0, "order_time": "2026-09-27 12:00"},
            ],
            "shopStatus": "营业中",
        }

        import asyncio
        result = asyncio.run(agent.chat("user_1", "我的订单", context=context))
        assert result["reply"] == "回复"
        assert "session_id" in result

    def test_context_empty(self, mock_redis):
        """无上下文时 system prompt 不包含订单信息。"""
        agent = ChatAgent(redis_client=mock_redis)

        async def fake_invoke(messages):
            system_msg = messages[0]
            assert "最近的订单信息" not in system_msg.content
            return AIMessage(content="回复")

        agent.llm = MagicMock()
        agent.llm.ainvoke = fake_invoke
        agent.llm.bind_tools = lambda tools: agent.llm

        import asyncio
        result = asyncio.run(agent.chat("user_1", "你好", context={}))
        assert result["reply"] == "回复"
