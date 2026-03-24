package com.jigu.cloud.domain.favorite;

import java.util.List;

/**
 * 收藏仓储接口。
 */
public interface UserFavoriteRepository {

    boolean existsByUserIdAndArtifactId(Long userId, Long artifactId);

    void save(Long userId, Long artifactId);

    void deleteByUserIdAndArtifactId(Long userId, Long artifactId);

    List<Long> findArtifactIdsByUserId(Long userId);
}
