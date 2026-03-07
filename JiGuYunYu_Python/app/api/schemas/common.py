from __future__ import annotations

from pydantic import BaseModel
from typing import Any, Generic, Optional, TypeVar

T = TypeVar("T")


class InternalResponse(BaseModel, Generic[T]):
    """
    统一内部响应契约（与总纲一致）：
      成功: {"code": 200, "message": "Success", "data": ...}
      失败: {"code": HTTP 状态码, "message": "...", "details"?: {...}}
    """

    code: int = 200
    message: str = "Success"
    data: Optional[T] = None  # type: ignore[assignment]

    @staticmethod
    def ok(data: Any = None) -> "InternalResponse":
      return InternalResponse(code=200, message="Success", data=data)

    @staticmethod
    def fail(code: int, message: str, data: Any = None) -> "InternalResponse":
        return InternalResponse(code=code, message=message, data=data)


# 保留别名，兼容已有代码
BaseResponse = InternalResponse
