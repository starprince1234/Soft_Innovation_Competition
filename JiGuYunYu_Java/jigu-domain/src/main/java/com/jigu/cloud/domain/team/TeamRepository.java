package com.jigu.cloud.domain.team;

import java.util.List;
import java.util.Optional;

/**
 * 团队仓储接口。
 */
public interface TeamRepository {

    Optional<Team> findById(Long id);

    Team save(Team team);

    void deleteById(Long id);

    /** 检查用户是否属于指定团队 */
    boolean isMember(Long userId, Long teamId);

    /** 查询用户所属的所有团队 */
    List<Team> findByUserId(Long userId);

    /** 查询用户拥有的所有团队 */
    List<Team> findByOwnerId(Long ownerId);

    /** 检查团队名称是否已存在 */
    boolean existsByName(String name);

    // ---- 成员管理 ----

    /** 添加成员 */
    UserTeamMembership addMember(Long userId, Long teamId, String role);

    /** 移除成员 */
    void removeMember(Long userId, Long teamId);

    /** 查询团队所有成员 */
    List<UserTeamMembership> findMembers(Long teamId);

    /** 查询用户在团队中的角色 */
    Optional<UserTeamMembership> findMembership(Long userId, Long teamId);
}
