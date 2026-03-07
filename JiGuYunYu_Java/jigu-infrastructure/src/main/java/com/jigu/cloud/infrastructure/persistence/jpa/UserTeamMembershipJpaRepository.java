package com.jigu.cloud.infrastructure.persistence.jpa;

import com.jigu.cloud.domain.team.UserTeamMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用户-团队关联 Spring Data JPA 接口。
 */
public interface UserTeamMembershipJpaRepository extends JpaRepository<UserTeamMembership, Long> {

    boolean existsByUserIdAndTeamId(Long userId, Long teamId);

    Optional<UserTeamMembership> findByUserIdAndTeamId(Long userId, Long teamId);

    List<UserTeamMembership> findByTeamId(Long teamId);

    @Query("SELECT m.teamId FROM UserTeamMembership m WHERE m.userId = :userId")
    List<Long> findTeamIdsByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndTeamId(Long userId, Long teamId);
}
