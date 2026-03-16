package com.jigu.cloud.api.artifact.dto.response;

import com.jigu.cloud.domain.artifact.Artifact;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 文物对外展示 VO。
 */
public record ArtifactResponse(
        Long id,
        String name,
        String category,
        String era,
        String imageUrl,
        String description,
        String thumbnailUrl,
        List<String> tags,
        String location,
        String status,
        LocalDateTime createdAt
) {
    public static ArtifactResponse from(Artifact a) {
        List<String> tags = parseTags(a.getTags());
        String category = resolveCategory(tags);
        return new ArtifactResponse(
                a.getId(), a.getName(), category, a.getEra(), a.getImageUrl(), a.getDescription(),
                a.getThumbnailUrl(), tags, a.getLocation(),
                a.getStatus(), a.getCreatedAt()
        );
    }

    private static List<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawTags.split("[,，\\s]+"))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .toList();
    }

    private static String resolveCategory(List<String> tags) {
        if (tags.stream().anyMatch(t -> t.contains("青铜"))) return "青铜";
        if (tags.stream().anyMatch(t -> t.contains("陶"))) return "陶器";
        if (tags.stream().anyMatch(t -> t.contains("玉"))) return "玉器";
        if (tags.stream().anyMatch(t -> t.contains("书画") || t.contains("画"))) return "书画";
        return tags.isEmpty() ? "其他" : tags.get(0);
    }
}
