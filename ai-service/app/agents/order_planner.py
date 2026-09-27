"""OrderPlannerAgent —— 规则引擎点餐推荐（链式，不依赖 LLM）。

推荐算法说明
=============
本模块实现了一套基于规则引擎的点餐推荐算法，核心流程为：拉取 → 过滤 → 评分 → 贪心选择 → 组装。
整体不依赖大语言模型（LLG），完全基于确定性规则，保证推荐结果的可解释性与稳定性。

算法步骤：
1. 数据拉取：从数据库查询所有在售菜品（status = 1）。
2. 忌口过滤：根据用户提供的忌口食材列表，排除包含忌口的菜品。
3. 口味评分：对每道菜品按用户口味偏好计算匹配分（match_taste），分数越高越符合口味。
4. 排序策略：先按口味匹配分降序排列，分数相同时按价格升序排列（同等口味下优先选便宜的）。
5. 贪心选择：遍历排序后的候选菜品，按预算和人数约束逐个选取——
   - 主食类菜品按人数配份（每人一份），非主食按 1 份配；
   - 若加入某菜品后超出预算，主食尝试降为 1 份，非主食直接跳过；
   - 最多选取 people * 2 道菜。
6. 结果组装：返回菜品列表、总价和推荐理由。

数据结构
---------
- 输入：用餐人数、预算上限、口味偏好列表、忌口食材列表。
- 输出：{"items": [...], "total": float, "reason: str}
  - items: 推荐菜品列表，每项包含 id / name / price / quantity / reason。
  - total: 推荐菜品总价（保留两位小数）。
  - reason: 整体推荐理由文本，说明人数、菜品数、口味匹配和预算控制情况。

设计取舍
---------
- 使用贪心而非动态规划：菜品数量通常较少（几十道），贪心已足够高效且结果直观可解释。
- 主食按人数配：保证每人都有主食，避免推荐结果缺少饱腹感菜品。
- 多口味叠加：同一道菜可匹配多个口味偏好，分数累加，鼓励"多口味满足"的菜品。
"""
from __future__ import annotations

import logging
from typing import Any

from app.db import execute_readonly

logger = logging.getLogger(__name__)

# 主食关键词：菜品名称或描述中包含任一关键词即识别为主食。
# 匹配到的菜品数量按人数配（每人一份）。
STAPLE_KEYWORDS = ("饭", "面", "粥", "粉", "饺", "包", "馒")

# 口味偏好 -> (匹配关键词元组, 加分值)
# 用于 TASTE_SCORES 字典的参考（实际评分逻辑在 match_taste 中实现，此处保留供对照）。
# 不同口味权重不同：辣/清淡/甜/酸 权重较高(2)，咸类权重较低(1)。
TASTE_SCORES: dict[str, tuple[tuple[str, ...], int]] = {
    "spicy": (("辣", "麻辣", "香辣"), 2),
    "mild_spicy": (("微辣",), 2),
    "light": (("清淡", "清口", "蒸", "煮", "白"), 2),
    "sweet": (("甜", "蜜", "糖"), 2),
    "sour": (("酸", "醋"), 2),
    "salty": (("咸", "酱", "腌"), 1),
}


def _is_staple(dish: dict[str, Any]) -> bool:
    """判断菜品是否为主食。

    主食识别逻辑
    -------------
    将菜品的 name（名称）和 description（描述）拼接为一个"待检索字符串"，
    逐一检查 STAPLE_KEYWORDS 中的关键词是否出现在该字符串中。
    只要命中任一关键词即判定为主食。

    注意：关键词匹配为子串匹配，可能存在误判（如"面包"中的"包"），
    但在餐饮场景下可接受，且主食按人数配份的业务逻辑容忍少量误判。

    :param dish: 菜品字典，至少包含 "name" 和 "description" 键。
    :return: 若识别为主食返回 True，否则返回 False。
    """
    # 拼接名称和描述作为检索源；缺失字段降级为空字符串
    haystack = f"{dish.get('name', '')}{dish.get('description', '')}"
    # 任一关键词命中即判定为主食
    return any(k in haystack for k in STAPLE_KEYWORDS)


