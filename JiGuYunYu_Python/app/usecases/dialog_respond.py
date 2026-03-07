"""对话编排（RAG + 千帆融合 + Redis 多级缓存）

完整流水线：
  1. 查 Redis 对话缓存 → 命中则直接返回
  2. 嵌入 query → ChromaDB 本地检索 topK chunks
  3. 按置信度阈值过滤 + 去重 + 按分数排序
  4. 查 Redis 检索缓存（search 级） → 命中则跳过 LLM 调用
  5. 拼接 system_prompt + RAG 上下文 + 用户角色/历史 + 用户提问
  6. 调 LLM（千帆 web_summary 自带网页检索，即"融合"本地知识 + 网络知识）
  7. 合并本地 RAG chunks + 千帆网页来源 → 统一 sources
  8. 写入 Redis 检索缓存 + 对话缓存
  9. 组装 DialogResult 返回
"""

import logging
import time
from pathlib import Path
from typing import List, Optional

from app.api.schemas.dialog import DialogRequest
from app.domain.models.dialog import DialogResult, Source
from app.domain.ports.embedding import EmbeddingPort
from app.domain.ports.llm import LLMPort
from app.infra.cache.redis_cache import RedisCache
from app.rag.retriever import Retriever

logger = logging.getLogger(__name__)

# 读取 system prompt 模板
_PROMPT_DIR = Path(__file__).resolve().parent.parent / "infra" / "llm" / "prompts"
_SYSTEM_PROMPT = (_PROMPT_DIR / "system_prompt.md").read_text(encoding="utf-8").strip()

# 缓存过期时间（秒）
_DIALOG_CACHE_TTL = 600   # 对话级：10 分钟
_SEARCH_CACHE_TTL = 1800  # 检索级：30 分钟


