"""千帆 LLM 适配器（实现 LLMPort）

当前为 stub，后续接入真实 API 只需改 chat() 内部实现。
"""

import logging
import re
import time
import uuid

from app.domain.ports.llm import LLMPort, LLMResult, WebSource
from app.exceptions import DependencyError
from app.settings import settings
import httpx

from typing import Any, Dict, List, Optional

logger = logging.getLogger(__name__)


class QianfanClient(LLMPort):
    def __init__(self) -> None:
        self.api_key = settings.QIANFAN_API_KEY
        self.base_url = settings.QIANFAN_BASE_URL

    async def chat(self, prompt: str, *, timeout: float | None = None) -> LLMResult:
        timeout = timeout or settings.LLM_TIMEOUT
        start = time.perf_counter()
        try:
            # --- stub: 不回显系统提示词，改为基于用户问题的可读回复 ---
            answer = self._build_stub_answer(prompt)
            elapsed = (time.perf_counter() - start) * 1000
            logger.info("llm.chat cost_ms=%.2f prompt_len=%d", elapsed, len(prompt))
            return LLMResult(
                text=answer,
                model="stub-safe",
                prompt_tokens=len(prompt),
                completion_tokens=len(answer),
            )
        except Exception as exc:
            logger.exception("llm.chat failed")
            raise DependencyError(f"LLM error: {exc}") from exc

    @staticmethod
    def _extract_section(text: str, heading: str) -> str:
        marker = f"{heading}\n"
        if marker not in text:
            return ""
        tail = text.split(marker, 1)[1]
        next_heading = re.search(r"\n###\s", tail)
        if next_heading:
            return tail[: next_heading.start()].strip()
        return tail.strip()

    @classmethod
    def _build_stub_answer(cls, prompt: str) -> str:
        user_question = cls._extract_section(prompt, "### 用户提问")
        if "\n\n请结合以上参考资料" in user_question:
            user_question = user_question.split("\n\n请结合以上参考资料", 1)[0].strip()
        if not user_question:
            user_question = "根据当前输入，我无法提取到有效问题。"

        reference_block = cls._extract_section(prompt, "### 参考资料（本地知识库检索结果）")
        history_block = cls._extract_section(prompt, "### 对话历史")

        detect_context = ""
        for line in history_block.splitlines():
            if "识别上下文" in line or "识别结果" in line:
                detect_context = line.strip()
                break

        source_tag = ""
        for line in reference_block.splitlines():
            if line.strip().startswith("[来源"):
                source_tag = line.strip().split("]", 1)[0] + "]"
                break

        parts = []
        if detect_context:
            parts.append(f"已结合本次识别信息：{detect_context}。")
        if source_tag:
            parts.append(f"基于知识库检索结果，先给出结论：与“{user_question}”相关的信息可参考 {source_tag}。")
        else:
            parts.append(f"你问的是：{user_question}。目前本地参考资料不足，无法给出确定结论。")
        parts.append("这是本地降级回答（stub），如需完整联网回答请配置真实千帆密钥。")
        return "\n".join(parts)

    async def health(self) -> bool:
        return self.api_key is not None


