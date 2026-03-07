from pydantic import BaseModel, ConfigDict, Field
from typing import List, Optional


class DetectRequest(BaseModel):
    """检测请求（与 Java DTO 对齐）"""
    model_config = ConfigDict(populate_by_name=True)

    image_url: str = Field(alias="imageUrl")         # 图片 URL（BOS / 公网）
    task_id: Optional[str] = Field(default=None, alias="taskId")  # 任务 ID
    image_base64: Optional[str] = Field(default=None, alias="imageBase64")  # Base64 图片数据（BOS 不可达时的后备）


class DetectionItem(BaseModel):
    """单条检测结果"""
    label: str
    confidence: float
    bbox: List[float] = []            # [x1, y1, x2, y2]


class DetectData(BaseModel):
    """检测响应 data 体"""
    task_id: Optional[str] = None
    image_url: Optional[str] = None
    detections: List[DetectionItem] = []
    raw_result: Optional[str] = None  # 模型原始返回文本（调试用）
