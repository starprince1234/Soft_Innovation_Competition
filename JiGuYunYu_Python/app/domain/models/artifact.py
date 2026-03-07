"""文档 / Embedding 领域模型（纯数据）"""

from dataclasses import dataclass, field
from typing import Dict, List


@dataclass
class Chunk:
    """一段文档切片（用于向量检索）"""
    chunk_id: str = ""
    text: str = ""
    title: str = ""
    url: str = ""
    score: float = 0.0
    metadata: Dict[str, str] = field(default_factory=dict)


@dataclass
class Embedding:
    """向量 + 关联 ID"""
    id: str = ""
    vector: List[float] = field(default_factory=list)
