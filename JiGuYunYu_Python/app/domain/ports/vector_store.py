"""向量检索抽象端口"""

from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional

from app.domain.models.artifact import Chunk


class VectorStorePort(ABC):
    @abstractmethod
    async def search(
        self,
        query_embedding: List[float],
        *,
        top_k: int = 5,
        filters: Optional[Dict[str, Any]] = None,
    ) -> List[Chunk]:
        """按向量相似度检索 topK 文档片段"""
        ...

    async def add_documents(
        self,
        chunks: List[Chunk],
        embeddings: List[List[float]],
    ) -> int:
        """增量添加文档，返回添加数量"""
        raise NotImplementedError("add_documents not supported")

    async def rebuild(
        self,
        chunks: List[Chunk],
        embeddings: List[List[float]],
    ) -> int:
        """清空并重建整个索引，返回文档数量"""
        raise NotImplementedError("rebuild not supported")

    async def delete_documents(self, ids: List[str]) -> int:
        """按 ID 列表删除文档，返回删除数量"""
        raise NotImplementedError("delete_documents not supported")

    async def count(self) -> int:
        """索引中的文档数量"""
        return 0

    async def health(self) -> bool:
        """向量库是否可达（默认 True）"""
        return True
