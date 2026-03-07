"""检索策略模块（从 usecase 中剥离，便于迭代）

支持：阈值过滤、去重、分组、可选 rerank。
"""

import logging
from typing import Any, Dict, List, Optional

from app.domain.models.artifact import Chunk
from app.domain.ports.vector_store import VectorStorePort

logger = logging.getLogger(__name__)


class Retriever:
    def __init__(
        self,
        vector_store: VectorStorePort,
        *,
        score_threshold: float = 0.0,
    ) -> None:
        self._vs = vector_store
        self._threshold = score_threshold

    async def retrieve(
        self,
        query_embedding: List[float],
        *,
        top_k: int = 5,
        filters: Optional[Dict[str, Any]] = None,
    ) -> List[Chunk]:
        """检索 topK 片段，并做阈值过滤 + 去重"""
        raw = await self._vs.search(query_embedding, top_k=top_k, filters=filters)

        # 阈值过滤
        filtered = [c for c in raw if c.score >= self._threshold]

        # chunk_id 去重
        seen: set[str] = set()
        unique: List[Chunk] = []
        for c in filtered:
            if c.chunk_id not in seen:
                seen.add(c.chunk_id)
                unique.append(c)

        logger.debug(
            "retriever  raw=%d filtered=%d unique=%d",
            len(raw), len(filtered), len(unique),
        )
        return unique
