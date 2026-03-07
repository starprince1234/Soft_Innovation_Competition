package com.jigu.cloud.api.auth;

import com.jigu.cloud.api.auth.dto.request.LoginRequest;
import com.jigu.cloud.api.auth.dto.request.RegisterRequest;
import com.jigu.cloud.api.auth.dto.response.LoginResponse;
import com.jigu.cloud.application.auth.AuthService;
import com.jigu.cloud.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口：POST /auth/register、POST /auth/login、POST /auth/logout。
 */
@RestController
@RequestMapping(path = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req.username(), req.password());
        return ApiResponse.ok();
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthService.LoginResult result = authService.login(req.username(), req.password());
        LoginResponse response = new LoginResponse(result.token(), result.expiresIn(), result.roles());
        return ApiResponse.ok(response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            authService.logout(token);
        }
        return ApiResponse.ok();
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
