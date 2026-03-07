"""检测器实现（YOLO，实现 DetectorPort）

当前为 stub，后续接入 ultralytics / onnxruntime 只需改 detect() 内部实现。
模型加载应在 startup 或懒加载单例，避免每次请求加载。
"""

import logging

from app.domain.models.detection import Detection, DetectionResult
from app.domain.ports.detector import DetectorPort
from app.exceptions import DependencyError

logger = logging.getLogger(__name__)


class YoloDetector(DetectorPort):
    def __init__(self) -> None:
        self._model_loaded = False  # 后续接真实模型后置为 True

    async def detect(self, image_bytes: bytes) -> DetectionResult:
        try:
            # --- stub: 返回固定检测结果 ---
            logger.debug("yolo.detect image_size=%d", len(image_bytes))
            return DetectionResult(
                detections=[
                    Detection(label="person", confidence=0.98, bbox=[10, 20, 200, 400]),
                    Detection(label="bicycle", confidence=0.70, bbox=[150, 100, 350, 300]),
                ],
            )
        except Exception as exc:
            logger.exception("yolo.detect failed")
            raise DependencyError(f"Detector error: {exc}") from exc

    async def health(self) -> bool:
        return True  # stub 始终可用
