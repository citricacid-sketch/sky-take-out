"""OrderPlannerAgent 单元测试。

用 pytest + unittest.mock 模拟数据库，覆盖推荐、预算不足、忌口过滤、口味匹配。"""
from __future__ import annotations

from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient

from app.agents import order_planner
from app.main import app


@pytest.fixture
def client():
    return TestClient(app)


# 模拟在售菜品
FAKE_DISHES = [
    {"id": 1, "name": "麻辣牛肉面", "price": 28.0, "description": "香辣口味", "status": 1},
    {"id": 2, "name": "扬州炒饭", "price": 22.0, "description": "清淡可口", "status": 1},
    {"id": 3, "name": "酸菜鱼", "price": 48.0, "description": "酸辣下饭", "status": 1},
    {"id": 4, "name": "蜜汁叉烧包", "price": 12.0, "description": "甜香松软", "status": 1},
    {"id": 5, "name": "白粥", "price": 6.0, "description": "清淡养胃", "status": 1},
    {"id": 6, "name": "酱香鸭", "price": 38.0, "description": "咸香浓郁", "status": 1},
]


def _mock_db(dishes):
    return patch.object(order_planner, "execute_readonly", return_value={"rows": dishes})


class TestMatchTaste:
    def test_spicy_match(self):
        dish = {"name": "麻辣牛肉面", "description": "香辣口味"}
        assert order_planner.match_taste(dish, ["spicy"]) == 2

    def test_no_spicy_match(self):
        dish = {"name": "白粥", "description": "清淡"}
        assert order_planner.match_taste(dish, ["no_spicy"]) == 1

    def test_light_match(self):
        dish = {"name": "白粥", "description": "清淡养胃"}
        assert order_planner.match_taste(dish, ["light"]) == 2

    def test_sweet_match(self):
        dish = {"name": "蜜汁叉烧包", "description": "甜香"}
        assert order_planner.match_taste(dish, ["sweet"]) == 2

    def test_sour_match(self):
        dish = {"name": "酸菜鱼", "description": "酸辣"}
        assert order_planner.match_taste(dish, ["sour"]) == 2

    def test_salty_match(self):
        dish = {"name": "酱香鸭", "description": "咸香浓郁"}
        assert order_planner.match_taste(dish, ["salty"]) == 1

    def test_no_prefs(self):
        dish = {"name": "麻辣牛肉面", "description": ""}
        assert order_planner.match_taste(dish, []) == 0


class TestPlanNormal:
    """正常推荐：2人/100元/辣 → 返回合理组合。"""

    def test_normal_recommendation(self):
        with _mock_db(FAKE_DISHES):
            result = order_planner.plan.__wrapped__ if hasattr(order_planner.plan, "__wrapped__") else None
            import asyncio
            result = asyncio.run(order_planner.plan(people=2, budget=100, tastes=["spicy"]))
        assert result["items"], "应返回至少一道菜"
        assert result["total"] <= 100, "总价不应超过预算"
        # 辣菜应优先排在前面
        assert "辣" in result["items"][0]["name"] or "麻辣" in result["items"][0]["name"]
        # 推荐理由非空
        assert result["reason"]
        # 每道菜都有必要字段
        for item in result["items"]:
            assert {"id", "name", "price", "quantity", "reason"}.issubset(item.keys())


class TestPlanBudget:
    """预算不足 → 返回部分推荐 + 原因说明。"""

    def test_tight_budget(self):
        with _mock_db(FAKE_DISHES):
            import asyncio
            result = asyncio.run(order_planner.plan(people=2, budget=20, tastes=[]))
        # 预算极低，可能只返回低价主食（单份）或空
        assert result["total"] <= 20 or len(result["items"]) == 0
        assert isinstance(result["reason"], str)

    def test_zero_budget_unlimited(self):
        """budget=0 表示不限预算，应返回完整推荐。"""
        with _mock_db(FAKE_DISHES):
            import asyncio
            result = asyncio.run(order_planner.plan(people=1, budget=0, tastes=[]))
        assert len(result["items"]) > 0


class TestPlanExcludes:
    """忌口过滤 → 不包含忌口食材。"""

    def test_exclude_ingredient(self):
        with _mock_db(FAKE_DISHES):
            import asyncio
            result = asyncio.run(order_planner.plan(people=2, budget=200, excludes=["鱼"]))
        names = [i["name"] for i in result["items"]]
        assert not any("鱼" in n for n in names), f"应过滤含'鱼'菜品，实际: {names}"

    def test_exclude_all(self):
        """忌口过滤后为空。"""
        with _mock_db(FAKE_DISHES):
            import asyncio
            result = asyncio.run(order_planner.plan(people=1, budget=200, excludes=["面", "饭", "鱼", "包", "粥", "鸭"]))
        assert result["items"] == []
        assert "无可用菜品" in result["reason"]


class TestPlanTastePriority:
    """口味匹配 → 辣的偏好优先返回辣菜。"""

    def test_spicy_priority(self):
        with _mock_db(FAKE_DISHES):
            import asyncio
            result = asyncio.run(order_planner.plan(people=1, budget=200, tastes=["spicy"]))
        # 最高口味分的菜应排在最前
        assert result["items"][0]["name"] == "麻辣牛肉面"


class TestPlanStapleQuantity:
    """主食按人数配数量。"""

    def test_staple_quantity_equals_people(self):
        with _mock_db(FAKE_DISHES):
            import asyncio
            result = asyncio.run(order_planner.plan(people=3, budget=500, tastes=[]))
        staples = [i for i in result["items"] if i["quantity"] == 3]
        assert len(staples) > 0, "应存在按人数配量的主食"


class TestPlanDbError:
    """数据库异常 → 友好提示。"""

    def test_db_failure(self):
        with patch.object(order_planner, "execute_readonly", side_effect=Exception("连接超时")):
            import asyncio
            result = asyncio.run(order_planner.plan(people=2, budget=100))
        assert result["items"] == []
        assert "数据库暂不可用" in result["reason"]


class TestRouterIntegration:
    """路由集成测试：通过 TestClient 调用 /api/v1/order/plan。"""

    @pytest.fixture
    def auth_headers(self):
        from app.config import settings
        return {"X-Internal-Token": settings.ai_service_token or "test_token"}

    def test_endpoint_returns_200(self, client, auth_headers):
        with _mock_db(FAKE_DISHES):
            r = client.post("/api/v1/order/plan", json={"people": 2, "budget": 100, "tastes": ["spicy"]}, headers=auth_headers)
        assert r.status_code == 200
        body = r.json()
        assert "items" in body
        assert "total" in body
        assert "reason" in body
