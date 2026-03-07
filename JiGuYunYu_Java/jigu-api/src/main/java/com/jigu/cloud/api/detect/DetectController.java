package com.jigu.cloud.api.detect;

import com.jigu.cloud.api.detect.dto.request.DetectRequest;
import com.jigu.cloud.api.detect.dto.response.DetectResponse;
import com.jigu.cloud.application.detect.DetectService;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.domain.detect.DetectionTask;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 检测接口：POST /detect/tasks、GET /detect/tasks/{taskId}/status、GET /detect/results/{taskId}。
 */
@RestController
@RequestMapping(path = "/api/v1/detect", produces = MediaType.APPLICATION_JSON_VALUE)
public class DetectController {

    private final DetectService detectService;

    public DetectController(DetectService detectService) {
        this.detectService = detectService;
    }

    @PostMapping("/tasks")
    public ApiResponse<DetectResponse> createTask(@Valid @RequestBody DetectRequest req,
                                                  HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        DetectionTask task = detectService.createTask(userId, req.imageBase64());
        return ApiResponse.ok(DetectResponse.fromStatus(task));
    }

    @GetMapping("/tasks/{taskId}/status")
    public ApiResponse<DetectResponse> getTaskStatus(@PathVariable Long taskId,
                                                     HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        String role = (String) httpReq.getAttribute("role");
        DetectionTask task = detectService.getTaskStatus(taskId, userId, role);
        return ApiResponse.ok(DetectResponse.fromStatus(task));
    }

    @GetMapping("/results/{taskId}")
    public ApiResponse<DetectResponse> getTaskResult(@PathVariable Long taskId,
                                                     HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        String role = (String) httpReq.getAttribute("role");
        DetectionTask task = detectService.getTaskResult(taskId, userId, role);
        Long matchedArtifactId = detectService.resolveArtifactId(task.getDetectedLabel());
        return ApiResponse.ok(DetectResponse.fromResult(task, matchedArtifactId));
    }
}
