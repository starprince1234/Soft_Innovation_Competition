package com.jigu.cloud.infrastructure.persistence.adapter;

import com.jigu.cloud.domain.feedback.Feedback;
import com.jigu.cloud.domain.feedback.FeedbackRepository;
import com.jigu.cloud.infrastructure.persistence.jpa.FeedbackJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * FeedbackRepository 适配器。
 */
@Repository
public class FeedbackRepositoryImpl implements FeedbackRepository {

    private final FeedbackJpaRepository jpaRepository;

    public FeedbackRepositoryImpl(FeedbackJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Feedback> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Feedback save(Feedback feedback) {
        return jpaRepository.save(feedback);
    }

    @Override
    public List<Feedback> findByStatus(String status, int page, int size, String sortBy, String direction) {
        PageRequest pageable = PageRequest.of(page, size, buildSort(sortBy, direction));
        return jpaRepository.findByStatus(status, pageable).getContent();
    }

    @Override
    public List<Feedback> findAll(int page, int size, String sortBy, String direction) {
        PageRequest pageable = PageRequest.of(page, size, buildSort(sortBy, direction));
        return jpaRepository.findAll(pageable).getContent();
    }

    @Override
    public long countByStatus(String status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    private Sort buildSort(String sortBy, String direction) {
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, sortBy);
    }
}
