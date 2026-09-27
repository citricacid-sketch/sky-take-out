"""LLM 客户端工厂 (langchain-openai)。

提供两个模型实例：
- chat_llm: 客服对话 + 总结 (temperature=0.7)
- sql_llm:   SQL 生成 (temperature=0.1, 更低温度提高确定性)
"""

import logging

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_openai import ChatOpenAI

from app.config import settings

logger = logging.getLogger(__name__)


def _build_llm(temperature: float) -> BaseChatModel:
    return ChatOpenAI(
        model=settings.llm_model,
        api_key=settings.llm_api_key,
        base_url=settings.llm_base_url,
        max_tokens=settings.llm_max_tokens,
        temperature=temperature,
        timeout=60,
        api_version="",  # OpenAI 兼容接口不需要 api_version
    )


def get_chat_llm() -> BaseChatModel:
    return _build_llm(settings.llm_temperature)


def get_sql_llm() -> BaseChatModel:
    return _build_llm(settings.llm_sql_temperature)
