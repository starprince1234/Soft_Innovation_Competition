package com.jigu.cloud.infrastructure.persistence.jpa;

import com.jigu.cloud.domain.favorite.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 收藏 Spring Data JPA 接口。
 */
public interface UserFavoriteJpaRepository extends JpaRepository<UserFavorite, Long> {

    boolean existsByUserIdAndArtifactId(Long userId, Long artifactId);

    @Modifying
    @Query("DELETE FROM UserFavorite uf WHERE uf.userId = :userId AND uf.artifactId = :artifactId")
    void deleteByUserIdAndArtifactId(@Param("userId") Long userId,
                                     @Param("artifactId") Long artifactId);

    @Query("SELECT uf.artifactId FROM UserFavorite uf WHERE uf.userId = :userId ORDER BY uf.createdAt DESC")
    List<Long> findArtifactIdsByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
