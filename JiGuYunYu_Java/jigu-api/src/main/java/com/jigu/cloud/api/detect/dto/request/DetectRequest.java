package com.jigu.cloud.api.detect.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 检测任务创建请求（仅接收 base64）。
 */
public record DetectRequest(
        @NotBlank(message = "图片数据不能为空") String imageBase64
) {
}
