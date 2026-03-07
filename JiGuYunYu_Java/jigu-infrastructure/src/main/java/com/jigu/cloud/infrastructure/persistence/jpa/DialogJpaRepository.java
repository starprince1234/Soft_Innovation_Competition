package com.jigu.cloud.infrastructure.persistence.jpa;

import com.jigu.cloud.domain.dialog.Dialog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 对话记录 Spring Data JPA 接口。
 */
public interface DialogJpaRepository extends JpaRepository<Dialog, Long> {

    Page<Dialog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    @Query("SELECT COALESCE(MAX(d.turnId), 0) FROM Dialog d WHERE d.userId = :userId")
    int findMaxTurnIdByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Dialog d WHERE d.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Dialog d WHERE d.userId = :userId AND d.artifactId = :artifactId")
    void deleteAllByUserIdAndArtifactId(@Param("userId") Long userId, @Param("artifactId") Long artifactId);

    Page<Dialog> findByTeamIdOrderByCreatedAtDesc(Long teamId, Pageable pageable);

    long countByTeamId(Long teamId);
}
