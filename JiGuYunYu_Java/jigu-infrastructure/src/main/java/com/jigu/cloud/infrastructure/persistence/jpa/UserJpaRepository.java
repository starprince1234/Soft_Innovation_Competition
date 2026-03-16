package com.jigu.cloud.infrastructure.persistence.jpa;

import com.jigu.cloud.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data JPA 接口（仅 infrastructure 内部使用）。
 * <p>
 * 禁止 application 直接注入。
 */
public interface UserJpaRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginAt >= :since")
    long countActiveUsersSince(@Param("since") LocalDateTime since);

    long countByStatus(String status);

    Page<User> findByStatus(String status, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.username LIKE CONCAT('%', :username, '%')")
    Page<User> findByUsernameLike(@Param("username") String username, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.username LIKE CONCAT('%', :username, '%') AND u.status = :status")
    Page<User> findByUsernameLikeAndStatus(@Param("username") String username,
                                           @Param("status") String status,
                                           Pageable pageable);
}
