"""DataAnalysisAgent 测试。"""

from __future__ import annotations

from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.agents import data_analysis
from app.db import QueryRejected


def _fake_llm_response(content: str) -> MagicMock:
    resp = MagicMock()
    resp.content = content
    return resp


def _patch_llms(sql_content: str, summary_content: str):
    """返回 patch 上下文：sql_llm / chat_llm / db 全部 mock。"""
    sql_llm = MagicMock()
    sql_llm.ainvoke = AsyncMock(return_value=_fake_llm_response(sql_content))
    chat_llm = MagicMock()
    chat_llm.ainvoke = AsyncMock(return_value=_fake_llm_response(summary_content))

    return (
        patch("app.agents.data_analysis.get_sql_llm", return_value=sql_llm),
        patch("app.agents.data_analysis.get_chat_llm", return_value=chat_llm),
    )


class TestExtractSql:
    def test_plain(self):
        assert data_analysis._extract_sql("SELECT 1") == "SELECT 1"

    def test_markdown_sql(self):
        s = "```sql\nSELECT id FROM orders\n```"
        assert data_analysis._extract_sql(s) == "SELECT id FROM orders"

    def test_markdown_plain(self):
        s = "```\nSELECT id FROM orders\n```"
        assert data_analysis._extract_sql(s) == "SELECT id FROM orders"

    def test_with_explanation_trailing_text(self):
        s = "SELECT amount FROM orders; 这是解释"
        assert data_analysis._extract_sql(s) == "SELECT amount FROM orders"

    def test_empty(self):
        assert data_analysis._extract_sql("") == ""

    def test_none(self):
        assert data_analysis._extract_sql(None) == ""  # type: ignore[arg-type]


class TestAnalyze:
    @pytest.mark.asyncio
    async def test_normal_question_returns_sql_result_and_answer(self):
        sql = "SELECT id, amount FROM orders"
        columns = ["id", "amount"]
        rows = [{"id": 1, "amount": 99.0}]

        sql_patch, chat_patch = _patch_llms(sql, "今日共 1 笔订单")
        with sql_patch, chat_patch, patch("app.agents.data_analysis.validate_sql", return_value=sql), \
                patch("app.agents.data_analysis.execute_readonly", return_value={"columns": columns, "rows": rows}):
            out = await data_analysis.analyze("今天多少订单")

        assert out["question"] == "今天多少订单"
        assert out["sql"] == sql
        assert out["result"]["columns"] == columns
        assert out["result"]["rows"] == rows
        assert out["answer"] == "今日共 1 笔订单"

    @pytest.mark.asyncio
    async def test_markdown_sql_extracted_before_validate(self):
        wrapped = "```sql\nSELECT id FROM orders\n```"
        sql_patch, chat_patch = _patch_llms(wrapped, "ok")
        with sql_patch, chat_patch, \
                patch("app.agents.data_analysis.validate_sql", return_value="SELECT id FROM orders") as v, \
                patch("app.agents.data_analysis.execute_readonly", return_value={"columns": ["id"], "rows": []}):
            out = await data_analysis.analyze("q")

        # 验证 validate_sql 收到的是提取后的纯 SQL
        v.assert_called_once_with("SELECT id FROM orders")
        assert out["sql"] == "SELECT id FROM orders"

    @pytest.mark.asyncio
    async def test_injection_insert_rejected(self):
        sql_patch, chat_patch = _patch_llms("INSERT INTO orders(id) VALUES (1)", "x")
        with sql_patch, chat_patch, \
                patch("app.agents.data_analysis.validate_sql", side_effect=QueryRejected("只允许 SELECT 查询")):
            out = await data_analysis.analyze("注入")

        assert out["sql"] == ""
        assert out["result"] == {}
        assert "查询被拒绝" in out["answer"]

    @pytest.mark.asyncio
    async def test_injection_union_rejected(self):
        sql_patch, chat_patch = _patch_llms(
            "SELECT id FROM orders UNION SELECT password FROM employee", "x"
        )
        with sql_patch, chat_patch, \
                patch("app.agents.data_analysis.validate_sql", side_effect=QueryRejected("禁止的关键字: UNION")):
            out = await data_analysis.analyze("联合查询")

        assert out["sql"] == ""
        assert "查询被拒绝" in out["answer"]

    @pytest.mark.asyncio
    async def test_injection_drop_rejected(self):
        sql_patch, chat_patch = _patch_llms("DROP TABLE orders", "x")
        with sql_patch, chat_patch, \
                patch("app.agents.data_analysis.validate_sql", side_effect=QueryRejected("只允许 SELECT 查询")):
            out = await data_analysis.analyze("删表")

        assert out["sql"] == ""
        assert out["result"] == {}
        assert "查询被拒绝" in out["answer"]

    @pytest.mark.asyncio
    async def test_forbidden_table_rejected(self):
        sql_patch, chat_patch = _patch_llms("SELECT * FROM user", "x")
        with sql_patch, chat_patch, \
                patch("app.agents.data_analysis.validate_sql", side_effect=QueryRejected("禁止访问表: {'user'}")):
            out = await data_analysis.analyze("查用户表")

        assert out["sql"] == ""
        assert "查询被拒绝" in out["answer"]

    @pytest.mark.asyncio
    async def test_forbidden_column_rejected(self):
        sql_patch, chat_patch = _patch_llms("SELECT password FROM orders", "x")
        with sql_patch, chat_patch, \
                patch("app.agents.data_analysis.validate_sql", side_effect=QueryRejected("禁止访问敏感字段")):
            out = await data_analysis.analyze("查密码")

        assert out["sql"] == ""
        assert "查询被拒绝" in out["answer"]

    @pytest.mark.asyncio
    async def test_unexpected_exception_returns_analysis_failed(self):
        sql_patch, chat_patch = _patch_llms("SELECT id FROM orders", "x")
        with sql_patch, chat_patch, \
                patch("app.agents.data_analysis.validate_sql", side_effect=RuntimeError("db down")):
            out = await data_analysis.analyze("q")

        assert out["sql"] == "SELECT id FROM orders"
        assert out["result"] == {}
        assert "分析失败" in out["answer"]
