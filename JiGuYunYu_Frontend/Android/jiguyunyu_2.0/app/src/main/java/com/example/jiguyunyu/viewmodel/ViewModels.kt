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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import retrofit2.HttpException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
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

private object LocalSeedArtifactStore {
    val items = listOf(
        Artifact(
            id = -1L,
            name = "测试文物·青铜礼器",
            era = "商代",
            category = "青铜",
            imageUrl = "https://picsum.photos/seed/local_artifact_1/800/800",
            description = "本地兜底测试文物：用于在后端暂无已审核数据时，验证首页展示链路。",
            location = "应用内置",
            tags = listOf("测试", "青铜")
        ),
        Artifact(
            id = -2L,
            name = "测试文物·陶器残片",
            era = "战国",
            category = "陶器",
            imageUrl = "https://picsum.photos/seed/local_artifact_2/800/800",
            description = "本地兜底测试文物：用于验证分类与详情跳转展示。",
            location = "应用内置",
            tags = listOf("测试", "陶器")
        )
    )

    fun byId(id: Long): Artifact? = items.firstOrNull { it.id == id }
}

private object DetectDetailBridge {
    private const val START_ID = -10000L
    private val mockedArtifacts = linkedMapOf<Long, Artifact>()

    fun save(result: DetectionResult): Long {
        val id = START_ID - mockedArtifacts.size
        mockedArtifacts[id] = Artifact(
            id = id,
            name = result.label.ifBlank { "识别结果" },
            era = "待考证",
            category = "识别结果",
            imageUrl = result.imageUrl,
            description = result.description.ifBlank {
                "当前模型服务未关联到文物库条目，已展示本次识别文本。后续可在管理端补充文物并完成自动关联。"
            },
            location = "识别结果",
            tags = listOf("识别", "临时")
        )
        return id
    }

    fun get(id: Long): Artifact? = mockedArtifacts[id]
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
                val normalizedUsername = username.trim()
                if (normalizedUsername.isEmpty() || pass.isEmpty()) {
                    errorMsg = "请输入用户名和密码"
                    return@launch
                }
                if (normalizedUsername.length < 3) {
                    errorMsg = "用户名至少 3 位"
                    return@launch
                }
                if (pass.length < 6) {
                    errorMsg = "密码至少 6 位"
                    return@launch
                }

