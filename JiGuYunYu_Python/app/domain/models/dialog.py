"""对话领域模型（纯数据，不依赖第三方框架）"""

from dataclasses import dataclass, field
from typing import List


@dataclass
class Source:
    """RAG 检索来源条目"""
    title: str = ""
    url: str = ""
    score: float = 0.0
    chunk_id: str = ""


@dataclass
class DialogResult:
    """一次对话的完整结果"""
    answer: str = ""
    sources: List[Source] = field(default_factory=list)
    model: str = ""           # 使用的模型名
    cost_ms: float = 0.0      # LLM 调用耗时
