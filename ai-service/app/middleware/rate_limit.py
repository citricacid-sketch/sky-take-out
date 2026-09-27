"""速率限制中间件（滑动窗口算法）。

限制每个用户的 AI 接口调用频率，防止滥用和恶意刷接口。
使用内存存储（适合单实例部署），分布式场景可替换为 Redis 实现。

规则：
- 每个用户每分钟最多 N 次请求（默认 20）
- 超出限制返回 429 Too Many Requests
"""

import time
import logging
from collections import defaultdict
from typing import Dict, List

logger = logging.getLogger(__name__)


class RateLimiter:
    """滑动窗口速率限制器。

    使用示例：
        limiter = RateLimiter(max_requests=20, window_seconds=60)
        if not limiter.allow(user_id):
            raise HTTPException(429, "请求过于频繁")
    """

    def __init__(self, max_requests: int = 20, window_seconds: int = 60):
        """
        Args:
            max_requests: 窗口内最大请求数
            window_seconds: 窗口大小（秒）
        """
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        # 用户 -> 请求时间戳列表
        self._requests: Dict[str, List[float]] = defaultdict(list)

    def allow(self, user_id: str) -> bool:
        """检查用户是否允许发起请求。

        清理过期时间戳后，判断当前请求数是否超限。

        Args:
            user_id: 用户标识

        Returns:
            True 允许 / False 拒绝
        """
        now = time.time()
        window_start = now - self.window_seconds

        # 清理窗口外的时间戳
        timestamps = self._requests[user_id]
        self._requests[user_id] = [t for t in timestamps if t > window_start]

        # 判断是否超限
        if len(self._requests[user_id]) >= self.max_requests:
            logger.warning("用户 %s 触发速率限制 (%d/%d)", user_id, len(self._requests[user_id]), self.max_requests)
            return False

        # 记录本次请求
        self._requests[user_id].append(now)
        return True

    def remaining(self, user_id: str) -> int:
        """获取用户剩余可用请求数。"""
        now = time.time()
        window_start = now - self.window_seconds
        timestamps = [t for t in self._requests[user_id] if t > window_start]
        return max(0, self.max_requests - len(timestamps))


# 全局限制器实例（chat 和 analysis 共用）
ai_rate_limiter = RateLimiter(max_requests=20, window_seconds=60)
