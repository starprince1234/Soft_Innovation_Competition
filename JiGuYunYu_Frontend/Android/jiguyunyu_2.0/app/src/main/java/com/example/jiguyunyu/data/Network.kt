package com.example.jiguyunyu.data

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.UUID
import java.util.concurrent.TimeUnit

// 登录请求体
data class LoginRequest(val username: String, val password: String)

// 注册请求体
data class RegisterRequest(val username: String, val password: String)

// 登录响应体
data class LoginResponse(
    val token: String,
    val roles: List<String>,
    val expiresIn: Long
)

// API 接口定义
interface ApiService {

    // --- 认证 ---
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<Unit>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Header("Authorization") token: String): ApiResponse<Unit>

    // --- 文物 ---
    @GET("api/v1/artifacts")
    suspend fun getArtifacts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
        @Query("name") name: String? = null,
        @Query("era") era: String? = null,
        @Query("keyword") keyword: String? = null
    ): ApiResponse<Page<Artifact>>

    @GET("api/v1/artifact/{id}")
    suspend fun getArtifactDetail(@Path("id") id: Long): ApiResponse<Artifact>

    // --- 检测识别 ---
    @POST("api/v1/detect/tasks")
    suspend fun createDetectTask(@Header("Authorization") token: String, @Body req: DetectRequest): ApiResponse<DetectResultResponse>

    @GET("api/v1/detect/tasks/{taskId}/status")
    suspend fun getDetectTaskStatus(@Header("Authorization") token: String, @Path("taskId") taskId: Long): ApiResponse<DetectResultResponse>

    @GET("api/v1/detect/results/{taskId}")
    suspend fun getDetectTaskResult(@Header("Authorization") token: String, @Path("taskId") taskId: Long): ApiResponse<DetectResultResponse>

    // --- 对话 ---
    @POST("api/v1/dialog/requests")
    suspend fun createDialogRequest(@Header("Authorization") token: String, @Body req: DialogRequest): ApiResponse<DialogResultResponse>

    @GET("api/v1/dialog/results/{taskId}")
    suspend fun getDialogResult(@Header("Authorization") token: String, @Path("taskId") taskId: Long): ApiResponse<DialogResultResponse>

    // --- 历史记录 ---
    @GET("api/v1/dialog/histories")
    suspend fun getDialogHistories(@Header("Authorization") token: String, @Query("page") page: Int = 0, @Query("size") size: Int = 10): ApiResponse<Page<DialogHistory>>

    @GET("api/v1/dialog/conversations/{conversationId}/context")
    suspend fun getDialogConversationContext(
        @Header("Authorization") token: String,
        @Path("conversationId") conversationId: Long
    ): ApiResponse<List<DialogMessage>>

    @DELETE("api/v1/dialog/histories")
    suspend fun clearDialogHistories(@Header("Authorization") token: String): ApiResponse<Unit>

    // --- 反馈 ---
    @POST("api/v1/feedback")
    suspend fun submitFeedback(@Header("Authorization") token: String, @Body request: FeedbackRequest): ApiResponse<FeedbackResponse>

    // --- 考古录入 ---
    @POST("api/v1/archaeology/artifacts")
    suspend fun submitArchaeologyArtifact(@Header("Authorization") token: String, @Body request: ArchaeologySubmitRequest): ApiResponse<Unit>

    // --- 管理员接口 ---
    @GET("api/v1/admin/feedback")
    suspend fun getAdminFeedbacks(@Header("Authorization") token: String, @Query("page") page: Int = 0, @Query("size") size: Int = 10, @Query("status") status: String? = null): ApiResponse<Page<AdminFeedback>>

    @PUT("api/v1/admin/feedback/{id}/status")
    suspend fun updateFeedbackStatus(@Header("Authorization") token: String, @Path("id") id: Long, @Body body: UpdateStatusRequest): ApiResponse<Unit>

    @GET("api/v1/admin/users")
    suspend fun getAdminUsers(
        @Header("Authorization") token: String,
        @Query("includeInactive") includeInactive: Boolean = true,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 200
    ): ApiResponse<Page<AdminUser>>

    @PUT("api/v1/admin/users/{userId}/status")
    suspend fun updateUserStatus(@Header("Authorization") token: String, @Path("userId") userId: Long, @Body body: UpdateStatusRequest): ApiResponse<Unit>

    @PUT("api/v1/admin/users/{userId}/role")
    suspend fun updateUserRole(@Header("Authorization") token: String, @Path("userId") userId: Long, @Body body: UpdateRoleRequest): ApiResponse<Unit>

    // --- 管理员：文物管理（新增/编辑/删除）---
    @POST("api/v1/admin/artifacts")
    suspend fun createArtifact(@Header("Authorization") token: String, @Body request: AdminArtifactRequest): ApiResponse<Artifact>

    @PUT("api/v1/admin/artifacts/{id}")
    suspend fun updateArtifact(@Header("Authorization") token: String, @Path("id") id: Long, @Body request: AdminArtifactRequest): ApiResponse<Unit>

    @DELETE("api/v1/admin/artifacts/{id}")
    suspend fun deleteArtifact(@Header("Authorization") token: String, @Path("id") id: Long): ApiResponse<Unit>

    // --- 管理员：待审核文物 ---
    @GET("api/v1/admin/artifacts/pending")
    suspend fun getPendingArtifacts(@Header("Authorization") token: String, @Query("page") page: Int = 0, @Query("size") size: Int = 10): ApiResponse<Page<Artifact>>

    // --- 管理员：审核文物 ---
    @PUT("api/v1/admin/artifacts/{id}/status")
    suspend fun auditArtifact(@Header("Authorization") token: String, @Path("id") id: Long, @Body body: UpdateStatusRequest): ApiResponse<Unit>

    // --- 管理员：查看所有对话历史 ---
    @GET("api/v1/admin/dialog/histories")
    suspend fun getAllDialogHistories(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 200,
        @Query("recentHours") recentHours: Int = 24,
        @Query("userId") userId: Long? = null,
        @Query("artifactId") artifactId: Long? = null
    ): ApiResponse<Page<DialogHistory>>

    // --- 管理员：仪表盘概览 ---
    @GET("api/v1/admin/dashboard/overview")
    suspend fun getDashboardOverview(@Header("Authorization") token: String): ApiResponse<DashboardOverview>

    // --- 用户管理：修改密码 ---
    @PUT("api/v1/users/me/password")
    suspend fun changePassword(@Header("Authorization") token: String, @Body request: ChangePasswordRequest): ApiResponse<Unit>

    @GET("api/v1/health")
    suspend fun checkHealth(): ApiResponse<Map<String, Any?>>
}

