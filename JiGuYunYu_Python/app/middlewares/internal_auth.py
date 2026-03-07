import ipaddress
import logging

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse

from app.settings import settings

logger = logging.getLogger(__name__)

# 错误码常量（与统一 API 规范一致）
_CODE_AUTH_FAIL = 401
_CODE_IP_FORBIDDEN = 403


def _get_client_ip(request: Request) -> str:
    """优先从反向代理 header 取真实 IP，兜底用 socket 地址"""
    return (
        request.headers.get("X-Real-IP")
        or request.headers.get("X-Forwarded-For", "").split(",")[0].strip()
        or (request.client.host if request.client else "")
    )


def _ip_allowed(client_ip: str) -> bool:
    """使用 settings.internal_ip_whitelist() 解析后的列表做比较"""
    whitelist = settings.internal_ip_whitelist()
    if not whitelist:
        return True  # 未配置白名单则放行
    try:
        addr = ipaddress.ip_address(client_ip)
    except ValueError:
        return False
    return addr in whitelist


class InternalAuthMiddleware(BaseHTTPMiddleware):
    """
    仅拦截 /internal/* 路由：
      1. 校验 X-Internal-Token（常量时间比较可选）
      2. 校验 IP 白名单
    非 internal 路由直接放行。
    """

    async def dispatch(self, request: Request, call_next):
        if request.url.path.startswith("/internal"):
            # ---- token 校验 ----
            token = request.headers.get("X-Internal-Token")
            if not token or token != settings.INTERNAL_TOKEN:
                logger.warning(
                    "auth_fail path=%s ip=%s reason=bad_token",
                    request.url.path,
                    _get_client_ip(request),
                )
                return JSONResponse(
                    {"code": _CODE_AUTH_FAIL, "message": "Unauthorized", "data": None},
                    status_code=401,
                )

            # ---- IP 白名单校验 ----
            client_ip = _get_client_ip(request)
            if not _ip_allowed(client_ip):
                logger.warning(
                    "auth_fail path=%s ip=%s reason=ip_forbidden",
                    request.url.path,
                    client_ip,
                )
                return JSONResponse(
                    {"code": _CODE_IP_FORBIDDEN, "message": "Forbidden", "data": None},
                    status_code=403,
                )

        response = await call_next(request)
        return response
