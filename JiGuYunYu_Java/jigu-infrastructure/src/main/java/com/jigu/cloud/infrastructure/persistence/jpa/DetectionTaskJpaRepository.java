package com.jigu.cloud.infrastructure.persistence.jpa;

import com.jigu.cloud.domain.detect.DetectionTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

/**
 * 检测任务 Spring Data JPA 接口。
 */
public interface DetectionTaskJpaRepository extends JpaRepository<DetectionTask, Long> {

	long countByStatus(String status);

	long countByCreatedAtAfter(LocalDateTime since);
}
