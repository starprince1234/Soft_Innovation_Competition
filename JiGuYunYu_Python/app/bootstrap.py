"""
Composition Root —— 依赖装配中心

职责：
  1. 读取 settings
  2. 创建 infra 实例（HTTP client / LLM / vector / storage / cache / detector / embedder）
  3. 创建 usecases（传入 ports 实现）
  4. 提供 startup / shutdown 钩子

自动检测策略：
  - VLM_BASE_URL 已配置 → 使用 VLMDetector（Qwen3-VL-LoRA 真实推理）
  - VLM_BASE_URL 未配置 → 回退到 YoloDetector stub
  - QIANFAN_API_KEY 已配置 → 使用 QianfanHttpClient（真实千帆 API）
  - QIANFAN_API_KEY 未配置 → 回退到 QianfanClient stub
  - Embedder 使用 SentenceEmbedding（sentence-transformers 语义嵌入）
  - VectorStore 使用 ChromaDB（持久化向量检索）
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from typing import Any, Dict, Optional, Union

from app.settings import settings

# ---- infra ----
from app.infra.http.client import HttpClient
from app.infra.llm.qianfan_client import QianfanClient, QianfanHttpClient
from app.infra.vector.chroma_store import ChromaStore
from app.infra.storage.bos_storage import BosStorage
from app.infra.cache.redis_cache import RedisCache
from app.infra.detector.yolo_detector import YoloDetector
from app.infra.detector.vlm_detector import VLMDetector
from app.infra.embedding.sentence_embedding import SentenceEmbedding

# ---- domain ports ----
from app.domain.ports.detector import DetectorPort
from app.domain.ports.llm import LLMPort
from app.domain.ports.embedding import EmbeddingPort

# ---- usecases ----
from app.usecases.detect_process import DetectUseCase
from app.usecases.dialog_respond import DialogUseCase
from app.usecases.health_check import HealthCheckUseCase

# ---- rag ----
from app.rag.retriever import Retriever

logger = logging.getLogger(__name__)


def _build_detector() -> DetectorPort:
    """根据配置选择检测器实现"""
    if settings.VLM_BASE_URL:
        logger.info(
            "detector: VLMDetector  base_url=%s  model=%s",
            settings.VLM_BASE_URL,
            settings.VLM_MODEL_NAME,
        )
        return VLMDetector(
            base_url=settings.VLM_BASE_URL,
            api_key=settings.VLM_API_KEY or "sk-123456",
            model=settings.VLM_MODEL_NAME,
            timeout=settings.VLM_TIMEOUT,
        )
    logger.warning("detector: YoloDetector(stub)  VLM_BASE_URL not set")
    return YoloDetector()


def _build_llm() -> LLMPort:
    """根据配置选择 LLM 实现"""
    if settings.QIANFAN_API_KEY:
        logger.info("llm: QianfanHttpClient(real)  base_url=%s", settings.QIANFAN_BASE_URL)
        return QianfanHttpClient(base_url=settings.QIANFAN_BASE_URL)
    logger.warning("llm: QianfanClient(stub)  QIANFAN_API_KEY not set")
    return QianfanClient()


def _build_embedder() -> EmbeddingPort:
    """构建嵌入器（Sentence-BERT 语义嵌入）"""
    model_name = settings.EMBEDDING_MODEL
    logger.info("embedder: SentenceEmbedding  model=%s", model_name)
    return SentenceEmbedding(model_name=model_name)


@dataclass
class Container:
    """全局依赖容器，挂到 app.state.container"""

    # infra
    http_client: HttpClient = field(default_factory=HttpClient)
    llm: LLMPort = field(default=None)  # type: ignore[assignment]
    vector_store: ChromaStore = field(default=None)  # type: ignore[assignment]
    storage: BosStorage = field(default_factory=BosStorage)
    cache: RedisCache = field(default_factory=RedisCache)
    detector: DetectorPort = field(default=None)  # type: ignore[assignment]
    embedder: EmbeddingPort = field(default=None)  # type: ignore[assignment]

    # rag
    retriever: Retriever = field(default=None)  # type: ignore[assignment]

    # usecases
    detect_uc: DetectUseCase = field(default=None)  # type: ignore[assignment]
    dialog_uc: DialogUseCase = field(default=None)  # type: ignore[assignment]
    health_uc: HealthCheckUseCase = field(default=None)  # type: ignore[assignment]

    def __post_init__(self) -> None:
        # infra — 根据配置自动选择实现
        if self.detector is None:
            self.detector = _build_detector()
        if self.llm is None:
            self.llm = _build_llm()
        if self.embedder is None:
            self.embedder = _build_embedder()
        if self.vector_store is None:
            self.vector_store = ChromaStore()

        # rag
        if self.retriever is None:
            self.retriever = Retriever(vector_store=self.vector_store)

        # usecases
        if self.detect_uc is None:
            self.detect_uc = DetectUseCase(
                detector=self.detector,
                storage=self.storage,
            )
        if self.dialog_uc is None:
            self.dialog_uc = DialogUseCase(
                llm=self.llm,
                retriever=self.retriever,
                embedder=self.embedder,
                cache=self.cache,
            )
        if self.health_uc is None:
            self.health_uc = HealthCheckUseCase(
                llm=self.llm,
                vector_store=self.vector_store,
                cache=self.cache,
                storage=self.storage,
                detector=self.detector,
            )


def create_container() -> Container:
    """bootstrap 装配入口"""
    container = Container()
    logger.info("container assembled  env=%s", settings.ENV)
    return container


async def startup(container: Container) -> None:
    """应用启动时初始化资源（连接池、ChromaDB 集合、Redis 连接等）"""
    logger.info("startup: initializing resources ...")

    # Redis 连接池
    await container.cache.init()

    # ChromaDB 集合 + 加载种子知识库
    await container.vector_store.init(embedder=container.embedder)

    logger.info("startup: all resources initialized")


async def shutdown(container: Container) -> None:
    """应用关闭时释放资源"""
    logger.info("shutdown: releasing resources ...")
    await container.cache.close()
    logger.info("shutdown: all resources released")

