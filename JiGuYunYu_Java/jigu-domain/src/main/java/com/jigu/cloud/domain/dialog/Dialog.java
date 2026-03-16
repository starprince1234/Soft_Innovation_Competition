package com.jigu.cloud.domain.dialog;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 对话记录领域模型（映射 dialog_records 表）。
 */
@Entity
@Table(name = "dialog_records")
public class Dialog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "artifact_id")
    private Long artifactId;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(name = "turn_id", nullable = false)
    private Integer turnId;

    @Column(name = "user_query", nullable = false, columnDefinition = "TEXT")
    private String userQuery;

    @Column(name = "ai_response", nullable = false, columnDefinition = "TEXT")
    private String aiResponse;

    @Column(name = "context_snapshot", columnDefinition = "TEXT")
    private String contextSnapshot;

    @Column(name = "rag_sources", columnDefinition = "TEXT")
    private String ragSources;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Dialog() {
    }

    public Dialog(Long userId, Long artifactId, Long conversationId, Integer turnId, String userQuery, String aiResponse) {
        this.userId = userId;
        this.artifactId = artifactId;
        this.conversationId = conversationId;
        this.turnId = turnId;
        this.userQuery = userQuery;
        this.aiResponse = aiResponse;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getArtifactId() { return artifactId; }
    public void setArtifactId(Long artifactId) { this.artifactId = artifactId; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public Integer getTurnId() { return turnId; }
    public void setTurnId(Integer turnId) { this.turnId = turnId; }

    public String getUserQuery() { return userQuery; }
    public void setUserQuery(String userQuery) { this.userQuery = userQuery; }

    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }

    public String getContextSnapshot() { return contextSnapshot; }
    public void setContextSnapshot(String contextSnapshot) { this.contextSnapshot = contextSnapshot; }

    public String getRagSources() { return ragSources; }
    public void setRagSources(String ragSources) { this.ragSources = ragSources; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
