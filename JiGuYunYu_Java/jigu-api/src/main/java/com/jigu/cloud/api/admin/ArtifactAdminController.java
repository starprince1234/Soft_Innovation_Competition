package com.jigu.cloud.api.admin;

import com.jigu.cloud.api.admin.dto.response.PendingArtifactResponse;
import com.jigu.cloud.api.artifact.dto.request.ArtifactRequest;
import com.jigu.cloud.api.artifact.dto.response.ArtifactResponse;
import com.jigu.cloud.application.admin.ArtifactAdminService;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.common.response.PageResponse;
import com.jigu.cloud.domain.artifact.Artifact;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 文物审核管理接口（仅 MANAGER 角色可访问）。
 */
@RestController
@RequestMapping(path = "/api/v1/admin/artifacts", produces = MediaType.APPLICATION_JSON_VALUE)
public class ArtifactAdminController {

    private final ArtifactAdminService artifactAdminService;

    public ArtifactAdminController(ArtifactAdminService artifactAdminService) {
        this.artifactAdminService = artifactAdminService;
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
        artifact.setImageUrl(req.imageUrl());
        artifact.setThumbnailUrl(req.thumbnailUrl());
        artifact.setTags(req.tags());
        artifact.setLocation(req.location());
        artifact.setEra(req.era());
        artifact.setStatus("PENDING");
        Artifact saved = artifactAdminService.createArtifact(artifact);
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
        updated.setImageUrl(req.imageUrl());
        updated.setThumbnailUrl(req.thumbnailUrl());
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
}
