package com.jigu.cloud.api.feedback.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 反馈提交请求。
 */
public record FeedbackRequest(
        String type,
        @NotBlank(message = "反馈内容不能为空") String textContent,
        Integer rating,
        String screenshotBase64
) {
}
