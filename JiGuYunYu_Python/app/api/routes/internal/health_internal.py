from fastapi import APIRouter, Depends

from app.api.deps import get_health_uc
from app.api.schemas.common import InternalResponse

router = APIRouter(tags=["internal.health"])


@router.get("/health")
async def internal_health(health_uc=Depends(get_health_uc)):
    """
    聚合依赖健康探测（需鉴权，走 internal router 依赖）。
    返回每个依赖的 ok / latency / error。
    """
    result = await health_uc.execute()
    return InternalResponse.ok(data=result)
