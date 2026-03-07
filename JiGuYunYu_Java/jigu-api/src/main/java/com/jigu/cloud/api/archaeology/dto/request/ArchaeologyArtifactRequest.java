package com.jigu.cloud.api.archaeology.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 考古人员提交文物请求。
 */
public record ArchaeologyArtifactRequest(
        @NotBlank(message = "文物名称不能为空") String name,
        String description,
        String imageBase64,
        String location,
        String era,
        List<String> tags,
        String notes
) {
}
