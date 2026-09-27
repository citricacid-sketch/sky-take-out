"""内部服务认证 (支持 Authorization 或 X-Internal-Token, HMAC)。"""

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

    支持两种 header:
    - Authorization: <token>
    - X-Internal-Token: <token>

    开发模式 (AI_SERVICE_TOKEN 未设置) 时跳过校验，但应确保服务仅绑定 localhost。
    """
    if not settings.ai_service_token:
        return  # 开发模式：不校验

    # 取 token (优先 Authorization，兼容 X-Internal-Token)
    token = authorization or x_internal_token

    if not token:
        logger.warning("缺少内部令牌 (Authorization / X-Internal-Token)")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"error": "missing_token", "message": "缺少 X-Internal-Token"},
        )

    if not hmac.compare_digest(token, settings.ai_service_token):
        logger.warning("内部令牌校验失败")
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail={"error": "invalid_token", "message": "令牌无效"},
        )
