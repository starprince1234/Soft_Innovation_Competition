package com.example.jiguyunyu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.jiguyunyu.ui.navigation.Routes
import com.example.jiguyunyu.ui.theme.*
import com.example.jiguyunyu.viewmodel.LoginViewModel

@Composable
fun LoginScreen(navController: NavController, viewModel: LoginViewModel = viewModel()) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(IvoryWhite)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 品牌标识
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Parchment)
                    .border(2.dp, Bronze, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Museum, null, tint = Bronze, modifier = Modifier.size(50.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 标题优化：Serif 衬线体
            Text(
                "稽古云语",
                fontSize = 36.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = IndigoInk,
                letterSpacing = 4.sp
            )

            Text(
                "JIGUYUNYU",
                fontSize = 12.sp,
                color = Bronze,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 快速角色切换测试区 (为了演示角色区分)
            Text("--- 开发测试：点击快速填入账号（密码均为 123456）---", fontSize = 10.sp, color = Bronze)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoleTestChip("公众用户", "user", "123456") { u, p -> username = u; password = p }
                RoleTestChip("考古专家", "arc_li", "123456") { u, p -> username = u; password = p }
                RoleTestChip("管理员", "admin", "123456") { u, p -> username = u; password = p }
            }
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("用户名 / 考古号") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Bronze,
                    unfocusedBorderColor = Bronze.copy(alpha=0.3f),
                    focusedLabelColor = Bronze,
                    cursorColor = Bronze
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密 码") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Bronze,
                    unfocusedBorderColor = Bronze.copy(alpha=0.3f),
                    focusedLabelColor = Bronze,
                    cursorColor = Bronze
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (viewModel.errorMsg.isNotEmpty()) {
                Text(
                    text = viewModel.errorMsg,
                    color = CinnabarRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isRegisterMode) {
                        viewModel.register(username, password) {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    } else {
                        viewModel.login(username, password) {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).shadow(4.dp, RoundedCornerShape(25.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = CinnabarRed),
                shape = RoundedCornerShape(25.dp)
            ) {
                if(viewModel.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (isRegisterMode) "注 册" else "登 录", fontSize = 18.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Serif)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                Text(
                    text = if (isRegisterMode) "已有账号？去登录" else "没有账号？去注册",
                    color = IndigoInk,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun RoleTestChip(label: String, u: String, p: String, onClick: (String, String) -> Unit) {
    Surface(
        onClick = { onClick(u, p) },
        shape = RoundedCornerShape(16.dp),
        color = Parchment,
        border = androidx.compose.foundation.BorderStroke(1.dp, Bronze.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Charcoal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
