package com.jigu.cloud.infrastructure.persistence.adapter;

import com.jigu.cloud.domain.team.Team;
import com.jigu.cloud.domain.team.TeamRepository;
import com.jigu.cloud.domain.team.UserTeamMembership;
import com.jigu.cloud.infrastructure.persistence.jpa.TeamJpaRepository;
import com.jigu.cloud.infrastructure.persistence.jpa.UserTeamMembershipJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * TeamRepository 适配器。
 */
@Repository
public class TeamRepositoryImpl implements TeamRepository {

    private final TeamJpaRepository teamJpaRepository;
    private final UserTeamMembershipJpaRepository membershipJpaRepository;

    public TeamRepositoryImpl(TeamJpaRepository teamJpaRepository,
                              UserTeamMembershipJpaRepository membershipJpaRepository) {
        this.teamJpaRepository = teamJpaRepository;
        this.membershipJpaRepository = membershipJpaRepository;
    }

    @Override
    public Optional<Team> findById(Long id) {
        return teamJpaRepository.findById(id);
    }

    @Override
    public Team save(Team team) {
        return teamJpaRepository.save(team);
    }

    @Override
    public void deleteById(Long id) {
        teamJpaRepository.deleteById(id);
    }

    @Override
    public boolean isMember(Long userId, Long teamId) {
        return membershipJpaRepository.existsByUserIdAndTeamId(userId, teamId);
    }

    @Override
    public List<Team> findByUserId(Long userId) {
        List<Long> teamIds = membershipJpaRepository.findTeamIdsByUserId(userId);
        if (teamIds.isEmpty()) {
            return List.of();
        }
        return teamJpaRepository.findAllById(teamIds);
    }

    @Override
    public List<Team> findByOwnerId(Long ownerId) {
        return teamJpaRepository.findByOwnerId(ownerId);
    }

    @Override
    public boolean existsByName(String name) {
        return teamJpaRepository.existsByName(name);
    }

    @Override
    public UserTeamMembership addMember(Long userId, Long teamId, String role) {
        UserTeamMembership membership = new UserTeamMembership(userId, teamId, role);
        return membershipJpaRepository.save(membership);
    }

    @Override
    @Transactional
    public void removeMember(Long userId, Long teamId) {
        membershipJpaRepository.deleteByUserIdAndTeamId(userId, teamId);
    }

    @Override
    public List<UserTeamMembership> findMembers(Long teamId) {
        return membershipJpaRepository.findByTeamId(teamId);
    }

    @Override
    public Optional<UserTeamMembership> findMembership(Long userId, Long teamId) {
        return membershipJpaRepository.findByUserIdAndTeamId(userId, teamId);
    }
}
