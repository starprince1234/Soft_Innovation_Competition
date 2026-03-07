package com.example.jiguyunyu.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.jiguyunyu.data.Artifact
import com.example.jiguyunyu.data.ChatMessage
import com.example.jiguyunyu.data.ChatRole
import com.example.jiguyunyu.data.RagSource
import com.example.jiguyunyu.ui.navigation.Routes
import com.example.jiguyunyu.ui.theme.*
import com.example.jiguyunyu.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, artifactId: String? = null, viewModel: ChatViewModel = viewModel()) {
    var text by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size - 1)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    Column(modifier = Modifier.fillMaxSize().background(IvoryWhite)) {
        CenterAlignedTopAppBar(
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = IndigoInk)
                }
            },
            title = { Text("云语助手", color = IndigoInk, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif) },
            actions = {
                IconButton(onClick = { viewModel.clearHistory() }) {
                    Icon(Icons.Default.DeleteSweep, "清空历史", tint = Bronze)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = IvoryWhite)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(viewModel.messages) { msg ->
                ChatBubble(msg, navController)
            }
            if (viewModel.isSending) {
                item { ThinkingIndicator() }
            }
        }

        // 输入区域
        Surface(
            shadowElevation = 12.dp,
            color = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (selectedImageUri != null) {
                    SelectedImagePreview(uri = selectedImageUri!!) { selectedImageUri = null }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.background(Parchment, CircleShape).size(40.dp)
                    ) { Icon(Icons.Default.Add, null, tint = Bronze) }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("询问历史...", color = Color.Gray) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoInk,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = IvoryWhite,
                            unfocusedContainerColor = IvoryWhite
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val hasInput = text.isNotBlank() || selectedImageUri != null
                    IconButton(
                        onClick = {
                            if (hasInput) {
                                viewModel.sendMessage(text, selectedImageUri, artifactId?.toLongOrNull())
                                text = ""
                                selectedImageUri = null
                            }
                        },
                        modifier = Modifier.background(if (hasInput) IndigoInk else Bronze, CircleShape).size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (hasInput) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                            contentDescription = if (hasInput) "发送" else "语音输入",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage, navController: NavController) {
    val isUser = msg.role == ChatRole.USER
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (!isUser) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Parchment).border(1.dp, Bronze, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SmartToy, null, tint = Bronze, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                Surface(
                    color = if (isUser) IndigoInk else Color.White,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if(isUser) 16.dp else 4.dp, bottomEnd = if(isUser) 4.dp else 16.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (msg.imageUri != null) {
                            AsyncImage(model = msg.imageUri, contentDescription = null, modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp)).padding(bottom = if (msg.content.isNotBlank()) 8.dp else 0.dp), contentScale = ContentScale.Crop)
                        }
                        if (msg.content.isNotBlank()) {
                            Text(text = msg.content, color = if (isUser) Color.White else Charcoal, fontSize = 15.sp, lineHeight = 22.sp)
                        }
                    }
                }

                // 知识来源展示
                if (!isUser && !msg.ragSources.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        msg.ragSources.forEach { source ->
                            RagSourceCard(source) { source.url?.let { uriHandler.openUri(it) } }
                        }
                    }
                }

                if (msg.relatedArtifact != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ChatArtifactCard(msg.relatedArtifact, navController)
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(CinnabarRed.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = CinnabarRed, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun RagSourceCard(source: RagSource, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.width(280.dp).clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = Parchment.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Bronze.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (source.source.contains("百度") || source.url != null) Icons.Default.Web else Icons.Default.Info,
                contentDescription = null,
                tint = Bronze,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(source.source, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoInk)
                if (!source.content.isNullOrBlank()) {
                    Text(source.content, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Charcoal.copy(alpha = 0.6f))
                }
            }
            if (!source.url.isNullOrBlank()) {
                Icon(Icons.Default.OpenInNew, null, tint = Bronze, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun ThinkingIndicator() {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Start) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Parchment).border(1.dp, Bronze, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.SmartToy, null, tint = Bronze, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(color = Color.White, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp), shadowElevation = 2.dp) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Bronze, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI 正在思考...", fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SelectedImagePreview(uri: Uri, onRemove: () -> Unit) {
    Box(modifier = Modifier.padding(bottom = 12.dp)) {
        AsyncImage(model = uri, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ChatArtifactCard(artifact: Artifact, navController: NavController) {
    Card(
        modifier = Modifier.width(280.dp).clickable { navController.navigate(Routes.detail(artifact.id)) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = artifact.imageUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(artifact.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Serif)
                Text(artifact.era, fontSize = 12.sp, color = Bronze)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("点击查看详情", fontSize = 10.sp, color = IndigoInk)
                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(12.dp), tint = IndigoInk)
                }
            }
        }
    }
}
