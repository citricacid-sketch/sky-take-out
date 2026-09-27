"""DataAnalysisAgent：NL2SQL 固定链路。

链路：
  用户问题 → sql_llm 生成 SQL → 安全校验 → SQLAlchemy 执行 → chat_llm 中文总结
"""

from __future__ import annotations

import logging
import re
from typing import Any

from langchain_core.messages import HumanMessage, SystemMessage

from app.db import QueryRejected, execute_readonly, validate_sql
from app.llm import get_chat_llm, get_sql_llm

logger = logging.getLogger(__name__)

SQL_SYSTEM_PROMPT = """你是苍穹外卖数据分析助手。你的职责是将用户的自然语言问题转换为 MySQL 查询 SQL。

以下是苍穹外卖系统的 MySQL 数据库表结构：

1. orders（订单表）: id, number(订单号), status(1待付款 2待接单 3已接单 4派送中 5已完成 6已取消),
   user_id, address_book_id, order_time(下单时间), checkout_time(结账时间), pay_method(支付方式),
   pay_status(支付状态 0未支付 1已支付), amount(金额), remark, estimated_delivery_time, delivery_time, pack_amount, tableware_number

2. order_detail（订单明细表）: id, name(商品名称), order_id, dish_id, setmeal_id, dishFlavor(口味), number(数量), amount

3. dish（菜品表）: id, name, category_id, price, description, status(0停售 1起售), create_time, update_time

4. setmeal（套餐表）: id, category_id, name, price, status(0停售 1起售), description, create_time, update_time

5. category（分类表）: id, type(1菜品分类 2套餐分类), name, sort, status

注意：order_time 和 create_time 等时间字段是 datetime 类型，可以使用 DATE() 函数提取日期，
使用 DATE_SUB(CURDATE(), INTERVAL 7 DAY) 等方式表示时间范围。
禁止访问 user、employee 表，禁止访问 password、phone、address、id_number 等敏感字段。

规则：
1. 只生成 SELECT 查询语句
2. 只使用上面列出的表名（orders, order_detail, dish, setmeal, category）
3. 只使用表中列出的字段，禁止访问敏感字段（password, phone, address, id_number）
4. 禁止使用 SELECT *，必须明确指定字段名
5. 只返回纯 SQL 语句，不要包含任何解释、注释或 markdown 标记
6. 只生成单条 SQL 语句，禁止多语句
7. 如果需要时间范围，默认使用合理的时间区间（如最近7天、本月等）
8. 金额字段使用 amount，时间字段使用 order_time
9. 状态字段：订单状态 5=已完成，6=已取消，1=待付款，2=待接单
10. 如果需要排序，默认按相关数值降序排列
11. 对于可能返回大量结果的查询，使用 LIMIT 限制返回行数（默认 LIMIT 100）
12. 禁止 UNION、WITH/CTE 等复杂结构
"""

SUMMARY_SYSTEM_PROMPT = """你是苍穹外卖数据分析助手。根据用户的问题和查询结果，生成简洁、专业的中文分析总结。

规则：
1. 用简洁的中文回答
2. 突出关键数据和趋势
3. 如果数据为空，说明没有找到相关数据
4. 不要重复 SQL 语句
5. 可以给出简要的业务建议
"""

# LLM 输出可能包裹 ```sql ... ``` 或 ```，需要提取纯 SQL
_SQL_FENCE_RE = re.compile(r"```(?:sql)?\s*", re.IGNORECASE)


def _extract_sql(text: str) -> str:
    """从 LLM 输出中提取纯 SQL。

    处理以下情况：
    - 纯 SQL
    - ```sql ... ``` markdown 包裹
    - 附带解释文字（取第一条语句）
    """
    if not text:
        return ""
    sql = text.strip()
    # 去掉 ```sql / ``` 围栏
    sql = _SQL_FENCE_RE.sub("", sql).strip()
    sql = sql.rstrip("`").strip()
    # 只取第一条语句（分号前）
    sql = sql.split("\n```")[0].strip()
    if ";" in sql:
        sql = sql.split(";")[0].strip()
    return sql


def _format_result(result: dict[str, Any]) -> str:
    """将查询结果格式化为可读文本供总结 LLM 使用。"""
    columns = result.get("columns", [])
    rows = result.get("rows", [])
    if not columns:
        return "查询结果为空。"
    lines = [" | ".join(columns), "-" * 40]
    for row in rows[:20]:
        lines.append(" | ".join(str(row.get(c, "")) for c in columns))
    if len(rows) > 20:
        lines.append(f"... 共 {len(rows)} 行，仅展示前 20 行")
    return "\n".join(lines)


async def analyze(question: str) -> dict[str, Any]:
    """执行 NL2SQL 分析。

    返回：{"question", "sql", "result": {"columns", "rows"}, "answer"}
    """
    result: dict[str, Any] = {"columns": [], "rows": []}
    sql = ""

    try:
        # 1. sql_llm 生成 SQL
        sql_llm = get_sql_llm()
        sql_resp = await sql_llm.ainvoke(
            [
                SystemMessage(content=SQL_SYSTEM_PROMPT),
                HumanMessage(content=question),
            ]
        )
        raw_sql = getattr(sql_resp, "content", "") or ""
        sql = _extract_sql(raw_sql)
        logger.info("生成 SQL: %s", sql)

        # 2. 安全校验 + 3. 执行
        validate_sql(sql)
        result = execute_readonly(sql)

        # 4. chat_llm 中文总结
        chat_llm = get_chat_llm()
        summary_prompt = (
            f"用户问题：{question}\n\n"
            f"查询结果：\n{_format_result(result)}\n\n"
            f"请生成分析总结。"
        )
        summary_resp = await chat_llm.ainvoke(
            [
                SystemMessage(content=SUMMARY_SYSTEM_PROMPT),
                HumanMessage(content=summary_prompt),
            ]
        )
        answer = getattr(summary_resp, "content", "") or ""
        return {"question": question, "sql": sql, "result": result, "answer": answer}

    except QueryRejected as e:
        logger.warning("查询被拒绝: %s", e)
        return {"question": question, "sql": "", "result": {}, "answer": f"查询被拒绝：{e}"}

    except Exception as e:  # noqa: BLE001
        logger.exception("分析失败")
        return {"question": question, "sql": sql, "result": {}, "answer": f"分析失败：{e}"}
