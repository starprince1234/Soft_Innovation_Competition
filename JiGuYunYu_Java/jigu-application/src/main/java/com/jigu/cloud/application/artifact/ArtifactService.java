package com.jigu.cloud.application.artifact;

import com.jigu.cloud.common.enums.ErrorCode;
import com.jigu.cloud.common.exception.BizException;
import com.jigu.cloud.common.response.PageResponse;
import com.jigu.cloud.domain.artifact.Artifact;
import com.jigu.cloud.domain.artifact.ArtifactRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文物列表/详情服务。
 * <p>
 * 普通用户只能看 APPROVED；MANAGER 可看全状态。
 */
@Service
public class ArtifactService {

    private final ArtifactRepository artifactRepository;

    public ArtifactService(ArtifactRepository artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    /**
     * 文物列表查询。
     * <p>
     * 非 MANAGER 角色强制只能看 APPROVED 状态，忽略前端传入的 status 参数。
     * MANAGER 可按 status 筛选（如 PENDING/REJECTED），不传则查全部。
     */
    public PageResponse<Artifact> listArtifacts(String role, String status,
                                                String era, String name, String tags, String keyword,
                                                int page, int size, String sortBy, String direction) {
        String effectiveStatus;
        if ("MANAGER".equals(role)) {
            // MANAGER 可按任意状态筛选，不传 status 则查全部（null → JPQL 忽略该条件）
            effectiveStatus = status;
        } else {
            // 非 MANAGER（包括匿名访问）强制 APPROVED
            effectiveStatus = "APPROVED";
        }
        List<Artifact> list = artifactRepository.findByConditions(effectiveStatus, era, name, tags, keyword, page, size, sortBy, direction);
        long total = artifactRepository.countByConditions(effectiveStatus, era, name, tags, keyword);
        return PageResponse.of(list, total, page, size);
    }

    /**
     * 文物详情。
     * <p>
     * 非 MANAGER 角色仅能查看 APPROVED 状态的文物，
     * 未审核/已拒绝的文物对外不可见，避免信息泄露。
     */
    public Artifact getById(Long id, String role) {
        Artifact artifact = artifactRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.ARTIFACT_NOT_FOUND));
        if (!"MANAGER".equals(role) && !"APPROVED".equals(artifact.getStatus())) {
            throw new BizException(ErrorCode.ARTIFACT_NOT_FOUND);
        }
        return artifact;
    }
}
