"""VLM 文物检测器（Qwen3-VL-LoRA，实现 DetectorPort）

基于 OpenAI-compatible API 调用视觉语言模型进行文物识别。
模型通过分析图片内容识别文物名称、类别及描述信息。
连接配置通过 VLM_BASE_URL / VLM_API_KEY / VLM_MODEL_NAME 环境变量注入。
"""

from __future__ import annotations

import base64
import json
import logging
import re
from typing import Optional

import httpx

from app.domain.models.detection import Detection, DetectionResult
from app.domain.ports.detector import DetectorPort
from app.exceptions import DependencyError

logger = logging.getLogger(__name__)

# ---- 识别提示词 ----
_DETECT_PROMPT = """你是一位专业的中国文物鉴定专家。请仔细观察图片中的物品，完成以下任务：
1. 判断图中是否包含文物
2. 如果是文物，识别其名称、类别和大致年代
3. 给出你的判断置信度（0.0-1.0）

请严格按照以下 JSON 格式返回结果，不要添加任何额外文字或 markdown 标记：
{"name": "文物名称", "confidence": 0.95, "category": "类别(如青铜器/陶瓷/玉器等)", "era": "大致年代", "description": "简要描述（50字以内）"}

如果图中没有文物，返回：
{"name": "未识别", "confidence": 0.0, "category": "未知", "era": "未知", "description": "图片中未发现可识别的文物"}"""


class VLMDetector(DetectorPort):
    """基于视觉语言模型的文物检测器（OpenAI-compatible API）"""

    def __init__(
        self,
        base_url: str,
        api_key: str = "sk-123456",
        model: str = "qwen3-vl-lora",
        timeout: float = 60.0,
    ) -> None:
        self._base_url = base_url.rstrip("/")
        self._api_key = api_key
        self._model = model
        self._timeout = timeout

    async def detect(self, image_bytes: bytes) -> DetectionResult:
        """
        将图片发送到 VLM 模型进行文物识别。

        Args:
            image_bytes: 图片的原始字节数据（JPEG/PNG）。

        Returns:
            DetectionResult: 包含识别标签、置信度的检测结果。
        """
        if not image_bytes:
            raise DependencyError("image_bytes is empty, cannot run VLM detection")

        base64_image = base64.b64encode(image_bytes).decode("utf-8")

        url = f"{self._base_url}/chat/completions"
        headers = {
            "Authorization": f"Bearer {self._api_key}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": self._model,
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": _DETECT_PROMPT},
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": f"data:image/jpeg;base64,{base64_image}",
                            },
                        },
                    ],
                }
            ],
            "temperature": 0.6,
            "top_p": 0.8,
            "max_tokens": 1024,
            "stop": ["<|im_end|>", "<|endoftext|>", "<|end|>"],
        }

        try:
            async with httpx.AsyncClient(timeout=self._timeout) as client:
                resp = await client.post(url, json=payload, headers=headers)

            if resp.status_code != 200:
                raise DependencyError(
                    f"VLM API error: status={resp.status_code} body={resp.text[:500]}"
                )

            data = resp.json()
            raw_text = self._extract_text(data)

            logger.info(
                "vlm.detect  model=%s raw_length=%d", self._model, len(raw_text)
            )

            # 尝试解析结构化 JSON 响应
            parsed = self._parse_vlm_response(raw_text)

            detection = Detection(
                label=parsed.get("name", "未识别"),
                confidence=float(parsed.get("confidence", 0.0)),
                bbox=[],  # VLM 不返回边界框
            )

            return DetectionResult(
                detections=[detection] if detection.confidence > 0 else [],
                raw={
                    "vlm_response": raw_text,
                    "category": parsed.get("category", ""),
                    "era": parsed.get("era", ""),
                    "description": parsed.get("description", ""),
                },
            )

        except httpx.RequestError as exc:
            logger.exception("vlm.detect request failed")
            raise DependencyError(f"VLM request error: {exc}") from exc
        except DependencyError:
            raise
        except Exception as exc:
            logger.exception("vlm.detect unexpected error")
            raise DependencyError(f"VLM detection error: {exc}") from exc

    async def health(self) -> bool:
        """检查 VLM 服务是否可达（调用 /models 端点）"""
        try:
            url = f"{self._base_url}/models"
            headers = {"Authorization": f"Bearer {self._api_key}"}
            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.get(url, headers=headers)
            return resp.status_code == 200
        except Exception as exc:
            logger.warning("vlm health check failed: %s", exc)
            return False

    # ---- 内部方法 ----

    @staticmethod
    def _extract_text(data: dict) -> str:
        """从 OpenAI-compatible API 响应中提取文本内容"""
        choices = data.get("choices") or []
        if not choices:
            return ""
        message = choices[0].get("message") or choices[0].get("delta") or {}
        return message.get("content", "")

    @staticmethod
    def _parse_vlm_response(raw_text: str) -> dict:
        """
        解析 VLM 返回的文本，尝试提取 JSON 结构化数据。
        支持多种格式：纯 JSON、markdown 包裹的 JSON、或纯文本。
        """
        text = raw_text.strip()

        # 1. 尝试直接解析 JSON
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            pass

        # 2. 尝试从 markdown 代码块中提取 JSON
        json_match = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", text, re.DOTALL)
        if json_match:
            try:
                return json.loads(json_match.group(1))
            except json.JSONDecodeError:
                pass

        # 3. 尝试从文本中提取第一个 JSON 对象
        brace_match = re.search(r"\{[^{}]*\}", text, re.DOTALL)
        if brace_match:
            try:
                return json.loads(brace_match.group(0))
            except json.JSONDecodeError:
                pass

        # 4. 兜底：用原始文本作为 label
        logger.warning("vlm response is not structured JSON, using raw text as label")
        # 尝试提取第一行非空文本作为名称
        first_line = text.split("\n")[0].strip()[:100]
        return {
            "name": first_line or "未识别",
            "confidence": 0.5,
            "category": "未知",
            "era": "未知",
            "description": text[:200],
        }
