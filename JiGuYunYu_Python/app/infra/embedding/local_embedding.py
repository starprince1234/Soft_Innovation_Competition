"""本地轻量嵌入实现（无需外部 API / GPU）

使用 jieba 分词 + 字符 n-gram 哈希技巧，生成定长向量。
向量经 L2 归一化后适配 FAISS IndexFlatIP（余弦相似度）。

优点：
  - 零外部依赖（仅 numpy，随 faiss-cpu 自动安装）
  - 确定性输出，适合调试
  - 中文字符 n-gram 对文物名称 / 朝代 / 材质等关键词匹配效果尚可

局限：
  - 非语义嵌入，同义词不匹配
  - 生产环境建议替换为 Qianfan Embedding API 或 sentence-transformers
"""

from __future__ import annotations

import hashlib
import logging
from typing import List

import numpy as np

from app.domain.ports.embedding import EmbeddingPort

logger = logging.getLogger(__name__)

# 尝试导入 jieba（可选依赖，未安装则退化为字符级 n-gram）
try:
    import jieba

    _HAS_JIEBA = True
except ImportError:
    _HAS_JIEBA = False
    logger.info("jieba not installed, falling back to char-level n-gram embedding")


class LocalEmbedding(EmbeddingPort):
    """轻量级本地嵌入：jieba 分词 + 字符 n-gram 哈希 → 定长向量"""

    def __init__(self, dim: int = 256) -> None:
        self._dim = dim

    @property
    def dim(self) -> int:
        return self._dim

    # ---- public ----

    async def embed(self, text: str) -> List[float]:
        return self._embed_sync(text)

    async def embed_batch(self, texts: List[str]) -> List[List[float]]:
        return [self._embed_sync(t) for t in texts]

    # ---- internal ----

    def _embed_sync(self, text: str) -> List[float]:
        vec = np.zeros(self._dim, dtype=np.float32)

        tokens = self._tokenize(text)

        for token in tokens:
            # 对每个 token 生成 unigram/bigram/trigram 哈希
            for n in (1, 2, 3):
                for i in range(max(1, len(token) - n + 1)):
                    gram = token[i : i + n]
                    h = int(hashlib.md5(gram.encode("utf-8")).hexdigest(), 16)
                    idx = h % self._dim
                    vec[idx] += 1.0

        # L2 归一化（FAISS IndexFlatIP = 余弦相似度 when vectors are normed）
        norm = np.linalg.norm(vec)
        if norm > 0:
            vec /= norm

        return vec.tolist()

    @staticmethod
    def _tokenize(text: str) -> List[str]:
        """分词：优先 jieba，否则按字符拆分"""
        text = text.strip()
        if not text:
            return []
        if _HAS_JIEBA:
            return list(jieba.cut(text))
        # 退化：按单字拆分
        return list(text)
