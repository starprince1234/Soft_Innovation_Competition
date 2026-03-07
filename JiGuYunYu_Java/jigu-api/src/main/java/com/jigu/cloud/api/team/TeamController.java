package com.jigu.cloud.api.team;

import com.jigu.cloud.api.team.dto.request.CreateTeamRequest;
import com.jigu.cloud.api.team.dto.request.AddMemberRequest;
import com.jigu.cloud.api.team.dto.request.UpdateTeamRequest;
import com.jigu.cloud.api.team.dto.response.TeamResponse;
import com.jigu.cloud.api.team.dto.response.MemberResponse;
import com.jigu.cloud.application.team.TeamService;
import com.jigu.cloud.common.response.ApiResponse;
import com.jigu.cloud.domain.team.Team;
import com.jigu.cloud.domain.team.UserTeamMembership;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 团队管理 API。
 * <p>
 * 提供团队创建、查询、更新、删除，以及成员添加、移除、列表查询。
 * <p>
 * 所有接口需认证（JWT），权限由 TeamService 内部校验。
 */
@RestController
@RequestMapping(path = "/api/v1/teams", produces = MediaType.APPLICATION_JSON_VALUE)
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    // ===================== 团队 CRUD =====================

    /**
     * 创建团队。
     */
    @PostMapping
    public ApiResponse<TeamResponse> createTeam(
            @Valid @RequestBody CreateTeamRequest req,
            HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        Team team = teamService.createTeam(userId, req.name());
        return ApiResponse.ok(TeamResponse.from(team));
    }

    /**
     * 查询当前用户所属的所有团队。
     */
    @GetMapping
    public ApiResponse<List<TeamResponse>> listMyTeams(HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        List<Team> teams = teamService.listMyTeams(userId);
        return ApiResponse.ok(teams.stream().map(TeamResponse::from).toList());
    }

    /**
     * 获取团队详情。
     */
    @GetMapping("/{teamId}")
    public ApiResponse<TeamResponse> getTeam(
            @PathVariable Long teamId,
            HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        String role = (String) httpReq.getAttribute("role");
        Team team = teamService.getTeam(teamId, userId, role);
        return ApiResponse.ok(TeamResponse.from(team));
    }

    /**
     * 更新团队信息。
     */
    @PutMapping("/{teamId}")
    public ApiResponse<TeamResponse> updateTeam(
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamRequest req,
            HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        String role = (String) httpReq.getAttribute("role");
        Team team = teamService.updateTeam(teamId, userId, role, req.name());
        return ApiResponse.ok(TeamResponse.from(team));
    }

    /**
     * 删除团队。
     */
    @DeleteMapping("/{teamId}")
    public ApiResponse<Void> deleteTeam(
            @PathVariable Long teamId,
            HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        String role = (String) httpReq.getAttribute("role");
        teamService.deleteTeam(teamId, userId, role);
        return ApiResponse.ok();
    }

    // ===================== 成员管理 =====================

    /**
     * 添加团队成员。
     */
    @PostMapping("/{teamId}/members")
    public ApiResponse<MemberResponse> addMember(
            @PathVariable Long teamId,
            @Valid @RequestBody AddMemberRequest req,
            HttpServletRequest httpReq) {
        Long operatorId = (Long) httpReq.getAttribute("userId");
        String operatorRole = (String) httpReq.getAttribute("role");
        UserTeamMembership membership = teamService.addMember(
                teamId, req.userId(), req.role(), operatorId, operatorRole);
        return ApiResponse.ok(MemberResponse.from(membership));
    }

    /**
     * 移除团队成员。
     */
    @DeleteMapping("/{teamId}/members/{userId}")
    public ApiResponse<Void> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            HttpServletRequest httpReq) {
        Long operatorId = (Long) httpReq.getAttribute("userId");
        String operatorRole = (String) httpReq.getAttribute("role");
        teamService.removeMember(teamId, userId, operatorId, operatorRole);
        return ApiResponse.ok();
    }

    /**
     * 查询团队成员列表。
     */
    @GetMapping("/{teamId}/members")
    public ApiResponse<List<MemberResponse>> listMembers(
            @PathVariable Long teamId,
            HttpServletRequest httpReq) {
        Long userId = (Long) httpReq.getAttribute("userId");
        String role = (String) httpReq.getAttribute("role");
        List<UserTeamMembership> members = teamService.listMembers(teamId, userId, role);
        return ApiResponse.ok(members.stream().map(MemberResponse::from).toList());
    }
}
