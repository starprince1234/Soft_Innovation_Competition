"""知识库管理路由（重嵌入 / 增量添加）

重建流程：
  1. 从 Java 内部 API 拉取所有 APPROVED 文物列表
  2. 生成 Sentence-BERT 语义嵌入向量
  3. 重建 ChromaDB 集合索引
  4. 将 vector_embedding_id 写回 Java 端（建立文物 ↔ 向量索引关联）

种子数据仅作为 Java 端 DB 为空时的 fallback。
"""

from fastapi import APIRouter, Depends
import json
import logging
from pathlib import Path
from typing import List

import httpx

from app.api.deps import get_container
from app.api.schemas.common import InternalResponse
from app.domain.models.artifact import Chunk
from app.settings import settings

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/knowledge", tags=["internal.knowledge"])

# 种子数据路径（仅作为 fallback）
_SEEDS_PATH = Path(__file__).resolve().parent.parent.parent.parent / "data" / "knowledge_seeds.json"


async def _fetch_approved_artifacts() -> List[dict]:
    """从 Java 内部 API 获取所有已审核文物。"""
    url = f"{settings.JAVA_BASE_URL}/api/internal/artifacts/approved"
    headers = {"X-Internal-Token": settings.INTERNAL_TOKEN}
    async with httpx.AsyncClient(timeout=10.0) as client:
        resp = await client.get(url, headers=headers)
        resp.raise_for_status()
        body = resp.json()
        # Java ApiResponse 格式: {"code": 200, "data": [...]}
        return body.get("data", [])


async def _write_back_vector_ids(updates: List[dict]) -> dict:
    """将 vector_embedding_id 批量写回 Java 端。"""
    if not updates:
        return {"success": 0, "failed": 0}
    url = f"{settings.JAVA_BASE_URL}/api/internal/artifacts/vector-ids"
    headers = {"X-Internal-Token": settings.INTERNAL_TOKEN}
    async with httpx.AsyncClient(timeout=10.0) as client:
        resp = await client.post(url, json=updates, headers=headers)
        resp.raise_for_status()
        body = resp.json()
        return body.get("data", {})


def _artifacts_to_chunks(artifacts: List[dict]) -> tuple[List[Chunk], List[str], List[dict]]:
    """将文物列表转换为 Chunk + 文本 + 回写映射。"""
    chunks: List[Chunk] = []
    texts: List[str] = []
    id_map: List[dict] = []  # [{artifactId, vectorEmbeddingId}]

    for a in artifacts:
        artifact_id = a.get("id")
        name = a.get("name", "")
        desc = a.get("description", "")
        text = f"{name}。{desc}" if desc else name
        chunk_id = f"artifact_{artifact_id}"

        chunk = Chunk(
            chunk_id=chunk_id,
            text=text,
            title=name,
            url="",
            score=0.0,
            metadata={
                "era": a.get("era", ""),
                "location": a.get("location", ""),
                "tags": a.get("tags", ""),
            },
        )
        chunks.append(chunk)
        texts.append(text)
        id_map.append({
            "artifactId": artifact_id,
            "vectorEmbeddingId": chunk_id,
        })

    return chunks, texts, id_map


def _seeds_to_chunks() -> tuple[List[Chunk], List[str]]:
    """Fallback: 从种子 JSON 加载（当 Java 不可达或 DB 为空时）。"""
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
    return chunks, texts


