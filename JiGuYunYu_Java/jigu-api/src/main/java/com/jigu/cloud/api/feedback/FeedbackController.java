package com.jigu.cloud.api.feedback;

import com.jigu.cloud.api.feedback.dto.request.FeedbackRequest;
import com.jigu.cloud.api.feedback.dto.response.FeedbackResponse;
import com.jigu.cloud.application.feedback.FeedbackService;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.domain.feedback.Feedback;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 反馈接口：POST /feedback。
 */
@RestController
@RequestMapping(path = "/api/v1/feedback", produces = MediaType.APPLICATION_JSON_VALUE)
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ApiResponse<FeedbackResponse> submit(@Valid @RequestBody FeedbackRequest req,
                                                HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        Feedback feedback = feedbackService.submitFeedback(
                userId, req.type(), req.textContent(), req.rating(), req.screenshotBase64());
        return ApiResponse.ok(new FeedbackResponse(feedback.getId()));
    }
}
