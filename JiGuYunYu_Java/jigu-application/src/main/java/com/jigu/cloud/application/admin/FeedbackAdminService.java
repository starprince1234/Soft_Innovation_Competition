package com.jigu.cloud.application.admin;

import com.jigu.cloud.common.enums.ErrorCode;
import com.jigu.cloud.common.exception.BizException;
import com.jigu.cloud.common.response.PageResponse;
import com.jigu.cloud.domain.feedback.Feedback;
import com.jigu.cloud.domain.feedback.FeedbackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 反馈管理服务（分页查询、标记处理状态）。
 */
@Service
public class FeedbackAdminService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackAdminService.class);

    private final FeedbackRepository feedbackRepository;

    public FeedbackAdminService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    /**
     * 分页查询反馈列表（可按状态筛选）。
     */
    public PageResponse<Feedback> listFeedback(String status, int page, int size,
                                                  String sortBy, String direction) {
        List<Feedback> list;
        long total;
        if (status != null && !status.isBlank()) {
            list = feedbackRepository.findByStatus(status, page, size, sortBy, direction);
            total = feedbackRepository.countByStatus(status);
        } else {
            list = feedbackRepository.findAll(page, size, sortBy, direction);
            total = feedbackRepository.count();
        }
        return PageResponse.of(list, total, page, size);
    }

    /**
     * 标记反馈处理状态。
     */
    @Transactional
    public void updateFeedbackStatus(Long feedbackId, String status, Long handlerId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new BizException(ErrorCode.FEEDBACK_NOT_FOUND));
        feedback.setStatus(status);
        feedback.setHandlerId(handlerId);
        feedbackRepository.save(feedback);
        log.info("Feedback status updated: id={}, status={}, handlerId={}", feedbackId, status, handlerId);
    }
}
