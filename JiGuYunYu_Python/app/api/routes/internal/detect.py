from fastapi import APIRouter, Depends

from app.api.deps import get_detect_uc
from app.api.schemas.common import InternalResponse
from app.api.schemas.detect import DetectRequest, DetectData, DetectionItem

router = APIRouter(prefix="/detect", tags=["internal.detect"])


@router.post("/process")
async def process(req: DetectRequest, detect_uc=Depends(get_detect_uc)):
    """内部检测接口：解析请求 → 调用 usecase → 统一返回"""
    result = await detect_uc.execute(req)
    data = DetectData(
        task_id=req.task_id,
        image_url=req.image_url,
        detections=[
            DetectionItem(
                label=d.label,
                confidence=d.confidence,
                bbox=d.bbox,
            )
            for d in result.detections
        ],
        raw_result=result.raw.get("vlm_response") if result.raw else None,
    )
    return InternalResponse.ok(data=data)
