package com.jigu.cloud.api.admin;

import com.jigu.cloud.application.admin.DashboardAdminService;
import com.jigu.cloud.common.response.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统概览看板接口（仅 MANAGER 角色可访问）。
 */
@RestController
@RequestMapping(path = "/api/v1/admin/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
public class DashboardAdminController {

    private final DashboardAdminService dashboardAdminService;

    public DashboardAdminController(DashboardAdminService dashboardAdminService) {
        this.dashboardAdminService = dashboardAdminService;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.ok(dashboardAdminService.getOverview());
    }
}
