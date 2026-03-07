package com.jigu.cloud.application.archaeology;

import com.jigu.cloud.common.enums.ErrorCode;
import com.jigu.cloud.common.exception.BizException;
import com.jigu.cloud.domain.artifact.Artifact;
import com.jigu.cloud.domain.artifact.ArtifactRepository;
import com.jigu.cloud.infrastructure.bos.BosClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 考古人员文物录入服务。
 * <p>
 * 考古人员提交文物信息，状态为 PENDING，需管理者审核后才正式入库。
 */
@Service
public class ArchaeologyService {

    private static final Logger log = LoggerFactory.getLogger(ArchaeologyService.class);

    private final ArtifactRepository artifactRepository;
    private final BosClient bosClient;

    public ArchaeologyService(ArtifactRepository artifactRepository, BosClient bosClient) {
        this.artifactRepository = artifactRepository;
        this.bosClient = bosClient;
    }

    /**
     * 考古人员提交新文物信息（待审核）。
     */
    @Transactional
    public Artifact submitArtifact(Long creatorId, String name, String description,
                                   String imageBase64, String location, String era,
                                   String tags, String notes) {
        if (artifactRepository.existsByName(name)) {
            throw new BizException(ErrorCode.ARTIFACT_NAME_EXISTS);
        }

        Artifact artifact = new Artifact();
        artifact.setName(name);
        artifact.setDescription(description);
        artifact.setLocation(location);
        artifact.setEra(era);
        artifact.setTags(tags);
        artifact.setCreatorId(creatorId);
        artifact.setStatus("PENDING");

        // 上传文物图片到 BOS
        if (imageBase64 != null && !imageBase64.isBlank()) {
            String imageUrl = bosClient.uploadBase64(imageBase64, "artifact/", ".jpg");
            artifact.setImageUrl(imageUrl);
        }

        artifact = artifactRepository.save(artifact);
        log.info("Archaeologist submitted artifact: id={}, name={}, creatorId={}", artifact.getId(), name, creatorId);
        return artifact;
    }
}