                val response = NetworkModule.api.login(LoginRequest(normalizedUsername, pass))
                if (response.code == 200 && response.data != null) {
                    auth.saveSession(normalizedUsername, response.data.token, response.data.roles)
                    onSuccess()
                } else {
                    errorMsg = response.message.ifBlank { "登录失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = extractErrorMessage(e, "登录失败")
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
                val normalizedUsername = username.trim()
                if (normalizedUsername.isEmpty() || pass.isEmpty()) {
                    errorMsg = "请输入用户名和密码"
                    return@launch
                }
                if (normalizedUsername.length < 3) {
                    errorMsg = "用户名至少 3 位"
                    return@launch
                }
                if (pass.length < 6) {
                    errorMsg = "密码至少 6 位"
                    return@launch
                }

                val response = NetworkModule.api.register(RegisterRequest(normalizedUsername, pass))
                if (response.code == 200) {
                    // 注册成功后自动登录
                    login(normalizedUsername, pass, onSuccess)
                } else {
                    errorMsg = response.message.ifBlank { "注册失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = extractErrorMessage(e, "注册失败")
            } finally {
                isLoading = false
            }
        }
    }

    private fun extractErrorMessage(e: Throwable, fallback: String): String {
        if (e is HttpException) {
            val body = e.response()?.errorBody()?.string()
            if (!body.isNullOrBlank()) {
                return try {
                    val json = JSONObject(body)
                    val message = json.optString("message")
                    if (message.isNullOrBlank()) "$fallback：HTTP ${e.code()}" else message
                } catch (_: Exception) {
                    "$fallback：HTTP ${e.code()}"
                }
            }
            return "$fallback：HTTP ${e.code()}"
        }
        return "网络异常: ${e.message}"
    }
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    // UI状态数据
    private val _artifacts = MutableStateFlow<List<Artifact>>(emptyList())
    val artifacts = _artifacts.asStateFlow()
    private val _availableCategories = MutableStateFlow(ArtifactCategoryCatalog.allWithAllOption(emptyList()))
    val availableCategories = _availableCategories.asStateFlow()

    var isLoading by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
    var isLoadingMore by mutableStateOf(false)

    // 搜索和筛选状态
    var searchQuery by mutableStateOf("")
    var selectedCategory by mutableStateOf("全部")
    var selectedTags by mutableStateOf<List<String>>(emptyList())

    private var currentPage = 0
    private var isLastPage = false
    private val prefs = application.getSharedPreferences("home_cache", Application.MODE_PRIVATE)
    private val gson = Gson()
    private val cacheType = object : TypeToken<List<Artifact>>() {}.type

    init {
        restoreFromCache()
        loadArtifacts(refresh = true)
    }

    private fun restoreFromCache() {
        val json = prefs.getString("artifacts_json", null) ?: return
        runCatching {
            val cached: List<Artifact> = gson.fromJson(json, cacheType) ?: emptyList()
            if (cached.isNotEmpty()) {
                _artifacts.value = cached
                updateAvailableCategories(cached)
            }
        }
    }

    private fun cacheArtifacts(items: List<Artifact>) {
        runCatching {
            prefs.edit().putString("artifacts_json", gson.toJson(items)).apply()
        }
    }

    private fun updateAvailableCategories(items: List<Artifact>) {
        _availableCategories.value = ArtifactCategoryCatalog.allWithAllOption(emptyList())
        if (selectedCategory != "全部" && !_availableCategories.value.contains(selectedCategory)) {
            selectedCategory = "全部"
        }
    }

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
                val eraQuery: String? = null
                val keywordQuery = if (selectedCategory != "全部") selectedCategory else null
                val noFilter = nameQuery == null && keywordQuery == null

                val response = NetworkModule.api.getArtifacts(
                    page = currentPage,
                    size = 10,
                    name = nameQuery,
                    era = eraQuery,
                    keyword = keywordQuery
                )

                if (response.code == 200 && response.data != null) {
                    val responseItems = response.data.list
                    val mergedItems = responseItems
                    updateAvailableCategories(mergedItems)

                    val newItems = if (selectedCategory == "全部") {
                        mergedItems
                    } else {
                        mergedItems.filter {
                            it.category.contains(selectedCategory, ignoreCase = true)
                                    || it.tags.any { tag -> tag.contains(selectedCategory, ignoreCase = true) }
                                    || it.name.contains(selectedCategory, ignoreCase = true)
                        }
                    }
                    if (refresh) {
                        _artifacts.value = newItems
                        cacheArtifacts(newItems)
                    } else {
                        val appended = (_artifacts.value + newItems).distinctBy { it.id }
                        _artifacts.value = appended
                        cacheArtifacts(appended)
                    }

                    isLastPage = response.data.currentPage >= response.data.totalPages - 1
                    if (!isLastPage) {
                        currentPage++
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (refresh) {
                    _artifacts.value = emptyList()
                    updateAvailableCategories(emptyList())
                }
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
    private val auth = AuthRepository.getInstance(application)
    private val favoriteRepository = FavoriteRepository.getInstance(application)

    var artifact by mutableStateOf<Artifact?>(null)
    var isFavorite by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf("")

    private fun userFavoriteKey(): String {
        val user = auth.currentUser.value
        return user?.username ?: user?.id ?: "guest"
    }

    fun refreshFavoriteState(id: Long?) {
        if (id == null) {
            isFavorite = false
            return
        }
        isFavorite = favoriteRepository.isFavorite(userFavoriteKey(), id)
    }

    fun toggleFavorite(id: Long): Boolean {
        isFavorite = favoriteRepository.toggleFavorite(userFavoriteKey(), id)
        return isFavorite
    }

    fun loadArtifact(id: Long) {
        viewModelScope.launch {
            isLoading = true
            errorMsg = ""
            try {
                if (id < 0) {
                    artifact = LocalSeedArtifactStore.byId(id) ?: DetectDetailBridge.get(id)
                    if (artifact == null) {
                        errorMsg = "未找到本地文物详情"
                    }
                    return@launch
                }
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
                refreshFavoriteState(id)
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
                            val status = statusResponse.data.status
                            if (status == "COMPLETED") {
                                val resultResponse = NetworkModule.api.getDetectTaskResult(token, taskId)
                                if (resultResponse.code == 200 && resultResponse.data != null) {
                                    val top = resultResponse.data.detectedArtifacts?.firstOrNull()
                                    val descriptionText = buildString {
                                        append("识别状态: ${resultResponse.data.status}")
                                        if (top?.label?.isNotBlank() == true) {
                                            append("；识别目标: ${top.label}")
                                        }
                                        append("。若暂未关联到文物库，系统将进入临时详情页展示本次识别文本。")
                                    }
                                    val bridgedArtifactId = top?.artifactId ?: DetectDetailBridge.save(
                                        DetectionResult(
                                            label = top?.label ?: "未知文物",
                                            confidence = top?.confidence ?: 0f,
                                            description = descriptionText,
                                            imageUrl = resultResponse.data.imageUrl ?: selectedImageUri.toString(),
                                            artifactId = null
                                        )
                                    )
                                    detectionResult = DetectionResult(
                                        label = top?.label ?: "未知文物",
                                        confidence = top?.confidence ?: 0f,
                                        description = descriptionText,
                                        imageUrl = resultResponse.data.imageUrl ?: selectedImageUri.toString(),
                                        artifactId = bridgedArtifactId
                                    )
                                    showResultSheet = true
                                    isCompleted = true
                                } else {
                                    errorMsg = resultResponse.message.ifBlank { "获取检测结果失败" }
                                }
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
    private var pendingDetectContext: String? = null
    private var pinnedArtifactContext: String? = null
    private var pinnedArtifactId: Long? = null
    private var activeConversationId: Long? = null
    private var restoredConversationId: Long? = null

    init {
        messages.add(ChatMessage(UUID.randomUUID().toString(), ChatRole.AI, "您好，我是稽古云语智能助手。"))
    }

    fun seedDetectContext(context: String) {
        val normalized = context.trim()
        if (normalized.isBlank()) return
        if (pendingDetectContext == normalized) return
        pendingDetectContext = normalized

        val contextHint = "已关联识别结果：$normalized"
        if (messages.none { it.role == ChatRole.AI && it.content == contextHint }) {
            messages.add(ChatMessage(UUID.randomUUID().toString(), ChatRole.AI, contextHint))
        }
    }

    fun seedArtifactContext(artifactId: Long) {
        if (pinnedArtifactId == artifactId && !pinnedArtifactContext.isNullOrBlank()) {
            return
        }

        viewModelScope.launch {
            try {
                val artifact = if (artifactId < 0) {
                    LocalSeedArtifactStore.byId(artifactId) ?: DetectDetailBridge.get(artifactId)
                } else {
                    val response = NetworkModule.api.getArtifactDetail(artifactId)
                    if (response.code == 200) response.data else null
                }

                if (artifact != null) {
                    pinnedArtifactId = artifactId
                    pinnedArtifactContext = buildString {
                        append("[识别上下文]\n")
                        append("识别结果文物名称: ${artifact.name}\n")
                        append("artifact name: ${artifact.name}\n")
                        append("年代: ${artifact.era.ifBlank { "未知" }}\n")
                        append("类别: ${artifact.category.ifBlank { "未知" }}\n")
                        append("馆藏地: ${artifact.location.ifBlank { "未知" }}\n")
                        if (artifact.tags.isNotEmpty()) {
                            append("标签: ${artifact.tags.joinToString("、")}\n")
                        }
                        val desc = artifact.description.ifBlank { "暂无描述" }
                        append("简介: ${desc}\n")
                        append("artifact description: ${desc}")
                    }

                    val contextHint = "已关联文物：${artifact.name}（${artifact.era.ifBlank { "年代待考" }}）"
                    if (messages.none { it.role == ChatRole.AI && it.content == contextHint }) {
                        messages.add(ChatMessage(UUID.randomUUID().toString(), ChatRole.AI, contextHint))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreConversation(conversationId: Long) {
        if (restoredConversationId == conversationId) {
            return
        }

        viewModelScope.launch {
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.getDialogConversationContext(token, conversationId)
                if (response.code == 200 && response.data != null) {
                    activeConversationId = conversationId
                    restoredConversationId = conversationId
                    messages.clear()
                    messages.add(ChatMessage(UUID.randomUUID().toString(), ChatRole.AI, "您好，我是稽古云语智能助手。"))

                    response.data.forEach { turn ->
                        val role = when (turn.role.uppercase()) {
                            "USER" -> ChatRole.USER
                            else -> ChatRole.AI
                        }
                        messages.add(ChatMessage(UUID.randomUUID().toString(), role, turn.content))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

                val systemContexts = mutableListOf<DialogMessage>()
                if (!pendingDetectContext.isNullOrBlank()) {
                    systemContexts.add(
                        DialogMessage(
                            role = "SYSTEM",
                            content = "[识别上下文]\n${pendingDetectContext}"
                        )
                    )
                }
                if (!pinnedArtifactContext.isNullOrBlank()) {
                    systemContexts.add(
                        DialogMessage(
                            role = "SYSTEM",
                            content = pinnedArtifactContext!!
                        )
                    )
                }

                val contextWithDetect = systemContexts + contextHistory

                // 提交对话请求
                val taskResponse = NetworkModule.api.createDialogRequest(
                    token = token,
                    req = DialogRequest(
                        query = text,
                        artifactId = artifactId,
                        conversationId = activeConversationId,
                        contextHistory = contextWithDetect
                    )
                )

                if (taskResponse.code == 200 && taskResponse.data != null) {
                    val replyText = taskResponse.data.aiResponse.ifBlank { "抱歉，我没有理解您的问题。" }
                    messages.add(ChatMessage(UUID.randomUUID().toString(), ChatRole.AI, replyText))
                    if (taskResponse.data.conversationId != null) {
                        activeConversationId = taskResponse.data.conversationId
                        restoredConversationId = taskResponse.data.conversationId
                    }
                    pendingDetectContext = null
                } else {
                    errorMsg = taskResponse.message.ifBlank { "发送失败" }
                }

                if (errorMsg.isNotEmpty()) {
                    messages.add(ChatMessage(UUID.randomUUID().toString(), ChatRole.AI, errorMsg))
                }

            } catch (e: Exception) {
                e.printStackTrace()
                val errorText = when (e) {
                    is SocketTimeoutException, is InterruptedIOException ->
                        "本次回复耗时较长，请稍候再看历史足迹，结果不会丢失。"
                    else -> "请求失败：${e.message ?: "未知异常"}"
                }
                errorMsg = errorText
                messages.add(ChatMessage(UUID.randomUUID().toString(), ChatRole.AI, errorText))
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

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = AuthRepository.getInstance(application)
    private val favoriteRepository = FavoriteRepository.getInstance(application)

    val favorites = mutableStateListOf<Artifact>()
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf("")

    private fun userFavoriteKey(): String {
        val user = auth.currentUser.value
        return user?.username ?: user?.id ?: "guest"
    }

    fun loadFavorites() {
        viewModelScope.launch {
            isLoading = true
            errorMsg = ""
            favorites.clear()
            try {
                val ids = favoriteRepository.getFavoriteIds(userFavoriteKey()).toList().asReversed()
                for (id in ids) {
                    val artifact = if (id < 0) {
                        LocalSeedArtifactStore.byId(id) ?: DetectDetailBridge.get(id)
                    } else {
                        try {
                            val response = NetworkModule.api.getArtifactDetail(id)
                            if (response.code == 200) response.data else null
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (artifact != null) {
                        favorites.add(artifact)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "加载收藏失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun removeFavorite(artifactId: Long) {
        val key = userFavoriteKey()
        if (favoriteRepository.isFavorite(key, artifactId)) {
            favoriteRepository.toggleFavorite(key, artifactId)
        }
        favorites.removeAll { it.id == artifactId }
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
    var isSubmittingArtifact by mutableStateOf(false)
    var isAuditingArtifact by mutableStateOf(false)
    var isUpdatingFeedback by mutableStateOf(false)
    var operationHint by mutableStateOf("")
    var errorMsg by mutableStateOf("")
    // 新增属性
    var pendingArtifacts = mutableStateListOf<Artifact>()
    var dashboardOverview by mutableStateOf<DashboardOverview?>(null)
    val selectableCategories = mutableStateListOf<String>()

    fun loadSelectableCategories() {
        viewModelScope.launch {
            if (selectableCategories.isEmpty()) {
                selectableCategories.addAll(ArtifactCategoryCatalog.fallback)
            }
        }
    }

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
            isAuditingArtifact = true
            operationHint = if (status == "APPROVED") "正在审批通过..." else "正在审批拒绝..."
            val startedAt = System.currentTimeMillis()
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.auditArtifact(token, id, UpdateStatusRequest(status, notes))
                if (response.code == 200) {
                    val elapsed = System.currentTimeMillis() - startedAt
                    if (elapsed < 600) delay(600 - elapsed)
                    onSuccess()
                    loadPendingArtifacts() // 刷新列表
                    operationHint = "审批完成"
                } else {
                    errorMsg = response.message.ifBlank { "审核失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            } finally {
                isAuditingArtifact = false
            }
        }
    }

    // 新增文物
    fun createArtifact(request: AdminArtifactRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isSubmittingArtifact = true
            operationHint = "正在上传文物..."
            val startedAt = System.currentTimeMillis()
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.createArtifact(token, request)
                if (response.code in 200..299) {
                    val elapsed = System.currentTimeMillis() - startedAt
                    if (elapsed < 600) delay(600 - elapsed)
                    onSuccess()
                    operationHint = "上传完成"
                } else {
                    errorMsg = response.message.ifBlank { "创建文物失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            } finally {
                isSubmittingArtifact = false
            }
        }
    }

    // 更新文物
    fun updateArtifact(id: Long, request: AdminArtifactRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isSubmittingArtifact = true
            operationHint = "正在更新文物..."
            val startedAt = System.currentTimeMillis()
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.updateArtifact(token, id, request)
                if (response.code == 200) {
                    val elapsed = System.currentTimeMillis() - startedAt
                    if (elapsed < 600) delay(600 - elapsed)
                    onSuccess()
                    operationHint = "更新完成"
                } else {
                    errorMsg = response.message.ifBlank { "更新文物失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            } finally {
                isSubmittingArtifact = false
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
                val response = NetworkModule.api.getAllDialogHistories(
                    token = token,
                    page = 0,
                    size = 200,
                    recentHours = 24,
                    userId = userId,
                    artifactId = artifactId
                )
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
                val response = NetworkModule.api.getAdminUsers(token, includeInactive = true, page = 0, size = 200)
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
            isUpdatingFeedback = true
            operationHint = "正在更新反馈状态..."
            try {
                val token = auth.bearerToken()
                val response = NetworkModule.api.updateFeedbackStatus(token, id, UpdateStatusRequest(status))
                if (response.code == 200) {
                    loadFeedbacks()
                    operationHint = "反馈状态已更新"
                } else {
                    errorMsg = response.message.ifBlank { "更新失败" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "网络异常: ${e.message}"
            } finally {
                isUpdatingFeedback = false
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
