"""安全只读 SQL 执行器。

对 LLM 生成的 SQL 执行多层安全校验，防止 SQL 注入和越权访问。

安全防线（按执行顺序）：
  1. 空值检查 — 拒绝空 SQL
  2. 注释检查 — 防止注释绕过 (-- # /*)
  3. 前缀检查 — 必须以 SELECT 开头
  4. 关键字黑名单 — 禁止 INSERT/UPDATE/DROP/UNION/INTO/SLEEP 等
  5. sqlglot AST 解析 — 解析为抽象语法树
  6. 语句类型校验 — AST 必须是 select 类型
  7. 表名白名单 — 仅允许 orders/order_detail/dish/setmeal/category
  8. 字段黑名单 — 禁止访问 password/phone/address/id_number
  9. 强制 LIMIT — 防止全表扫描
  10. 查询超时 — MySQL MAX_EXECUTION_TIME
  11. 数据库只读账号 — DBA 层兜底

使用示例：
    result = execute_readonly("SELECT id, name FROM dish WHERE status = 1 LIMIT 10")
    # => {"columns": ["id", "name"], "rows": [{"id": 1, "name": "鱼香肉丝"}, ...]}
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

# 允许查询的表（与 Java SqlSecurityValidator 保持一致）
ALLOWED_TABLES = {"orders", "order_detail", "dish", "setmeal", "category"}

# 禁止访问的敏感字段（用户隐私数据）
FORBIDDEN_COLUMNS = {"password", "phone", "address", "id_number"}

# SQL 必须以 SELECT 开头
SELECT_RE = re.compile(r"^\s*SELECT\b", re.IGNORECASE)

# 危险关键字黑名单（任何位置出现都拒绝）
DANGER_KEYWORDS = {
    "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "ALTER", "CREATE", "REPLACE",
    "UNION", "INTO", "INFORMATION_SCHEMA", "SLEEP", "BENCHMARK", "LOAD_FILE",
    "OUTFILE", "DUMPFILE",
}

# 注释正则（防止注释绕过）
COMMENT_RE = re.compile(r"(--|#|/\*|\*/)")


class QueryRejected(Exception):
    """SQL 被安全规则拒绝。"""


def _build_engine() -> Engine:
    """构造 SQLAlchemy 引擎。"""
    pw = settings.mysql_password
    url = (
        f"mysql+pymysql://{settings.mysql_user}:{pw}"
        f"@{settings.mysql_host}:{settings.mysql_port}/{settings.mysql_database}"
        "?charset=utf8mb4"
    )
    return create_engine(url, pool_pre_ping=True, pool_recycle=3600)


# 模块级引擎单例（避免重复创建连接池）
_engine: Engine | None = None


def get_engine() -> Engine:
    """获取或创建数据库引擎（单例）。"""
    global _engine
    if _engine is None:
        _engine = _build_engine()
    return _engine


def validate_sql(sql: str) -> str:
    """校验 SQL 安全性。

    通过全部校验后返回清洗后的 SQL，否则抛出 QueryRejected。

    Args:
        sql: 待校验的 SQL 语句。

    Returns:
        清洗后的 SQL（去除末尾分号）。

    Raises:
        QueryRejected: 任意一项安全校验失败。
    """
    # 1. 空值检查
    if not sql or not sql.strip():
        raise QueryRejected("SQL 为空")

    # 2. 注释检查（防止如 SELECT * FROM orders -- 注入）
    if COMMENT_RE.search(sql):
        raise QueryRejected("禁止使用注释")

    # 3. 前缀必须是 SELECT
    if not SELECT_RE.match(sql):
        raise QueryRejected("只允许 SELECT 查询")

    # 4. 关键字黑名单检查
    kw = sql.upper()
    for d in DANGER_KEYWORDS:
        if re.search(rf"\b{d}\b", kw):
            raise QueryRejected(f"禁止的关键字: {d}")

    # 5. sqlglot AST 解析
    try:
        parsed = sqlglot.parse_one(sql, read="mysql")
    except Exception as e:
        raise QueryRejected(f"SQL 解析失败: {e}")

    # 6. AST 类型必须是 select
    if parsed.key.lower() != "select":
        raise QueryRejected("只允许 SELECT 查询")

    # 7. 表名白名单
    tables = {t.name.lower() for t in parsed.find_all(sqlglot.exp.Table)}
    forbidden = tables - ALLOWED_TABLES
    if forbidden:
        raise QueryRejected(f"禁止访问表: {forbidden}")

    # 8. 字段黑名单
    cols = {c.name.lower() for c in parsed.find_all(sqlglot.exp.Column)}
    bad_cols = cols & FORBIDDEN_COLUMNS
    if bad_cols:
        raise QueryRejected(f"禁止访问敏感字段: {bad_cols}")

    return sql.strip().rstrip(";")


def ensure_limit(sql: str) -> str:
    """确保 SQL 有 LIMIT 子句（防止全表扫描）。

    如果已有 LIMIT 则直接返回，否则追加默认 LIMIT。
    """
    if re.search(r"\bLIMIT\b", sql, re.IGNORECASE):
        return sql
    return f"{sql} LIMIT {settings.query_max_rows}"


def execute_readonly(sql: str) -> dict[str, Any]:
    """执行安全的只读查询。

    流程：校验 → 加 LIMIT → 设置超时 → 执行 → 返回结构化结果。

    Args:
        sql: 经过 LLM 生成的 SELECT 语句。

    Returns:
        {"columns": [...], "rows": [{...}, ...]}

    Raises:
        QueryRejected: SQL 校验失败。
        SQLAlchemyError: 数据库执行异常。
    """
    cleaned = validate_sql(sql)
    limited = ensure_limit(cleaned)
    timeout_sec = max(1, settings.query_timeout_ms // 1000)
    logger.info("执行 SQL (timeout=%ds): %s", timeout_sec, limited)

    with get_engine().connect() as conn:
        # 设置 MySQL 查询超时（毫秒）
        conn.execute(text(f"SET SESSION MAX_EXECUTION_TIME={settings.query_timeout_ms}"))
        result = conn.execute(text(limited))
        columns = list(result.keys())
        rows = [dict(zip(columns, row)) for row in result.fetchall()]
        return {"columns": columns, "rows": rows}
