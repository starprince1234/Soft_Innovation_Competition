package com.jigu.cloud.api.user.dto.response;

import com.jigu.cloud.domain.user.User;

import java.time.LocalDateTime;

/**
 * 用户信息响应 VO（不暴露 passwordHash 等敏感字段）。
 */
public record UserResponse(
        Long id,
        String username,
        String role,
        String status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(), u.getUsername(), u.getRole(),
                u.getStatus(), u.getCreatedAt(), u.getLastLoginAt()
        );
    }
}
