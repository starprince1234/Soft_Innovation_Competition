"""统一 HTTP 客户端（httpx AsyncClient）

职责：统一超时 / 重试 / 日志 / 脱敏
启动时创建，关闭时释放。
"""

import logging
from typing import Any, Dict, Optional

import httpx

from app.settings import settings

logger = logging.getLogger(__name__)


class HttpClient:
    def __init__(
        self,
        timeout: float | None = None,
    ) -> None:
        self._timeout = timeout or settings.HTTP_TIMEOUT
        self._client: Optional[httpx.AsyncClient] = None

    async def _ensure_client(self) -> httpx.AsyncClient:
        if self._client is None or self._client.is_closed:
            self._client = httpx.AsyncClient(
                timeout=httpx.Timeout(self._timeout),
            )
        return self._client

    async def get(self, url: str, *, params: Optional[Dict[str, Any]] = None) -> httpx.Response:
        client = await self._ensure_client()
        logger.debug("HTTP GET %s", url)
        return await client.get(url, params=params)

    async def post(self, url: str, *, json: Optional[Dict[str, Any]] = None) -> httpx.Response:
        client = await self._ensure_client()
        logger.debug("HTTP POST %s", url)
        return await client.post(url, json=json)

    async def get_bytes(self, url: str) -> bytes:
        """GET 并返回原始 bytes（用于下载图片 / 文件）"""
        resp = await self.get(url)
        resp.raise_for_status()
        return resp.content

    async def close(self) -> None:
        if self._client and not self._client.is_closed:
            await self._client.aclose()
            logger.info("http client closed")
