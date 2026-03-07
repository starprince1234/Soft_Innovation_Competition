package com.jigu.cloud.infrastructure.python.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 检测处理响应（Python → Java，匹配 Python DetectData schema）。
 * <p>
 * Python 返回 detections 数组，Java 在 Service 层取最优结果。
 */
public record DetectProcessResponse(
        @JsonProperty("task_id") String taskId,
        @JsonProperty("image_url") String imageUrl,
        @JsonProperty("detections") List<DetectedItem> detections,
        @JsonProperty("raw_result") String rawResult
) {

    public record DetectedItem(
            @JsonProperty("label") String label,
            @JsonProperty("confidence") Float confidence,
            @JsonProperty("bbox") List<Float> bbox
    ) {
    }
}
