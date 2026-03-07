package com.jigu.cloud.application.feedback;

import com.jigu.cloud.common.enums.ErrorCode;
import com.jigu.cloud.common.exception.BizException;
import com.jigu.cloud.domain.feedback.Feedback;
import com.jigu.cloud.domain.feedback.FeedbackRepository;
import com.jigu.cloud.infrastructure.bos.BosClient;
import com.jigu.cloud.infrastructure.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 反馈服务：接收反馈（含截图 base64）、上传 BOS、入库。
 * <p>
 * screenshotBase64 可选：存在则上传 BOS 得到 screenshotUrl。
 * 严禁把 base64 原文持久化进数据库（仅存 URL）。
 */
@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private final FeedbackRepository feedbackRepository;
    private final BosClient bosClient;
    private final AppProperties appProperties;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           BosClient bosClient,
                           AppProperties appProperties) {
        this.feedbackRepository = feedbackRepository;
        this.bosClient = bosClient;
        this.appProperties = appProperties;
    }

    /**
     * 提交反馈。
     */
    @Transactional
    public Feedback submitFeedback(Long userId, String type, String textContent,
                                   Integer rating, String screenshotBase64) {
        Feedback feedback = new Feedback(userId, type, textContent, rating);

        // 处理截图上传
        if (screenshotBase64 != null && !screenshotBase64.isBlank()) {
            // 大小校验（Base64 编码后约 4/3 原始大小）
            long estimatedSize = (long) (screenshotBase64.length() * 3.0 / 4.0);
            if (estimatedSize > appProperties.getMaxScreenshotSize()) {
                throw new BizException(ErrorCode.SCREENSHOT_TOO_LARGE);
            }
            String screenshotUrl = bosClient.uploadBase64(screenshotBase64, "screenshot/", ".png");
            feedback.setScreenshotUrl(screenshotUrl);
        }

        feedback = feedbackRepository.save(feedback);
        log.info("Feedback submitted: id={}, userId={}, type={}", feedback.getId(), userId, type);
        return feedback;
    }
}
