package com.jigu.cloud.api.team.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 添加团队成员请求。
 */
public record AddMemberRequest(
        @NotNull(message = "用户 ID 不能为空")
        Long userId,

        /** 成员角色：MEMBER（默认）/ ADMIN */
        String role
) {}
