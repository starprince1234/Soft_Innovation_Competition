package com.jigu.cloud.application.team;

import com.jigu.cloud.common.enums.ErrorCode;
import com.jigu.cloud.common.exception.BizException;
import com.jigu.cloud.domain.team.Team;
import com.jigu.cloud.domain.team.TeamRepository;
import com.jigu.cloud.domain.team.UserTeamMembership;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 团队管理服务。
 * <p>
 * 提供创建/更新/删除团队、成员添加/移除/查询等完整 CRUD 能力。
 * <p>
 * 权限模型：
 *   - OWNER：团队创建者，拥有全部权限（更新、删除团队，管理成员）
 *   - ADMIN：管理员，可管理成员
 *   - MEMBER：普通成员，仅查看权限
 *   - MANAGER 角色（系统管理员）可跨团队操作
 */
@Service
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    // ===================== 团队 CRUD =====================

    /**
     * 创建团队。创建者自动成为 OWNER 和首位成员。
     */
    @Transactional
    public Team createTeam(Long userId, String name) {
        if (teamRepository.existsByName(name)) {
            throw new BizException(ErrorCode.CONFLICT, "团队名称已存在: " + name);
        }

        Team team = new Team(name, userId);
        team = teamRepository.save(team);

        // 创建者自动加入为 OWNER
        teamRepository.addMember(userId, team.getId(), "OWNER");

        log.info("Team created: id={}, name={}, ownerId={}", team.getId(), name, userId);
        return team;
    }

    /**
     * 获取团队详情（需为成员或 MANAGER）。
     */
    public Team getTeam(Long teamId, Long userId, String role) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "团队不存在"));

        if (!"MANAGER".equals(role) && !teamRepository.isMember(userId, teamId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "非团队成员，无法查看");
        }

        return team;
    }

    /**
     * 查询用户所属的所有团队。
     */
    public List<Team> listMyTeams(Long userId) {
        return teamRepository.findByUserId(userId);
    }

    /**
     * 更新团队信息（仅 OWNER 或 MANAGER 可操作）。
     */
    @Transactional
    public Team updateTeam(Long teamId, Long userId, String role, String newName) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "团队不存在"));

        assertOwnerOrManager(team, userId, role);

        if (newName != null && !newName.isBlank() && !newName.equals(team.getName())) {
            if (teamRepository.existsByName(newName)) {
                throw new BizException(ErrorCode.CONFLICT, "团队名称已存在: " + newName);
            }
            team.setName(newName);
        }

        team = teamRepository.save(team);
        log.info("Team updated: id={}, newName={}", teamId, newName);
        return team;
    }

    /**
     * 删除团队（仅 OWNER 或 MANAGER 可操作）。
     */
    @Transactional
    public void deleteTeam(Long teamId, Long userId, String role) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "团队不存在"));

        assertOwnerOrManager(team, userId, role);

        // 先删除所有成员关系
        List<UserTeamMembership> members = teamRepository.findMembers(teamId);
        for (UserTeamMembership m : members) {
            teamRepository.removeMember(m.getUserId(), teamId);
        }

        teamRepository.deleteById(teamId);
        log.info("Team deleted: id={}, by userId={}", teamId, userId);
    }

    // ===================== 成员管理 =====================

    /**
     * 添加成员（需 OWNER/ADMIN 或 MANAGER）。
     */
    @Transactional
    public UserTeamMembership addMember(Long teamId, Long targetUserId, String memberRole,
                                         Long operatorId, String operatorRole) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "团队不存在"));

        assertCanManageMembers(team, operatorId, operatorRole);

        if (teamRepository.isMember(targetUserId, teamId)) {
            throw new BizException(ErrorCode.CONFLICT, "用户已是团队成员");
        }

        // 成员角色默认 MEMBER
        String role = (memberRole != null && !memberRole.isBlank()) ? memberRole : "MEMBER";
        UserTeamMembership membership = teamRepository.addMember(targetUserId, teamId, role);

        log.info("Member added: teamId={}, userId={}, role={}", teamId, targetUserId, role);
        return membership;
    }

    /**
     * 移除成员（需 OWNER/ADMIN 或 MANAGER；不能移除 OWNER）。
     */
    @Transactional
    public void removeMember(Long teamId, Long targetUserId, Long operatorId, String operatorRole) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "团队不存在"));

        assertCanManageMembers(team, operatorId, operatorRole);

        // 不能移除 OWNER
        if (team.getOwnerId().equals(targetUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无法移除团队创建者");
        }

        if (!teamRepository.isMember(targetUserId, teamId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不是团队成员");
        }

        teamRepository.removeMember(targetUserId, teamId);
        log.info("Member removed: teamId={}, userId={}", teamId, targetUserId);
    }

    /**
     * 查询团队成员列表。
     */
    public List<UserTeamMembership> listMembers(Long teamId, Long userId, String role) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "团队不存在"));

        if (!"MANAGER".equals(role) && !teamRepository.isMember(userId, teamId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "非团队成员，无法查看");
        }

        return teamRepository.findMembers(teamId);
    }

    // ===================== 权限校验 =====================

    private void assertOwnerOrManager(Team team, Long userId, String role) {
        if ("MANAGER".equals(role)) return;
        if (!team.getOwnerId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅团队创建者可执行此操作");
        }
    }

    private void assertCanManageMembers(Team team, Long userId, String role) {
        if ("MANAGER".equals(role)) return;
        if (team.getOwnerId().equals(userId)) return;

        // 团队 ADMIN 也可以管理成员
        teamRepository.findMembership(userId, team.getId())
                .filter(m -> "ADMIN".equals(m.getRole()) || "OWNER".equals(m.getRole()))
                .orElseThrow(() -> new BizException(ErrorCode.FORBIDDEN, "需要团队管理权限"));
    }
}
