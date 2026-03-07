package com.jigu.cloud.api.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新团队请求。
 */
public record UpdateTeamRequest(
        @NotBlank(message = "团队名称不能为空")
        @Size(min = 2, max = 50, message = "团队名称长度 2-50 字符")
        String name
) {}
