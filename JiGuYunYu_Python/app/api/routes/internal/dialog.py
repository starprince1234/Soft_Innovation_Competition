from fastapi import APIRouter, Depends

from app.api.deps import get_dialog_uc
from app.api.schemas.common import InternalResponse
from app.api.schemas.dialog import DialogRequest, DialogData, SourceItem

router = APIRouter(prefix="/dialog", tags=["internal.dialog"])


@router.post("/responses")
async def responses(req: DialogRequest, dialog_uc=Depends(get_dialog_uc)):
    """内部对话接口：解析请求 → RAG + LLM 编排 → 统一返回"""
    result = await dialog_uc.execute(req)
    sources = [
        SourceItem(
            title=s.title,
            url=s.url,
            score=s.score,
            chunk_id=s.chunk_id,
        )
        for s in result.sources
    ]
    data = DialogData(
        dialog_task_id=req.dialog_task_id,
        answer=result.answer,
        sources=sources,
        model=result.model,
        cost_ms=result.cost_ms,
    )
    return InternalResponse.ok(data=data)
