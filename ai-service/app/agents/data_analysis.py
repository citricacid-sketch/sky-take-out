"""DataAnalysisAgent：NL2SQL（自然语言转 SQL）固定链路。

本模块实现 "苍穹外卖" 数据分析助手的核心流程：将用户输入的自然语言问题
自动转换为安全的 MySQL 查询，执行后将结果以中文自然语言总结返回。

链路（Pipeline）：
  用户问题
    → [1] sql_llm 根据系统提示词生成候选 SQL
    → [2] _extract_sql 从 LLM 输出中剥离 markdown 围栏，提取纯 SQL
    → [3] validate_sql 进行安全校验（只读 / 白名单表 / 黑名单字段）
    → [4] execute_readonly 通过 SQLAlchemy 执行查询
    → [5] _format_result 将结果集格式化为可读文本（含截断策略）
    → [6] chat_llm 根据问题与结果生成中文分析总结
    → 返回 {question, sql, result, answer}

异常处理：
  - QueryRejected：SQL 未通过安全校验（如包含写操作或敏感字段），
    直接返回拒绝提示，不执行、不总结。
  - 其他 Exception：记录日志并返回友好的失败提示，避免异常冒泡到调用方。
"""

from __future__ import annotations

import logging
import re
from typing import Any

from langchain_core.messages import HumanMessage, SystemMessage

from app.db import QueryRejected, execute_readonly, validate_sql
from app.llm import get_chat_llm, get_sql_llm

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# SQL 生成专用系统提示词
# 通过严格的规则约束 LLM 输出，降低注入风险并保证 SQL 可执行。
# ---------------------------------------------------------------------------
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
1. 只生成 SELECT 查询语句 —— 限定只读，防止数据被篡改
2. 只使用上面列出的表名（orders, order_detail, dish, setmeal, category） —— 表名白名单
3. 只使用表中列出的字段，禁止访问敏感字段（password, phone, address, id_number） —— 字段级安全
4. 禁止使用 SELECT *，必须明确指定字段名 —— 减少数据传输量，避免泄露敏感列
5. 只返回纯 SQL 语句，不要包含任何解释、注释或 markdown 标记 —— 便于程序解析
6. 只生成单条 SQL 语句，禁止多语句 —— 防止堆叠查询注入
7. 如果需要时间范围，默认使用合理的时间区间（如最近7天、本月等） —— 避免全表扫描
8. 金额字段使用 amount，时间字段使用 order_time —— 统一字段语义
9. 状态字段：订单状态 5=已完成，6=已取消，1=待付款，2=待接单 —— 枚举映射
10. 如果需要排序，默认按相关数值降序排列 —— 让用户优先看到 Top-N
11. 对于可能返回大量结果的查询，使用 LIMIT 限制返回行数（默认 LIMIT 100） —— 防止结果集过大
12. 禁止 UNION、WITH/CTE 等复杂结构 —— 降低注入面，便于静态校验
"""

# ---------------------------------------------------------------------------
# 中文总结专用系统提示词
# 指导 LLM 根据查询结果生成面向业务人员的可读分析。
# ---------------------------------------------------------------------------
SUMMARY_SYSTEM_PROMPT = """你是苍穹外卖数据分析助手。根据用户的问题和查询结果，生成简洁、专业的中文分析总结。

