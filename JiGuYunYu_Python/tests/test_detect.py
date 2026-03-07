"""/internal/detect/process 契约测试"""

from tests.conftest import AUTH_HEADERS


def test_detect_no_token(client):
    """缺 token 应返回 401"""
    r = client.post(
        "/internal/detect/process",
        json={"taskId": "t1", "imageUrl": "http://example.com/img.jpg"},
    )
    assert r.status_code == 401
    assert r.json()["code"] == 401


def test_detect_bad_token(client):
    """错误 token 应返回 401"""
    r = client.post(
        "/internal/detect/process",
        json={"taskId": "t1", "imageUrl": "http://example.com/img.jpg"},
        headers={"X-Internal-Token": "wrong"},
    )
    assert r.status_code == 401


def test_detect_ok(client):
    """正常调用返回 code=0 + detections"""
    r = client.post(
        "/internal/detect/process",
        json={"taskId": "t1", "imageUrl": "http://example.com/img.jpg"},
        headers=AUTH_HEADERS,
    )
    assert r.status_code == 200
    body = r.json()
    assert body["code"] == 200
    assert body["message"] == "Success"
    assert isinstance(body["data"]["detections"], list)
    # stub 应至少有结果
    assert len(body["data"]["detections"]) > 0
    item = body["data"]["detections"][0]
    assert "label" in item
    assert "confidence" in item