def match_taste(dish: dict[str, Any], taste_prefs: list[str]) -> int:
    """计算菜品与口味偏好的匹配分。

    口味匹配逻辑（逐项说明）
    -------------------------
    将菜品名称与描述拼接后转为小写，作为匹配基底（haystack）。
    遍历用户的每项口味偏好，按以下规则逐项判断并累加分数：

    - "spicy"（辣）: 菜名/描述中包含"辣"、"麻辣"、"香辣"任一分 +2。
      覆盖常见的辣味表述，权重较高以体现辣味偏好的明确性。
    - "mild_spicy"（微辣）: 包含"微辣"一分 +2。
      与 spicy 独立判断，用户可同时偏好"辣"和"微辣"（累加得分）。
    - "no_spicy"（不吃辣）: 菜名/描述中不含"辣"字 +1。
      作为奖励分而非惩罚分，鼓励不辣的菜品排到前面。
      注意：仅检查"辣"字，可能误判含"辣"字但不辣的菜品（如"辣椒"装饰）。
    - "light"（清淡）: 包含"清淡"、"清口"、"蒸"、"煮"、"白"任一分 +2。
      蒸煮白等烹饪方式关键词帮助识别清淡类菜品。
    - "sweet"（甜）: 包含"甜"、"蜜"、"糖"任一分 +2。
    - "sour"（酸）: 包含"酸"、"醋"任一分 +2。
    - "salty"（咸）: 包含"咸"、"酱"、"腌"任一分 +1。
      咸味权重较低(1)，因为咸味较普遍，不宜过度影响排序。

    多口味叠加：同一道菜匹配多个偏好时分数累加，最高可叠加多个 +2/+1。
    例如一道"酸甜辣"菜品可同时命中 sweet(+2)、sour(+2)、spicy(+2)，得 6 分。

    :param dish: 菜品字典，至少包含 "name" 和 "description" 键。
    :param taste_prefs: 口味偏好字符串列表，如 ["spicy", "light"]。
    :return: 口味匹配总分（>= 0），分数越高表示越符合用户口味。
    """
    # 无口味偏好时直接返回 0 分
    if not taste_prefs:
        return 0

    # 拼接名称和描述并转小写，作为统一的匹配基底
    haystack = f"{dish.get('name', '')} {dish.get('description', '')}".lower()
    score = 0

    # 逐项检查口味偏好，累加得分
    for pref in taste_prefs:
        if pref == "spicy" and any(k in haystack for k in ("辣", "麻辣", "香辣")):
            # 辣味匹配：命中"辣"、"麻辣"、"香辣"任一关键词
            score += 2
        elif pref == "mild_spicy" and "微辣" in haystack:
            # 微辣匹配：精确匹配"微辣"
            score += 2
        elif pref == "no_spicy" and "辣" not in haystack:
            # 不吃辣奖励：完全不包含"辣"字的菜品给予正向加分
            score += 1
        elif pref == "light" and any(k in haystack for k in ("清淡", "清口", "蒸", "煮", "白")):
            # 清淡匹配：包含清淡描述或蒸煮烹饪方式关键词
            score += 2
        elif pref == "sweet" and any(k in haystack for k in ("甜", "蜜", "糖")):
            # 甜味匹配
            score += 2
        elif pref == "sour" and any(k in haystack for k in ("酸", "醋")):
            # 酸味匹配
            score += 2
        elif pref == "salty" and any(k in haystack for k in ("咸", "酱", "腌")):
            # 咸味匹配，权重较低
            score += 1
    return score


def _is_excluded(dish: dict[str, Any], excludes: list[str]) -> bool:
    """判断菜品是否包含忌口食材。

    忌口过滤逻辑
    -------------
    将菜品名称与描述拼接后转为小写，逐一检查每个忌口食材是否作为子串出现在其中。
    任一忌口命中即判定该菜品应被排除。

    注意：
    - 匹配为大小写不敏感的（通过 lower() 转换）。
    - 子串匹配可能导致误判（如忌口"虾"会匹配到"虾仁"、"虾皮"），
      在餐饮场景下这通常是期望行为（用户可能对相关食材均过敏）。

    :param dish: 菜品字典，至少包含 "name" 和 "description" 键。
    :param excludes: 忌口食材字符串列表，如 ["虾", "花生"]。
    :return: 若菜品包含任一忌口食材返回 True，否则返回 False。
    """
    # 无忌口时直接返回 False（不排除任何菜品）
    if not excludes:
        return False

    # 拼接名称和描述并转小写，作为统一的匹配基底
    haystack = f"{dish.get('name', '')} {dish.get('description', '')}".lower()
    # 任一忌口食材命中即排除该菜品
    return any(e.lower() in haystack for e in excludes)


