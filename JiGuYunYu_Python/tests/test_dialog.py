"""/internal/dialog/responses 契约测试"""

from tests.conftest import AUTH_HEADERS


def test_dialog_no_token(client):
    """缺 token 应返回 401"""
    r = client.post("/internal/dialog/responses", json={"query": "hello"})
    assert r.status_code == 401
    assert r.json()["code"] == 401


def test_dialog_ok(client):
    """正常对话返回 code=0 + answer + sources"""
    r = client.post(
        "/internal/dialog/responses",
        json={"dialogTaskId": "d1", "query": "hello", "top_k": 2},
        headers=AUTH_HEADERS,
    )
    assert r.status_code == 200
    body = r.json()
    assert body["code"] == 200
    assert body["message"] == "Success"
    data = body["data"]
    assert "answer" in data
    assert isinstance(data["sources"], list)
    assert "model" in data
    assert "cost_ms" in data


def test_dialog_missing_query(client):
    """缺少必填参数 query 应返回 422"""
    r = client.post(
        "/internal/dialog/responses",
        json={},
        headers=AUTH_HEADERS,
    )
    assert r.status_code == 422
