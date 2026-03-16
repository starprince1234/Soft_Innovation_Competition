"""缓存层实现（Redis）

使用 redis.asyncio（redis-py >= 5.0 内置异步支持）。
key 命名规范： jigu:{biz}:{id}

功能：
  - get / set：通用 JSON 缓存
  - get_dialog / set_dialog：对话结果缓存（自动序列化/反序列化）
  - init / close：连接池生命周期
  - health：PING 探测
"""

from __future__ import annotations

import hashlib
import json
import logging
from typing import Any, Optional

from app.settings import settings

logger = logging.getLogger(__name__)


class RedisCache:
    """异步 Redis 缓存层"""

    def __init__(self, url: Optional[str] = None) -> None:
        self.url = url or settings.REDIS_URL
        self._redis = None  # redis.asyncio.Redis 实例

    # ---- 生命周期 ----

    async def init(self) -> None:
        """startup 时调用，初始化连接池"""
        if not self.url:
            logger.warning("redis.init skipped: REDIS_URL not configured")
            return

        try:
            import redis.asyncio as aioredis

            self._redis = aioredis.from_url(
                self.url,
                decode_responses=True,
                max_connections=20,
                socket_connect_timeout=5,
                socket_timeout=5,
            )
            # 验证连接
            await self._redis.ping()
            logger.info("redis.init  url=%s  connected", self.url)
        except Exception as exc:
            logger.error("redis.init failed: %s", exc)
            self._redis = None

    async def close(self) -> None:
        """shutdown 时调用，关闭连接池"""
        if self._redis:
            try:
                await self._redis.aclose()
                logger.info("redis.close  done")
            except Exception as exc:
                logger.warning("redis.close error: %s", exc)
            finally:
                self._redis = None

    # ---- 通用 KV ----

    async def get(self, key: str) -> Optional[Any]:
        """获取缓存值（自动 JSON 反序列化）"""
        if not self._redis:
            return None
        try:
            raw = await self._redis.get(key)
            if raw is None:
                return None
            return json.loads(raw)
        except json.JSONDecodeError:
            return raw  # 非 JSON 字符串直接返回
        except Exception as exc:
            logger.warning("redis.get failed key=%s: %s", key, exc)
            return None

    async def set(self, key: str, value: Any, *, ex: int = 300) -> bool:
        """设置缓存值（自动 JSON 序列化），ex 为过期秒数"""
        if not self._redis:
            return False
        try:
            serialized = json.dumps(value, ensure_ascii=False) if not isinstance(value, str) else value
            await self._redis.set(key, serialized, ex=ex)
            logger.debug("redis.set  key=%s  ex=%d", key, ex)
            return True
        except Exception as exc:
            logger.warning("redis.set failed key=%s: %s", key, exc)
            return False

    async def delete(self, key: str) -> bool:
        """删除缓存"""
        if not self._redis:
            return False
        try:
            await self._redis.delete(key)
            return True
        except Exception as exc:
            logger.warning("redis.delete failed key=%s: %s", key, exc)
            return False

    # ---- 对话缓存（便捷方法）----

    @staticmethod
    def dialog_cache_key(
        query: str,
        artifact_id: Optional[int] = None,
        context_fingerprint: Optional[str] = None,
    ) -> str:
        """生成对话缓存 key：jigu:dialog:{hash}"""
        raw = f"{query}|{artifact_id or ''}|{context_fingerprint or ''}"
        h = hashlib.md5(raw.encode("utf-8")).hexdigest()[:16]
        return f"jigu:dialog:{h}"

    async def get_dialog(
        self,
        query: str,
        artifact_id: Optional[int] = None,
        context_fingerprint: Optional[str] = None,
    ) -> Optional[dict]:
        """获取对话缓存"""
        key = self.dialog_cache_key(query, artifact_id, context_fingerprint)
        return await self.get(key)

    async def set_dialog(
        self,
        query: str,
        result: dict,
        artifact_id: Optional[int] = None,
        context_fingerprint: Optional[str] = None,
        *,
        ex: int = 600,
    ) -> bool:
        """缓存对话结果，默认 10 分钟过期"""
        key = self.dialog_cache_key(query, artifact_id, context_fingerprint)
        return await self.set(key, result, ex=ex)

    # ---- 千帆检索缓存（search 级别）----

    @staticmethod
    def search_cache_key(
        query: str,
        artifact_id: Optional[int] = None,
        context_fingerprint: Optional[str] = None,
    ) -> str:
        """生成检索缓存 key：jigu:search:{hash}"""
        raw = f"search|{query}|{artifact_id or ''}|{context_fingerprint or ''}"
        h = hashlib.md5(raw.encode("utf-8")).hexdigest()[:16]
        return f"jigu:search:{h}"

    async def get_search(
        self,
        query: str,
        artifact_id: Optional[int] = None,
        context_fingerprint: Optional[str] = None,
    ) -> Optional[dict]:
        """获取检索缓存（千帆 web_summary 网页检索结果）"""
        key = self.search_cache_key(query, artifact_id, context_fingerprint)
        return await self.get(key)

    async def set_search(
        self,
        query: str,
        result: dict,
        artifact_id: Optional[int] = None,
        context_fingerprint: Optional[str] = None,
        *,
        ex: int = 1800,
    ) -> bool:
        """缓存检索结果，默认 30 分钟过期（检索结果变化慢于对话结果）"""
        key = self.search_cache_key(query, artifact_id, context_fingerprint)
        return await self.set(key, result, ex=ex)

    # ---- 健康检查 ----

    async def health(self) -> bool:
        """PING 探测 Redis 是否可达"""
        if not self._redis:
            return self.url is not None  # 有 URL 但未连接 → 视为不健康但可恢复
        try:
            return await self._redis.ping()
        except Exception:
            return False

