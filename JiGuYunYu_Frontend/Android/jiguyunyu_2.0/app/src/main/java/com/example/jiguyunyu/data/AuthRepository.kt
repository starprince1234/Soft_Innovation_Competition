package com.example.jiguyunyu.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    // 用于通知 UI 层需要跳转到登录页
    private val _requireLogin = MutableStateFlow(false)
    val requireLogin = _requireLogin.asStateFlow()

    init {
        // App启动时尝试恢复会话
        val token = prefs.getString("token", null)
        val username = prefs.getString("username", null)
        val roleStr = prefs.getString("role", null)

        if (token != null && username != null) {
            val role = try {
                UserRole.valueOf(roleStr ?: "PUBLIC")
            } catch (e: Exception) { UserRole.PUBLIC }

            _currentUser.value = User(id = "local", username = username, role = role, token = token)
        }
    }

    fun saveSession(username: String, token: String, roleStrings: List<String>) {
        // 将字符串列表转换为第一个匹配的 UserRole 枚举，默认 PUBLIC
        val role = roleStrings.mapNotNull {
            try { UserRole.valueOf(it) } catch (e: Exception) { null }
        }.firstOrNull() ?: UserRole.PUBLIC

        prefs.edit {
            putString("token", token)
            putString("username", username)
            putString("role", role.name)
        }
        _currentUser.value = User(id = "local", username = username, role = role, token = token)
        _requireLogin.value = false
    }

    fun clearSession() {
        prefs.edit { clear() }
        _currentUser.value = null
    }

    fun onUnauthorized() {
        clearSession()
        _requireLogin.value = true
    }

    fun resetRequireLogin() {
        _requireLogin.value = false
    }

    fun bearerToken(): String {
        val token = prefs.getString("token", "") ?: ""
        return if (token.isNotEmpty()) "Bearer $token" else ""
    }

    companion object {
        @Volatile
        private var instance: AuthRepository? = null
        fun getInstance(context: Context): AuthRepository = instance ?: synchronized(this) {
            instance ?: AuthRepository(context.applicationContext).also { instance = it }
        }
    }
}
