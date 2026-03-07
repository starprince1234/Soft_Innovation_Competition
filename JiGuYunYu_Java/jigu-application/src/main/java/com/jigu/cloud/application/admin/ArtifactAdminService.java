package com.jigu.cloud.application.admin;

import com.jigu.cloud.common.enums.ErrorCode;
import com.jigu.cloud.common.exception.BizException;
import com.jigu.cloud.common.response.PageResponse;
import com.jigu.cloud.domain.artifact.Artifact;
import com.jigu.cloud.domain.artifact.ArtifactRepository;
import com.jigu.cloud.domain.user.UserRepository;
import com.jigu.cloud.infrastructure.python.PythonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文物审核管理服务。
 */
@Service
public class ArtifactAdminService {

    private static final Logger log = LoggerFactory.getLogger(ArtifactAdminService.class);

    private final ArtifactRepository artifactRepository;
    private final UserRepository userRepository;
    private final PythonClient pythonClient;

    public ArtifactAdminService(ArtifactRepository artifactRepository,
                                UserRepository userRepository,
                                PythonClient pythonClient) {
        this.artifactRepository = artifactRepository;
        this.userRepository = userRepository;
        this.pythonClient = pythonClient;
    }

    /**
     * 查询待审核文物列表。
     */
    public PageResponse<Artifact> listPending(int page, int size,
                                                String sortBy, String direction) {
        List<Artifact> list = artifactRepository.findByStatus("PENDING", page, size, sortBy, direction);
        long total = artifactRepository.countByStatus("PENDING");
        return PageResponse.of(list, total, page, size);
    }

    /**
     * 根据 creatorId 列表批量查询用户名映射。
     */
    public Map<Long, String> resolveCreatorNames(List<Artifact> artifacts) {
        return artifacts.stream()
                .map(Artifact::getCreatorId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> userRepository.findById(id)
                                .map(u -> u.getUsername())
                                .orElse("未知用户"),
                        (a, b) -> a
                ));
    }

    /**
     * 创建文物。
     */
    @Transactional
    public Artifact createArtifact(Artifact artifact) {
        if (artifactRepository.existsByName(artifact.getName())) {
            throw new BizException(ErrorCode.ARTIFACT_NAME_EXISTS);
        }
        artifact = artifactRepository.save(artifact);
        log.info("Artifact created: id={}, name={}", artifact.getId(), artifact.getName());
        return artifact;
    }

    /**
     * 更新文物状态（PENDING → APPROVED / REJECTED）。
     */
    @Transactional
    public void updateStatus(Long artifactId, String status) {
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new BizException(ErrorCode.ARTIFACT_NOT_FOUND));
        artifact.setStatus(status);
        artifactRepository.save(artifact);
        log.info("Artifact status updated: id={}, status={}", artifactId, status);

        // APPROVED → 触发 Python 向量入库；REJECTED → 删除已有向量
        if ("APPROVED".equalsIgnoreCase(status)) {
            triggerVectorUpsert(artifact);
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            triggerVectorDelete(artifactId);
        }
    }

    /**
     * 更新文物信息。
     */
    @Transactional
    public Artifact updateArtifact(Long artifactId, Artifact updated) {
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new BizException(ErrorCode.ARTIFACT_NOT_FOUND));
        if (updated.getName() != null) artifact.setName(updated.getName());
        if (updated.getDescription() != null) artifact.setDescription(updated.getDescription());
        if (updated.getImageUrl() != null) artifact.setImageUrl(updated.getImageUrl());
        if (updated.getThumbnailUrl() != null) artifact.setThumbnailUrl(updated.getThumbnailUrl());
        if (updated.getTags() != null) artifact.setTags(updated.getTags());
        if (updated.getLocation() != null) artifact.setLocation(updated.getLocation());
        if (updated.getEra() != null) artifact.setEra(updated.getEra());
        Artifact saved = artifactRepository.save(artifact);

        // 已审核文物信息变更 → 同步更新向量
        if ("APPROVED".equalsIgnoreCase(saved.getStatus())) {
            triggerVectorUpsert(saved);
        }
        return saved;
    }

    /**
     * 删除文物。
     */
    @Transactional
    public void deleteArtifact(Long artifactId) {
        if (artifactRepository.findById(artifactId).isEmpty()) {
            throw new BizException(ErrorCode.ARTIFACT_NOT_FOUND);
        }
        // 先删除向量再删除数据库记录
        triggerVectorDelete(artifactId);
        artifactRepository.deleteById(artifactId);
        log.info("Artifact deleted: id={}", artifactId);
    }

    // -------- 向量同步辅助方法（fire-and-forget，异常不影响主流程） --------

    private void triggerVectorUpsert(Artifact artifact) {
        try {
            pythonClient.upsertArtifactVector(
                    artifact.getId(),
                    artifact.getName(),
                    artifact.getDescription(),
                    artifact.getEra(),
                    artifact.getLocation(),
                    artifact.getTags()
            );
            log.info("Vector upsert triggered: artifactId={}", artifact.getId());
        } catch (Exception e) {
            log.warn("Vector upsert failed (non-blocking): artifactId={}, error={}",
                    artifact.getId(), e.getMessage());
        }
    }

    private void triggerVectorDelete(Long artifactId) {
        try {
            pythonClient.deleteArtifactVector(artifactId);
            log.info("Vector delete triggered: artifactId={}", artifactId);
        } catch (Exception e) {
            log.warn("Vector delete failed (non-blocking): artifactId={}, error={}",
                    artifactId, e.getMessage());
        }
    }
}