class DialogUseCase:
    def __init__(
        self,
        llm: LLMPort,
        retriever: Retriever,
        embedder: Optional[EmbeddingPort] = None,
        cache: Optional[RedisCache] = None,
    ) -> None:
        self._llm = llm
        self._retriever = retriever
        self._embedder = embedder
        self._cache = cache

    async def execute(self, req: DialogRequest) -> DialogResult:
        start = time.perf_counter()

        # ---- 1. 查 Redis 对话级缓存 ----
        cached = await self._try_cache_get(req)
        if cached is not None:
            logger.info("dialog  dialog_cache_hit  query_len=%d", len(req.query))
            return cached

        # ---- 2. 嵌入 + 检索 + 去重 + 排序 ----
        chunks = await self._retrieve_chunks(req)

        # ---- 3. 查 Redis 检索级缓存 ----
        search_cached = await self._try_search_cache_get(req)
        if search_cached is not None:
            logger.info("dialog  search_cache_hit  query_len=%d", len(req.query))
            cost_ms = round((time.perf_counter() - start) * 1000, 2)
            sources = [
                Source(title=c.title, url=c.url, score=c.score, chunk_id=c.chunk_id)
                for c in chunks
            ]
            # 从缓存恢复千帆网页来源
            for ws in search_cached.get("web_sources", []):
                if isinstance(ws, dict):
                    sources.append(Source(
                        title=ws.get("title", ""),
                        url=ws.get("url", ""),
                        score=0.0,
                        chunk_id=f"web:{ws.get('url', '')[:80]}",
                    ))
            result = DialogResult(
                answer=search_cached["answer"],
                sources=sources,
                model=search_cached.get("model", "search_cache"),
                cost_ms=cost_ms,
            )
            # 写入对话级缓存（更短 TTL）
            await self._try_cache_set(req, result)
            return result

        # ---- 4. 拼接完整 prompt ----
        full_prompt = self._build_prompt(req, chunks)

        # ---- 5. 调 LLM（千帆 web_summary = 本地 RAG + 网络知识融合） ----
        llm_result = await self._llm.chat(full_prompt)

        cost_ms = round((time.perf_counter() - start) * 1000, 2)

        # ---- 6. 组装响应 ----
        sources = [
            Source(title=c.title, url=c.url, score=c.score, chunk_id=c.chunk_id)
            for c in chunks
        ]

        # 合并千帆 web_summary 返回的网页检索来源
        if llm_result.web_sources:
            for ws in llm_result.web_sources:
                sources.append(Source(
                    title=ws.title,
                    url=ws.url,
                    score=0.0,
                    chunk_id=f"web:{ws.url[:80]}",
                ))

        result = DialogResult(
            answer=llm_result.text,
            sources=sources,
            model=llm_result.model,
            cost_ms=cost_ms,
        )

        logger.info(
            "dialog  query_len=%d chunks=%d cost_ms=%.2f model=%s",
            len(req.query), len(chunks), cost_ms, llm_result.model,
        )

        # ---- 7. 写入 Redis 检索级 + 对话级缓存 ----
        await self._try_search_cache_set(req, llm_result)
        await self._try_cache_set(req, result)

        return result

    # ---- 内部方法 ----

    async def _retrieve_chunks(self, req: DialogRequest) -> list:
        """嵌入 query → ChromaDB 检索 → 阈值过滤 → 去重 → 按分数降序排序"""
        query_embedding: List[float] = []

        if self._embedder:
            try:
                query_embedding = await self._embedder.embed(req.query)
            except Exception as exc:
                logger.warning("embedding failed, using empty vector: %s", exc)

        chunks = await self._retriever.retrieve(query_embedding, top_k=req.top_k)

        # 按 Java 传入的本地 RAG 置信度阈值过滤
        if req.local_rag_confidence_threshold is not None:
            chunks = [c for c in chunks if c.score >= req.local_rag_confidence_threshold]

        # 去重（按 chunk_id）
        seen: set = set()
        unique_chunks = []
        for c in chunks:
            if c.chunk_id not in seen:
                seen.add(c.chunk_id)
                unique_chunks.append(c)

        # 按分数降序排序
        unique_chunks.sort(key=lambda c: c.score, reverse=True)

        return unique_chunks

    @staticmethod
    def _build_prompt(req: DialogRequest, chunks: list) -> str:
        """拼接系统提示 + RAG 上下文 + 角色/历史 + 用户提问"""
        # RAG 上下文
        rag_context = ""
        if chunks:
            rag_lines = []
            for i, c in enumerate(chunks, 1):
                meta = ""
                if c.metadata:
                    era = c.metadata.get("era", "")
                    cat = c.metadata.get("category", "")
                    if era or cat:
                        meta = f" [{cat} · {era}]"
                rag_lines.append(f"[来源{i}{meta}] {c.title}: {c.text[:500]}")
            rag_context = "\n\n".join(rag_lines)

        # 用户角色
        role_hint = f"用户角色: {req.user_role}\n" if req.user_role else ""

        # 对话历史
        history_hint = ""
        if req.context_history:
            lines = []
            for turn in req.context_history:
                role = turn.get("role", "")
                content = turn.get("content", "")
                if role or content:
                    lines.append(f"{role}: {content}")
            if lines:
                history_hint = "\n".join(lines) + "\n"

        return (
            f"{_SYSTEM_PROMPT}\n\n"
            f"{role_hint}"
            f"### 参考资料（本地知识库检索结果）\n{rag_context}\n\n"
            f"### 对话历史\n{history_hint}\n"
            f"### 用户提问\n{req.query}\n\n"
            f"请结合以上参考资料和你的知识回答。如引用了参考资料，请标注 [来源N]。"
        )

    async def _try_cache_get(self, req: DialogRequest) -> Optional[DialogResult]:
        """尝试从 Redis 获取缓存"""
        if not self._cache:
            return None
        try:
            data = await self._cache.get_dialog(req.query, req.artifact_id)
            if data and isinstance(data, dict):
                sources = [
                    Source(**s) for s in data.get("sources", [])
                ]
                return DialogResult(
                    answer=data["answer"],
                    sources=sources,
                    model=data.get("model", "cache"),
                    cost_ms=0.0,
                )
        except Exception as exc:
            logger.debug("dialog cache get failed: %s", exc)
        return None

    async def _try_cache_set(self, req: DialogRequest, result: DialogResult) -> None:
        """尝试将结果写入 Redis 缓存"""
        if not self._cache:
            return
        try:
            data = {
                "answer": result.answer,
                "sources": [
                    {"title": s.title, "url": s.url, "score": s.score, "chunk_id": s.chunk_id}
                    for s in result.sources
                ],
                "model": result.model,
            }
            await self._cache.set_dialog(req.query, data, req.artifact_id, ex=_DIALOG_CACHE_TTL)
        except Exception as exc:
            logger.debug("dialog cache set failed: %s", exc)

    async def _try_search_cache_get(self, req: DialogRequest) -> Optional[dict]:
        """尝试从 Redis 获取检索级缓存（千帆 search 结果）"""
        if not self._cache:
            return None
        try:
            data = await self._cache.get_search(req.query, req.artifact_id)
            if data and isinstance(data, dict) and "answer" in data:
                return data
        except Exception as exc:
            logger.debug("search cache get failed: %s", exc)
        return None

    async def _try_search_cache_set(self, req: DialogRequest, llm_result) -> None:
        """将 LLM 检索结果写入 Redis 检索级缓存"""
        if not self._cache:
            return
        try:
            web_sources_data = []
            if hasattr(llm_result, "web_sources") and llm_result.web_sources:
                web_sources_data = [
                    {"title": ws.title, "url": ws.url, "snippet": ws.snippet}
                    for ws in llm_result.web_sources
                ]
            data = {
                "answer": llm_result.text,
                "model": llm_result.model,
                "web_sources": web_sources_data,
            }
            await self._cache.set_search(req.query, data, req.artifact_id, ex=_SEARCH_CACHE_TTL)
        except Exception as exc:
            logger.debug("search cache set failed: %s", exc)