@router.post("/reindex")
async def reindex(container=Depends(get_container)):
    """
    触发知识库向量重嵌入。

    由 Java 管理端调用，对所有已审核文物重新进行嵌入和向量存储。
    完成后将 vector_embedding_id 写回 Java 端数据库。
    """
    logger.info("knowledge reindex triggered")

    embedder = container.embedder
    vector_store = container.vector_store

    if embedder is None:
        return InternalResponse.fail(500, "embedder not configured")

    try:
        # 1. 从 Java 拉取 APPROVED 文物
        id_map: List[dict] = []
        try:
            artifacts = await _fetch_approved_artifacts()
            if artifacts:
                chunks, texts, id_map = _artifacts_to_chunks(artifacts)
                logger.info("reindex: fetched %d APPROVED artifacts from Java", len(artifacts))
            else:
                raise ValueError("No APPROVED artifacts found in Java DB")
        except Exception as fetch_exc:
            logger.warning("reindex: Java fetch failed (%s), falling back to seeds", fetch_exc)
            if not _SEEDS_PATH.exists():
                return InternalResponse.fail(500, f"Java API unreachable and seeds file not found")
            chunks, texts = _seeds_to_chunks()
            logger.info("reindex: loaded %d seed entries as fallback", len(chunks))

        # 2. 生成嵌入向量
        embeddings = await embedder.embed_batch(texts)

        # 3. 重建 ChromaDB 索引
        count = await vector_store.rebuild(chunks, embeddings)
        logger.info("knowledge reindex completed: %d documents indexed", count)

        # 4. 写回 vector_embedding_id 到 Java 端
        writeback_result = {}
        if id_map:
            try:
                writeback_result = await _write_back_vector_ids(id_map)
                logger.info("reindex: vector_ids written back: %s", writeback_result)
            except Exception as wb_exc:
                logger.warning("reindex: vector_id writeback failed: %s", wb_exc)
                writeback_result = {"error": str(wb_exc)}

        return InternalResponse.ok(data={
            "status": "reindex_completed",
            "documents_indexed": count,
            "source": "java_db" if id_map else "seed_fallback",
            "writeback": writeback_result,
        })

    except Exception as exc:
        logger.exception("knowledge reindex failed")
        return InternalResponse.fail(500, f"reindex failed: {exc}")


@router.post("/upsert")
async def upsert_artifact(body: dict, container=Depends(get_container)):
    """
    单个文物向量 upsert（新增/更新已审核文物的向量）。

    Java 端文物 APPROVED 或信息更新时调用。
    body: {"artifactId": 1, "name": "...", "description": "...", "era": "...", ...}
    """
    logger.info("knowledge upsert triggered  artifact=%s", body.get("artifactId"))

    embedder = container.embedder
    vector_store = container.vector_store

    if embedder is None:
        return InternalResponse.fail(500, "embedder not configured")

    try:
        artifact_id = body.get("artifactId")
        name = body.get("name", "")
        desc = body.get("description", "")
        text = f"{name}。{desc}" if desc else name
        chunk_id = f"artifact_{artifact_id}"

        chunk = Chunk(
            chunk_id=chunk_id,
            text=text,
            title=name,
            url="",
            score=0.0,
            metadata={
                "era": body.get("era", ""),
                "location": body.get("location", ""),
                "tags": body.get("tags", ""),
            },
        )

        embeddings = await embedder.embed_batch([text])
        await vector_store.add_documents([chunk], embeddings)

        logger.info("knowledge upsert completed  chunk_id=%s", chunk_id)
        return InternalResponse.ok(data={
            "status": "upsert_completed",
            "chunk_id": chunk_id,
        })

    except Exception as exc:
        logger.exception("knowledge upsert failed")
        return InternalResponse.fail(500, f"upsert failed: {exc}")


@router.post("/delete")
async def delete_artifact_vector(body: dict, container=Depends(get_container)):
    """
    删除指定文物的向量。

    Java 端文物删除或从 APPROVED 变为 REJECTED 时调用。
    body: {"artifactId": 1}
    """
    artifact_id = body.get("artifactId")
    logger.info("knowledge delete triggered  artifact=%s", artifact_id)

    vector_store = container.vector_store

    try:
        chunk_id = f"artifact_{artifact_id}"
        deleted = await vector_store.delete_documents([chunk_id])
        logger.info("knowledge delete completed  chunk_id=%s deleted=%d", chunk_id, deleted)
        return InternalResponse.ok(data={
            "status": "delete_completed",
            "chunk_id": chunk_id,
            "deleted": deleted,
        })

    except Exception as exc:
        logger.exception("knowledge delete failed")
        return InternalResponse.fail(500, f"delete failed: {exc}")
