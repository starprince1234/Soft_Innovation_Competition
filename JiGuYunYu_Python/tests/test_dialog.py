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


def test_dialog_name_question_goes_through_llm(client):
    """名称类问题不走关键字直答，应走 LLM 路径。"""
    payload = {
        "dialogTaskId": "d-name-1",
        "query": "这个文物叫什么名字？",
        "contextHistory": [
            {
                "role": "user",
                "content": "[识别上下文]\n识别结果文物名称：青铜鼎\n年代：商代\n类别：青铜\n标签：礼器、祭祀\n简介：商代重器。",
            }
        ],
    }
    r = client.post(
        "/internal/dialog/responses",
        json=payload,
        headers=AUTH_HEADERS,
    )
    assert r.status_code == 200
    body = r.json()
    assert body["code"] == 200
    assert body["data"]["model"] != "context_grounded"
    assert body["data"]["answer"]


def test_dialog_era_question_goes_through_llm(client):
    """朝代问题不走关键字直答，应走 LLM 路径。"""
    payload = {
        "dialogTaskId": "d-era-1",
        "query": "这个是哪个朝代的？",
        "contextHistory": [
            {
                "role": "system",
                "content": "[识别上下文]\nartifact name: Bronze Ding\nera: Shang\ncategory: Bronze\nartifact description: ritual vessel",
            }
        ],
    }
    r = client.post(
        "/internal/dialog/responses",
        json=payload,
        headers=AUTH_HEADERS,
    )
    assert r.status_code == 200
    body = r.json()
    assert body["code"] == 200
    assert body["data"]["model"] != "context_grounded"
    assert body["data"]["answer"]


def test_dialog_description_question_goes_through_llm(client):
    """介绍问题不走关键字直答，应走 LLM 路径。"""
    payload = {
        "dialogTaskId": "d-desc-1",
        "query": "介绍一下这个文物",
        "contextHistory": [
            {
                "role": "SYSTEM",
                "content": "[识别上下文]\n识别结果文物名称：青铜神树\n简介：古蜀文明代表性青铜祭祀器。",
            }
        ],
    }
    r = client.post(
        "/internal/dialog/responses",
        json=payload,
        headers=AUTH_HEADERS,
    )
    assert r.status_code == 200
    body = r.json()
    assert body["code"] == 200
    assert body["data"]["model"] != "context_grounded"
    assert body["data"]["answer"]
