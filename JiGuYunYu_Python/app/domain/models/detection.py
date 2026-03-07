"""检测领域模型（纯数据，不依赖第三方框架）"""

from dataclasses import dataclass, field
from typing import List


@dataclass
class Detection:
    """单条检测框"""
    label: str
    confidence: float
    bbox: List[float] = field(default_factory=list)  # [x1, y1, x2, y2]


@dataclass
class DetectionResult:
    """一次检测的完整结果"""
    detections: List[Detection] = field(default_factory=list)
    raw: dict = field(default_factory=dict)  # 可选：保留原始推理输出
