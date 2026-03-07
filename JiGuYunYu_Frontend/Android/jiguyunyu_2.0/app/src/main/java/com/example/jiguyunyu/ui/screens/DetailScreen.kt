package com.example.jiguyunyu.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.jiguyunyu.ui.navigation.Routes
import com.example.jiguyunyu.ui.theme.*
import com.example.jiguyunyu.viewmodel.DetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(navController: NavController, artifactId: String?, viewModel: DetailViewModel = viewModel()) {
    val id = artifactId?.toLongOrNull()

    LaunchedEffect(id) {
        if (id != null) {
            viewModel.loadArtifact(id)
        }
    }

    val artifact = viewModel.artifact
    val scrollState = rememberScrollState()

    Scaffold(
        floatingActionButton = {
            AnimatedVisibility(
                visible = !viewModel.isLoading && artifact != null,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        navController.navigate(Routes.chat(id))
                    },
                    containerColor = CinnabarRed,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.ChatBubble, null) },
                    text = { Text("智能问答", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(32.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                )
            }
        },
        containerColor = IvoryWhite
    ) { padding ->
        if (viewModel.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BronzeLoadingSpinner()
            }
        } else if (viewModel.errorMsg.isNotEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(viewModel.errorMsg, color = CinnabarRed, fontFamily = FontFamily.Serif)
            }
        } else if (artifact == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("未找到文物信息", color = Bronze, fontFamily = FontFamily.Serif)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                // 背景底层图片
                AsyncImage(
                    model = artifact.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp),
                    contentScale = ContentScale.Crop
                )

                // 渐变蒙层，营造光影感
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    IvoryWhite
                                ),
                                startY = 0f,
                                endY = 1200f
                            )
                        )
                )

                // 滚动内容层
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    Spacer(modifier = Modifier.height(380.dp))

                    // 内容卡片区
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(24.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                        color = IvoryWhite,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 32.dp)
                                .fillMaxWidth()
                        ) {
                            // 标题区域
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = artifact.name,
                                        fontSize = 32.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        color = Charcoal,
                                        lineHeight = 40.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, null, tint = Bronze, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = artifact.location.ifBlank { "藏于云语博物馆" },
                                            fontSize = 14.sp,
                                            color = Bronze,
                                            fontFamily = FontFamily.Serif
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { /* 语音播放功能 */ },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Parchment, CircleShape)
                                        .border(1.dp, Bronze.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(Icons.Default.VolumeUp, null, tint = IndigoInk, modifier = Modifier.size(28.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // 基础信息展示
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                InfoBadge(label = "年代", value = artifact.era)
                                InfoBadge(label = "类别", value = artifact.category)
                            }

                            // 标签列表
                            if (!artifact.tags.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(artifact.tags) { tag ->
                                        TagBadge(tag)
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 32.dp),
                                color = Bronze.copy(alpha = 0.15f)
                            )

                            // 详细介绍
                            Text(
                                text = "文物赏析",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Charcoal,
                                fontFamily = FontFamily.Serif
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = artifact.description.ifBlank { "这是一件珍贵的历史文物，展现了中国古代精湛的工艺与深厚的文化底蕴。" },
                                fontSize = 17.sp,
                                lineHeight = 32.sp,
                                color = Charcoal.copy(alpha = 0.9f),
                                letterSpacing = 0.6.sp,
                                fontFamily = FontFamily.Serif
                            )

                            Spacer(modifier = Modifier.height(120.dp)) // 留出 FAB 空间
                        }
                    }
                }

                // 悬浮顶部操作栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoBadge(label: String, value: String) {
    Surface(
        color = IndigoInk.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(60.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(label, fontSize = 10.sp, color = Bronze, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 15.sp, color = IndigoInk, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        }
    }
}

@Composable
fun TagBadge(tag: String) {
    Box(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(0.5.dp, Bronze.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(tag, fontSize = 12.sp, color = Bronze, fontFamily = FontFamily.Serif)
    }
}
