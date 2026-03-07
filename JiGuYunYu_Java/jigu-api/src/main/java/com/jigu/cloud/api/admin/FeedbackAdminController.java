package com.jigu.cloud.api.admin;

import com.jigu.cloud.application.admin.FeedbackAdminService;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.common.response.PageResponse;
import com.jigu.cloud.domain.feedback.Feedback;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 反馈管理接口（仅 MANAGER 角色可访问）。
 */
@RestController
@RequestMapping(path = "/api/v1/admin/feedback", produces = MediaType.APPLICATION_JSON_VALUE)
public class FeedbackAdminController {

    private final FeedbackAdminService feedbackAdminService;

    public FeedbackAdminController(FeedbackAdminService feedbackAdminService) {
        this.feedbackAdminService = feedbackAdminService;
    }

    @GetMapping
    public ApiResponse<PageResponse<Feedback>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.ok(feedbackAdminService.listFeedback(status, page, size, sortBy, direction));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          HttpServletRequest httpReq) {
        Long handlerId = (Long) httpReq.getAttribute("userId");
        feedbackAdminService.updateFeedbackStatus(id, body.get("status"), handlerId);
        return ApiResponse.ok(null);
    }
}
