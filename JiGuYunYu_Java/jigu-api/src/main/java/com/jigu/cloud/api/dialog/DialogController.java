package com.jigu.cloud.api.dialog;

import com.jigu.cloud.api.dialog.dto.request.DialogRequest;
import com.jigu.cloud.api.dialog.dto.response.DialogResponse;
import com.jigu.cloud.application.dialog.DialogService;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.common.response.PageResponse;
import com.jigu.cloud.domain.dialog.Dialog;
import com.jigu.cloud.infrastructure.python.dto.DialogInternalRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 对话接口：POST /dialog/requests、GET /dialog/results/{id}、
 * GET /dialog/histories、DELETE /dialog/histories。
 */
@RestController
@RequestMapping(path = "/api/v1/dialog", produces = MediaType.APPLICATION_JSON_VALUE)
public class DialogController {

    private final DialogService dialogService;

    public DialogController(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    @PostMapping("/requests")
    public ApiResponse<DialogResponse> createDialog(@Valid @RequestBody DialogRequest req,
                                                    HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        String role = (String) httpReq.getAttribute("role");

        // 转换 contextHistory
        List<DialogInternalRequest.ContextTurn> contextHistory = req.contextHistory() != null
                ? req.contextHistory().stream()
                    .map(ct -> new DialogInternalRequest.ContextTurn(ct.role(), ct.content()))
                    .toList()
                : Collections.emptyList();

        Dialog dialog = dialogService.createDialog(userId, role, req.query(), req.artifactId(), contextHistory);
        return ApiResponse.ok(DialogResponse.from(dialog));
    }

    @GetMapping("/results/{id}")
    public ApiResponse<DialogResponse> getResult(@PathVariable Long id,
                                                  HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        String role = (String) httpReq.getAttribute("role");
        Dialog dialog = dialogService.getById(id, userId, role);
        return ApiResponse.ok(DialogResponse.from(dialog));
    }

    @GetMapping("/histories")
    public ApiResponse<PageResponse<DialogResponse>> getHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        PageResponse<Dialog> result = dialogService.getHistory(userId, page, size);
        PageResponse<DialogResponse> response = PageResponse.of(
                result.getList().stream().map(DialogResponse::from).toList(),
                result.getTotalElements(), result.getCurrentPage(), result.getPageSize());
        return ApiResponse.ok(response);
    }

    @DeleteMapping("/histories")
    public ApiResponse<Void> clearHistories(
            @RequestParam(required = false) Long artifactId,
            HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        dialogService.clearHistory(userId, artifactId);
        return ApiResponse.ok();
    }
}
