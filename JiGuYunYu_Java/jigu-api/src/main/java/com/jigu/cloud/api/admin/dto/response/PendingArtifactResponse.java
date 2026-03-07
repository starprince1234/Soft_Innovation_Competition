package com.jigu.cloud.api.admin.dto.response;

import com.jigu.cloud.domain.artifact.Artifact;

import java.time.LocalDateTime;

/**
 * 待审核文物响应 VO（含提交者用户名）。
 */
public record PendingArtifactResponse(
        Long id,
        String name,
        String creator,
        String status,
        LocalDateTime submittedAt,
        String thumbnailUrl
) {
    public static PendingArtifactResponse from(Artifact a, String creatorUsername) {
        return new PendingArtifactResponse(
                a.getId(),
                a.getName(),
                creatorUsername,
                a.getStatus(),
                a.getCreatedAt(),
                a.getThumbnailUrl()
        );
    }
}
