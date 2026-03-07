"""检测流程编排

流程：校验输入 → 获取图片 bytes（优先 base64，其次 BOS，最后 HTTP 下载） → 调用 DetectorPort → 返回 DetectionResult
"""

import base64
import logging
from typing import Optional

import httpx

from app.api.schemas.detect import DetectRequest
from app.domain.models.detection import DetectionResult
from app.domain.ports.detector import DetectorPort
from app.domain.ports.object_storage import ObjectStoragePort
from app.exceptions import BizError

logger = logging.getLogger(__name__)


class DetectUseCase:
    def __init__(
        self,
        detector: DetectorPort,
        storage: Optional[ObjectStoragePort] = None,
    ) -> None:
        self._detector = detector
        self._storage = storage

    async def execute(self, req: DetectRequest) -> DetectionResult:
        """1. 校验  2. 获取图片 bytes  3. 检测  4. 返回"""
        if not req.image_url and not req.image_base64:
            raise BizError("imageUrl or imageBase64 is required")

        image_bytes = await self._resolve_image_bytes(req)

        logger.info(
            "detect  url=%s image_size=%d task_id=%s",
            req.image_url,
            len(image_bytes),
            req.task_id,
        )

        result = await self._detector.detect(image_bytes)
        return result

    async def _resolve_image_bytes(self, req: DetectRequest) -> bytes:
        """
        按优先级获取图片字节数据：
        1. image_base64 — Java 直传的 Base64 编码（BOS 不可达时的后备）
        2. BOS storage — 通过对象存储下载
        3. HTTP 下载 — 直接从 URL 下载（兜底方案）
        """
        # 优先使用 base64 数据（最可靠，不依赖额外网络请求）
        if req.image_base64:
            try:
                raw = req.image_base64
                # 兼容 "data:image/xxx;base64,..." 格式
                if "," in raw:
                    raw = raw.split(",", 1)[1]
                return base64.b64decode(raw)
            except Exception as exc:
                logger.warning("Failed to decode image_base64: %s", exc)

        # 其次通过 BOS 下载
        if self._storage and req.image_url:
            try:
                data = await self._storage.get_bytes(req.image_url)
                if data and len(data) > 0:
                    return data
            except Exception as exc:
                logger.warning("BOS download failed: %s, falling back to HTTP", exc)

        # 最后直接 HTTP 下载
        if req.image_url and req.image_url.startswith("http"):
            try:
                async with httpx.AsyncClient(timeout=30.0) as client:
                    resp = await client.get(req.image_url)
                    resp.raise_for_status()
                    return resp.content
            except Exception as exc:
                logger.warning("HTTP download failed: %s", exc)

        raise BizError("Cannot obtain image data from any source")
