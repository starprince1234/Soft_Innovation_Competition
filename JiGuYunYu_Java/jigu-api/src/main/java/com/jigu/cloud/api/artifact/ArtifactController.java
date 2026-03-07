package com.jigu.cloud.api.artifact;

import com.jigu.cloud.api.artifact.dto.response.ArtifactResponse;
import com.jigu.cloud.application.artifact.ArtifactService;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.common.response.PageResponse;
import com.jigu.cloud.domain.artifact.Artifact;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 文物公开接口：GET /artifacts、GET /artifact/{id}。
 * <p>
 * 列表支持 name、era、tags、keyword 筛选 + sortBy/direction 排序。
 * 文物详情路径为单数 /artifact/{id}，与文档保持一致。
 */
@RestController
public class ArtifactController {

    private final ArtifactService artifactService;

    public ArtifactController(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    @GetMapping(path = "/api/v1/artifacts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<PageResponse<ArtifactResponse>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String era,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest httpReq) {
        String role = (String) httpReq.getAttribute("role");
        PageResponse<Artifact> result = artifactService.listArtifacts(
                role, status, era, name, tags, keyword, page, size, sortBy, direction);
        PageResponse<ArtifactResponse> response = PageResponse.of(
                result.getList().stream().map(ArtifactResponse::from).toList(),
                result.getTotalElements(), result.getCurrentPage(), result.getPageSize());
        return ApiResponse.ok(response);
    }

    @GetMapping(path = "/api/v1/artifact/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ArtifactResponse> getById(@PathVariable Long id,
                                                  HttpServletRequest httpReq) {
        String role = (String) httpReq.getAttribute("role");
        Artifact artifact = artifactService.getById(id, role);
        return ApiResponse.ok(ArtifactResponse.from(artifact));
    }
}
