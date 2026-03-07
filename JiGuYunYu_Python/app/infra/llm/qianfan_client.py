"""千帆 LLM 适配器（实现 LLMPort）

当前为 stub，后续接入真实 API 只需改 chat() 内部实现。
"""

import logging
import time

from app.domain.ports.llm import LLMPort, LLMResult, WebSource
from app.exceptions import DependencyError
from app.settings import settings
import httpx

from typing import Any, Dict, Optional

logger = logging.getLogger(__name__)


class QianfanClient(LLMPort):
    def __init__(self) -> None:
        self.api_key = settings.QIANFAN_API_KEY
        self.base_url = settings.QIANFAN_BASE_URL

    async def chat(self, prompt: str, *, timeout: float | None = None) -> LLMResult:
        timeout = timeout or settings.LLM_TIMEOUT
        start = time.perf_counter()
        try:
            # --- stub: 用固定回复模拟，接入真实 API 后替换 ---
            answer = f"[LLM_STUB] {prompt[:120]}"
            elapsed = (time.perf_counter() - start) * 1000
            logger.info("llm.chat cost_ms=%.2f prompt_len=%d", elapsed, len(prompt))
            return LLMResult(text=answer, model="stub", prompt_tokens=len(prompt), completion_tokens=len(answer))
        except Exception as exc:
            logger.exception("llm.chat failed")
            raise DependencyError(f"LLM error: {exc}") from exc

    async def health(self) -> bool:
        return self.api_key is not None


class QianfanHttpClient(LLMPort):
    """真实千帆 HTTP 客户端（非流式）

    不替换现有 `QianfanClient` stub，容器仍可按需切换为本实现。
    """

    DEFAULT_BASE = "https://qianfan.baidubce.com"

    def __init__(self, base_url: Optional[str] = None) -> None:
        self.api_key = settings.QIANFAN_API_KEY
        self.base = (base_url or settings.QIANFAN_BASE_URL or self.DEFAULT_BASE).rstrip("/")

    async def chat(self, prompt: str, *, timeout: float | None = None) -> LLMResult:
        timeout = timeout or settings.LLM_TIMEOUT
        url = f"{self.base}/v2/ai_search/web_summary"
        headers = {
            "X-Appbuilder-Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }

        payload: Dict[str, Any] = {
            "messages": [{"role": "user", "content": prompt}],
            "stream": False,
            # 默认只检索网页模态，top_k 可按需调整
            "resource_type_filter": [{"type": "web", "top_k": 20}],
        }

        start = time.perf_counter()
        try:
            async with httpx.AsyncClient(timeout=timeout) as client:
                resp = await client.post(url, json=payload, headers=headers)

            elapsed = (time.perf_counter() - start) * 1000
            logger.info("qianfan.chat cost_ms=%.2f status=%d", elapsed, resp.status_code)

            if resp.status_code != 200:
                raise DependencyError(f"Qianfan API error status={resp.status_code} body={resp.text}")

            data = resp.json()

            # 解析返回文本（取第一个 choice）
            choices = data.get("choices") or []
            text = ""
            if choices:
                first = choices[0]
                message = first.get("message") or {}
                text = message.get("content", "")

            model = data.get("model") or "qianfan"

            # 解析网页检索来源（search_results / references）
            web_sources: list[WebSource] = []
            search_results = data.get("search_results") or data.get("references") or []
            for ref in search_results:
                if isinstance(ref, dict):
                    web_sources.append(WebSource(
                        title=ref.get("title", ""),
                        url=ref.get("url", ref.get("link", "")),
                        snippet=ref.get("content", ref.get("snippet", "")),
                    ))

            return LLMResult(
                text=text,
                model=model,
                prompt_tokens=len(prompt),
                completion_tokens=len(text),
                web_sources=web_sources,
            )

        except httpx.RequestError as exc:
            logger.exception("qianfan request failed")
            raise DependencyError(f"Qianfan request error: {exc}") from exc
        except Exception as exc:
            logger.exception("qianfan chat failed")
            if isinstance(exc, DependencyError):
                raise
            raise DependencyError(f"Qianfan error: {exc}") from exc
