"""ChromaDB 向量检索实现（实现 VectorStorePort）

使用 chromadb 进行向量相似度搜索。
距离度量：cosine（配合归一化嵌入效果最佳）。
持久化目录通过 settings.CHROMA_PERSIST_DIR 配置。
集合名称通过 settings.CHROMA_COLLECTION 配置。

线程安全：ChromaDB 客户端内部已做线程安全处理。
add_documents / rebuild 通过 asyncio.Lock 串行化以避免并发重建冲突。
"""

from __future__ import annotations

import asyncio
import json
import logging
from pathlib import Path
from typing import Any, Dict, List, Optional

from app.domain.models.artifact import Chunk
from app.domain.ports.vector_store import VectorStorePort
from app.exceptions import DependencyError
from app.settings import settings

logger = logging.getLogger(__name__)

# 种子数据文件
_SEEDS_PATH = Path(__file__).resolve().parent.parent.parent / "data" / "knowledge_seeds.json"


class ChromaStore(VectorStorePort):
    """ChromaDB 向量检索（实现 VectorStorePort）"""

    def __init__(self) -> None:
        self._lock = asyncio.Lock()
        self._client = None
        self._collection = None
        self._ready = False

    # ---- lifecycle ----

    async def init(self, embedder=None) -> None:
        """启动时初始化 ChromaDB 客户端和集合，并可选加载种子数据"""
        import chromadb  # noqa: E402  延迟导入

        persist_dir = settings.CHROMA_PERSIST_DIR
        collection_name = settings.CHROMA_COLLECTION

        self._client = await asyncio.to_thread(
            chromadb.PersistentClient, path=persist_dir
        )

        self._collection = await asyncio.to_thread(
            self._client.get_or_create_collection,
            name=collection_name,
            metadata={"hnsw:space": "cosine"},
        )

        self._ready = True
        count = await self.count()
        logger.info(
            "chroma.init  persist_dir=%s  collection=%s  existing_docs=%d",
            persist_dir, collection_name, count,
        )

        # 如果集合为空且有 embedder + 种子文件，自动加载
        if count == 0 and embedder and _SEEDS_PATH.exists():
            await self._load_seeds(embedder)

    # ---- VectorStorePort 实现 ----

    async def search(
        self,
        query_embedding: List[float],
        *,
        top_k: int = 5,
        filters: Optional[Dict[str, Any]] = None,
    ) -> List[Chunk]:
        if not self._ready or self._collection is None:
            logger.warning("chroma.search called before init, returning empty")
            return []

        try:
            n = await self.count()
            if n == 0:
                return []

            k = min(top_k, n)

            # ChromaDB query（CPU 密集，放到线程中）
            result = await asyncio.to_thread(
                self._collection.query,
                query_embeddings=[query_embedding],
                n_results=k,
                include=["documents", "metadatas", "distances"],
            )

            chunks: List[Chunk] = []
            if result and result.get("ids"):
                ids = result["ids"][0]
                documents = (result.get("documents") or [[]])[0]
                metadatas = (result.get("metadatas") or [[]])[0]
                distances = (result.get("distances") or [[]])[0]

                for i, doc_id in enumerate(ids):
                    meta = metadatas[i] if i < len(metadatas) else {}
                    doc_text = documents[i] if i < len(documents) else ""
                    distance = distances[i] if i < len(distances) else 1.0

                    # ChromaDB cosine 距离 → 相似度分数（1 - distance）
                    score = max(0.0, 1.0 - distance)

                    chunks.append(
                        Chunk(
                            chunk_id=doc_id,
                            text=doc_text,
                            title=meta.get("title", ""),
                            url=meta.get("url", ""),
                            score=score,
                            metadata={
                                k: v for k, v in meta.items()
                                if k not in ("title", "url")
                            },
                        )
                    )

            logger.debug("chroma.search  top_k=%d results=%d total=%d", top_k, len(chunks), n)
            return chunks

        except Exception as exc:
            logger.exception("chroma.search failed")
            raise DependencyError(f"Vector search error: {exc}") from exc

    async def add_documents(
        self,
        chunks: List[Chunk],
        embeddings: List[List[float]],
    ) -> int:
        if not self._ready or self._collection is None:
            raise DependencyError("ChromaDB collection not initialized")
        if len(chunks) != len(embeddings):
            raise ValueError("chunks and embeddings must have same length")
        if not chunks:
            return 0

        async with self._lock:
            ids = [c.chunk_id for c in chunks]
            documents = [c.text for c in chunks]
            metadatas = [
                {
                    "title": c.title,
                    "url": c.url,
                    **{k: str(v) for k, v in c.metadata.items()},
                }
                for c in chunks
            ]

            # upsert 避免重复 ID 冲突
            await asyncio.to_thread(
                self._collection.upsert,
                ids=ids,
                embeddings=embeddings,
                documents=documents,
                metadatas=metadatas,
            )

            total = await self.count()
            logger.info("chroma.add_documents  added=%d total=%d", len(chunks), total)
            return len(chunks)

    async def rebuild(
        self,
        chunks: List[Chunk],
        embeddings: List[List[float]],
    ) -> int:
        if not self._ready or self._client is None:
            raise DependencyError("ChromaDB not initialized")
        if len(chunks) != len(embeddings):
            raise ValueError("chunks and embeddings must have same length")

        async with self._lock:
            collection_name = settings.CHROMA_COLLECTION

            # 删除旧集合并重建
            try:
                await asyncio.to_thread(
                    self._client.delete_collection, name=collection_name
                )
            except Exception:
                pass  # 集合不存在时忽略

            self._collection = await asyncio.to_thread(
                self._client.create_collection,
                name=collection_name,
                metadata={"hnsw:space": "cosine"},
            )

            if chunks:
                ids = [c.chunk_id for c in chunks]
                documents = [c.text for c in chunks]
                metadatas = [
                    {
                        "title": c.title,
                        "url": c.url,
                        **{k: str(v) for k, v in c.metadata.items()},
                    }
                    for c in chunks
                ]

                await asyncio.to_thread(
                    self._collection.add,
                    ids=ids,
                    embeddings=embeddings,
                    documents=documents,
                    metadatas=metadatas,
                )

            total = await self.count()
            logger.info("chroma.rebuild  total=%d", total)
            return total

    async def delete_documents(self, ids: List[str]) -> int:
        if not self._ready or self._collection is None:
            raise DependencyError("ChromaDB collection not initialized")
        if not ids:
            return 0

        async with self._lock:
            # ChromaDB delete by IDs
            await asyncio.to_thread(self._collection.delete, ids=ids)
            logger.info("chroma.delete_documents  deleted=%d", len(ids))
            return len(ids)

    async def count(self) -> int:
        if self._collection is None:
            return 0
        return await asyncio.to_thread(self._collection.count)

    async def health(self) -> bool:
        return self._ready and self._collection is not None

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
            logger.info("chroma.load_seeds  loaded=%d from %s", len(chunks), _SEEDS_PATH.name)

        except Exception as exc:
            logger.error("chroma.load_seeds failed: %s", exc)