规则：
1. 用简洁的中文回答 —— 面向中文业务用户
2. 突出关键数据和趋势 —— 聚焦业务价值
3. 如果数据为空，说明没有找到相关数据 —— 避免空输出
4. 不要重复 SQL 语句 —— 用户无需关心实现细节
5. 可以给出简要的业务建议 —— 提升分析深度
"""

# 用于剥离 LLM 输出中 ```sql / ``` 等 markdown 围栏的正则
# 匹配开头的 ```sql 或 ```（忽略大小写），后续再配合 rstrip("`") 清理尾部
_SQL_FENCE_RE = re.compile(r"```(?:sql)?\s*", re.IGNORECASE)


def _extract_sql(text: str) -> str:
    """从 LLM 输出中提取纯 SQL。

    由于 LLM 可能将 SQL 包裹在 markdown 代码块（```sql ... ```）中，
    或在 SQL 前后附带解释文字，本函数按以下顺序清洗：
      1. 去除首尾空白
      2. 用正则剥除开头的 ```sql / ``` 围栏标记
      3. 去除末尾残留的反引号
      4. 以 "\n```" 为界，只保留第一段（即代码块内的内容）
      5. 以第一个分号为止，截取第一条语句（防御多语句注入）

    Args:
        text: LLM 的原始输出字符串，可能包含 markdown 与解释文字。

    Returns:
        清洗后的单条纯 SQL 字符串；若输入为空则返回空字符串。
    """
    if not text:
        return ""
    sql = text.strip()
    # 去掉 ```sql / ``` 围栏（开头）
    sql = _SQL_FENCE_RE.sub("", sql).strip()
    # 去掉末尾残留的反引号
    sql = sql.rstrip("`").strip()
    # 只取第一条语句：先以换行+```截断，再以分号截断，防止多语句
    sql = sql.split("\n```")[0].strip()
    if ";" in sql:
        sql = sql.split(";")[0].strip()
    return sql


def _format_result(result: dict[str, Any]) -> str:
    """将查询结果格式化为可读文本，供总结 LLM 使用。

    采用 "截断策略"：仅展示前 20 行，超过时追加总行数提示。
    原因：
      - LLM 上下文长度有限，全量传入会浪费 token 甚至溢出；
      - 业务总结通常基于 Top-N 样本即可，无需全量数据。

    Args:
        result: 执行结果字典，包含 columns（列名列表）与 rows（行列表）。

    Returns:
        格式化的表格文本；若列名为空则返回 "查询结果为空。"。
    """
    columns = result.get("columns", [])
    rows = result.get("rows", [])
    if not columns:
        return "查询结果为空。"
    # 表头
    lines = [" | ".join(columns), "-" * 40]
    # 截断：只渲染前 20 行，避免 prompt 过长
    for row in rows[:20]:
        lines.append(" | ".join(str(row.get(c, "")) for c in columns))
    # 超出部分仅展示总行数摘要
    if len(rows) > 20:
        lines.append(f"... 共 {len(rows)} 行，仅展示前 20 行")
    return "\n".join(lines)


async def analyze(question: str) -> dict[str, Any]:
    """执行 NL2SQL 分析的主流程。

    依次完成：生成 SQL → 安全校验 → 执行查询 → 中文总结。
    任何阶段失败都会被捕获，并返回包含友好错误信息的统一结构。

    Args:
        question: 用户的自然语言问题，例如 "最近7天销量最高的菜品是什么？"。

    Returns:
        统一返回字典，字段如下：
          - question: 原始问题
          - sql: 最终执行的纯 SQL（失败时可能为空字符串）
          - result: 查询结果 {"columns": [...], "rows": [...]}，失败时为 {}
          - answer: 中文总结或错误提示

    Raises:
        本函数不抛出异常；所有异常均被捕获并转换为 answer 字段的错误说明。
    """
    result: dict[str, Any] = {"columns": [], "rows": []}
    sql = ""

    try:
        # ---------------------------------------------------------------
        # 步骤 1：调用 sql_llm 生成候选 SQL
        # 将系统提示词（表结构 + 规则）与用户问题拼接，让 LLM 输出 SQL
        # ---------------------------------------------------------------
        sql_llm = get_sql_llm()
        sql_resp = await sql_llm.ainvoke(
            [
                SystemMessage(content=SQL_SYSTEM_PROMPT),
                HumanMessage(content=question),
            ]
        )
        raw_sql = getattr(sql_resp, "content", "") or ""
        # 从 LLM 输出中提取纯 SQL（去除 markdown 围栏与多余解释）
        sql = _extract_sql(raw_sql)
        logger.info("生成 SQL: %s", sql)

        # ---------------------------------------------------------------
        # 步骤 2：安全校验 —— 在执行业务 SQL 前拦截危险操作
        # 步骤 3：通过 SQLAlchemy 以只读方式执行已校验的 SQL
        # 校验与执行合并到同一 try 块，便于统一捕获异常
        # ---------------------------------------------------------------
        validate_sql(sql)          # 校验失败会抛出 QueryRejected
        result = execute_readonly(sql)

        # ---------------------------------------------------------------
        # 步骤 4：调用 chat_llm 根据问题与结果生成中文总结
        # 将格式化后的结果集嵌入提示词，让 LLM 面向业务进行解读
        # ---------------------------------------------------------------
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
        # QueryRejected：SQL 未通过安全校验（如包含写操作、访问敏感表/字段）
        # 直接返回拒绝提示，不执行、不总结，记录警告日志
        logger.warning("查询被拒绝: %s", e)
        return {"question": question, "sql": "", "result": {}, "answer": f"查询被拒绝：{e}"}

    except Exception as e:  # noqa: BLE001
        # 兜底异常处理：记录完整堆栈，向用户返回友好提示
        # 避免异常冒泡到上层调用方，保证接口稳定性
        logger.exception("分析失败")
        return {"question": question, "sql": sql, "result": {}, "answer": f"分析失败：{e}"}
