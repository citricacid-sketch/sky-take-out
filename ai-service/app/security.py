"""内部服务认证中间件。

支持两种 header 格式（兼容不同调用方）：
- Authorization: <token>（标准）
- X-Internal-Token: <token>（Java 端使用）

安全特性：
- HMAC 比较（防时序攻击）
- 开发模式自动放行（AI_SERVICE_TOKEN 未设置时）
- 缺失/无效 token 返回标准 HTTP 状态码（401/403）
"""

import hmac
import logging
from typing import Optional

from fastapi import Header, HTTPException, status

from app.config import settings

logger = logging.getLogger(__name__)


async def verify_internal_token(
    authorization: Optional[str] = Header(default=None),
    x_internal_token: Optional[str] = Header(default=None, alias="X-Internal-Token"),
) -> None:
    """校验 Java 端发来的内部令牌。

    校验流程：
    1. 开发模式（AI_SERVICE_TOKEN 未设置）→ 直接放行
    2. 提取 token（优先 Authorization，兼容 X-Internal-Token）
    3. 缺失 → 401 Unauthorized
    4. 不匹配 → 403 Forbidden

    Raises:
        HTTPException: 401（缺失）或 403（无效）
    """
    # 开发模式：不校验
    if not settings.ai_service_token:
        return

    # 取 token（优先 Authorization，兼容 X-Internal-Token）
    token = authorization or x_internal_token

    if not token:
        logger.warning("缺少内部令牌 (Authorization / X-Internal-Token)")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"error": "missing_token", "message": "缺少 X-Internal-Token"},
        )

    # HMAC 比较（防时序攻击）
    if not hmac.compare_digest(token, settings.ai_service_token):
        logger.warning("内部令牌校验失败")
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail={"error": "invalid_token", "message": "令牌无效"},
        )