object NetworkModule {
    // Cloud production endpoint for real-device testing.
    private const val BASE_URL = "http://123.58.215.154:28080/"
    private const val ENABLE_MOCK = false
    var authRepository: AuthRepository? = null

    private val authInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        if (response.code == 401) authRepository?.onUnauthorized()
        response
    }

    private val mockInterceptor = Interceptor { chain ->
        val request = chain.request()
        val path = request.url.encodedPath
        val eraParam = request.url.queryParameter("era")
        val nameParam = request.url.queryParameter("name")

        Thread.sleep(300)

        val jsonResponse = when {
            path.endsWith("/auth/login") -> {
                val buffer = Buffer()
                request.body?.writeTo(buffer)
                val bodyString = buffer.readUtf8()
                
                val roles = when {
                    bodyString.contains("admin") -> listOf("MANAGER")
                    bodyString.contains("arc") -> listOf("ARCHAEOLOGIST")
                    else -> listOf("PUBLIC")
                }

                """
                {
                    "code": 200, 
                    "message": "success",
                    "data": {
                        "token": "mock-token-${UUID.randomUUID()}",
                        "roles": ${roles.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" }},
                        "expiresIn": 3600
                    }
                }
                """.trimIndent()
            }

            path.endsWith("/artifacts") -> {
                val allItems = listOf(
                    Artifact(1, "青铜神树", "商代", "青铜", "https://picsum.photos/seed/artifact1/400/400", description = "出土于三星堆遗址，是中国发现的体量最大的青铜器之一。", location = "三星堆博物馆", tags = listOf("国宝", "青铜")),
                    Artifact(2, "越王勾践剑", "春秋", "青铜器", "https://picsum.photos/seed/artifact2/400/400", description = "春秋晚期越国青铜器，国家一级文物。", location = "湖北省博物馆", tags = listOf("兵器", "春秋")),
                    Artifact(3, "千里江山图", "北宋", "书画", "https://picsum.photos/seed/art3/400/400", description = "中国青绿山水画的巅峰之作。", location = "故宫博物院", tags = listOf("名画", "山水")),
                    Artifact(4, "金缕玉衣", "西汉", "玉器", "https://picsum.photos/seed/art4/400/400", description = "汉代最高等级的葬服。", location = "河北博物院", tags = listOf("汉代", "玉器")),
                    Artifact(5, "曾侯乙编钟", "战国", "青铜器", "https://picsum.photos/seed/art5/400/400", description = "展现了极高的铸造水平和音乐成就。", location = "湖北省博物馆", tags = listOf("乐器", "曾侯乙"))
                )
                val filtered = allItems.filter { 
                    (eraParam == null || it.era == eraParam || it.category == eraParam) &&
                    (nameParam == null || it.name.contains(nameParam))
                }
                """{
                    "code": 200, "message": "success", 
                    "data": {
                        "list": ${com.google.gson.Gson().toJson(filtered)},
                        "totalElements": ${filtered.size}, "totalPages": 1, "currentPage": 0, "pageSize": 10
                    }
                }"""
            }

            path.contains("/api/v1/artifact/") -> {
                val id = path.substringAfterLast("/").toLongOrNull() ?: 1L
                val detail = when(id) {
                    1L -> Artifact(1, "青铜神树", "商代", "青铜", "https://picsum.photos/seed/artifact1/800/800", "三星堆遗址出土的青铜神树，造型奇特，工艺精湛，是古蜀文明的代表。", "三星堆博物馆", listOf("国宝", "祭祀"))
                    2L -> Artifact(2, "越王勾践剑", "春秋", "青铜器", "https://picsum.photos/seed/artifact2/800/800", "千年不锈的‘天下第一剑’，剑身满布菱形暗格纹，极其锋利。", "湖北省博物馆", listOf("兵器", "国宝"))
                    3L -> Artifact(3, "千里江山图", "北宋", "书画", "https://picsum.photos/seed/art3/800/800", "天才少年王希孟的传世之作，长卷气势磅礴，色彩绚丽夺目。", "故宫博物院", listOf("名画", "青绿山水"))
                    else -> Artifact(id, "传世珍宝", "古代", "文物", "https://picsum.photos/seed/artifact$id/800/800", "这是一件极具历史价值的珍贵文物，见证了中华文明的辉煌。", "中国国家博物馆", listOf("珍品"))
                }
                """{"code": 200, "message": "OK", "data": ${com.google.gson.Gson().toJson(detail)}}"""
            }

            else -> """{"code": 200, "message": "success", "data": null}"""
        }

        Response.Builder()
            .code(200).message("OK").protocol(Protocol.HTTP_1_1).request(request)
            .body(jsonResponse.toResponseBody("application/json".toMediaTypeOrNull()))
            .build()
    }

    private val client = OkHttpClient.Builder().apply {
        // 对话接口可能耗时较长，避免客户端过早超时导致“服务端已成功但前端报网络错”。
        connectTimeout(20, TimeUnit.SECONDS)
        readTimeout(180, TimeUnit.SECONDS)
        writeTimeout(20, TimeUnit.SECONDS)
        callTimeout(200, TimeUnit.SECONDS)
        if (ENABLE_MOCK) {
            addInterceptor(mockInterceptor)
        }
        addInterceptor(authInterceptor)
    }.build()

    val api: ApiService by lazy {
        Retrofit.Builder().baseUrl(BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
    }
}
