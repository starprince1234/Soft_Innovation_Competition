package com.jigu.cloud.infrastructure.persistence.jpa;

import com.jigu.cloud.domain.team.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 团队 Spring Data JPA 接口。
 */
public interface TeamJpaRepository extends JpaRepository<Team, Long> {

    List<Team> findByOwnerId(Long ownerId);

    boolean existsByName(String name);
}
