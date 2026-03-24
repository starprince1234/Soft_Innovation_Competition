package com.jigu.cloud.api.favorite;

import com.jigu.cloud.api.artifact.dto.response.ArtifactResponse;
import com.jigu.cloud.application.favorite.FavoriteService;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.domain.artifact.Artifact;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 收藏接口。
 */
@RestController
@RequestMapping(path = "/api/v1/favorites", produces = MediaType.APPLICATION_JSON_VALUE)
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public ApiResponse<List<ArtifactResponse>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Artifact> artifacts = favoriteService.listFavorites(userId);
        return ApiResponse.ok(artifacts.stream().map(ArtifactResponse::from).toList());
    }

    @GetMapping("/{artifactId}/exists")
    public ApiResponse<Map<String, Boolean>> exists(@PathVariable Long artifactId,
                                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(Map.of("favorite", favoriteService.isFavorite(userId, artifactId)));
    }

    @PutMapping("/{artifactId}")
    public ApiResponse<Map<String, Boolean>> add(@PathVariable Long artifactId,
                                                 HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(Map.of("favorite", favoriteService.addFavorite(userId, artifactId)));
    }

    @DeleteMapping("/{artifactId}")
    public ApiResponse<Map<String, Boolean>> remove(@PathVariable Long artifactId,
                                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(Map.of("favorite", favoriteService.removeFavorite(userId, artifactId)));
    }
}
