package com.jigu.cloud.api.auth.dto.response;

import java.util.List;

public record LoginResponse(
        String token,
        long expiresIn,
        List<String> roles
) {
}