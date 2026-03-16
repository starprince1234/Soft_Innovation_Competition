package com.jigu.cloud.api.dialog.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 对话请求。
 */
public record DialogRequest(
        @NotBlank(message = "查询内容不能为空") String query,
        Long artifactId,
        Long conversationId,
        List<ContextTurn> contextHistory
) {
    public record ContextTurn(
            String role,
            String content
    ) {
    }
}
