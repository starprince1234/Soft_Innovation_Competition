package com.jigu.cloud.application.favorite;

import com.jigu.cloud.common.enums.ErrorCode;
import com.jigu.cloud.common.exception.BizException;
import com.jigu.cloud.domain.artifact.Artifact;
import com.jigu.cloud.domain.artifact.ArtifactRepository;
import com.jigu.cloud.domain.favorite.UserFavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收藏业务服务。
 */
@Service
public class FavoriteService {

    private final UserFavoriteRepository favoriteRepository;
    private final ArtifactRepository artifactRepository;

    public FavoriteService(UserFavoriteRepository favoriteRepository,
                           ArtifactRepository artifactRepository) {
        this.favoriteRepository = favoriteRepository;
        this.artifactRepository = artifactRepository;
    }

    public boolean isFavorite(Long userId, Long artifactId) {
        return favoriteRepository.existsByUserIdAndArtifactId(userId, artifactId);
    }

    @Transactional
    public boolean addFavorite(Long userId, Long artifactId) {
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new BizException(ErrorCode.ARTIFACT_NOT_FOUND));
        if (!"APPROVED".equals(artifact.getStatus())) {
            throw new BizException(ErrorCode.ARTIFACT_NOT_FOUND);
        }
        favoriteRepository.save(userId, artifactId);
        return true;
    }

    @Transactional
    public boolean removeFavorite(Long userId, Long artifactId) {
        favoriteRepository.deleteByUserIdAndArtifactId(userId, artifactId);
        return false;
    }

    public List<Artifact> listFavorites(Long userId) {
        List<Long> ids = favoriteRepository.findArtifactIdsByUserId(userId);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Artifact> artifacts = artifactRepository.findAllByIds(ids);
        if (artifacts.isEmpty()) {
            return List.of();
        }
        Map<Long, Artifact> byId = new HashMap<>();
        for (Artifact artifact : artifacts) {
            if ("APPROVED".equals(artifact.getStatus())) {
                byId.put(artifact.getId(), artifact);
            }
        }
        List<Artifact> ordered = new ArrayList<>();
        for (Long id : ids) {
            Artifact artifact = byId.get(id);
            if (artifact != null) {
                ordered.add(artifact);
            }
        }
        return ordered;
    }
}
