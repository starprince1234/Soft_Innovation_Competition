package com.jigu.cloud.api.admin;

import com.jigu.cloud.api.admin.dto.response.PendingArtifactResponse;
import com.jigu.cloud.api.artifact.dto.request.ArtifactRequest;
import com.jigu.cloud.api.artifact.dto.response.ArtifactResponse;
import com.jigu.cloud.application.admin.ArtifactAdminService;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.common.response.PageResponse;
import com.jigu.cloud.domain.artifact.Artifact;
import com.jigu.cloud.infrastructure.bos.BosClient;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 文物审核管理接口（仅 MANAGER 角色可访问）。
 */
@RestController
@RequestMapping(path = "/api/v1/admin/artifacts", produces = MediaType.APPLICATION_JSON_VALUE)
public class ArtifactAdminController {

    private static final Logger log = LoggerFactory.getLogger(ArtifactAdminController.class);
    private static final String DEFAULT_IMAGE_URL = "https://bos-mock.jigu.cloud/default/artifact-placeholder.jpg";

    private final ArtifactAdminService artifactAdminService;
    private final BosClient bosClient;

    public ArtifactAdminController(ArtifactAdminService artifactAdminService,
                                   BosClient bosClient) {
        this.artifactAdminService = artifactAdminService;
        this.bosClient = bosClient;
    }

    @GetMapping("/pending")
    public ApiResponse<PageResponse<PendingArtifactResponse>> listPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        PageResponse<Artifact> result = artifactAdminService.listPending(page, size, sortBy, direction);
        Map<Long, String> creatorNames = artifactAdminService.resolveCreatorNames(result.getList());
        List<PendingArtifactResponse> dtoList = result.getList().stream()
                .map(a -> PendingArtifactResponse.from(a, creatorNames.getOrDefault(a.getCreatorId(), null)))
                .toList();
        PageResponse<PendingArtifactResponse> response = PageResponse.of(
                dtoList, result.getTotalElements(), result.getCurrentPage(), result.getPageSize());
        return ApiResponse.ok(response);
    }

    @PostMapping
    public ApiResponse<ArtifactResponse> create(@Valid @RequestBody ArtifactRequest req) {
        Artifact artifact = new Artifact();
        artifact.setName(req.name());
        artifact.setDescription(req.description());
        String imageUrl = req.imageUrl();
        if ((imageUrl == null || imageUrl.isBlank()) && (req.imageBase64() == null || req.imageBase64().isBlank())) {
            imageUrl = DEFAULT_IMAGE_URL;
        }
        artifact.setImageUrl(imageUrl);
        artifact.setThumbnailUrl(req.thumbnailUrl() != null && !req.thumbnailUrl().isBlank() ? req.thumbnailUrl() : imageUrl);
        artifact.setTags(req.tags());
        artifact.setLocation(req.location());
        artifact.setEra(req.era());
        // 管理员直传文物后应立即可见，避免前端刷新后仍看不到。
        artifact.setStatus(req.status() != null && !req.status().isBlank() ? req.status() : "APPROVED");
        Artifact saved = artifactAdminService.createArtifact(artifact);

        // 统一将图片与描述文件放入 OSS: 文物列表/artifact-{id}/
        String artifactDir = "文物列表/artifact-" + saved.getId() + "/";
        Artifact storagePatch = new Artifact();
        boolean changed = false;

        try {
            if (req.imageBase64() != null && !req.imageBase64().isBlank()) {
                String uploadedImageUrl = bosClient.uploadBase64(req.imageBase64(), artifactDir, inferImageExtension(req.imageBase64()));
                storagePatch.setImageUrl(uploadedImageUrl);
                storagePatch.setThumbnailUrl(uploadedImageUrl);
                changed = true;
            }

            if (req.description() != null && !req.description().isBlank()) {
                // 描述同样落到该文物目录下，便于与图片一起归档
                bosClient.uploadBytes(req.description().getBytes(StandardCharsets.UTF_8), artifactDir, ".txt");
            }
        } catch (Exception e) {
            // 上传失败不应阻断文物创建主流程，避免前端收到 500 且误以为提交失败。
            log.warn("Artifact OSS upload skipped: artifactId={}, error={}", saved.getId(), e.getMessage());
        }

        if (changed) {
            saved = artifactAdminService.updateArtifact(saved.getId(), storagePatch);
        }

        return ApiResponse.ok(ArtifactResponse.from(saved));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        artifactAdminService.updateStatus(id, body.get("status"));
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<ArtifactResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody ArtifactRequest req) {
        Artifact updated = new Artifact();
        updated.setName(req.name());
        updated.setDescription(req.description());
        String imageUrl = resolveImageUrl(id, req);
        if (imageUrl != null && !imageUrl.isBlank()) {
            updated.setImageUrl(imageUrl);
            updated.setThumbnailUrl(req.thumbnailUrl() != null && !req.thumbnailUrl().isBlank() ? req.thumbnailUrl() : imageUrl);
        }
        updated.setTags(req.tags());
        updated.setLocation(req.location());
        updated.setEra(req.era());
        Artifact result = artifactAdminService.updateArtifact(id, updated);
        return ApiResponse.ok(ArtifactResponse.from(result));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        artifactAdminService.deleteArtifact(id);
        return ApiResponse.ok(null);
    }

    private String resolveImageUrl(Long artifactId, ArtifactRequest req) {
        if (req.imageBase64() != null && !req.imageBase64().isBlank()) {
            String artifactDir = "文物列表/artifact-" + artifactId + "/";
            return bosClient.uploadBase64(req.imageBase64(), artifactDir, inferImageExtension(req.imageBase64()));
        }
        if (req.imageUrl() == null || req.imageUrl().isBlank()) {
            return DEFAULT_IMAGE_URL;
        }
        return req.imageUrl();
    }

    private String inferImageExtension(String base64Data) {
        if (base64Data == null) {
            return ".jpg";
        }
        String normalized = base64Data.trim().toLowerCase();
        if (normalized.startsWith("data:image/png")) {
            return ".png";
        }
        if (normalized.startsWith("data:image/webp")) {
            return ".webp";
        }
        if (normalized.startsWith("data:image/gif")) {
            return ".gif";
        }
        return ".jpg";
    }
}
