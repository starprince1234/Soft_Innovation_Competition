from fastapi import APIRouter

router = APIRouter(tags=["health"])


@router.get("/health")
async def health():
    """轻量进程级健康检查（不鉴权，给 LB / 运维 / Docker healthcheck）"""
    return {"status": "ok"}
