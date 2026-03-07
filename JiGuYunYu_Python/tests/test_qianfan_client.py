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

    async def fake_post(self, url, json=None, headers=None):
        return httpx.Response(200, content=jsonlib.dumps(sample_resp).encode("utf-8"))

    # small helper to avoid name clash
    jsonlib = json

    monkeypatch.setattr(httpx.AsyncClient, "post", fake_post)

    client = QianfanHttpClient(base_url="https://qianfan.baidubce.com")
    res = await client.chat("今天天气如何")

    assert "回复内容" in res.text
    assert res.model == "qianfan"


@pytest.mark.asyncio
async def test_qianfan_chat_error(monkeypatch):
    async def fake_post_err(self, url, json=None, headers=None):
        return httpx.Response(500, content=b"server error")

    monkeypatch.setattr(httpx.AsyncClient, "post", fake_post_err)

    client = QianfanHttpClient(base_url="https://qianfan.baidubce.com")
    with pytest.raises(Exception):
        await client.chat("触发错误")
