package com.jigu.cloud.infrastructure.persistence.adapter;

import com.jigu.cloud.domain.detect.DetectionTask;
import com.jigu.cloud.domain.detect.DetectionTaskRepository;
import com.jigu.cloud.infrastructure.persistence.jpa.DetectionTaskJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * DetectionTaskRepository 适配器。
 */
@Repository
public class DetectionTaskRepositoryImpl implements DetectionTaskRepository {

    private final DetectionTaskJpaRepository jpaRepository;

    public DetectionTaskRepositoryImpl(DetectionTaskJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<DetectionTask> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public DetectionTask save(DetectionTask task) {
        return jpaRepository.save(task);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByStatus(String status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long countByCreatedAfter(LocalDateTime since) {
        return jpaRepository.countByCreatedAtAfter(since);
    }
}
