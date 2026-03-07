package com.jigu.cloud.domain.detect;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 检测任务领域模型（映射 detection_tasks 表）。
 * <p>
 * 状态机：PENDING → PROCESSING → COMPLETED / FAILED
 */
@Entity
@Table(name = "detection_tasks")
public class DetectionTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "detected_label")
    private String detectedLabel;

    @Column
    private Float confidence;

    @Column(columnDefinition = "TEXT")
    private String bbox;

    @Column(name = "raw_result", columnDefinition = "TEXT")
    private String rawResult;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected DetectionTask() {
    }

    public DetectionTask(Long userId, String imageUrl) {
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.status = "PENDING";
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = "PENDING";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // -------- 状态流转方法 --------

    public void markProcessing() {
        this.status = "PROCESSING";
    }

    public void markCompleted(String detectedLabel, Float confidence, String bbox, String rawResult) {
        this.status = "COMPLETED";
        this.detectedLabel = detectedLabel;
        this.confidence = confidence;
        this.bbox = bbox;
        this.rawResult = rawResult;
    }

    public void markFailed(String rawResult) {
        this.status = "FAILED";
        this.rawResult = rawResult;
    }

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetectedLabel() { return detectedLabel; }
    public void setDetectedLabel(String detectedLabel) { this.detectedLabel = detectedLabel; }

    public Float getConfidence() { return confidence; }
    public void setConfidence(Float confidence) { this.confidence = confidence; }

    public String getBbox() { return bbox; }
    public void setBbox(String bbox) { this.bbox = bbox; }

    public String getRawResult() { return rawResult; }
    public void setRawResult(String rawResult) { this.rawResult = rawResult; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
