package com.jigu.cloud.infrastructure.persistence.jpa;

import com.jigu.cloud.domain.feedback.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 反馈 Spring Data JPA 接口。
 */
public interface FeedbackJpaRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);
}
