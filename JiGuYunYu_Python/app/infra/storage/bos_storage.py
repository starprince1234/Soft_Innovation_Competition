"""对象存储实现（百度 BOS，实现 ObjectStoragePort）

使用 httpx + BOS v1 签名算法直接调用 BOS HTTP API，无需额外 SDK 依赖。
支持操作：
  - get_bytes：从 BOS 下载文件
  - upload：上传字节数据到 BOS
  - presign：生成预签名 URL（有效期默认 1 小时）
  - health：HEAD bucket 探测

签名算法参考：https://cloud.baidu.com/doc/BOS/s/Ojwvyrpgd
"""

from __future__ import annotations

import hashlib
import hmac
import logging
import uuid
from datetime import datetime, timezone
from typing import Optional
from urllib.parse import quote

import httpx

from app.domain.ports.object_storage import ObjectStoragePort
from app.exceptions import DependencyError
from app.settings import settings

logger = logging.getLogger(__name__)


class BosStorage(ObjectStoragePort):
    """百度 BOS 对象存储（HTTP API + BCE 签名）"""

    def __init__(
        self,
        endpoint: Optional[str] = None,
        access_key: Optional[str] = None,
        secret_key: Optional[str] = None,
        bucket: Optional[str] = None,
    ) -> None:
        self.endpoint = (endpoint or settings.BOS_ENDPOINT or "").rstrip("/")
        self._ak = access_key or settings.BOS_ACCESS_KEY or ""
        self._sk = secret_key or settings.BOS_SECRET_KEY or ""
        self._bucket = bucket or getattr(settings, "BOS_BUCKET", "") or ""

    def is_configured(self) -> bool:
        return bool(self.endpoint and self._ak and self._sk and self._bucket)

    # ---- ObjectStoragePort 实现 ----

    async def get_bytes(self, url: str) -> bytes:
        """从 BOS 下载文件字节"""
        if not self.is_configured():
            logger.debug("bos.get_bytes skipped: not configured  url=%s", url)
            return b""

        try:
            object_key = self._extract_key(url)
            bos_url = f"{self.endpoint}/{self._bucket}/{quote(object_key, safe='/')}"
            headers = self._sign("GET", f"/{self._bucket}/{quote(object_key, safe='/')}")

            async with httpx.AsyncClient(timeout=30.0) as client:
                resp = await client.get(bos_url, headers=headers)

            if resp.status_code == 404:
                logger.warning("bos.get_bytes  key=%s  not found", object_key)
                return b""

            resp.raise_for_status()
            logger.info("bos.get_bytes  key=%s  size=%d", object_key, len(resp.content))
            return resp.content

        except httpx.RequestError as exc:
            logger.error("bos.get_bytes request failed: %s", exc)
            raise DependencyError(f"BOS download error: {exc}") from exc
        except DependencyError:
            raise
        except Exception as exc:
            logger.exception("bos.get_bytes failed")
            raise DependencyError(f"Storage error: {exc}") from exc

    async def upload(self, key: str, data: bytes) -> str:
        """上传字节数据到 BOS，返回访问 URL"""
        if not self.is_configured():
            # 未配置时返回 mock URL
            mock_url = f"https://bos-mock.jigu.cloud/{self._bucket}/{key}"
            logger.warning("bos.upload skipped (not configured), mock URL: %s", mock_url)
            return mock_url

        try:
            canonical_path = f"/{self._bucket}/{quote(key, safe='/')}"
            bos_url = f"{self.endpoint}{canonical_path}"
            headers = self._sign(
                "PUT",
                canonical_path,
                content_type="application/octet-stream",
            )
            headers["Content-Type"] = "application/octet-stream"

            async with httpx.AsyncClient(timeout=60.0) as client:
                resp = await client.put(bos_url, content=data, headers=headers)

            resp.raise_for_status()
            result_url = f"{self.endpoint}/{self._bucket}/{key}"
            logger.info("bos.upload  key=%s  size=%d  url=%s", key, len(data), result_url)
            return result_url

        except httpx.RequestError as exc:
            logger.error("bos.upload request failed: %s", exc)
            raise DependencyError(f"BOS upload error: {exc}") from exc
        except Exception as exc:
            logger.exception("bos.upload failed")
            raise DependencyError(f"Storage upload error: {exc}") from exc

    async def presign(self, key: str, *, expires: int = 3600) -> str:
        """生成预签名 URL（有效期默认 1 小时）"""
        if not self.is_configured():
            return f"https://bos-mock.jigu.cloud/{self._bucket}/{key}"

        timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
        canonical_path = f"/{self._bucket}/{quote(key, safe='/')}"
        auth_prefix = f"bce-auth-v1/{self._ak}/{timestamp}/{expires}"
        signing_key = hmac.new(
            self._sk.encode(), auth_prefix.encode(), hashlib.sha256
        ).hexdigest()

        canonical_request = f"GET\n{canonical_path}\n\nhost:{self._host()}"
        signature = hmac.new(
            signing_key.encode(), canonical_request.encode(), hashlib.sha256
        ).hexdigest()

        authorization = f"{auth_prefix}/host/{signature}"
        return f"{self.endpoint}{canonical_path}?authorization={quote(authorization)}"

    async def health(self) -> bool:
        """HEAD bucket 探测"""
        if not self.is_configured():
            return False
        try:
            canonical_path = f"/{self._bucket}"
            bos_url = f"{self.endpoint}{canonical_path}"
            headers = self._sign("HEAD", canonical_path)

            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.head(bos_url, headers=headers)
            return resp.status_code in (200, 403)  # 403 = 有 bucket 但无 list 权限
        except Exception as exc:
            logger.warning("bos.health failed: %s", exc)
            return False

    # ---- BOS v1 签名 ----

    def _host(self) -> str:
        """提取 endpoint 中的 host"""
        host = self.endpoint.replace("https://", "").replace("http://", "")
        return host.split("/")[0]

    def _sign(
        self,
        method: str,
        canonical_path: str,
        *,
        content_type: str = "",
    ) -> dict:
        """生成 BOS v1 Authorization 签名头"""
        timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
        expiration = 1800  # 30 分钟有效

        auth_prefix = f"bce-auth-v1/{self._ak}/{timestamp}/{expiration}"
        signing_key = hmac.new(
            self._sk.encode(), auth_prefix.encode(), hashlib.sha256
        ).hexdigest()

        # 构建 canonical headers（只签 host）
        host = self._host()
        signed_headers = "host"
        canonical_headers = f"host:{host}"

        if content_type:
            signed_headers = "content-type;host"
            canonical_headers = f"content-type:{content_type}\nhost:{host}"

        canonical_request = f"{method}\n{canonical_path}\n\n{canonical_headers}"
        signature = hmac.new(
            signing_key.encode(), canonical_request.encode(), hashlib.sha256
        ).hexdigest()

        authorization = f"{auth_prefix}/{signed_headers}/{signature}"
        return {
            "Authorization": authorization,
            "Host": host,
            "x-bce-date": timestamp,
        }

    @staticmethod
    def _extract_key(url: str) -> str:
        """从 BOS URL 或 bos:// scheme 中提取 object key"""
        if url.startswith("bos://"):
            return url[6:]  # bos://path/to/file → path/to/file
        # https://endpoint/bucket/path/to/file → path/to/file
        parts = url.split("/")
        if len(parts) > 4:
            return "/".join(parts[4:])
        return url

    @staticmethod
    def generate_key(directory: str, extension: str = ".jpg") -> str:
        """生成唯一 object key"""
        return f"{directory.rstrip('/')}/{uuid.uuid4().hex}{extension}"

