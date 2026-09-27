"""Step 1 骨架测试：能启动、health 通、路由注册正确。"""

import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.config import settings


@pytest.fixture
def client():
    return TestClient(app)


class TestHealth:
    def test_health_ok(self, client):
        r = client.get("/api/v1/health")
        assert r.status_code == 200
        assert r.json()["status"] == "ok"

    def test_ready(self, client):
        r = client.get("/api/v1/ready")
        assert r.status_code == 200
        assert "llm_configured" in r.json()


class TestSecurity:
    def test_protected_without_token_dev_mode(self, client):
        """未配置 token 时开发模式放行；chat 缺 userId 返回 400。"""
        r = client.post("/api/v1/chat", json={"message": "hi"})
        assert r.status_code == 400

    def test_analysis_implemented(self, client):
        """DataAnalysisAgent 已实现：空 question 返回 400。"""
        r = client.post("/api/v1/analysis/ask", json={"question": ""})
        assert r.status_code == 400

    def test_order_plan_implemented(self, client):
        """Step 4 已实现：endpoint 应返回 200（无 DB 时返回友好提示）。"""
        r = client.post("/api/v1/order/plan", json={"people": 2, "budget": 100})
        assert r.status_code == 200
        body = r.json()
        assert "items" in body
        assert "total" in body
        assert "reason" in body


class TestSqlSecurity:
    def test_reject_insert(self):
        from app import db
        with pytest.raises(db.QueryRejected):
            db.validate_sql("INSERT INTO orders(id) VALUES (1)")

    def test_reject_drop(self):
        from app import db
        with pytest.raises(db.QueryRejected):
            db.validate_sql("DROP TABLE orders")

    def test_reject_forbidden_table(self):
        from app import db
        with pytest.raises(db.QueryRejected):
            db.validate_sql("SELECT * FROM user")

    def test_reject_forbidden_column(self):
        from app import db
        with pytest.raises(db.QueryRejected):
            db.validate_sql("SELECT password FROM orders")

    def test_reject_comment(self):
        from app import db
        with pytest.raises(db.QueryRejected):
            db.validate_sql("SELECT id FROM orders -- 注入")

    def test_reject_union(self):
        from app import db
        with pytest.raises(db.QueryRejected):
            db.validate_sql("SELECT id FROM orders UNION SELECT password FROM employee")

    def test_select_ok(self):
        from app import db
        cleaned = db.validate_sql("SELECT id, amount FROM orders")
        assert "orders" in cleaned

    def test_ensure_limit(self):
        from app import db
        assert "LIMIT" in db.ensure_limit("SELECT id FROM orders")
        out = db.ensure_limit("SELECT id FROM orders LIMIT 10")
        assert out.endswith("LIMIT 10")