def _safe_float(value: Any) -> float:
    """安全转 float，处理 Decimal 等类型。

    数据库返回的价格字段可能为 Decimal 类型，统一转为 float 以便算术运算。
    若值为 None 则降级为 0.0，避免 TypeError。

    :param value: 待转换的值，可为 Decimal、int、float、str 或 None。
    :return: 转换后的 float 值，None 时返回 0.0。
    """
    # None 值降级为 0.0，防止后续算术运算异常
    if value is None:
        return 0.0
    return float(value)


async def plan(
    people: int,
    budget: float,
    tastes: list[str] | None = None,
    excludes: list[str] | None = None,
) -> dict[str, Any]:
    """点餐推荐入口 —— 基于规则引擎的贪心组合推荐。

    整体流程
    --------
    1. 参数归一化：将 None 转为空列表，人数至少为 1。
    2. 数据拉取：查询在售菜品。
    3. 忌口过滤：排除含忌口食材的菜品。
    4. 排序：按口味匹配分降序、价格升序。
    5. 贪心选择：按预算和人数约束逐个选取菜品。
    6. 结果组装：返回 items / total / reason。

    :param people: 用餐人数，至少为 1。
    :param budget: 预算上限（元），0 或负数表示不限预算。
    :param tastes: 口味偏好列表，可选，如 ["spicy", "light"]。
    :param excludes: 忌口食材列表，可选，如 ["虾", "花生"]。
    :return: 推荐结果字典，结构如下：
        - items: 推荐菜品列表，每项包含：
            - id: 菜品 ID
            - name: 菜品名称
            - price: 单价
            - quantity: 推荐数量（主食按人数，非主食为 1）
            - reason: 单个菜品的推荐理由文本
        - total: 推荐菜品总价（float，保留两位小数）
        - reason: 整体推荐理由文本（说明人数、菜品数、口味匹配、预算控制）
    :raises: 本函数不抛出异常，数据库错误时返回空列表 + 错误原因文本。
    """
    # ---- 1. 参数归一化 ----
    # 将 None 转为空列表，避免后续逻辑判断冗余
    tastes = list(tastes or [])
    excludes = list(excludes or [])
    # 人数至少为 1，防止非法输入导致后续计算异常
    people = max(1, people)

    # ---- 2. 拉取在售菜品 ----
    # 仅查询 status = 1 的菜品（1 表示在售/启用状态）
    try:
        result = execute_readonly(
            "SELECT id, name, price, description, status FROM dish WHERE status = 1"
        )
        dishes: list[dict[str, Any]] = result["rows"]
    except Exception as e:
        # 数据库查询失败时记录日志并返回友好的错误信息
        logger.exception("OrderPlannerAgent: 查询菜品失败")
        return {
            "items": [],
            "total": 0.0,
            "reason": f"数据库暂不可用，请稍后重试（{e}）",
        }

    # 无在售菜品时提前返回
    if not dishes:
        return {"items": [], "total": 0.0, "reason": "当前没有在售菜品"}

    # ---- 3. 过滤忌口 ----
    # 使用 _is_excluded 逐一检查，排除包含忌口食材的菜品
    candidates = [d for d in dishes if not _is_excluded(d, excludes)]
    # 过滤后无可用菜品时提前返回
    if not candidates:
        return {"items": [], "total": 0.0, "reason": "过滤忌口后无可用菜品"}

    # ---- 4. 按口味匹配分降序、价格升序排序 ----
    # 排序键：(-口味分, 价格)
    # - 口味分取负值实现降序（分数高的优先）
    # - 价格升序：口味相同时优先选便宜的，提高预算利用率
    candidates.sort(
        key=lambda d: (-match_taste(d, tastes), _safe_float(d.get("price")))
    )

    # ---- 5. 贪心选择 ----
    # 贪心策略说明：
    # 按排序后的顺序遍历候选菜品，逐个尝试加入推荐列表。
    # 约束条件：
    #   - 菜品数量上限：people * 2（每人最多 2 道菜）
    #   - 预算约束：加入后总价不超过预算（budget_unlimited 为 True 时忽略）
    #   - 主食配份：主食按人数配（每人一份），非主食配 1 份
    #   - 预算超限补救：主食按人数超预算时，尝试降为 1 份
    items: list[dict[str, Any]] = []  # 已选菜品列表
    total = 0.0  # 当前累计总价
    max_items = people * 2  # 菜品数量上限
    budget_unlimited = budget <= 0  # 预算是否无限（0 或负数表示不限）

    for dish in candidates:
        # 达到菜品数量上限时停止选择
        if len(items) >= max_items:
            break

        price = _safe_float(dish.get("price"))
        # 跳过价格为 0 或负数的异常菜品（避免无意义推荐或计算异常）
        if price <= 0:
            continue

        # 主食按人数配份，非主食配 1 份
        quantity = people if _is_staple(dish) else 1
        cost = price * quantity  # 该菜品的总花费

        # 预算检查：无限预算时跳过检查
        if not budget_unlimited and total + cost > budget:
            # 超出预算时的补救策略
            if quantity > 1:
                # 主食按人数超预算：尝试降为 1 份（保证至少有一份主食）
                quantity = 1
                cost = price  # 重新计算花费（单份）
                if total + cost > budget:
                    # 降为 1 份后仍超预算，跳过该菜品
                    continue
            else:
                # 非主食超预算：直接跳过（非必需品）
                continue

        # 将该菜品加入推荐列表
        items.append({
            "id": dish["id"],
            "name": dish.get("name", ""),
            "price": price,
            "quantity": quantity,
            "reason": _item_reason(dish, tastes),  # 单个菜品的推荐理由
        })
        total += cost  # 累加总价

    # ---- 6. 组装结果 ----
    # 生成整体推荐理由文本，并返回最终结构
    reason = _build_reason(items, total, budget, people, tastes, budget_unlimited)
    return {"items": items, "total": round(total, 2), "reason": reason}


