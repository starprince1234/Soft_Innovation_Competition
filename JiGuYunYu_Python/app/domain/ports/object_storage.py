"""对象存储抽象端口"""

from abc import ABC, abstractmethod
from typing import Optional


class ObjectStoragePort(ABC):
    @abstractmethod
    async def get_bytes(self, url: str) -> bytes:
        """从 URL/key 下载文件 bytes"""
        ...

    async def upload(self, key: str, data: bytes) -> str:
        """上传 bytes，返回访问 URL（可选实现）"""
        raise NotImplementedError("upload not implemented")

    async def presign(self, key: str, *, expires: int = 3600) -> str:
        """生成预签名 URL（可选实现）"""
        raise NotImplementedError("presign not implemented")

    async def health(self) -> bool:
        """存储是否可达（默认 True）"""
        return True
