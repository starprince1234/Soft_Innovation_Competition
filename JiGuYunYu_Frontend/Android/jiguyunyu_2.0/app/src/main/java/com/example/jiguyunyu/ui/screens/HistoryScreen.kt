package com.example.jiguyunyu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.jiguyunyu.ui.navigation.Routes
import com.example.jiguyunyu.ui.theme.*
import com.example.jiguyunyu.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController, viewModel: HistoryViewModel = viewModel()) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史足迹", color = IndigoInk, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif) },
                navigationIcon = {
                    IconButton(onClick={navController.popBackStack()}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint=IndigoInk)
                    }
                },
                actions = {
                    if (viewModel.histories.isNotEmpty()) {
                        IconButton(onClick={showDialog=true}) {
                            Icon(Icons.Default.DeleteSweep, "清空", tint=CinnabarRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IvoryWhite)
            )
        },
        containerColor = IvoryWhite
    ) { padding ->
        if (viewModel.histories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Bronze.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("暂无对话记录", color = Bronze, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                items(viewModel.histories) { item ->
                    HistoryCard(item, navController)
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("确认清空", fontWeight = FontWeight.Bold, color = Charcoal) },
                text = { Text("确定要删除所有对话历史吗？此操作不可恢复。", color = Charcoal.copy(alpha = 0.8f)) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.clearAllHistory(); showDialog=false },
                        colors = ButtonDefaults.buttonColors(containerColor = CinnabarRed)
                    ) {
                        Text("清空", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog=false }) {
                        Text("取消", color = Bronze)
                    }
                },
                containerColor = Color.White
            )
        }
    }
}

@Composable
fun HistoryCard(item: com.example.jiguyunyu.data.DialogHistory, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clickable {
                // 点击历史记录跳转到对话页面
                navController.navigate(Routes.chat(item.artifactId))
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 头部：时间和标签
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Parchment),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Bronze, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(item.createdAt, fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.weight(1f))

                if (item.artifactName != null) {
                    Surface(
                        color = IndigoInk.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, IndigoInk.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = item.artifactName,
                            fontSize = 11.sp,
                            color = IndigoInk,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 用户问题
            Text(
                text = item.userQuery,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Charcoal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // AI 回复预览
            Surface(
                color = IvoryWhite,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.aiResponse,
                    fontSize = 13.sp,
                    color = Charcoal.copy(0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
