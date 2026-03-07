package com.jigu.cloud.api.artifact.dto.response;

import com.jigu.cloud.domain.artifact.Artifact;

import java.time.LocalDateTime;

/**
 * 文物对外展示 VO。
 */
public record ArtifactResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        String thumbnailUrl,
        String tags,
        String location,
        String era,
        String status,
        LocalDateTime createdAt
) {
    public static ArtifactResponse from(Artifact a) {
        return new ArtifactResponse(
                a.getId(), a.getName(), a.getDescription(),
                a.getImageUrl(), a.getThumbnailUrl(),
                a.getTags(), a.getLocation(), a.getEra(),
                a.getStatus(), a.getCreatedAt()
        );
    }
}
