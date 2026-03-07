from fastapi import HTTPException, Request
import ipaddress

from app.settings import settings
from app.bootstrap import Container


def get_container(request: Request) -> Container:
    """返回 bootstrap 装配的全局容器"""
    return request.app.state.container


def get_detect_uc(request: Request):
    """快捷依赖：检测用例"""
    return get_container(request).detect_uc


def get_dialog_uc(request: Request):
    """快捷依赖：对话用例"""
    return get_container(request).dialog_uc


def get_health_uc(request: Request):
    """快捷依赖：健康检查用例"""
    return get_container(request).health_uc


# ---- internal auth（路由级依赖） ----

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


def internal_auth(request: Request):
    # 1. 校验 token
    token = request.headers.get("X-Internal-Token")
    if not token or token != settings.INTERNAL_TOKEN:
        raise HTTPException(status_code=401, detail="unauthorized")

    # 2. 校验 IP 白名单
    client_ip = (
        request.headers.get("X-Real-IP")
        or request.headers.get("X-Forwarded-For", "").split(",")[0].strip()
        or (request.client.host if request.client else "")
    )
    if not _ip_allowed(client_ip):
        raise HTTPException(status_code=403, detail="forbidden")

