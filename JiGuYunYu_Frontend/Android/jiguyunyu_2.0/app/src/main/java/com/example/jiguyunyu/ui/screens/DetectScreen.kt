package com.example.jiguyunyu.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.jiguyunyu.ui.navigation.Routes
import com.example.jiguyunyu.ui.theme.*
import com.example.jiguyunyu.viewmodel.DetectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectScreen(navController: NavController, viewModel: DetectViewModel = viewModel()) {
    val context = LocalContext.current

    // 相册选择器
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.selectedImageUri = uri
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            }
            viewModel.detect(bitmap)
        }
    }

    // 相机启动器 (简化实现：直接调用系统相机并获取缩略图，实际项目应使用 FileProvider 获取原图)
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            // 暂时将 bitmap 转存为 uri 或直接使用 (此处简化逻辑直接传bitmap)
            viewModel.detect(bitmap)
        }
    }

    LaunchedEffect(viewModel.errorMsg) {
        if (viewModel.errorMsg.isNotEmpty()) {
            Toast.makeText(context, viewModel.errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 模拟相机预览区域 (实际应为 CameraX PreviewView)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp) // 留出底部操作区
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            if (viewModel.selectedImageUri != null) {
                AsyncImage(
                    model = viewModel.selectedImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("相机预览区域", color = Color.White)
                // 这里可以用 CameraX 实现真实预览
            }

            // 扫描框动画效果
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .border(2.dp, CinnabarRed.copy(alpha=0.6f), RoundedCornerShape(16.dp))
            )

            if (viewModel.isDetecting) {
                CircularProgressIndicator(color = CinnabarRed, modifier = Modifier.size(64.dp))
            }
        }

        // 底部操作栏
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(180.dp)
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 相册按钮
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { galleryLauncher.launch("image/*") }) {
                    Icon(Icons.Outlined.Image, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Text("相册", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }

                // 拍照快门 (大按钮)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { cameraLauncher.launch(null) }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.Black, CircleShape)
                            .background(CinnabarRed)
                    )
                }

                // 占位/帮助
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CenterFocusStrong, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Text("对焦", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        // 结果弹窗 (ModalBottomSheet)
        if (viewModel.showResultSheet && viewModel.detectionResult != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.showResultSheet = false },
                containerColor = IvoryWhite,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Bronze) }
            ) {
                val result = viewModel.detectionResult!!
                Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(result.label, fontSize = 24.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = IndigoInk)
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(color = CinnabarRed.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text("置信度 ${(result.confidence * 100).toInt()}%", color = CinnabarRed, fontSize = 12.sp, modifier = Modifier.padding(4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(result.description, color = Charcoal, lineHeight = 24.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val hasDetail = result.artifactId != null
                        Button(
                            onClick = {
                                viewModel.showResultSheet = false
                                result.artifactId?.let { navController.navigate(Routes.detail(it)) }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled = hasDetail,
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoInk)
                        ) {
                            Text("查看详情")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.showResultSheet = false
                                val detectContext = "识别结果: ${result.label}; 置信度: ${(result.confidence * 100).toInt()}%; ${result.description}"
                                navController.navigate(Routes.chat(result.artifactId, detectContext))
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CinnabarRed)
                        ) {
                            Icon(Icons.Outlined.ChatBubble, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("直接提问")
                        }
                    }
                }
            }
        }
    }
}
