"""安全工具模块。

提供输入校验、XSS 防护、Token 刷新等安全功能。

功能：
- sanitize_input: 清理用户输入（去除危险字符、限制长度）
- escape_html: HTML 实体编码（防 XSS）
- validate_token: Token 格式校验
- security_headers: 安全响应头
"""

import re
import html
import time
import logging
from typing import Optional, Tuple

logger = logging.getLogger(__name__)


def sanitize_input(text: str, max_length: int = 500, allow_empty: bool = False) -> Tuple[str, Optional[str]]:
    """清理用户输入。

    规则：
    - 去除首尾空白
    - 限制长度（默认 500 字符）
    - 去除控制字符（除换行符）
    - 拒绝空字符串（除非 allow_empty=True）

    Args:
        text: 原始输入
        max_length: 最大长度
        allow_empty: 是否允许空字符串

    Returns:
        (清理后的文本, 错误信息) — 无错误时错误信息为 None
    """
    if not text:
        if allow_empty:
            return "", None
        return "", "输入不能为空"

    # 去除首尾空白
    text = text.strip()

    # 限制长度
    if len(text) > max_length:
        text = text[:max_length]

    # 去除控制字符（保留换行符 \n）
    text = re.sub(r'[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]', '', text)

    return text, None


def escape_html(text: str) -> str:
    """HTML 实体编码（防 XSS）。

    将 < > & " ' 等字符转为 HTML 实体，防止 XSS 攻击。

    Args:
        text: 原始文本

    Returns:
        编码后的安全文本
    """
    return html.escape(text, quote=True)


def validate_token_format(token: str) -> bool:
    """校验 Token 格式。

    检查是否为非空字符串且长度合理。

    Args:
        token: JWT token 字符串

    Returns:
        True 格式有效 / False 无效
    """
    if not token or not isinstance(token, str):
        return False
    # JWT 通常为 100-200 字符
    if len(token) < 20 or len(token) > 500:
        return False
    return True


# 安全响应头（防 XSS、点击劫持、MIME 嗅探等）
SECURITY_HEADERS = {
    "X-Content-Type-Options": "nosniff",
    "X-Frame-Options": "DENY",
    "X-XSS-Protection": "1; mode=block",
    "Referrer-Policy": "strict-origin-when-cross-origin",
    "Cache-Control": "no-store, no-cache, must-revalidate",
}
