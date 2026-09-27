"""LLM 响应缓存。

缓存高频问题的 LLM 响应，减少 API 调用次数和延迟。
使用 LRU 策略（最近最少使用），内存存储适合单实例。

缓存策略：
- 仅缓存"安全"问题（不涉及具体订单/用户的通用问题）
- TTL 5 分钟
- 最大 1000 条
"""

import time
import logging
import hashlib
from collections import OrderedDict
from typing import Optional, Any

logger = logging.getLogger(__name__)


class ResponseCache:
    """LRU 响应缓存。

    使用示例：
        cache = ResponseCache(max_size=1000, ttl_seconds=300)
        cached = cache.get("推荐菜品")
        if not cached:
            cached = await llm_call()
            cache.set("推荐菜品", cached)
    """

    def __init__(self, max_size: int = 1000, ttl_seconds: int = 300):
        """
        Args:
            max_size: 最大缓存条数
            ttl_seconds: 缓存过期时间（秒）
        """
        self.max_size = max_size
        self.ttl_seconds = ttl_seconds
        # OrderedDict 实现 LRU
        self._cache: OrderedDict[str, Any] = OrderedDict()

    @staticmethod
    def _make_key(question: str) -> str:
        """生成缓存 key（归一化 + MD5）。"""
        # 归一化：去空格、转小写
        normalized = question.strip().lower()
        return hashlib.md5(normalized.encode()).hexdigest()

    def get(self, question: str) -> Optional[str]:
        """获取缓存的响应。

        Args:
            question: 用户问题

        Returns:
            缓存的响应文本，未命中或过期返回 None
        """
        key = self._make_key(question)
        if key in self._cache:
            value, timestamp = self._cache[key]
            # 检查过期
            if time.time() - timestamp < self.ttl_seconds:
                # 移到末尾（最近使用）
                self._cache.move_to_end(key)
                return value
            else:
                # 过期删除
                del self._cache[key]
        return None

    def set(self, question: str, response: str) -> None:
        """写入缓存。

        Args:
            question: 用户问题
            response: LLM 响应
        """
        key = self._make_key(question)
        # 如果已存在，先删除（便于移到末尾）
        if key in self._cache:
            del self._cache[key]
        # 写入
        self._cache[key] = (response, time.time())
        # 超出容量，淘汰最旧的
        if len(self._cache) > self.max_size:
            self._cache.popitem(last=False)

    def is_cacheable(self, question: str) -> bool:
        """判断问题是否适合缓存。

        仅缓存通用问题（不涉及具体订单号、用户信息）。

        Args:
            question: 用户问题

        Returns:
            True 可缓存 / False 不可缓存
        """
        # 包含订单号（纯数字）的问题不缓存
        import re
        if re.search(r'\d{4,}', question):
            return False
        # 包含"我的"等个性化词汇不缓存
        if any(w in question for w in ['我的', '我', '订单号']):
            return False
        return True


# 全局缓存实例
response_cache = ResponseCache(max_size=1000, ttl_seconds=300)
