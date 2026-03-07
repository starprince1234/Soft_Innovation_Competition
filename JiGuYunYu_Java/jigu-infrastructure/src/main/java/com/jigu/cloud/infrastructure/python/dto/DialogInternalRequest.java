package com.jigu.cloud.infrastructure.python.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 对话请求（Java → Python /internal/dialog/responses）。
 * <p>
 * 字段名严格与 Python DialogRequest schema 对齐（snake_case，
 * Python 通过 populate_by_name=True 同时接受 field name 和 alias）。
 */
public record DialogInternalRequest(
        @JsonProperty("dialog_task_id") String dialogTaskId,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("user_role") String userRole,
        @JsonProperty("query") String query,
        @JsonProperty("artifact_id") Long artifactId,
        @JsonProperty("context_history") List<ContextTurn> contextHistory,
        @JsonProperty("top_k") Integer topK,
        @JsonProperty("local_rag_confidence_threshold") Float localRagConfidenceThreshold
) implements PythonRequest {

    public record ContextTurn(
            @JsonProperty("role") String role,
            @JsonProperty("content") String content
    ) {
    }
}
