"""检测器抽象端口"""

from abc import ABC, abstractmethod

from app.domain.models.detection import DetectionResult


class DetectorPort(ABC):
    @abstractmethod
    async def detect(self, image_bytes: bytes) -> DetectionResult:
        """对图片 bytes 执行检测，返回领域结果"""
        ...

    async def health(self) -> bool:
        """检测器是否可用（默认 True，子类可覆写）"""
        return True
