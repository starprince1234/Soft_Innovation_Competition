import asyncio
import json

import pytest

import httpx

from app.infra.llm.qianfan_client import QianfanHttpClient


@pytest.mark.asyncio
async def test_qianfan_chat_success(monkeypatch):
    sample_resp = {
        "request_id": "req-123",
        "choices": [
            {
                "index": 0,
                "finish_reason": "stop",
                "message": {"role": "assistant", "content": "这是回复内容"},
            }
        ],
    }

    captured = {}

    async def fake_post(self, url, json=None, headers=None):
        captured["url"] = url
        captured["json"] = json
        captured["headers"] = headers
        return httpx.Response(200, content=jsonlib.dumps(sample_resp).encode("utf-8"))

    # small helper to avoid name clash
    jsonlib = json

    monkeypatch.setattr(httpx.AsyncClient, "post", fake_post)

    client = QianfanHttpClient(base_url="https://qianfan.baidubce.com")
    prompt = "### 识别上下文\n青铜神树\n\n### 对话历史\nuser: 你好\nassistant: 你好\n\n### 用户提问\n这个是什么"
    res = await client.chat(prompt)

    assert "回复内容" in res.text
    assert res.model == "qianfan"
    assert captured["url"].endswith("/v2/ai_search/web_summary")
    assert captured["json"]["instruction"]
    assert captured["json"]["messages"][-1]["role"] == "user"
    assert captured["json"]["messages"][-1]["content"] == "这个是什么"
    assert "X-Appbuilder-Authorization" in captured["headers"]


@pytest.mark.asyncio
async def test_qianfan_chat_error(monkeypatch):
    async def fake_post_err(self, url, json=None, headers=None):
        return httpx.Response(500, content=b"server error")

    monkeypatch.setattr(httpx.AsyncClient, "post", fake_post_err)

    client = QianfanHttpClient(base_url="https://qianfan.baidubce.com")
    with pytest.raises(Exception):
        await client.chat("触发错误")
