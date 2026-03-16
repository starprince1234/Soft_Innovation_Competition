package com.jigu.cloud.api.admin;

import com.jigu.cloud.api.dialog.dto.response.DialogResponse;
import com.jigu.cloud.application.dialog.DialogService;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.common.response.PageResponse;
import com.jigu.cloud.domain.dialog.Dialog;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员对话历史查看（仅 MANAGER 角色可访问）。
 */
@RestController
@RequestMapping(path = "/api/v1/admin/dialog", produces = MediaType.APPLICATION_JSON_VALUE)
public class DialogAdminController {

    private final DialogService dialogService;

    public DialogAdminController(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    @GetMapping("/histories")
    public ApiResponse<PageResponse<DialogResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size,
            @RequestParam(defaultValue = "24") int recentHours) {
        PageResponse<Dialog> result = dialogService.getAllHistory(page, size, recentHours);
        PageResponse<DialogResponse> response = PageResponse.of(
                result.getList().stream().map(DialogResponse::from).toList(),
                result.getTotalElements(), result.getCurrentPage(), result.getPageSize());
        return ApiResponse.ok(response);
    }
}
