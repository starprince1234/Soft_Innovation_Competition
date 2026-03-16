package com.jigu.cloud.domain.user;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储接口（领域层定义能力，infrastructure 层提供实现）。
 */
public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    User save(User user);

    /** 分页查询所有用户（管理端） */
    List<User> findAll(int page, int size, String sortBy, String direction);

    /** 按状态分页查询用户（管理端） */
    List<User> findByStatus(String status, int page, int size, String sortBy, String direction);

    /** 按用户名模糊搜索（管理端） */
    List<User> findByUsernameLike(String username, int page, int size, String sortBy, String direction);

    /** 按用户名 + 状态模糊搜索（管理端） */
    List<User> findByUsernameLikeAndStatus(String username, String status, int page, int size, String sortBy, String direction);

    long countByUsernameLike(String username);

    long countByUsernameLikeAndStatus(String username, String status);

    long countByStatus(String status);

    long count();

    /** 统计活跃用户数（最近 N 天有登录） */
    long countActiveUsers(int recentDays);
}
