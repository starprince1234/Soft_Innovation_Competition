package com.jigu.cloud.infrastructure.python.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 检测处理请求（Java → Python /internal/detect/process）。
 * <p>
 * 同时传递 imageUrl（BOS 链接）和 imageBase64（原始 Base64），
 * Python 端优先使用 base64 数据，BOS 不可达时仍能正常检测。
 */
public record DetectProcessRequest(
        @JsonProperty("task_id") Long taskId,
        @JsonProperty("image_url") String imageUrl,
        @JsonProperty("image_base64") String imageBase64
) implements PythonRequest {
}
