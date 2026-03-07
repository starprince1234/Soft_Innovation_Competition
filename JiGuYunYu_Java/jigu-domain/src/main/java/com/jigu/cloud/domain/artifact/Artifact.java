package com.jigu.cloud.domain.artifact;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 文物领域模型（映射 artifacts 表）。
 * <p>
 * 普通用户默认仅可见 APPROVED 状态（由 application 层保证）。
 */
@Entity
@Table(name = "artifacts")
public class Artifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    @Column(length = 512)
    private String tags;

    @Column
    private String location;

    @Column
    private String era;

    @Column(name = "creator_id")
    private Long creatorId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "vector_embedding_id")
    private String vectorEmbeddingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Artifact() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = "APPROVED";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getEra() { return era; }
    public void setEra(String era) { this.era = era; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVectorEmbeddingId() { return vectorEmbeddingId; }
    public void setVectorEmbeddingId(String vectorEmbeddingId) { this.vectorEmbeddingId = vectorEmbeddingId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
