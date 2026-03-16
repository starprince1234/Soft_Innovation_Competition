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

        @Query("""
                        SELECT d FROM Dialog d
                        WHERE d.userId = :userId
                            AND d.id IN (
                                        SELECT MAX(d2.id)
                                        FROM Dialog d2
                                        WHERE d2.userId = :userId
                                        GROUP BY COALESCE(d2.conversationId, d2.id)
                            )
                        ORDER BY d.createdAt DESC, d.id DESC
                        """)
        Page<Dialog> findLatestConversationDialogsByUserId(@Param("userId") Long userId, Pageable pageable);

        @Query("SELECT COUNT(DISTINCT COALESCE(d.conversationId, d.id)) FROM Dialog d WHERE d.userId = :userId")
        long countDistinctConversationsByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(MAX(d.turnId), 0) FROM Dialog d WHERE d.userId = :userId")
    int findMaxTurnIdByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(MAX(d.turnId), 0) FROM Dialog d WHERE d.userId = :userId AND d.conversationId = :conversationId")
    int findMaxTurnIdByUserIdAndConversationId(@Param("userId") Long userId,
                                               @Param("conversationId") Long conversationId);

    @Query("SELECT COALESCE(MAX(d.conversationId), 0) FROM Dialog d WHERE d.userId = :userId")
    long findMaxConversationIdByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Dialog d WHERE d.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Dialog d WHERE d.userId = :userId AND d.artifactId = :artifactId")
    void deleteAllByUserIdAndArtifactId(@Param("userId") Long userId, @Param("artifactId") Long artifactId);

    java.util.List<Dialog> findByUserIdAndConversationIdOrderByCreatedAtAsc(Long userId, Long conversationId);

    Page<Dialog> findByTeamIdOrderByCreatedAtDesc(Long teamId, Pageable pageable);

    Page<Dialog> findByCreatedAtAfterOrderByCreatedAtDesc(java.time.LocalDateTime since, Pageable pageable);

    long countByTeamId(Long teamId);

    long countByCreatedAtAfter(java.time.LocalDateTime since);
}
