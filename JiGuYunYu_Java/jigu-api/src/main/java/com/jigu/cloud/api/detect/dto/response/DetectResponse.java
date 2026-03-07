package com.jigu.cloud.api.detect.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jigu.cloud.domain.detect.DetectionTask;

import java.util.ArrayList;
import java.util.List;

/**
 * 检测任务响应 VO。
 * <p>
 * 与文档保持一致：
 * - createTask / getTaskStatus → 仅返回 taskId + status
 * - getTaskResult → 返回 taskId + status + imageUrl + detectedArtifacts[]
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DetectResponse(
        Long taskId,
        String status,
        String imageUrl,
        List<DetectedArtifact> detectedArtifacts
) {

    /**
     * 检测到的文物条目。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DetectedArtifact(
            String label,
            Float confidence,
            List<Number> bbox,
            Long artifactId
    ) {
    }

    /**
     * 创建任务 / 查询状态（精简响应）。
     */
    public static DetectResponse fromStatus(DetectionTask t) {
        return new DetectResponse(t.getId(), t.getStatus(), null, null);
    }

    /**
     * 查询结果（完整响应，含 detectedArtifacts 和 imageUrl）。
     */
    public static DetectResponse fromResult(DetectionTask t, Long matchedArtifactId) {
        List<DetectedArtifact> artifacts = null;
        if (t.getDetectedLabel() != null) {
            List<Number> bboxList = parseBbox(t.getBbox());
            artifacts = List.of(new DetectedArtifact(
                    t.getDetectedLabel(), t.getConfidence(), bboxList, matchedArtifactId));
        }
        return new DetectResponse(t.getId(), t.getStatus(), t.getImageUrl(), artifacts);
    }

    /**
     * 解析 bbox JSON 字符串 "[x1, y1, x2, y2]" 为 List&lt;Number&gt;。
     */
    private static List<Number> parseBbox(String bbox) {
        if (bbox == null || bbox.isBlank()) return null;
        try {
            String stripped = bbox.replaceAll("[\\[\\]\\s]", "");
            if (stripped.isEmpty()) return null;
            String[] parts = stripped.split(",");
            List<Number> result = new ArrayList<>();
            for (String part : parts) {
                result.add(Float.parseFloat(part.trim()));
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }
}
