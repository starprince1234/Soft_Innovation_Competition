package com.jigu.cloud.domain.feedback;

import java.util.List;
import java.util.Optional;

/**
 * 反馈仓储接口。
 */
public interface FeedbackRepository {

    Optional<Feedback> findById(Long id);

    Feedback save(Feedback feedback);

    /** 分页查询（可按状态筛选，管理端用） */
    List<Feedback> findByStatus(String status, int page, int size, String sortBy, String direction);

    List<Feedback> findAll(int page, int size, String sortBy, String direction);

    long countByStatus(String status);

    long count();
}
