package com.jigu.cloud.api.archaeology;

import com.jigu.cloud.api.archaeology.dto.request.ArchaeologyArtifactRequest;
import com.jigu.cloud.api.dialog.dto.response.DialogResponse;
import com.jigu.cloud.application.archaeology.ArchaeologyService;
import com.jigu.cloud.application.dialog.DialogService;
import com.jigu.cloud.common.exception.BizException;
import com.jigu.cloud.common.enums.ErrorCode;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.common.response.PageResponse;
import com.jigu.cloud.domain.artifact.Artifact;
import com.jigu.cloud.domain.dialog.Dialog;
import com.jigu.cloud.domain.team.TeamRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 考古人员专属接口：
 * POST /archaeology/artifacts — 提交文物
 * GET /archaeology/teams/{teamId}/dialog/histories — 团队对话历史
 */
@RestController
@RequestMapping(path = "/api/v1/archaeology", produces = MediaType.APPLICATION_JSON_VALUE)
public class ArchaeologyController {

    private final ArchaeologyService archaeologyService;
    private final DialogService dialogService;
    private final TeamRepository teamRepository;

    public ArchaeologyController(ArchaeologyService archaeologyService,
                                 DialogService dialogService,
                                 TeamRepository teamRepository) {
        this.archaeologyService = archaeologyService;
        this.dialogService = dialogService;
        this.teamRepository = teamRepository;
    }

    /**
     * 考古人员提交新文物信息（待审核）。
     * 权限由 SecurityConfig 保证需认证，业务层可进一步校验角色。
     */
    @PostMapping("/artifacts")
    public ApiResponse<Map<String, Object>> submitArtifact(
            @Valid @RequestBody ArchaeologyArtifactRequest req,
            HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        String tagsStr = (req.tags() != null && !req.tags().isEmpty())
                ? String.join(",", req.tags()) : null;
        Artifact artifact = archaeologyService.submitArtifact(
                userId, req.name(), req.description(), req.imageBase64(),
                req.location(), req.era(), tagsStr, req.notes());
        return ApiResponse.ok(Map.of(
                "artifactId", artifact.getId(),
                "status", artifact.getStatus()
        ));
    }

    /**
     * 查看团队共享对话历史。
     * <p>
     * 校验当前用户是否为团队成员，仅团队成员可查看。
     */
    @GetMapping("/teams/{teamId}/dialog/histories")
    public ApiResponse<PageResponse<DialogResponse>> teamDialogHistories(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        // 校验团队存在
        teamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        // 校验用户是否属于该团队
        if (!teamRepository.isMember(userId, teamId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        PageResponse<Dialog> result = dialogService.getTeamHistory(teamId, page, size);
        PageResponse<DialogResponse> response = PageResponse.of(
                result.getList().stream().map(DialogResponse::from).toList(),
                result.getTotalElements(), result.getCurrentPage(), result.getPageSize());
        return ApiResponse.ok(response);
    }
}
