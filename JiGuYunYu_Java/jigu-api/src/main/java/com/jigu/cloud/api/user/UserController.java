package com.jigu.cloud.api.user;

import com.jigu.cloud.api.user.dto.request.ChangePasswordRequest;
import com.jigu.cloud.api.user.dto.response.UserResponse;
import com.jigu.cloud.application.auth.AuthService;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.domain.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 当前用户接口：GET /users/me、PUT /users/me/password。
 */
@RestController
@RequestMapping(path = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        User user = authService.getCurrentUser(userId);
        return ApiResponse.ok(UserResponse.from(user));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                            HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        authService.changePassword(userId, req.oldPassword(), req.newPassword());
        return ApiResponse.ok();
    }
}
