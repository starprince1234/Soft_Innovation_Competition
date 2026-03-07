package com.jigu.cloud.infrastructure.python.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 对话响应（Python → Java，匹配 Python DialogData schema）。
 */
public record DialogInternalResponse(
        @JsonProperty("dialog_task_id") String dialogTaskId,
        @JsonProperty("answer") String answer,
        @JsonProperty("sources") List<SourceItem> sources,
        @JsonProperty("model") String model,
        @JsonProperty("cost_ms") Float costMs
) {

    /**
     * RAG 检索来源条目（与 Python SourceItem schema 对齐）。
     */
    public record SourceItem(
            @JsonProperty("title") String title,
            @JsonProperty("url") String url,
            @JsonProperty("score") Float score,
            @JsonProperty("chunk_id") String chunkId
    ) {
    }
}
