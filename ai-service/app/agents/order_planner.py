"""OrderPlannerAgent —— 规则引擎点餐推荐（链式，不依赖 LLM）。

从数据库拉取在售菜品，按口味偏好、忌口、预算、人数做贪心组合推荐。
"""
from __future__ import annotations

import logging
from typing import Any

from app.db import execute_readonly

logger = logging.getLogger(__name__)

# 主食关键词：匹配到的菜品数量按人数配
STAPLE_KEYWORDS = ("饭", "面", "粥", "粉", "饺", "包", "馒")

# 口味偏好 -> (匹配关键词, 加分)
TASTE_SCORES: dict[str, tuple[tuple[str, ...], int]] = {
    "spicy": (("辣", "麻辣", "香辣"), 2),
    "mild_spicy": (("微辣",), 2),
    "light": (("清淡", "清口", "蒸", "煮", "白"), 2),
    "sweet": (("甜", "蜜", "糖"), 2),
    "sour": (("酸", "醋"), 2),
    "salty": (("咸", "酱", "腌"), 1),
}


def _is_staple(dish: dict[str, Any]) -> bool:
    """判断菜品是否为主食。"""
    haystack = f"{dish.get('name', '')}{dish.get('description', '')}"
    return any(k in haystack for k in STAPLE_KEYWORDS)


def match_taste(dish: dict[str, Any], taste_prefs: list[str]) -> int:
    """计算菜品与口味偏好的匹配分。

    与 miniapp 版一致：spicy/mild_spicy/light/sweet/sour/salty。
    """
    if not taste_prefs:
        return 0
    haystack = f"{dish.get('name', '')} {dish.get('description', '')}".lower()
    score = 0
    for pref in taste_prefs:
        if pref == "spicy" and any(k in haystack for k in ("辣", "麻辣", "香辣")):
            score += 2
        elif pref == "mild_spicy" and "微辣" in haystack:
            score += 2
        elif pref == "no_spicy" and "辣" not in haystack:
            score += 1
        elif pref == "light" and any(k in haystack for k in ("清淡", "清口", "蒸", "煮", "白")):
            score += 2
        elif pref == "sweet" and any(k in haystack for k in ("甜", "蜜", "糖")):
            score += 2
        elif pref == "sour" and any(k in haystack for k in ("酸", "醋")):
            score += 2
        elif pref == "salty" and any(k in haystack for k in ("咸", "酱", "腌")):
            score += 1
    return score


def _is_excluded(dish: dict[str, Any], excludes: list[str]) -> bool:
    """判断菜品是否包含忌口食材。"""
    if not excludes:
        return False
    haystack = f"{dish.get('name', '')} {dish.get('description', '')}".lower()
    return any(e.lower() in haystack for e in excludes)


def _safe_float(value: Any) -> float:
    """安全转 float，处理 Decimal 等。"""
    if value is None:
        return 0.0
    return float(value)


async def plan(
    people: int,
    budget: float,
    tastes: list[str] | None = None,
    excludes: list[str] | None = None,
) -> dict[str, Any]:
    """点餐推荐入口。

    :param people: 用餐人数
    :param budget: 预算上限（0 表示不限）
    :param tastes: 口味偏好列表
    :param excludes: 忌口食材列表
    :return: {"items": [...], "total": float, "reason": str}
    """
    tastes = list(tastes or [])
    excludes = list(excludes or [])
    people = max(1, people)

    # 1. 拉取在售菜品
    try:
        result = execute_readonly(
            "SELECT id, name, price, description, status FROM dish WHERE status = 1"
        )
        dishes: list[dict[str, Any]] = result["rows"]
    except Exception as e:
        logger.exception("OrderPlannerAgent: 查询菜品失败")
        return {
            "items": [],
            "total": 0.0,
            "reason": f"数据库暂不可用，请稍后重试（{e}）",
        }

    if not dishes:
        return {"items": [], "total": 0.0, "reason": "当前没有在售菜品"}

    # 2. 过滤忌口
    candidates = [d for d in dishes if not _is_excluded(d, excludes)]
    if not candidates:
        return {"items": [], "total": 0.0, "reason": "过滤忌口后无可用菜品"}

    # 3. 按口味匹配分降序、价格升序排序
    candidates.sort(
        key=lambda d: (-match_taste(d, tastes), _safe_float(d.get("price")))
    )

    # 4. 贪心选择
    items: list[dict[str, Any]] = []
    total = 0.0
    max_items = people * 2
    budget_unlimited = budget <= 0

    for dish in candidates:
        if len(items) >= max_items:
            break
        price = _safe_float(dish.get("price"))
        if price <= 0:
            continue

        quantity = people if _is_staple(dish) else 1
        cost = price * quantity

        if not budget_unlimited and total + cost > budget:
            # 主食按人数超预算时，尝试单份
            if quantity > 1:
                quantity = 1
                cost = price
                if total + cost > budget:
                    continue
            else:
                continue

        items.append({
            "id": dish["id"],
            "name": dish.get("name", ""),
            "price": price,
            "quantity": quantity,
            "reason": _item_reason(dish, tastes),
        })
        total += cost

    # 5. 组装结果
    reason = _build_reason(items, total, budget, people, tastes, budget_unlimited)
    return {"items": items, "total": round(total, 2), "reason": reason}


def _item_reason(dish: dict[str, Any], tastes: list[str]) -> str:
    """单个菜品的推荐理由。"""
    score = match_taste(dish, tastes)
    if score > 0:
        return "匹配您的口味偏好"
    if _is_staple(dish):
        return "主食推荐"
    return "丰富搭配"


def _build_reason(
    items: list[dict[str, Any]],
    total: float,
    budget: float,
    people: int,
    tastes: list[str],
    budget_unlimited: bool,
) -> str:
    """整体推荐理由。"""
    if not items:
        return "预算不足，未能推荐任何菜品"

    parts = [f"为 {people} 位客人推荐 {len(items)} 道菜品，合计 {total:.2f} 元"]
    if tastes:
        parts.append(f"已按口味偏好（{', '.join(tastes)}）优先匹配")
    if not budget_unlimited:
        parts.append(f"控制在预算 {budget:.2f} 元内")
    return "；".join(parts)
