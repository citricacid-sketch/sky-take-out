"""内部服务认证中间件 (X-Internal-Token)。"""

import hmac
import logging
from typing import Optional

from fastapi import Header, HTTPException, status

from app.config import settings

logger = logging.getLogger(__name__)


async def verify_internal_token(authorization: Optional[str] = Header(default=None)) -> None:
    """校验 Java 端发来的内部令牌。

    开发模式 (AI_SERVICE_TOKEN 未设置) 时跳过校验，但应确保服务仅绑定 localhost。
    """
    if not settings.ai_service_token:
        # 开发模式：不校验，但记录警告
        return

    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"error": "missing_token", "message": "缺少 X-Internal-Token"},
        )

    if not hmac.compare_digest(authorization, settings.ai_service_token):
        logger.warning("内部令牌校验失败")
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail={"error": "invalid_token", "message": "令牌无效"},
        )
