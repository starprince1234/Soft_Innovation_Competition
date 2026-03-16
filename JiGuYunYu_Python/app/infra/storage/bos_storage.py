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
from base64 import b64encode
from datetime import datetime, timezone
from email.utils import formatdate
from typing import Optional
from urllib.parse import quote, urlparse

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
        raw_endpoint = (endpoint or settings.BOS_ENDPOINT or "").rstrip("/")
        # Keep compatibility with existing .env values that omit scheme.
        if raw_endpoint and not raw_endpoint.startswith(("http://", "https://")):
            raw_endpoint = f"https://{raw_endpoint}"
        self.endpoint = raw_endpoint
        self._ak = access_key or settings.BOS_ACCESS_KEY or ""
        self._sk = secret_key or settings.BOS_SECRET_KEY or ""
        self._bucket = bucket or getattr(settings, "BOS_BUCKET", "") or ""

    def is_configured(self) -> bool:
        return bool(self.endpoint and self._ak and self._sk and self._bucket)

    def _is_aliyun_endpoint(self) -> bool:
        host = self._host().lower()
        return "aliyuncs.com" in host

    def _object_url(self, object_key: str) -> str:
        return self._object_url_with_endpoint(object_key, self.endpoint)

    def _object_url_with_endpoint(self, object_key: str, endpoint: str) -> str:
        endpoint = endpoint.rstrip("/")
        host = endpoint.replace("https://", "").replace("http://", "").split("/")[0]
        encoded_key = quote(object_key, safe="/")

        # Aliyun OSS typically uses virtual-host-style URL:
        # https://<bucket>.<endpoint>/<key>
        if self._is_aliyun_endpoint() and not host.startswith(f"{self._bucket}."):
            parsed = urlparse(endpoint)
            return f"{parsed.scheme}://{self._bucket}.{host}/{encoded_key}"

        # BOS/path-style fallback.
        return f"{endpoint}/{self._bucket}/{encoded_key}"

    # ---- ObjectStoragePort 实现 ----

    async def get_bytes(self, url: str) -> bytes:
        """从 BOS 下载文件字节"""
        if not self.is_configured():
            logger.debug("bos.get_bytes skipped: not configured  url=%s", url)
            return b""

        try:
            object_key = self._extract_key(url)
            canonical_key = quote(object_key, safe="/")
            canonical_path = f"/{self._bucket}/{canonical_key}"
            candidates: list[str] = []

            # For Aliyun, prefer caller-provided URL first (usually public endpoint from Java side).
            if self._is_aliyun_endpoint() and url.startswith(("http://", "https://")):
                candidates.append(url)

            candidates.append(self._object_url(object_key))

            # Local machines may not reach VPC internal endpoint.
            if self._is_aliyun_endpoint() and "-internal." in self.endpoint:
                public_endpoint = self.endpoint.replace("-internal.", ".")
                public_url = self._object_url_with_endpoint(object_key, public_endpoint)
                if public_url not in candidates:
                    candidates.append(public_url)

            last_resp: Optional[httpx.Response] = None
            async with httpx.AsyncClient(timeout=30.0) as client:
                for bos_url in candidates:
                    host_override = urlparse(bos_url).netloc
                    headers = self._sign("GET", canonical_path, host_override=host_override)
                    resp = await client.get(bos_url, headers=headers)
                    last_resp = resp

                    if resp.status_code == 404:
                        continue
                    if resp.status_code >= 500:
                        continue

                    resp.raise_for_status()
                    logger.info(
                        "bos.get_bytes  key=%s  size=%d  via=%s",
                        object_key,
                        len(resp.content),
                        bos_url,
                    )
                    return resp.content

            if last_resp is not None and last_resp.status_code == 404:
                logger.warning("bos.get_bytes  key=%s  not found", object_key)
                return b""
            if last_resp is not None:
                last_resp.raise_for_status()
            return b""

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
            bos_url = self._object_url(key)
            headers = self._sign(
                "PUT",
                canonical_path,
                content_type="application/octet-stream",
                content=data,
            )
            headers["Content-Type"] = "application/octet-stream"

            async with httpx.AsyncClient(timeout=60.0) as client:
                resp = await client.put(bos_url, content=data, headers=headers)

            resp.raise_for_status()
            result_url = self._object_url(key)
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
        content: Optional[bytes] = None,
        host_override: Optional[str] = None,
    ) -> dict:
        if self._is_aliyun_endpoint():
            return self._sign_aliyun(
                method,
                canonical_path,
                content_type=content_type,
                content=content,
                host_override=host_override,
            )

        return self._sign_bos(
            method,
            canonical_path,
            content_type=content_type,
            host_override=host_override,
        )

    def _sign_bos(
        self,
        method: str,
        canonical_path: str,
        *,
        content_type: str = "",
        host_override: Optional[str] = None,
    ) -> dict:
        """生成 BOS v1 Authorization 签名头"""
        timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
        expiration = 1800  # 30 分钟有效

        auth_prefix = f"bce-auth-v1/{self._ak}/{timestamp}/{expiration}"
        signing_key = hmac.new(
            self._sk.encode(), auth_prefix.encode(), hashlib.sha256
        ).hexdigest()

        # 构建 canonical headers（只签 host）
        host = host_override or self._host()
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

    def _sign_aliyun(
        self,
        method: str,
        canonical_path: str,
        *,
        content_type: str = "",
        content: Optional[bytes] = None,
        host_override: Optional[str] = None,
    ) -> dict:
        """生成阿里云 OSS V1 Authorization 签名头。"""
        date_value = formatdate(usegmt=True)
        content_md5 = ""
        if content:
            content_md5 = b64encode(hashlib.md5(content).digest()).decode("ascii")

        string_to_sign = (
            f"{method}\n"
            f"{content_md5}\n"
            f"{content_type}\n"
            f"{date_value}\n"
            f"{canonical_path}"
        )

        signature = b64encode(
            hmac.new(self._sk.encode("utf-8"), string_to_sign.encode("utf-8"), hashlib.sha1).digest()
        ).decode("ascii")

        host = host_override or self._host()
        headers = {
            "Authorization": f"OSS {self._ak}:{signature}",
            "Date": date_value,
            "Host": host if host.startswith(f"{self._bucket}.") else f"{self._bucket}.{host}",
        }
        if content_md5:
            headers["Content-MD5"] = content_md5
        return headers

    @staticmethod
    def _extract_key(url: str) -> str:
        """从 BOS URL 或 bos:// scheme 中提取 object key"""
        if url.startswith("bos://"):
            return url[6:]  # bos://path/to/file → path/to/file

        parsed = urlparse(url)
        if not parsed.scheme or not parsed.netloc:
            return url

        path = parsed.path.lstrip("/")
        if not path:
            return ""

        # path-style: /bucket/key
        if path.startswith(f"{settings.BOS_BUCKET}/"):
            return path.split("/", 1)[1]

        # virtual-host-style: bucket.endpoint/key
        return path

    @staticmethod
    def generate_key(directory: str, extension: str = ".jpg") -> str:
        """生成唯一 object key"""
        return f"{directory.rstrip('/')}/{uuid.uuid4().hex}{extension}"

