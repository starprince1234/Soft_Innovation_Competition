package com.jigu.cloud.application.admin;

import com.jigu.cloud.domain.artifact.ArtifactRepository;
import com.jigu.cloud.domain.dialog.DialogRepository;
import com.jigu.cloud.domain.feedback.FeedbackRepository;
import com.jigu.cloud.domain.user.UserRepository;
import org.springframework.stereotype.Service;

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
    private final FeedbackRepository feedbackRepository;
    private final DialogRepository dialogRepository;

    public DashboardAdminService(UserRepository userRepository,
                                 ArtifactRepository artifactRepository,
                                 FeedbackRepository feedbackRepository,
                                 DialogRepository dialogRepository) {
        this.userRepository = userRepository;
        this.artifactRepository = artifactRepository;
        this.feedbackRepository = feedbackRepository;
        this.dialogRepository = dialogRepository;
    }

    /**
     * 获取系统概览数据。
     */
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalUsers", userRepository.count());
        overview.put("activeUsers7d", userRepository.countActiveUsers(7));
        overview.put("totalArtifacts", artifactRepository.count());
        overview.put("pendingArtifacts", artifactRepository.countByStatus("PENDING"));
        overview.put("totalFeedback", feedbackRepository.count());
        overview.put("pendingFeedback", feedbackRepository.countByStatus("PENDING"));
        overview.put("totalDialogs", dialogRepository.count());
        return overview;
    }
}
