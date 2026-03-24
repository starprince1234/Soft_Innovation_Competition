package com.example.jiguyunyu.data

import com.google.gson.annotations.SerializedName

// 用户角色枚举
enum class UserRole {
    PUBLIC,         // 公众
    ARCHAEOLOGIST,  // 考古人员
    MANAGER         // 博物馆管理者
}

// 聊天角色枚举
enum class ChatRole {
    USER,
    AI
}

// 用户数据模型
data class User(
    val id: String,
    val username: String,
    val role: UserRole, // 保留为了向后兼容，或者可以移除，但需要修改很多地方
    val roles: List<UserRole> = listOf(role),
    val token: String,
    val avatarUrl: String = ""
)

// 文物数据模型 - ID修正为 Long
data class Artifact(
    val id: Long,
    val name: String,
    val era: String = "",
    val category: String = "其他",
    val imageUrl: String = "",
    val description: String = "",
    val location: String = "",
    val tags: List<String> = emptyList()
)

data class FavoriteStateResponse(
    val favorite: Boolean
)

// 定义来源数据类
data class RagSource(
    val source: String,
    val content: String? = null,
    val url: String? = null
)

// 聊天消息模型 - 增加关联文物信息用于显示卡片
data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val relatedArtifact: Artifact? = null, // 若不为空，显示文物卡片
    val imageUri: android.net.Uri? = null, // 用户上传的图片
    val ragSources: List<RagSource>? = null // 新增字段
)

// 检测任务结果模型
data class DetectionResult(
    val label: String,
    val confidence: Float,
    val description: String,
    val imageUrl: String,
    val artifactId: Long? = null // 若识别出具体文物，ID 为 Long
)

// API 响应封装
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)

// 分页响应封装
data class Page<T>(
    @SerializedName("content", alternate = ["list", "data"])
    val list: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int
)

// --- 业务专用模型 ---

// 对话历史列表项
data class DialogHistory(
    @SerializedName("dialogId", alternate = ["id"])
    val id: Long,
    val conversationId: Long? = null,
    val turnId: Int,
    val userQuery: String,
    val aiResponse: String,
    val createdAt: String,
    val createdAtEpochMs: Long? = null,
    val artifactId: Long? = null,
    val artifactName: String? = null
)

// 反馈请求
data class FeedbackRequest(
    val type: String,
    val textContent: String,
    val rating: Int?,
    val screenshotBase64: String? = null
)

// 反馈响应
data class FeedbackResponse(val feedbackId: Long)

// 管理端反馈列表项
data class AdminFeedback(
    val id: Long,
    val userId: Long? = null,
    val username: String? = null,
    val type: String,
    val textContent: String,
    val rating: Int?,
    val screenshotUrl: String? = null,
    val status: String, // PENDING, RESOLVED
    val createdAt: String = ""
)

// 考古录入请求
data class ArchaeologySubmitRequest(
    val name: String,
    val description: String,
    val imageBase64: String,
    val location: String,
    val era: String,
    val tags: List<String>
)

data class AdminArtifactRequest(
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val imageBase64: String? = null,
    val thumbnailUrl: String? = null,
    val tags: String? = null,
    val location: String? = null,
    val era: String? = null,
    val status: String? = null
)

// 管理端用户列表项
data class AdminUser(
    val id: Long,
    val username: String,
    val role: String,
    val status: String, // ACTIVE, INACTIVE
    val createdAt: String
)

// --- 新增：检测与对话相关模型 ---

data class DetectRequest(val imageBase64: String)

data class DetectArtifactItem(
    val label: String,
    val confidence: Float? = null,
    val bbox: List<Float>? = null,
    val artifactId: Long? = null
)

data class DetectResultResponse(
    val taskId: Long,
    val status: String, // PENDING, PROCESSING, COMPLETED, FAILED
    val imageUrl: String? = null,
    val detectedArtifacts: List<DetectArtifactItem>? = null
)

data class DialogMessage(
    val role: String, // "USER" or "AI"
    val content: String
)

data class DialogRequest(
    val query: String,
    val artifactId: Long? = null,
    val conversationId: Long? = null,
    val contextHistory: List<DialogMessage> = emptyList()
)

data class DialogResultResponse(
    val dialogId: Long,
    val conversationId: Long? = null,
    val turnId: Int,
    val userQuery: String,
    val aiResponse: String,
    val ragSources: List<Map<String, Any>>? = null,
    val createdAt: String,
    val createdAtEpochMs: Long? = null
)

// --- 新增：通用状态更新模型 ---

data class UpdateStatusRequest(
    val status: String, // RESOLVED, INACTIVE, etc.
    val resolutionNotes: String? = null
)

data class UpdateRoleRequest(val role: String)

data class DashboardOverview(
    val totalUsers: Long,
    val activeUsersLast24h: Long,
    val totalArtifacts: Long,
    val pendingArtifactsForReview: Long,
    val totalFeedback: Long,
    val unresolvedFeedback: Long,
    val detectionSuccessRate: Double,
    val dialogCountLast24h: Long
)

object ArtifactCategoryCatalog {
    val fallback = listOf("青铜", "书画", "玉器", "陶器")

    fun allWithAllOption(categories: List<String>): List<String> {
        return listOf("全部") + fallback
    }

    fun deriveFromArtifacts(artifacts: List<Artifact>): List<String> {
        val fromCategory = artifacts.mapNotNull { normalize(it.category) }
        val fromTags = artifacts.flatMap { it.tags }.mapNotNull { normalize(it) }
        return (fromCategory + fromTags).distinct()
    }

    fun normalize(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank() || value == "全部") return null
        return when {
            value.contains("青铜") -> "青铜"
            value.contains("书") || value.contains("画") -> "书画"
            value.contains("玉") -> "玉器"
            value.contains("陶") -> "陶器"
            else -> null
        }
    }
}

// 修改密码请求体
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)
