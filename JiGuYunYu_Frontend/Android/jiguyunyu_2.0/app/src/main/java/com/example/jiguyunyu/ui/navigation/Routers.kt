package com.example.jiguyunyu.ui.navigation

import android.net.Uri

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val DETAIL = "detail/{artifactId}"
    const val HISTORY = "history"
    const val FAVORITES = "favorites"
    const val FEEDBACK = "feedback"
    const val ARCHAEOLOGY_UPLOAD = "archaeology_upload"
    const val ADMIN_FEEDBACK = "admin_feedback"
    const val ADMIN_USERS = "admin_users"

    // 底部导航子路由
    const val HOME = "home"
    const val SCAN = "scan"
    const val CHAT = "chat?artifactId={artifactId}&detectContext={detectContext}&conversationId={conversationId}"
    fun chat(artifactId: Long? = null, detectContext: String? = null, conversationId: Long? = null): String {
        val params = mutableListOf<String>()
        if (artifactId != null) {
            params.add("artifactId=$artifactId")
        }
        if (!detectContext.isNullOrBlank()) {
            params.add("detectContext=${Uri.encode(detectContext)}")
        }
        if (conversationId != null) {
            params.add("conversationId=$conversationId")
        }
        return if (params.isEmpty()) "chat" else "chat?${params.joinToString("&")}" 
    }
    const val PROFILE = "profile"
    const val ADMIN_DASHBOARD = "admin_dashboard"

    // 新增管理员二级路由
    const val ADMIN_PENDING_ARTIFACTS = "admin_pending_artifacts"
    const val ADMIN_ADD_ARTIFACT = "admin_add_artifact"
    const val ADMIN_EDIT_ARTIFACT = "admin_edit_artifact/{artifactId}"
    fun adminEditArtifact(id: Long) = "admin_edit_artifact/$id"
    const val ADMIN_DIALOG_HISTORIES = "admin_dialog_histories"

    fun detail(id: Long) = "detail/$id"
}
