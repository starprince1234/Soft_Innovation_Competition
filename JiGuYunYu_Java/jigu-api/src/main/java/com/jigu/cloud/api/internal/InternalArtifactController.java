package com.jigu.cloud.api.internal;

import com.jigu.cloud.domain.artifact.Artifact;
import com.jigu.cloud.domain.artifact.ArtifactRepository;
import com.jigu.cloud.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Java 内部接口（供 Python 端调用）。
 * <p>
 * 安全：由 SecurityConfig 配置为仅内网可达或通过 token 校验。
 * 当前通过 /api/internal/** 路径前缀区分。
 */
@RestController
@RequestMapping(path = "/api/internal", produces = MediaType.APPLICATION_JSON_VALUE)
public class InternalArtifactController {

    private static final Logger log = LoggerFactory.getLogger(InternalArtifactController.class);

    private final ArtifactRepository artifactRepository;

    public InternalArtifactController(ArtifactRepository artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    /**
     * 获取所有已审核文物列表（供知识库重建使用）。
     * <p>
     * Python 端 reindex 时调用此接口获取最新的 APPROVED 文物数据，
     * 以替代静态种子文件。
     */
    @GetMapping("/artifacts/approved")
    public ApiResponse<List<Map<String, Object>>> listApproved() {
        log.info("InternalArtifactController.listApproved called");
        List<Artifact> artifacts = artifactRepository.findAllByStatus("APPROVED");

        List<Map<String, Object>> result = artifacts.stream()
                .map(a -> Map.<String, Object>of(
                        "id", a.getId(),
                        "name", a.getName() != null ? a.getName() : "",
                        "description", a.getDescription() != null ? a.getDescription() : "",
                        "location", a.getLocation() != null ? a.getLocation() : "",
                        "era", a.getEra() != null ? a.getEra() : "",
                        "tags", a.getTags() != null ? a.getTags() : "",
                        "vectorEmbeddingId", a.getVectorEmbeddingId() != null ? a.getVectorEmbeddingId() : ""
                ))
                .toList();

        log.info("InternalArtifactController.listApproved  count={}", result.size());
        return ApiResponse.ok(result);
    }

    /**
     * 批量更新文物的向量嵌入 ID。
     * <p>
     * Python 端 reindex 完成后调用此接口，将 ChromaDB 中的 chunk_id
     * 写回数据库，建立文物与向量索引的关联。
     *
     * @param updates 列表，每项包含 artifactId 和 vectorEmbeddingId
     */
    @PostMapping("/artifacts/vector-ids")
    public ApiResponse<Map<String, Object>> updateVectorIds(
            @RequestBody List<Map<String, Object>> updates) {
        log.info("InternalArtifactController.updateVectorIds  count={}", updates.size());

        int success = 0;
        int failed = 0;

        for (Map<String, Object> item : updates) {
            try {
                Long artifactId = ((Number) item.get("artifactId")).longValue();
                String vectorId = (String) item.get("vectorEmbeddingId");

                artifactRepository.findById(artifactId).ifPresent(artifact -> {
                    artifact.setVectorEmbeddingId(vectorId);
                    artifactRepository.save(artifact);
                });
                success++;
            } catch (Exception e) {
                log.warn("Failed to update vector id for item: {}", item, e);
                failed++;
            }
        }

        log.info("InternalArtifactController.updateVectorIds  success={} failed={}", success, failed);
        return ApiResponse.ok(Map.of("success", success, "failed", failed));
    }
}
