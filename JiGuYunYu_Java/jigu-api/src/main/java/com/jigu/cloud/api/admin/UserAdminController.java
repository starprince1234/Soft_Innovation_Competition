package com.jigu.cloud.api.admin;

import com.jigu.cloud.api.user.dto.response.UserResponse;
import com.jigu.cloud.application.admin.UserAdminService;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.common.response.PageResponse;
import com.jigu.cloud.domain.user.User;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理接口（仅 MANAGER 角色可访问）。
 * <p>
 * 支持按 username 模糊搜索，返回 UserResponse DTO（不暴露 passwordHash）。
 */
@RestController
@RequestMapping(path = "/api/v1/admin/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        PageResponse<User> users = userAdminService.listUsers(username, page, size, sortBy, direction);
        PageResponse<UserResponse> response = PageResponse.of(
                users.getList().stream().map(UserResponse::from).toList(),
                users.getTotalElements(), users.getCurrentPage(), users.getPageSize());
        return ApiResponse.ok(response);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        userAdminService.updateUserStatus(id, body.get("status"));
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/role")
    public ApiResponse<Void> updateRole(@PathVariable Long id,
                                        @RequestBody Map<String, String> body) {
        userAdminService.updateUserRole(id, body.get("role"));
        return ApiResponse.ok(null);
    }
}
