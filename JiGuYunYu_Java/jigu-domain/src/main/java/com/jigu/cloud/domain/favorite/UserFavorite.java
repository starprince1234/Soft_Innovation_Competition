package com.jigu.cloud.domain.favorite;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户收藏关系（映射 user_favorites 表）。
 */
@Entity
@Table(
        name = "user_favorites",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_favorite", columnNames = {"user_id", "artifact_id"})
)
public class UserFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "artifact_id", nullable = false)
    private Long artifactId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UserFavorite() {
    }

    public UserFavorite(Long userId, Long artifactId) {
        this.userId = userId;
        this.artifactId = artifactId;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(Long artifactId) {
        this.artifactId = artifactId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
