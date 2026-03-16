package com.jigu.cloud.domain.detect;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 检测任务仓储接口。
 */
public interface DetectionTaskRepository {

    Optional<DetectionTask> findById(Long id);

    DetectionTask save(DetectionTask task);

    long count();

    long countByStatus(String status);

    long countByCreatedAfter(LocalDateTime since);
}
