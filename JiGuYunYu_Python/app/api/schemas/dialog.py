from pydantic import BaseModel, ConfigDict, Field
from typing import Any, List, Optional


class DialogRequest(BaseModel):
    """对话请求（与 Java DTO 对齐）"""
    model_config = ConfigDict(populate_by_name=True)

    dialog_task_id: Optional[str] = Field(default=None, alias="dialogTaskId")
    user_id: Optional[int] = Field(default=None, alias="userId")
    user_role: Optional[str] = Field(default=None, alias="userRole")
    query: str
    top_k: int = 3
    artifact_id: Optional[int] = Field(default=None, alias="artifactId")
    context_history: Optional[List[dict[str, Any]]] = Field(default=None, alias="contextHistory")
    local_rag_confidence_threshold: Optional[float] = Field(
        default=None,
        alias="localRagConfidenceThreshold",
    )


class SourceItem(BaseModel):
    """检索来源条目（稳定 schema，Java 入库与展示使用）"""
    title: str = ""
    url: str = ""
    score: float = 0.0
    chunk_id: str = ""


class DialogData(BaseModel):
    """对话响应 data 体"""
    dialog_task_id: Optional[str] = None
    answer: str
    sources: List[SourceItem] = []
    model: str = ""
    cost_ms: float = 0.0
