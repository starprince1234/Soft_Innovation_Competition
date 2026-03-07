"""本地 FAISS 向量检索实现（实现 VectorStorePort）

使用 faiss-cpu 进行向量相似度搜索。
索引类型：IndexFlatIP（内积），配合 L2 归一化向量等价于余弦相似度。
元数据（Chunk 对象）内存维护在 _chunks 列表中，与 FAISS 索引行位置一一对应。

线程安全：search 并发安全；add_documents / rebuild 通过 asyncio.Lock 串行化。
"""

from __future__ import annotations

import asyncio
import json
import logging
from pathlib import Path
from typing import Any, Dict, List, Optional

import numpy as np

from app.domain.models.artifact import Chunk
from app.domain.ports.vector_store import VectorStorePort
from app.exceptions import DependencyError

logger = logging.getLogger(__name__)

# 种子数据文件
_SEEDS_PATH = Path(__file__).resolve().parent.parent.parent / "data" / "knowledge_seeds.json"


class FaissStore(VectorStorePort):
    """真实 FAISS 向量检索（faiss-cpu）"""

    def __init__(self, dim: int = 256) -> None:
        self._dim = dim
        self._chunks: List[Chunk] = []
        self._lock = asyncio.Lock()
        self._index = None  # 延迟初始化，startup 时调用 init()
        self._ready = False

    # ---- lifecycle ----

    async def init(self, embedder=None) -> None:
        """启动时初始化 FAISS 索引并加载种子数据"""
        import faiss  # noqa: E402  延迟导入避免启动时 faiss 未安装

        self._index = faiss.IndexFlatIP(self._dim)
        self._ready = True
        logger.info("faiss.init  dim=%d", self._dim)

        # 如果有 embedder 且种子文件存在，自动加载
        if embedder and _SEEDS_PATH.exists():
            await self._load_seeds(embedder)

    # ---- VectorStorePort 实现 ----

    async def search(
        self,
        query_embedding: List[float],
        *,
        top_k: int = 5,
        filters: Optional[Dict[str, Any]] = None,
    ) -> List[Chunk]:
        if not self._ready or self._index is None:
            logger.warning("faiss.search called before init, returning empty")
            return []

        try:
            n = self._index.ntotal
            if n == 0:
                return []

            k = min(top_k, n)
            query_vec = np.array([query_embedding], dtype=np.float32)

            # FAISS search 是 CPU 密集型，放到线程中避免阻塞事件循环
            scores, indices = await asyncio.to_thread(
                self._index.search, query_vec, k
            )

            results: List[Chunk] = []
            for score, idx in zip(scores[0], indices[0]):
                if idx < 0 or idx >= len(self._chunks):
                    continue
                chunk = self._chunks[idx]
                # 复制并设置相似度分数
                results.append(
                    Chunk(
                        chunk_id=chunk.chunk_id,
                        text=chunk.text,
                        title=chunk.title,
                        url=chunk.url,
                        score=float(score),
                        metadata=chunk.metadata,
                    )
                )

            logger.debug("faiss.search  top_k=%d results=%d total=%d", top_k, len(results), n)
            return results

        except Exception as exc:
            logger.exception("faiss.search failed")
            raise DependencyError(f"Vector search error: {exc}") from exc

    async def add_documents(
        self,
        chunks: List[Chunk],
        embeddings: List[List[float]],
    ) -> int:
        if not self._ready or self._index is None:
            raise DependencyError("FAISS index not initialized")
        if len(chunks) != len(embeddings):
            raise ValueError("chunks and embeddings must have same length")
        if not chunks:
            return 0

        async with self._lock:
            vectors = np.array(embeddings, dtype=np.float32)
            self._index.add(vectors)
            self._chunks.extend(chunks)
            logger.info("faiss.add_documents  added=%d total=%d", len(chunks), self._index.ntotal)
            return len(chunks)

    async def rebuild(
        self,
        chunks: List[Chunk],
        embeddings: List[List[float]],
    ) -> int:
        if not self._ready:
            raise DependencyError("FAISS index not initialized")
        if len(chunks) != len(embeddings):
            raise ValueError("chunks and embeddings must have same length")

        import faiss

        async with self._lock:
            # 重建索引
            self._index = faiss.IndexFlatIP(self._dim)
            self._chunks.clear()

            if chunks:
                vectors = np.array(embeddings, dtype=np.float32)
                self._index.add(vectors)
                self._chunks.extend(chunks)

            logger.info("faiss.rebuild  total=%d", self._index.ntotal)
            return len(chunks)

    async def count(self) -> int:
        if self._index is None:
            return 0
        return self._index.ntotal

    async def health(self) -> bool:
        return self._ready and self._index is not None

    # ---- 内部方法 ----

    async def _load_seeds(self, embedder) -> None:
        """从种子 JSON 加载初始知识库"""
        try:
            raw = json.loads(_SEEDS_PATH.read_text(encoding="utf-8"))
            chunks: List[Chunk] = []
            texts: List[str] = []

            for entry in raw:
                text = f"{entry['title']}。{entry['text']}"
                chunk = Chunk(
                    chunk_id=entry["id"],
                    text=text,
                    title=entry["title"],
                    url=entry.get("url", ""),
                    score=0.0,
                    metadata={
                        "category": entry.get("category", ""),
                        "era": entry.get("era", ""),
                    },
                )
                chunks.append(chunk)
                texts.append(text)

            embeddings = await embedder.embed_batch(texts)
            await self.add_documents(chunks, embeddings)
            logger.info("faiss.load_seeds  loaded=%d from %s", len(chunks), _SEEDS_PATH.name)

        except Exception as exc:
            logger.error("faiss.load_seeds failed: %s", exc)

