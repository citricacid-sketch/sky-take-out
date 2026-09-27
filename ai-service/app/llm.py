"""LLM 客户端工厂 (LongCat Anthropic 兼容接口, 直接 HTTP 调用)。

由于 langchain-anthropic 默认使用 x-api-key 头，而 LongCat 需要 Authorization: Bearer，
这里直接封装 HTTP 调用，绕过 langchain-anthropic 的认证限制。
"""

import json
import logging
from typing import Any

import httpx

from app.config import settings

logger = logging.getLogger(__name__)


class LongCatLLM:
    """LongCat LLM 客户端 (Anthropic 兼容格式)。"""

    def __init__(self, temperature: float = 0.7):
        self.model = settings.llm_model
        self.base_url = settings.llm_base_url.rstrip("/")
        self.api_key = settings.llm_api_key
        self.max_tokens = settings.llm_max_tokens
        self.temperature = temperature
        self.client = httpx.AsyncClient(timeout=60)

    async def ainvoke(self, messages: list[Any]) -> Any:
        """调用 LLM，返回 AIMessage-like 对象。

        Args:
            messages: langchain_core.messages.BaseMessage 列表。

        Returns:
            _AIMessage 对象，有 content 和 tool_calls 属性。
        """
        # 转换 langchain messages 到 Anthropic 格式
        anthropic_messages = []
        system_content = None
        for msg in messages:
            cls_name = msg.__class__.__name__
            if cls_name == "SystemMessage":
                # System 单独提取，不放入 messages 数组
                system_content = msg.content
            elif cls_name == "HumanMessage":
                anthropic_messages.append({"role": "user", "content": msg.content})
            elif cls_name == "AIMessage":
                anthropic_messages.append({"role": "assistant", "content": msg.content})
            elif cls_name == "ToolMessage":
                # LongCat 不支持 tool role，转为 user 消息
                anthropic_messages.append({"role": "user", "content": f"[Tool Result] {msg.content}"})

        payload: dict[str, Any] = {
            "model": self.model,
            "max_tokens": self.max_tokens,
            "temperature": self.temperature,
            "messages": anthropic_messages,
        }
        if system_content:
            payload["system"] = system_content

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.api_key}",
            "anthropic-version": "2023-06-01",
        }

        logger.info("LLM 调用: model=%s, messages=%d", self.model, len(anthropic_messages))
        resp = await self.client.post(
            f"{self.base_url}/v1/messages",
            json=payload,
            headers=headers,
        )
        resp.raise_for_status()
        data = resp.json()

        # 提取回复内容
        content = ""
        for block in data.get("content", []):
            if block.get("type") == "text":
                content += block.get("text", "")
            elif block.get("type") == "thinking":
                # 跳过 thinking 块
                pass

        # 返回简单的 AIMessage-like 对象
        return _AIMessage(content=content)


    def bind_tools(self, tools: list[Any]) -> "LongCatLLM":
        """LongCat 不支持 Anthropic tool 格式，返回自身（无操作）。"""
        return self


class _AIMessage:
    """简化的 AIMessage。"""

    def __init__(self, content: str):
        self.content = content
        self.tool_calls = []


def get_chat_llm() -> LongCatLLM:
    return LongCatLLM(temperature=settings.llm_temperature)


def get_sql_llm() -> LongCatLLM:
    return LongCatLLM(temperature=settings.llm_sql_temperature)