class QianfanHttpClient(LLMPort):
    """真实千帆 HTTP 客户端（非流式）

    不替换现有 `QianfanClient` stub，容器仍可按需切换为本实现。
    """

    DEFAULT_BASE = "https://qianfan.baidubce.com"

    def __init__(self, base_url: Optional[str] = None) -> None:
        self.api_key = settings.QIANFAN_API_KEY
        self.base = (base_url or settings.QIANFAN_BASE_URL or self.DEFAULT_BASE).rstrip("/")

    async def chat(self, prompt: str, *, timeout: float | None = None) -> LLMResult:
        if not self.api_key:
            raise DependencyError("Qianfan API key is not configured")

        timeout = timeout or settings.LLM_TIMEOUT
        url = f"{self.base}/v2/ai_search/web_summary"
        request_id = str(uuid.uuid4())
        headers = {
            "X-Appbuilder-Authorization": f"Bearer {self.api_key}",
            "X-Appbuilder-Request-Id": request_id,
            "X-Appbuilder-User-Id": "jiguyunyu-python",
            "Content-Type": "application/json",
        }

        instruction = self._build_instruction(prompt)
        messages = self._build_messages(prompt)

        payload: Dict[str, Any] = {
            "instruction": instruction,
            "messages": messages,
            "stream": False,
            # 默认只检索网页模态，top_k 可按需调整
            "resource_type_filter": [{"type": "web", "top_k": 20}],
            "response_format": {"type": "text"},
            "temperature": settings.QIANFAN_TEMPERATURE,
            "top_p": settings.QIANFAN_TOP_P,
        }

        start = time.perf_counter()
        try:
            async with httpx.AsyncClient(timeout=timeout) as client:
                resp = await client.post(url, json=payload, headers=headers)

            elapsed = (time.perf_counter() - start) * 1000
            logger.info("qianfan.chat cost_ms=%.2f status=%d request_id=%s", elapsed, resp.status_code, request_id)

            if resp.status_code != 200:
                raise DependencyError(f"Qianfan API error status={resp.status_code} body={resp.text}")

            data = resp.json()
            if data.get("code"):
                raise DependencyError(
                    f"Qianfan API business error code={data.get('code')} message={data.get('message')}"
                )

            # 解析返回文本（取第一个 choice）
            choices = data.get("choices") or []
            text = ""
            if choices:
                first = choices[0]
                message = first.get("message") or {}
                text = message.get("content", "")

            model = data.get("model") or "qianfan"

            # 解析网页检索来源（search_results / references）
            web_sources: list[WebSource] = []
            search_results = data.get("search_results") or data.get("references") or []
            for ref in search_results:
                if isinstance(ref, dict):
                    web_sources.append(WebSource(
                        title=ref.get("title", ""),
                        url=ref.get("url", ref.get("link", "")),
                        snippet=ref.get("content", ref.get("snippet", "")),
                    ))

            return LLMResult(
                text=text,
                model=model,
                prompt_tokens=len(prompt),
                completion_tokens=len(text),
                web_sources=web_sources,
            )

        except httpx.RequestError as exc:
            logger.exception("qianfan request failed")
            raise DependencyError(f"Qianfan request error: {exc}") from exc
        except Exception as exc:
            logger.exception("qianfan chat failed")
            if isinstance(exc, DependencyError):
                raise
            raise DependencyError(f"Qianfan error: {exc}") from exc

    @staticmethod
    def _extract_section(text: str, heading: str) -> str:
        marker = f"{heading}\n"
        if marker not in text:
            return ""
        tail = text.split(marker, 1)[1]
        next_heading = re.search(r"\n###\s", tail)
        if next_heading:
            return tail[: next_heading.start()].strip()
        return tail.strip()

    def _build_instruction(self, prompt: str) -> str:
        system_block = ""
        if "### 识别上下文" in prompt:
            system_block = prompt.split("### 识别上下文", 1)[0].strip()
        detect_context = self._extract_section(prompt, "### 识别上下文")
        reference_block = self._extract_section(prompt, "### 参考资料（本地知识库检索结果）")

        instruction_parts = [system_block] if system_block else []
        if detect_context and detect_context != "无":
            instruction_parts.append(f"请优先结合以下识别上下文回答：{detect_context}")
        if reference_block:
            instruction_parts.append(f"可参考本地资料：\n{reference_block[:4000]}")
        if not instruction_parts:
            return "你是吉辜云羽智能助手，请用中文简洁回答用户问题。"
        return "\n\n".join(instruction_parts)

    def _build_messages(self, prompt: str) -> List[Dict[str, str]]:
        history_block = self._extract_section(prompt, "### 对话历史")
        detect_context = self._extract_section(prompt, "### 识别上下文")
        detect_context = self._clean_detect_context(detect_context)
        user_question = self._extract_section(prompt, "### 用户提问")
        if "\n\n请结合以上参考资料" in user_question:
            user_question = user_question.split("\n\n请结合以上参考资料", 1)[0].strip()
        user_question = user_question.strip()
        if not user_question:
            user_question = "请根据上文继续回答。"

        raw_turns: List[Dict[str, str]] = []
        for line in history_block.splitlines():
            if ":" not in line:
                continue
            role_raw, content = line.split(":", 1)
            role = role_raw.strip().lower()
            content = content.strip()
            if role not in {"user", "assistant"}:
                continue
            if not content or content.isspace():
                continue
            raw_turns.append({"role": role, "content": content})

        normalized: List[Dict[str, str]] = []
        for turn in raw_turns:
            if not normalized:
                if turn["role"] != "user":
                    continue
                normalized.append(turn)
                continue
            if turn["role"] == normalized[-1]["role"]:
                continue
            normalized.append(turn)

        if not normalized:
            normalized = [{"role": "user", "content": user_question}]
        elif normalized[-1]["role"] == "user":
            normalized[-1] = {"role": "user", "content": user_question}
        else:
            normalized.append({"role": "user", "content": user_question})

        # 千帆 web_summary 的检索 query 主要取最后一条 user message。
        # 对“这个是什么”这类泛化提问，补充识别上下文关键词，提升联网检索命中率。
        if detect_context and normalized and normalized[-1]["role"] == "user":
            if self._is_generic_question(normalized[-1]["content"]):
                normalized[-1]["content"] = (
                    f"{normalized[-1]['content']}\n"
                    f"识别上下文关键词：{detect_context}\n"
                    "请基于该关键词进行联网检索并回答。"
                )

        if len(normalized) % 2 == 0:
            normalized.append({"role": "user", "content": normalized[-1]["content"]})

        return normalized

    @staticmethod
    def _clean_detect_context(text: str) -> str:
        if not text:
            return ""
        cleaned = text.strip()
        cleaned = re.sub(r"^(SYSTEM|USER|ASSISTANT)\s*[:：]\s*", "", cleaned, flags=re.IGNORECASE)
        cleaned = cleaned.replace("[识别上下文]", "").replace("识别上下文", "")
        cleaned = re.sub(r"\s+", " ", cleaned).strip("：: \n\r\t")
        return cleaned

    @staticmethod
    def _is_generic_question(text: str) -> bool:
        q = (text or "").strip()
        if not q:
            return True
        generic_patterns = [
            "这个是什么",
            "这是什么",
            "它是什么",
            "这是啥",
            "啥",
            "是什么",
        ]
        if q in generic_patterns:
            return True
        return len(q) <= 8 and ("什么" in q or "啥" in q)
