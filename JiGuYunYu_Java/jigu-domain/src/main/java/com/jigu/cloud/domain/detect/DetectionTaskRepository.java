package com.jigu.cloud.domain.detect;

import java.util.Optional;

/**
 * 检测任务仓储接口。
 */
public interface DetectionTaskRepository {

    Optional<DetectionTask> findById(Long id);

    DetectionTask save(DetectionTask task);
}
