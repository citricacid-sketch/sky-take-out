"""安全工具测试。"""

import pytest

from app.security_utils import (
    sanitize_input,
    escape_html,
    validate_token_format,
)


class TestSanitizeInput:
    """输入清理测试。"""

    def test_normal_input(self):
        """正常输入原样返回。"""
        text, err = sanitize_input("你好世界")
        assert text == "你好世界"
        assert err is None

    def test_trim_whitespace(self):
        """去除首尾空白。"""
        text, err = sanitize_input("  hello  ")
        assert text == "hello"

    def test_max_length(self):
        """超长输入截断。"""
        text, err = sanitize_input("a" * 1000, max_length=500)
        assert len(text) == 500

    def test_empty_rejected(self):
        """空输入被拒绝。"""
        text, err = sanitize_input("")
        assert err == "输入不能为空"

    def test_empty_allowed(self):
        """允许空输入时返回空字符串。"""
        text, err = sanitize_input("", allow_empty=True)
        assert text == ""
        assert err is None

    def test_control_chars_removed(self):
        """控制字符被去除。"""
        text, err = sanitize_input("hello\x00world\x07")
        assert text == "helloworld"


class TestEscapeHtml:
    """HTML 编码测试。"""

    def test_escape_tags(self):
        """HTML 标签被编码。"""
        assert escape_html("<script>alert('xss')</script>") == (
            "&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;"
        )

    def test_escape_ampersand(self):
        """& 被编码。"""
        assert escape_html("a & b") == "a &amp; b"

    def test_escape_quotes(self):
        """引号被编码。"""
        assert escape_html('"hello"') == "&quot;hello&quot;"


class TestValidateTokenFormat:
    """Token 格式校验测试。"""

    def test_valid_token(self):
        """有效 token 返回 True。"""
        assert validate_token_format("abc123def456." * 5) is True

    def test_empty_token(self):
        """空 token 返回 False。"""
        assert validate_token_format("") is False

    def test_short_token(self):
        """过短 token 返回 False。"""
        assert validate_token_format("abc") is False

    def test_long_token(self):
        """过长 token 返回 False。"""
        assert validate_token_format("a" * 600) is False
