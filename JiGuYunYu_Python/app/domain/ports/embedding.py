"""文本嵌入抽象端口"""

from abc import ABC, abstractmethod
from typing import List


class EmbeddingPort(ABC):
    """将文本转换为向量表示"""

    @abstractmethod
    async def embed(self, text: str) -> List[float]:
        """单条文本 → 向量"""
        ...

    @abstractmethod
    async def embed_batch(self, texts: List[str]) -> List[List[float]]:
        """批量文本 → 向量列表"""
        ...

    @property
    @abstractmethod
    def dim(self) -> int:
        """向量维度"""
        ...
