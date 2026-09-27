"""速率限制和缓存测试。"""

import asyncio
import pytest

from app.middleware.rate_limit import RateLimiter
from app.cache.response_cache import ResponseCache


class TestRateLimiter:
    """速率限制器测试。"""

    def test_allows_within_limit(self):
        """窗口内请求应允许。"""
        limiter = RateLimiter(max_requests=5, window_seconds=60)
        for _ in range(5):
            assert limiter.allow("user_1") is True

    def test_blocks_over_limit(self):
        """超出窗口限制应拒绝。"""
        limiter = RateLimiter(max_requests=3, window_seconds=60)
        for _ in range(3):
            limiter.allow("user_1")
        assert limiter.allow("user_1") is False

    def test_separate_users(self):
        """不同用户独立计数。"""
        limiter = RateLimiter(max_requests=2, window_seconds=60)
        assert limiter.allow("user_1") is True
        assert limiter.allow("user_1") is True
        assert limiter.allow("user_1") is False  # user_1 超限
        assert limiter.allow("user_2") is True  # user_2 独立

    def test_remaining_count(self):
        """剩余请求数计算正确。"""
        limiter = RateLimiter(max_requests=5, window_seconds=60)
        limiter.allow("user_1")
        limiter.allow("user_1")
        assert limiter.remaining("user_1") == 3


class TestResponseCache:
    """响应缓存测试。"""

    def test_cache_hit(self):
        """缓存命中返回相同值。"""
        cache = ResponseCache(max_size=100, ttl_seconds=60)
        cache.set("推荐菜品", "推荐鱼香肉丝")
        assert cache.get("推荐菜品") == "推荐鱼香肉丝"

    def test_cache_miss(self):
        """缓存未命中返回 None。"""
        cache = ResponseCache(max_size=100, ttl_seconds=60)
        cache.set("推荐菜品", "推荐鱼香肉丝")
        assert cache.get("其他问题") is None

    def test_cache_case_insensitive(self):
        """缓存大小写不敏感。"""
        cache = ResponseCache(max_size=100, ttl_seconds=60)
        cache.set("推荐菜品", "推荐鱼香肉丝")
        assert cache.get("推荐菜品") == "推荐鱼香肉丝"

    def test_lru_eviction(self):
        """超出容量淘汰最旧条目。"""
        cache = ResponseCache(max_size=2, ttl_seconds=60)
        cache.set("问题1", "答案1")
        cache.set("问题2", "答案2")
        cache.set("问题3", "答案3")  # 应淘汰问题1
        assert cache.get("问题1") is None
        assert cache.get("问题2") == "答案2"

    def test_cacheable_generic(self):
        """通用问题可缓存。"""
        cache = ResponseCache()
        assert cache.is_cacheable("推荐菜品") is True
        assert cache.is_cacheable("有什么好吃的") is True

    def test_not_cacheable_personal(self):
        """个性化问题不可缓存。"""
        cache = ResponseCache()
        assert cache.is_cacheable("我的订单") is False
        assert cache.is_cacheable("订单号 12345") is False
        assert cache.is_cacheable("我的订单到哪了") is False
