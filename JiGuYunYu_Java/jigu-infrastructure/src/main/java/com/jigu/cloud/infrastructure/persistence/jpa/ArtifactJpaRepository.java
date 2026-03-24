package com.jigu.cloud.infrastructure.persistence.jpa;

import com.jigu.cloud.domain.artifact.Artifact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文物 Spring Data JPA 接口。
 */
public interface ArtifactJpaRepository extends JpaRepository<Artifact, Long> {

        boolean existsByName(String name);

        boolean existsByNameAndDeletedFalse(String name);

        Optional<Artifact> findByNameAndDeletedFalse(String name);

        Optional<Artifact> findByIdAndDeletedFalse(Long id);

        List<Artifact> findAllByIdInAndDeletedFalse(List<Long> ids);

        Page<Artifact> findByStatusAndDeletedFalse(String status, Pageable pageable);

    @Query("SELECT a FROM Artifact a WHERE "
                        + "a.deleted = false "
                        + "AND (:status IS NULL OR a.status = :status) "
            + "AND (:era IS NULL OR a.era = :era) "
            + "AND (:name IS NULL OR a.name LIKE CONCAT('%', :name, '%')) "
            + "AND (:tags IS NULL OR a.tags LIKE CONCAT('%', :tags, '%')) "
            + "AND (:keyword IS NULL OR a.name LIKE CONCAT('%', :keyword, '%') OR a.description LIKE CONCAT('%', :keyword, '%') OR a.tags LIKE CONCAT('%', :keyword, '%'))")
    Page<Artifact> findByConditions(
            @Param("status") String status,
            @Param("era") String era,
            @Param("name") String name,
            @Param("tags") String tags,
            @Param("keyword") String keyword,
            Pageable pageable);

        @Query("SELECT COUNT(a) FROM Artifact a WHERE a.deleted = false AND a.status = :status")
        long countByStatus(@Param("status") String status);

        @Query("SELECT COUNT(a) FROM Artifact a WHERE a.deleted = false AND a.status = :status AND a.createdAt >= :since")
        long countByStatusAndCreatedAtAfter(@Param("status") String status, @Param("since") LocalDateTime since);

        @Query("SELECT COUNT(a) FROM Artifact a WHERE a.deleted = false")
        long countActiveArtifacts();

        List<Artifact> findAllByStatusAndDeletedFalse(String status);

        @Modifying
        @Query("UPDATE Artifact a SET a.deleted = true WHERE a.id = :id AND a.deleted = false")
        int softDeleteById(@Param("id") Long id);
}
