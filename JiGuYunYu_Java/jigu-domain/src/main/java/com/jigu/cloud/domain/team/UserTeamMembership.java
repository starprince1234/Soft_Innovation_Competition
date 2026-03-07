package com.jigu.cloud.domain.team;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户-团队关联模型（映射 user_team_membership 表）。
 */
@Entity
@Table(name = "user_team_membership",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "team_id"}))
public class UserTeamMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    protected UserTeamMembership() {
    }

    public UserTeamMembership(Long userId, Long teamId, String role) {
        this.userId = userId;
        this.teamId = teamId;
        this.role = role;
    }

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
}
