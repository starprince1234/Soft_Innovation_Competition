package com.jigu.cloud.application.admin;

import com.jigu.cloud.domain.artifact.ArtifactRepository;
import com.jigu.cloud.domain.detect.DetectionTaskRepository;
import com.jigu.cloud.domain.dialog.DialogRepository;
import com.jigu.cloud.domain.feedback.FeedbackRepository;
import com.jigu.cloud.domain.user.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统概览数据聚合服务。
 * <p>
 * 统计类接口读库聚合，高频指标可后续加 Redis 缓存快照。
 */
@Service
public class DashboardAdminService {

    private final UserRepository userRepository;
    private final ArtifactRepository artifactRepository;
    private final DetectionTaskRepository detectionTaskRepository;
    private final FeedbackRepository feedbackRepository;
    private final DialogRepository dialogRepository;

    public DashboardAdminService(UserRepository userRepository,
                                 ArtifactRepository artifactRepository,
                                 DetectionTaskRepository detectionTaskRepository,
                                 FeedbackRepository feedbackRepository,
                                 DialogRepository dialogRepository) {
        this.userRepository = userRepository;
        this.artifactRepository = artifactRepository;
        this.detectionTaskRepository = detectionTaskRepository;
        this.feedbackRepository = feedbackRepository;
        this.dialogRepository = dialogRepository;
    }

    /**
     * 获取系统概览数据。
     */
    public Map<String, Object> getOverview() {
        LocalDateTime since24h = LocalDateTime.now().minusHours(24);
        long completedDetect = detectionTaskRepository.countByStatus("COMPLETED");
        long failedDetect = detectionTaskRepository.countByStatus("FAILED");
        long finishedDetect = completedDetect + failedDetect;

        Map<String, Object> overview = new LinkedHashMap<>();
        // 与用户管理使用同一 users 表口径：总用户=全部用户，24h活跃=ACTIVE状态用户。
        overview.put("totalUsers", userRepository.count());
        overview.put("activeUsersLast24h", userRepository.countByStatus("ACTIVE"));
        overview.put("totalArtifacts", artifactRepository.count());
        overview.put("pendingArtifactsForReview", artifactRepository.countByStatus("PENDING"));
        overview.put("totalFeedback", feedbackRepository.count());
        overview.put("unresolvedFeedback", feedbackRepository.countByStatus("PENDING"));
        overview.put("detectionSuccessRate", finishedDetect == 0 ? 0.0d : (double) completedDetect / finishedDetect);
        overview.put("dialogCountLast24h", dialogRepository.countByCreatedAfter(since24h));
        return overview;
    }
}
