"""/health 与 /internal/health 契约测试"""

from tests.conftest import AUTH_HEADERS


def test_health_ok(client):
    """公开健康检查不需要 token"""
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"


def test_internal_health_no_token(client):
    """内部健康检查缺 token 应返回 401"""
    r = client.get("/internal/health")
    assert r.status_code == 401
    body = r.json()
    assert body["code"] == 401


def test_internal_health_with_token(client):
    """内部健康检查正常返回 code=0"""
    r = client.get("/internal/health", headers=AUTH_HEADERS)
    assert r.status_code == 200
    body = r.json()
    assert body["code"] == 200
    assert "data" in body