def _item_reason(dish: dict[str, Any], tastes: list[str]) -> str:
    """生成单个菜品的推荐理由。

    推荐理由判定逻辑（优先级从高到低）：
    1. 口味匹配分 > 0：说明该菜品符合用户口味偏好，返回"匹配您的口味偏好"。
    2. 是主食：返回"主食推荐"，强调饱腹属性。
    3. 其他：返回"丰富搭配"，说明该菜品用于丰富餐桌多样性。

    :param dish: 菜品字典，至少包含 "name" 和 "description" 键。
    :param tastes: 用户口味偏好列表。
    :return: 推荐理由字符串。
    """
    # 优先判断口味匹配
    score = match_taste(dish, tastes)
    if score > 0:
        return "匹配您的口味偏好"
    # 其次判断是否为主食
    if _is_staple(dish):
        return "主食推荐"
    # 兜底：丰富搭配
    return "丰富搭配"


def _build_reason(
    items: list[dict[str, Any]],
    total: float,
    budget: float,
    people: int,
    tastes: list[str],
    budget_unlimited: bool,
) -> str:
    """生成整体推荐理由文本。

    文本组装逻辑
    -------------
    根据推荐结果动态拼接说明文本：
    - 基础信息：为 N 位客人推荐 M 道菜品，合计 X 元。
    - 口味信息：若存在口味偏好，追加"已按口味偏好（xxx）优先匹配"说明。
    - 预算信息：若预算有限，追加"控制在预算 X 元内"说明。
    - 无推荐结果时：返回"预算不足，未能推荐任何菜品"。

    :param items: 已推荐的菜品列表。
    :param total: 推荐菜品总价。
    :param budget: 预算上限。
    :param people: 用餐人数。
    :param tastes: 口味偏好列表。
    :param budget_unlimited: 预算是否无限。
    :return: 整体推荐理由文本，各信息段以中文分号"；"连接。
    """
    # 无推荐结果时的特殊说明
    if not items:
        return "预算不足，未能推荐任何菜品"

    # 基础推荐信息：人数、菜品数、总价
    parts = [f"为 {people} 位客人推荐 {len(items)} 道菜品，合计 {total:.2f} 元"]
    # 有口味偏好时追加口味匹配说明
    if tastes:
        parts.append(f"已按口味偏好（{', '.join(tastes)}）优先匹配")
    # 预算有限时追加预算控制说明
    if not budget_unlimited:
        parts.append(f"控制在预算 {budget:.2f} 元内")
    # 使用中文分号连接各信息段
    return "；".join(parts)
