package com.jigu.cloud.api.artifact.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 文物创建/更新请求。
 */
public record ArtifactRequest(
        @NotBlank(message = "文物名称不能为空") String name,
        String description,
        String imageUrl,
        String thumbnailUrl,
        String tags,
        String location,
        String era,
        String status
) {
}
