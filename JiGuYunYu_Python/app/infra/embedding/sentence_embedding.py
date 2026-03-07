"""Sentence-BERT 嵌入实现（实现 EmbeddingPort）

使用 sentence-transformers 库加载预训练多语言模型，
生成语义向量。默认模型 paraphrase-multilingual-MiniLM-L12-v2，
支持中/英等 50+ 语言，输出 384 维向量，模型约 ~120MB。

特点：
  - 真正的语义嵌入，支持同义词 / 近义语句匹配
  - 基于 Transformer，首次加载需下载模型权重
  - 支持 GPU 加速（自动检测），CPU 下也有良好性能
  - 适合文物描述、朝代、材质等中文语义检索
"""

from __future__ import annotations

import asyncio
import logging
from typing import List

from app.domain.ports.embedding import EmbeddingPort
from app.settings import settings

logger = logging.getLogger(__name__)


class SentenceEmbedding(EmbeddingPort):
    """基于 sentence-transformers 的语义嵌入"""

    def __init__(self, model_name: str | None = None) -> None:
        self._model_name = model_name or settings.EMBEDDING_MODEL
        self._model = None  # 延迟加载
        self._dim: int | None = None

    def _ensure_model(self) -> None:
        """首次调用时加载模型（避免启动时阻塞过久）"""
        if self._model is not None:
            return

        from sentence_transformers import SentenceTransformer  # noqa: E402

        logger.info("sentence_embedding: loading model '%s' ...", self._model_name)
        self._model = SentenceTransformer(self._model_name)
        self._dim = self._model.get_sentence_embedding_dimension()
        logger.info(
            "sentence_embedding: model loaded  dim=%d  device=%s",
            self._dim, self._model.device,
        )

    @property
    def dim(self) -> int:
        if self._dim is not None:
            return self._dim
        # 未加载模型时返回默认维度（paraphrase-multilingual-MiniLM-L12-v2 = 384）
        return settings.EMBEDDING_DIM

    # ---- public ----

    async def embed(self, text: str) -> List[float]:
        """单条文本 → 语义向量"""
        self._ensure_model()
        # encode 是 CPU/GPU 密集型，放到线程中避免阻塞事件循环
        embedding = await asyncio.to_thread(
            self._model.encode,
            text,
            normalize_embeddings=True,
        )
        return embedding.tolist()

    async def embed_batch(self, texts: List[str]) -> List[List[float]]:
        """批量文本 → 语义向量列表"""
        if not texts:
            return []

        self._ensure_model()

        embeddings = await asyncio.to_thread(
            self._model.encode,
            texts,
            normalize_embeddings=True,
            batch_size=32,
            show_progress_bar=False,
        )
        return [e.tolist() for e in embeddings]
