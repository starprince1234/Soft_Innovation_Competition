"""健康检查编排

聚合各外部依赖的 health() 状态，返回结构化报告。
不应抛异常导致 500，应“可观测地失败”。
"""

import logging
import time
from typing import Any, Dict, Optional

from app.domain.ports.llm import LLMPort
from app.domain.ports.vector_store import VectorStorePort
from app.domain.ports.object_storage import ObjectStoragePort
from app.domain.ports.detector import DetectorPort
from app.infra.cache.redis_cache import RedisCache

logger = logging.getLogger(__name__)


async def _probe(name: str, check) -> Dict[str, Any]:
    """探测单个依赖，返回 {ok, latency_ms, error?}"""
    start = time.perf_counter()
    try:
        ok = await check()
        latency = round((time.perf_counter() - start) * 1000, 2)
        return {"name": name, "ok": ok, "latency_ms": latency}
    except Exception as exc:
        latency = round((time.perf_counter() - start) * 1000, 2)
        logger.warning("health probe %s failed: %s", name, exc)
        return {"name": name, "ok": False, "latency_ms": latency, "error": str(exc)}


class HealthCheckUseCase:
    def __init__(
        self,
        llm: Optional[LLMPort] = None,
        vector_store: Optional[VectorStorePort] = None,
        cache: Optional[RedisCache] = None,
        storage: Optional[ObjectStoragePort] = None,
        detector: Optional[DetectorPort] = None,
    ) -> None:
        self._llm = llm
        self._vs = vector_store
        self._cache = cache
        self._storage = storage
        self._detector = detector

    async def execute(self) -> Dict[str, Any]:
        probes = []
        if self._llm:
            probes.append(_probe("llm", self._llm.health))
        if self._vs:
            probes.append(_probe("vector_store", self._vs.health))
        if self._cache:
            probes.append(_probe("cache", self._cache.health))
        if self._storage:
            probes.append(_probe("storage", self._storage.health))
        if self._detector:
            probes.append(_probe("detector", self._detector.health))

        import asyncio
        results = await asyncio.gather(*probes)
        deps = list(results)
        all_ok = all(d["ok"] for d in deps)
        return {"healthy": all_ok, "dependencies": deps}
