package com.jigu.cloud.infrastructure.persistence.adapter;

import com.jigu.cloud.domain.favorite.UserFavorite;
import com.jigu.cloud.domain.favorite.UserFavoriteRepository;
import com.jigu.cloud.infrastructure.persistence.jpa.UserFavoriteJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * UserFavoriteRepository 适配器。
 */
@Repository
public class UserFavoriteRepositoryImpl implements UserFavoriteRepository {

    private final UserFavoriteJpaRepository jpaRepository;

    public UserFavoriteRepositoryImpl(UserFavoriteJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsByUserIdAndArtifactId(Long userId, Long artifactId) {
        return jpaRepository.existsByUserIdAndArtifactId(userId, artifactId);
    }

    @Override
    @Transactional
    public void save(Long userId, Long artifactId) {
        if (!jpaRepository.existsByUserIdAndArtifactId(userId, artifactId)) {
            jpaRepository.save(new UserFavorite(userId, artifactId));
        }
    }

    @Override
    @Transactional
    public void deleteByUserIdAndArtifactId(Long userId, Long artifactId) {
        jpaRepository.deleteByUserIdAndArtifactId(userId, artifactId);
    }

    @Override
    public List<Long> findArtifactIdsByUserId(Long userId) {
        return jpaRepository.findArtifactIdsByUserIdOrderByCreatedAtDesc(userId);
    }
}
