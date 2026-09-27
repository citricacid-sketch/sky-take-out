"""安全只读 SQL 执行器。

多层防线：
  1. 只允许 SELECT
  2. sqlglot 解析 AST
  3. 表名白名单
  4. 字段名黑名单 (敏感字段)
  5. 禁止 UNION / 多语句 / 注释绕过
  6. 强制 LIMIT
  7. 查询超时
  8. 数据库只读账号 (DBA 层)
"""

from __future__ import annotations

import logging
import re
from typing import Any

import sqlglot
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine

from app.config import settings

logger = logging.getLogger(__name__)

ALLOWED_TABLES = {"orders", "order_detail", "dish", "setmeal", "category"}
FORBIDDEN_COLUMNS = {"password", "phone", "address", "id_number"}
SELECT_RE = re.compile(r"^\s*SELECT\b", re.IGNORECASE)
DANGER_KEYWORDS = {"INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "ALTER", "CREATE", "REPLACE",
                   "UNION", "INTO", "INFORMATION_SCHEMA", "SLEEP", "BENCHMARK", "LOAD_FILE",
                   "OUTFILE", "DUMPFILE"}
COMMENT_RE = re.compile(r"(--|#|/\*|\*/)")


class QueryRejected(Exception):
    """SQL 被安全规则拒绝。"""


def _build_engine() -> Engine:
    pw = settings.mysql_password
    url = (
        f"mysql+pymysql://{settings.mysql_user}:{pw}"
        f"@{settings.mysql_host}:{settings.mysql_port}/{settings.mysql_database}"
        "?charset=utf8mb4"
    )
    return create_engine(url, pool_pre_ping=True, pool_recycle=3600)


_engine: Engine | None = None


def get_engine() -> Engine:
    global _engine
    if _engine is None:
        _engine = _build_engine()
    return _engine


def validate_sql(sql: str) -> str:
    if not sql or not sql.strip():
        raise QueryRejected("SQL 为空")
    if COMMENT_RE.search(sql):
        raise QueryRejected("禁止使用注释")
    if not SELECT_RE.match(sql):
        raise QueryRejected("只允许 SELECT 查询")

    kw = sql.upper()
    for d in DANGER_KEYWORDS:
        if re.search(rf"\b{d}\b", kw):
            raise QueryRejected(f"禁止的关键字: {d}")

    try:
        parsed = sqlglot.parse_one(sql, read="mysql")
    except Exception as e:
        raise QueryRejected(f"SQL 解析失败: {e}")

    if parsed.key.lower() != "select":
        raise QueryRejected("只允许 SELECT 查询")

    tables = {t.name.lower() for t in parsed.find_all(sqlglot.exp.Table)}
    forbidden = tables - ALLOWED_TABLES
    if forbidden:
        raise QueryRejected(f"禁止访问表: {forbidden}")

    cols = {c.name.lower() for c in parsed.find_all(sqlglot.exp.Column)}
    bad_cols = cols & FORBIDDEN_COLUMNS
    if bad_cols:
        raise QueryRejected(f"禁止访问敏感字段: {bad_cols}")

    return sql.strip().rstrip(";")


def ensure_limit(sql: str) -> str:
    if re.search(r"\bLIMIT\b", sql, re.IGNORECASE):
        return sql
    return f"{sql} LIMIT {settings.query_max_rows}"


def execute_readonly(sql: str) -> dict[str, Any]:
    cleaned = validate_sql(sql)
    limited = ensure_limit(cleaned)
    timeout_sec = max(1, settings.query_timeout_ms // 1000)
    logger.info("执行 SQL (timeout=%ds): %s", timeout_sec, limited)

    with get_engine().connect() as conn:
        conn.execute(text(f"SET SESSION MAX_EXECUTION_TIME={settings.query_timeout_ms}"))
        result = conn.execute(text(limited))
        columns = list(result.keys())
        rows = [dict(zip(columns, row)) for row in result.fetchall()]
        return {"columns": columns, "rows": rows}
