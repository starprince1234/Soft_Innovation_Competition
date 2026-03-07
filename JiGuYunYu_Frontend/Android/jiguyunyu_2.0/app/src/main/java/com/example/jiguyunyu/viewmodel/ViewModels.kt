package com.example.jiguyunyu.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.scale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jiguyunyu.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

// 统一图片工具
object ImageUtils {
    suspend fun bitmapToBase64(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        // 压缩图片尺寸，避免 OOM 和请求体过大，限制在 1080p 以内
        val maxWidth = 1080
        val maxHeight = 1080
        val scaleFactor = min(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)

        val scaledBitmap = if (scaleFactor < 1f) {
            bitmap.scale((bitmap.width * scaleFactor).toInt(), (bitmap.height * scaleFactor).toInt(), true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        // 质量 85%
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = AuthRepository.getInstance(application)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf("")

    fun login(username: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMsg = ""

            try {
                if (username.isEmpty() || pass.isEmpty()) {
                    errorMsg = "请输入用户名和密码"
                    return@launch
                }

                val response = NetworkModule.api.login(LoginRequest(username, pass))
                if (response.code == 200 && response.data != null) {
                    auth.saveSession(username, response.data.token, response.data.roles)
                    onSuccess()
                } else {
                    errorMsg = response.message.ifBlank { "登录失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun register(username: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMsg = ""

            try {
                if (username.isEmpty() || pass.isEmpty()) {
                    errorMsg = "请输入用户名和密码"
                    return@launch
                }

                val response = NetworkModule.api.register(RegisterRequest(username, pass))
                if (response.code == 200) {
                    // 注册成功后自动登录
                    login(username, pass, onSuccess)
                } else {
                    errorMsg = response.message.ifBlank { "注册失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    // UI状态数据
    private val _artifacts = MutableStateFlow<List<Artifact>>(emptyList())
    val artifacts = _artifacts.asStateFlow()

    var isLoading by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
    var isLoadingMore by mutableStateOf(false)

    // 搜索和筛选状态
    var searchQuery by mutableStateOf("")
    var selectedCategory by mutableStateOf("全部")
    var selectedTags by mutableStateOf<List<String>>(emptyList())

    private var currentPage = 0
    private var isLastPage = false

    init { loadArtifacts(refresh = true) }

    fun loadArtifacts(refresh: Boolean = false) {
        if (refresh) {
            currentPage = 0
            isLastPage = false
            isRefreshing = true
        } else {
            if (isLastPage || isLoading || isLoadingMore) return
            if (currentPage == 0) isLoading = true else isLoadingMore = true
        }

        viewModelScope.launch {
            try {
                val nameQuery = searchQuery.ifBlank { null }
                val eraQuery = if (selectedCategory != "全部") selectedCategory else null

                val response = NetworkModule.api.getArtifacts(
                    page = currentPage,
                    size = 10,
                    name = nameQuery,
                    era = eraQuery
                )

                if (response.code == 200 && response.data != null) {
                    val newItems = response.data.list
                    if (refresh) {
                        _artifacts.value = newItems
                    } else {
                        _artifacts.value += newItems
                    }

                    isLastPage = response.data.currentPage >= response.data.totalPages - 1
                    if (!isLastPage) {
                        currentPage++
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 发生错误时，如果是刷新，清空数据
                if (refresh) _artifacts.value = emptyList()
            } finally {
                isLoading = false
                isRefreshing = false
                isLoadingMore = false
            }
        }
    }

    fun updateSearch(query: String) {
        searchQuery = query
        loadArtifacts(refresh = true)
    }

    fun updateCategory(category: String) {
        selectedCategory = category
        loadArtifacts(refresh = true)
    }

    fun updateTags(tags: List<String>) {
        selectedTags = tags
        loadArtifacts(refresh = true)
    }
}

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    var artifact by mutableStateOf<Artifact?>(null)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf("")

    fun loadArtifact(id: Long) {
        viewModelScope.launch {
            isLoading = true
            errorMsg = ""
            try {
                val response = NetworkModule.api.getArtifactDetail(id)
                if (response.code == 200 && response.data != null) {
                    artifact = response.data
                } else {
                    errorMsg = response.message.ifBlank { "加载失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}

class DetectViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = AuthRepository.getInstance(application)
    var detectionResult by mutableStateOf<DetectionResult?>(null)
    var isDetecting by mutableStateOf(false)
    var selectedImageUri by mutableStateOf<Uri?>(null)
    var showResultSheet by mutableStateOf(false) // 控制结果弹窗
    var errorMsg by mutableStateOf("")

    fun detect(bitmap: Bitmap?) {
        if (bitmap == null) return
        viewModelScope.launch {
            isDetecting = true
            showResultSheet = false // 开始时关闭旧结果
            detectionResult = null
            errorMsg = ""

            try {
                // 1. 图片压缩与 Base64 转换
                val base64Image = ImageUtils.bitmapToBase64(bitmap)

                // 2. 提交检测任务
                val token = auth.bearerToken()
                val taskResponse = NetworkModule.api.createDetectTask(
                    token = token,
                    req = DetectRequest(base64Image)
                )

                if (taskResponse.code == 200 && taskResponse.data != null) {
                    val taskId = taskResponse.data.taskId

                    // 3. 异步轮询机制
                    var isCompleted = false
                    var retryCount = 0
                    val maxRetries = 15 // 最多轮询15次，每次2秒，共30秒

                    while (!isCompleted && retryCount < maxRetries) {
                        delay(2000) // 等待2秒
                        val statusResponse = NetworkModule.api.getDetectTaskStatus(token, taskId)

                        if (statusResponse.code == 200 && statusResponse.data != null) {
                            val status = statusResponse.data.status // 假设返回体包含 status 字段
                            if (status == "COMPLETED") {
                                isCompleted = true
                                val result = statusResponse.data.result
                                detectionResult = DetectionResult(
                                    label = result?.label ?: "未知文物",
                                    confidence = result?.confidence ?: 0f,
                                    description = result?.description ?: "暂无描述",
                                    imageUrl = selectedImageUri.toString(),
                                    artifactId = result?.artifactId
                                )
                                showResultSheet = true
                            } else if (status == "FAILED") {
                                errorMsg = "识别失败，请重试"
                                break
                            }
                        }
                        retryCount++
                    }

                    if (!isCompleted && errorMsg.isEmpty()) {
                        errorMsg = "识别超时，请稍后重试"
                    }
                } else {
                    errorMsg = taskResponse.message.ifBlank { "创建任务失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络请求异常: ${e.message}"
            } finally {
                isDetecting = false
            }
        }
    }

    fun reset() {
        detectionResult = null
        selectedImageUri = null
        showResultSheet = false
        errorMsg = ""
    }
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = AuthRepository.getInstance(application)
    val messages = mutableStateListOf<ChatMessage>()
    var isSending by mutableStateOf(false)
    var errorMsg by mutableStateOf("")

    init {
        messages.add(ChatMessage(UUID.randomUUID().toString(), ChatRole.AI, "您好，我是稽古云语智能助手。"))
    }

    fun sendMessage(text: String, imageUri: Uri? = null, artifactId: Long? = null) {
        if (text.isBlank() && imageUri == null) return

        // 构造用户消息
        messages.add(ChatMessage(UUID.randomUUID().toString(), ChatRole.USER, text, imageUri = imageUri))

        viewModelScope.launch {
            isSending = true
            errorMsg = ""

            try {
                val token = auth.bearerToken()

                // 构造上下文历史
                val contextHistory = messages.filter { it.role != ChatRole.AI || it.content != "您好，我是稽古云语智能助手。" }
                    .map {
                        DialogMessage(
                            role = if (it.role == ChatRole.USER) "USER" else "AI",
                            content = it.content
                        )
                    }

                // 提交对话请求
                val taskResponse = NetworkModule.api.createDialogRequest(
                    token = token,
                    req = DialogRequest(query = text, artifactId = artifactId, contextHistory = contextHistory)
                )

                if (taskResponse.code == 200 && taskResponse.data != null) {
                    val taskId = taskResponse.data.taskId

                    // 轮询结果
                    var isCompleted = false
                    var retryCount = 0
                    val maxRetries = 15

                    while (!isCompleted && retryCount < maxRetries) {
                        delay(2000)
                        val statusResponse = NetworkModule.api.getDialogResult(token, taskId)

                        if (statusResponse.code == 200 && statusResponse.data != null) {
                            val status = statusResponse.data.status
                            if (status == "COMPLETED") {
                                isCompleted = true
                                val replyText = statusResponse.data.reply ?: "抱歉，我没有理解您的问题。"
                                messages.add(ChatMessage(UUID.randomUUID().toString(), ChatRole.AI, replyText))
                            } else if (status == "FAILED") {
                                errorMsg = "对话失败，请重试"
                                break
                            }
                        }
                        retryCount++
                    }

                    if (!isCompleted && errorMsg.isEmpty()) {
                        errorMsg = "对话超时，请稍后重试"
                    }
                } else {
                    errorMsg = taskResponse.message.ifBlank { "发送失败" }
                }

                if (errorMsg.isNotEmpty()) {
                    messages.add(ChatMessage(UUID.randomUUID().toString(), ChatRole.AI, errorMsg))
                }

            } catch (e: Exception) {
                e.printStackTrace()
                messages.add(ChatMessage(UUID.randomUUID().toString(), ChatRole.AI, "网络异常: ${e.message}"))
            } finally {
                isSending = false
            }
        }
    }

    fun clearHistory(artifactId: Long? = null) {
        viewModelScope.launch {
            try {
                val token = auth.bearerToken()
                // API 接口中没有 artifactId 参数，如果需要可以加上
                NetworkModule.api.clearDialogHistories(token)
                messages.clear()
                messages.add(ChatMessage(UUID.randomUUID().toString(), ChatRole.AI, "对话已清空。您可以重新开始提问。"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = AuthRepository.getInstance(application)
    val currentUser = auth.currentUser

    // 新增状态
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf("")

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            try {
                // 调用后端登出接口
                NetworkModule.api.logout(auth.bearerToken())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // 无论后端是否成功，都清除本地状态
                auth.clearSession()
                onLogout()
            }
        }
    }

    // 新增修改密码方法
    fun changePassword(oldPassword: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMsg = ""
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.changePassword(token, ChangePasswordRequest(oldPassword, newPassword))
                if (response.code == 200) {
                    onSuccess()
                } else {
                    errorMsg = response.message.ifBlank { "修改密码失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = AuthRepository.getInstance(application)
    val histories = mutableStateListOf<DialogHistory>()
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf("")

    private var currentPage = 0
    private var isLastPage = false

    init { loadHistory(refresh = true) }

    fun loadHistory(refresh: Boolean = false) {
        if (refresh) {
            currentPage = 0
            isLastPage = false
        } else {
            if (isLastPage || isLoading) return
        }

        viewModelScope.launch {
            isLoading = true
            errorMsg = ""
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.getDialogHistories(token, page = currentPage, size = 10)

                if (response.code == 200 && response.data != null) {
                    if (refresh) histories.clear()
                    histories.addAll(response.data.list)

                    isLastPage = response.data.currentPage >= response.data.totalPages - 1
                    if (!isLastPage) {
                        currentPage++
                    }
                } else {
                    errorMsg = response.message.ifBlank { "加载历史记录失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.clearDialogHistories(token)
                if (response.code == 200) {
                    histories.clear()
                } else {
                    errorMsg = response.message.ifBlank { "清空失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            }
        }
    }
}

class FeedbackViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = AuthRepository.getInstance(application)
    var isSending by mutableStateOf(false)
    var errorMsg by mutableStateOf("")

    fun submitFeedback(type: String, content: String, rating: Int?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isSending = true
            errorMsg = ""
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.submitFeedback(
                    token,
                    FeedbackRequest(type, content, rating)
                )
                if (response.code == 200) {
                    onSuccess()
                } else {
                    errorMsg = response.message.ifBlank { "提交反馈失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            } finally {
                isSending = false
            }
        }
    }
}

class ArchaeologyViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = AuthRepository.getInstance(application)
    var isSubmitting by mutableStateOf(false)
    var errorMsg by mutableStateOf("")

    fun submitArtifact(name: String, desc: String, bitmap: Bitmap?, location: String, era: String, tags: List<String>, onSuccess: () -> Unit) {
        if (bitmap == null) return
        viewModelScope.launch {
            isSubmitting = true
            errorMsg = ""
            try {
                val base64 = ImageUtils.bitmapToBase64(bitmap)
                val token = auth.bearerToken()
                val response = NetworkModule.api.submitArchaeologyArtifact(
                    token,
                    ArchaeologySubmitRequest(name, desc, base64, location, era, tags)
                )
                if (response.code == 200) {
                    onSuccess()
                } else {
                    errorMsg = response.message.ifBlank { "提交失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            } finally {
                isSubmitting = false
            }
        }
    }
}

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = AuthRepository.getInstance(application)
    
    val feedbacks = mutableStateListOf<AdminFeedback>()
    val users = mutableStateListOf<AdminUser>()
    
    var isLoadingFeedbacks by mutableStateOf(false)
    var isLoadingUsers by mutableStateOf(false)
    var errorMsg by mutableStateOf("")
    // 新增属性
    var pendingArtifacts = mutableStateListOf<Artifact>()
    var dashboardOverview by mutableStateOf<DashboardOverview?>(null)

    // 加载待审核文物
    fun loadPendingArtifacts(refresh: Boolean = false) {
        viewModelScope.launch {
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.getPendingArtifacts(token)
                if (response.code == 200 && response.data != null) {
                    pendingArtifacts.clear()
                    pendingArtifacts.addAll(response.data.list)
                } else {
                    errorMsg = response.message.ifBlank { "加载待审核文物失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            }
        }
    }

    // 审核文物
    fun auditArtifact(id: Long, status: String, notes: String? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.auditArtifact(token, id, UpdateStatusRequest(status, notes))
                if (response.code == 200) {
                    onSuccess()
                    loadPendingArtifacts() // 刷新列表
                } else {
                    errorMsg = response.message.ifBlank { "审核失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            }
        }
    }

    // 新增文物
    fun createArtifact(request: ArchaeologySubmitRequest, onSuccess: (Artifact) -> Unit) {
        viewModelScope.launch {
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.createArtifact(token, request)
                if (response.code == 200 && response.data != null) {
                    onSuccess(response.data)
                } else {
                    errorMsg = response.message.ifBlank { "创建文物失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            }
        }
    }

    // 更新文物
    fun updateArtifact(id: Long, request: ArchaeologySubmitRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.updateArtifact(token, id, request)
                if (response.code == 200) {
                    onSuccess()
                } else {
                    errorMsg = response.message.ifBlank { "更新文物失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            }
        }
    }

    // 删除文物
    fun deleteArtifact(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.deleteArtifact(token, id)
                if (response.code == 200) {
                    onSuccess()
                } else {
                    errorMsg = response.message.ifBlank { "删除文物失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            }
        }
    }

    // 加载仪表盘概览
    fun loadDashboardOverview() {
        viewModelScope.launch {
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.getDashboardOverview(token)
                if (response.code == 200 && response.data != null) {
                    dashboardOverview = response.data
                } else {
                    errorMsg = response.message.ifBlank { "加载概览失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            }
        }
    }

    // 加载所有用户对话历史（可选，用于新页面）
    var allDialogHistories = mutableStateListOf<DialogHistory>()
    fun loadAllDialogHistories(userId: Long? = null, artifactId: Long? = null, refresh: Boolean = false) {
        viewModelScope.launch {
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.getAllDialogHistories(token, userId = userId, artifactId = artifactId)
                if (response.code == 200 && response.data != null) {
                    allDialogHistories.clear()
                    allDialogHistories.addAll(response.data.list)
                } else {
                    errorMsg = response.message.ifBlank { "加载历史记录失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            }
        }
    }
    fun loadFeedbacks() {
        viewModelScope.launch {
            isLoadingFeedbacks = true
            errorMsg = ""
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.getAdminFeedbacks(token)
                if (response.code == 200 && response.data != null) {
                    feedbacks.clear()
                    feedbacks.addAll(response.data.list)
                } else {
                    errorMsg = response.message.ifBlank { "加载反馈失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            } finally {
                isLoadingFeedbacks = false
            }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            isLoadingUsers = true
            errorMsg = ""
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.getAdminUsers(token)
                if (response.code == 200 && response.data != null) {
                    users.clear()
                    users.addAll(response.data.list)
                } else {
                    errorMsg = response.message.ifBlank { "加载用户失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            } finally {
                isLoadingUsers = false
            }
        }
    }

    fun updateFeedbackStatus(id: Long, status: String) {
        viewModelScope.launch {
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.updateFeedbackStatus(token, id, UpdateStatusRequest(status))
                if (response.code == 200) {
                    loadFeedbacks()
                } else {
                    errorMsg = response.message.ifBlank { "更新失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateUserStatus(userId: Long, status: String) {
        viewModelScope.launch {
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.updateUserStatus(token, userId, UpdateStatusRequest(status))
                if (response.code == 200) {
                    loadUsers()
                } else {
                    errorMsg = response.message.ifBlank { "更新失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateUserRole(userId: Long, role: String) {
        viewModelScope.launch {
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.updateUserRole(token, userId, UpdateRoleRequest(role))
                if (response.code == 200) {
                    loadUsers()
                } else {
                    errorMsg = response.message.ifBlank { "更新失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
