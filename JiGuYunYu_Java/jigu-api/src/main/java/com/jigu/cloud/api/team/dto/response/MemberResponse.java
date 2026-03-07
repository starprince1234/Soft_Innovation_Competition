package com.jigu.cloud.api.team.dto.response;

import com.jigu.cloud.domain.team.UserTeamMembership;

import java.time.LocalDateTime;

/**
 * 团队成员响应 VO。
 */
public record MemberResponse(
        Long id,
        Long userId,
        Long teamId,
        String role,
        LocalDateTime joinedAt
) {
    public static MemberResponse from(UserTeamMembership m) {
        return new MemberResponse(m.getId(), m.getUserId(), m.getTeamId(), m.getRole(), m.getJoinedAt());
    }
}
