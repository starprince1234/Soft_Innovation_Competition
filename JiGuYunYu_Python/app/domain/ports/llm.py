"""LLM 抽象端口"""

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import List


@dataclass
class WebSource:
    """千帆 web_summary 返回的网页检索来源"""
    title: str = ""
    url: str = ""
    snippet: str = ""


@dataclass
class LLMResult:
    """LLM 调用返回值"""
    text: str = ""
    model: str = ""
    prompt_tokens: int = 0
    completion_tokens: int = 0
    web_sources: List[WebSource] = field(default_factory=list)


class LLMPort(ABC):
    @abstractmethod
    async def chat(self, prompt: str, *, timeout: float | None = None) -> LLMResult:
        """发送 prompt 并获取回复"""
        ...

    async def health(self) -> bool:
        """LLM 服务是否可达（默认 True，子类可覆写）"""
        return True
