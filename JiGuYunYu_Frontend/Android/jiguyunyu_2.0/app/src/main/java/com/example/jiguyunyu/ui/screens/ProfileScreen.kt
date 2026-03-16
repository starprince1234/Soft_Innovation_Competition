package com.example.jiguyunyu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.jiguyunyu.data.UserRole
import com.example.jiguyunyu.ui.navigation.Routes
import com.example.jiguyunyu.ui.theme.*
import com.example.jiguyunyu.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(navController: NavController, viewModel: ProfileViewModel = viewModel()) {
    val user by viewModel.currentUser.collectAsState()
    val isLoading = viewModel.isLoading
    val errorMsg = viewModel.errorMsg

    var showChangePasswordDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(IvoryWhite),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 头部信息
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AccountCircle, null,
                        modifier = Modifier.size(80.dp),
                        tint = Bronze
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(user?.username ?: "未登录", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Charcoal)

                    user?.role?.let { role ->
                        val roleName = when(role) {
                            UserRole.ARCHAEOLOGIST -> "认证考古人员"
                            UserRole.MANAGER -> "博物馆管理者"
                            else -> "文物爱好者"
                        }
                        Surface(color = Parchment, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                            Text(roleName, fontSize = 12.sp, color = Bronze, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }

        // 菜单
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                MenuRow(Icons.Outlined.History, "历史识别记录") { navController.navigate(Routes.HISTORY) }
                MenuRow(Icons.Outlined.Lock, "修改密码") { showChangePasswordDialog = true }   // 新增修改密码入口
                MenuRow(Icons.Outlined.FavoriteBorder, "我的收藏") {
                    navController.navigate(Routes.FAVORITES) {
                        popUpTo(Routes.PROFILE) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                MenuRow(Icons.Outlined.Feedback, "问题反馈") { navController.navigate(Routes.FEEDBACK) }

                if (user?.role == UserRole.ARCHAEOLOGIST) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("专家功能", fontSize = 12.sp, color = Bronze, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom=8.dp))
                    MenuRow(Icons.Outlined.UploadFile, "文物信息录入") { navController.navigate(Routes.ARCHAEOLOGY_UPLOAD) }
                }

                if (user?.role == UserRole.MANAGER) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("管理功能", fontSize = 12.sp, color = IndigoInk, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom=8.dp))
                    MenuRow(Icons.Outlined.AdminPanelSettings, "反馈管理") { navController.navigate(Routes.ADMIN_FEEDBACK) }
                    MenuRow(Icons.Outlined.Group, "用户管理") { navController.navigate(Routes.ADMIN_USERS) }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // 红色退出按钮
                OutlinedButton(
                    onClick = { viewModel.logout { navController.navigate(Routes.LOGIN) { popUpTo(0) } } },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CinnabarRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinnabarRed)
                ) {
                    Text("退出登录", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // 修改密码对话框
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { oldPwd, newPwd ->
                viewModel.changePassword(oldPwd, newPwd) {
                    showChangePasswordDialog = false
                    // 可选：显示成功提示
                }
            },
            isLoading = isLoading,
            errorMsg = errorMsg
        )
    }
}

@Composable
fun MenuRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(60.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = IndigoInk, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Charcoal, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (oldPassword: String, newPassword: String) -> Unit,
    isLoading: Boolean,
    errorMsg: String
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改密码") },
        text = {
            Column {
                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = CinnabarRed, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("旧密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        passwordError = if (confirmPassword.isNotEmpty() && it != confirmPassword) "两次密码不一致" else null
                    },
                    label = { Text("新密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        passwordError = if (newPassword.isNotEmpty() && it != newPassword) "两次密码不一致" else null
                    },
                    label = { Text("确认新密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = passwordError != null
                )
                if (passwordError != null) {
                    Text(passwordError!!, color = CinnabarRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(oldPassword, newPassword) },
                enabled = !isLoading && oldPassword.isNotBlank() && newPassword.isNotBlank() && newPassword == confirmPassword && passwordError == null,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoInk)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("确认")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
