"""全局测试 fixtures。"""

import pytest

from app.cache.response_cache import response_cache
from app.middleware.rate_limit import ai_rate_limiter


@pytest.fixture(autouse=True)
def _reset_state():
    """每个测试前重置缓存和速率限制器状态。"""
    response_cache._cache.clear()
    ai_rate_limiter._requests.clear()
    yield
