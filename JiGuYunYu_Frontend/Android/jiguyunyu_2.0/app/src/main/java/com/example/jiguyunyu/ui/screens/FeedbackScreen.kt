package com.example.jiguyunyu.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.jiguyunyu.ui.theme.*
import com.example.jiguyunyu.viewmodel.FeedbackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(navController: NavController, viewModel: FeedbackViewModel = viewModel()) {
    var content by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("功能建议") }
    var rating by remember { mutableStateOf(5) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("意见反馈") }, 
                navigationIcon = { 
                    IconButton(onClick={navController.popBackStack()}) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null) 
                    } 
                }
            ) 
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // 类型
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("功能建议", "Bug反馈", "其他").forEach { t ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 评分
            Row {
                repeat(5) { i ->
                    IconButton(onClick = { rating = i + 1 }) {
                        Icon(if(i<rating) Icons.Filled.Star else Icons.Outlined.StarBorder, null, tint = Color(0xFFFFB300))
                    }
                }
            }

            // 内容
            OutlinedTextField(
                value = content,
                onValueChange = { content=it },
                label = { Text("详细描述") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 截图
            Text("上传截图 (可选)")
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.LightGray.copy(0.3f), RoundedCornerShape(8.dp))
                    .clickable { launcher.launch("image/*") }
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(bitmap!!.asImageBitmap(), null, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Add, null)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { 
                    viewModel.submitFeedback(type, content, rating) { 
                        navController.popBackStack() 
                    } 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoInk),
                enabled = !viewModel.isSending && content.isNotBlank()
            ) {
                if(viewModel.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text("提交")
                }
            }
        }
    }
}
