package com.jigu.cloud.api.team.dto.response;

import com.jigu.cloud.domain.team.Team;

import java.time.LocalDateTime;

/**
 * 团队响应 VO。
 */
public record TeamResponse(
        Long id,
        String name,
        Long ownerId,
        LocalDateTime createdAt
) {
    public static TeamResponse from(Team t) {
        return new TeamResponse(t.getId(), t.getName(), t.getOwnerId(), t.getCreatedAt());
    }
}
