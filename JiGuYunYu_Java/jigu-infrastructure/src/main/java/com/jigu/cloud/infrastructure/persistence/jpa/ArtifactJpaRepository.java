package com.jigu.cloud.infrastructure.persistence.jpa;

import com.jigu.cloud.domain.artifact.Artifact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 文物 Spring Data JPA 接口。
 */
public interface ArtifactJpaRepository extends JpaRepository<Artifact, Long> {

    boolean existsByName(String name);

    Optional<Artifact> findByName(String name);

    Page<Artifact> findByStatus(String status, Pageable pageable);

    @Query("SELECT a FROM Artifact a WHERE "
            + "(:status IS NULL OR a.status = :status) "
            + "AND (:era IS NULL OR a.era = :era) "
            + "AND (:name IS NULL OR a.name LIKE CONCAT('%', :name, '%')) "
            + "AND (:tags IS NULL OR a.tags LIKE CONCAT('%', :tags, '%')) "
            + "AND (:keyword IS NULL OR a.name LIKE CONCAT('%', :keyword, '%') OR a.description LIKE CONCAT('%', :keyword, '%'))")
    Page<Artifact> findByConditions(
            @Param("status") String status,
            @Param("era") String era,
            @Param("name") String name,
            @Param("tags") String tags,
            @Param("keyword") String keyword,
            Pageable pageable);

    long countByStatus(String status);

    List<Artifact> findAllByStatus(String status);
}
