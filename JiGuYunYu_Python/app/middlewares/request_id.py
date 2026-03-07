import time
import uuid
import logging

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

from app.logging_config import request_id_ctx

logger = logging.getLogger(__name__)


class RequestIDMiddleware(BaseHTTPMiddleware):
    """
    职责：
      1. 从请求头 X-Request-Id 透传或自动生成 request_id
      2. 写入 contextvars（日志自动带上）
      3. 记录请求耗时 cost_ms 并写入响应头
      4. 打印访问日志（path / method / status / cost_ms）
    """

    async def dispatch(self, request: Request, call_next) -> Response:
        # 1. 获取或生成 request_id
        request_id = request.headers.get("X-Request-Id") or str(uuid.uuid4())
        request.state.request_id = request_id

        # 2. 写入 contextvars，所有后续日志自动带上
        token = request_id_ctx.set(request_id)

        # 3. 计时
        start = time.perf_counter()
        try:
            response: Response = await call_next(request)
        except Exception:
            logger.exception("unhandled error  path=%s", request.url.path)
            raise
        finally:
            cost_ms = round((time.perf_counter() - start) * 1000, 2)
            # 还原 contextvars（避免跨请求污染）
            request_id_ctx.reset(token)

        # 4. 写响应头 & 访问日志
        response.headers["X-Request-Id"] = request_id
        response.headers["X-Cost-Ms"] = str(cost_ms)

        logger.info(
            "method=%s path=%s status=%s cost_ms=%.2f",
            request.method,
            request.url.path,
            response.status_code,
            cost_ms,
        )
        return response
