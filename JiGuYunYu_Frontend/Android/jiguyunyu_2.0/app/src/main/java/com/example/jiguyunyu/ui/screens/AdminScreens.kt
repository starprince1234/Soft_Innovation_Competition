package com.example.jiguyunyu.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.jiguyunyu.data.*
import com.example.jiguyunyu.ui.navigation.Routes
import com.example.jiguyunyu.ui.theme.*
import com.example.jiguyunyu.viewmodel.AdminViewModel
import java.io.ByteArrayOutputStream
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController, viewModel: AdminViewModel = viewModel()) {
    val overview = viewModel.dashboardOverview
    
    LaunchedEffect(Unit) {
        viewModel.loadDashboardOverview()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("管理控制台", color = IndigoInk, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IvoryWhite)
            )
        },
        containerColor = IvoryWhite
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (overview != null) {
                item { DashboardOverviewCard(overview) }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Bronze)
                    }
                }
            }

            item {
                Text("管理功能", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = IndigoInk, fontFamily = FontFamily.Serif)
            }

            item {
                AdminCard(title = "反馈管理", subtitle = "查看并处理用户提交的反馈", icon = Icons.Default.Feedback, color = CinnabarRed) { 
                    navController.navigate(route = Routes.ADMIN_FEEDBACK) 
                }
            }

            item {
                AdminCard(title = "用户管理", subtitle = "管理用户状态及权限分配", icon = Icons.Default.Group, color = IndigoInk) { 
                    navController.navigate(route = Routes.ADMIN_USERS) 
                }
            }

            item {
                AdminCard(title = "待审核文物", subtitle = "审核考古人员提交的文物", icon = Icons.Default.Pending, color = Bronze) { 
                    navController.navigate(route = "admin_pending_artifacts") 
                }
            }

            item {
                AdminCard(title = "新增文物", subtitle = "直接添加新文物到知识库", icon = Icons.Default.Add, color = Color(0xFF2E7D32)) { 
                    navController.navigate(route = "admin_add_artifact") 
                }
            }

            item {
                AdminCard(title = "对话历史", subtitle = "查看所有用户对话记录", icon = Icons.Default.History, color = Color(0xFF9C27B0)) { 
                    navController.navigate(route = "admin_dialog_histories") 
                }
            }
        }
    }
}

@Composable
fun DashboardOverviewCard(overview: DashboardOverview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("系统概览", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = IndigoInk)
            Spacer(modifier = Modifier.height(12.dp))
            val items = listOf(
                "用户总数" to overview.totalUsers,
                "24h活跃" to overview.activeUsersLast24h,
                "文物总数" to overview.totalArtifacts,
                "待审文物" to overview.pendingArtifactsForReview,
                "反馈总数" to overview.totalFeedback,
                "未处理反馈" to overview.unresolvedFeedback,
                "识别成功率" to "${(overview.detectionSuccessRate * 100).toInt()}%",
                "24h对话" to overview.dialogCountLast24h
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in items.indices step 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items.getOrNull(i)?.let { (label, value) ->
                            OverviewItem(label, value.toString(), Modifier.weight(1f))
                        }
                        items.getOrNull(i + 1)?.let { (label, value) ->
                            OverviewItem(label, value.toString(), Modifier.weight(1f))
                        } ?: Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewItem(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Parchment,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 12.sp, color = Bronze)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IndigoInk)
        }
    }
}

@Composable
fun AdminCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Charcoal, fontFamily = FontFamily.Serif)
                Text(subtitle, fontSize = 12.sp, color = Bronze)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFeedbackScreen(navController: NavController, viewModel: AdminViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("全部", "待处理", "已解决")

    val filteredList = when(selectedTab) {
        1 -> viewModel.feedbacks.filter { it.status == "PENDING" }
        2 -> viewModel.feedbacks.filter { it.status == "RESOLVED" }
        else -> viewModel.feedbacks
    }

    LaunchedEffect(Unit) { viewModel.loadFeedbacks() }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("反馈管理", fontFamily = FontFamily.Serif) }, 
                navigationIcon = { 
                    IconButton(onClick={navController.popBackStack()}) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null) 
                    } 
                }
            ) 
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = IvoryWhite, contentColor = IndigoInk) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().background(IvoryWhite).padding(16.dp)) {
                items(filteredList) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SuggestionChip(onClick = {}, label = { Text(item.type) })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item.username?.ifBlank { "匿名用户" } ?: "匿名用户", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(item.createdAt, fontSize = 12.sp, color = Bronze)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(item.textContent, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            if (item.status == "PENDING") {
                                Button(onClick = { viewModel.updateFeedbackStatus(item.id, "RESOLVED") }, colors = ButtonDefaults.buttonColors(containerColor = CinnabarRed)) {
                                    Text("标记解决")
                                }
                            } else {
                                Text("已解决", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (viewModel.isUpdatingFeedback) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("请稍候") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(viewModel.operationHint.ifBlank { "正在处理反馈..." })
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserScreen(navController: NavController, viewModel: AdminViewModel = viewModel()) {
    var showRoleDialog by remember { mutableStateOf<AdminUser?>(null) }
    LaunchedEffect(Unit) { viewModel.loadUsers() }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("用户管理", fontFamily = FontFamily.Serif) }, 
                navigationIcon = { 
                    IconButton(onClick={navController.popBackStack()}) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null) 
                    } 
                }
            ) 
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(viewModel.users) { user ->
                ListItem(
                    headlineContent = { Text(user.username, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("${user.role} | ${user.status}") },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { showRoleDialog = user }) { Icon(Icons.Default.Edit, null) }
                            Switch(checked = user.status == "ACTIVE", onCheckedChange = { viewModel.updateUserStatus(user.id, if (user.status == "ACTIVE") "INACTIVE" else "ACTIVE") })
                        }
                    }
                )
                HorizontalDivider()
            }
        }

        if (showRoleDialog != null) {
            AlertDialog(
                onDismissRequest = { showRoleDialog = null },
                title = { Text("修改角色") },
                text = {
                    Column {
                        listOf("PUBLIC", "ARCHAEOLOGIST", "MANAGER").forEach { role ->
                            TextButton(onClick = { viewModel.updateUserRole(showRoleDialog!!.id, role); showRoleDialog = null }) { Text(role) }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPendingArtifactsScreen(navController: NavController, viewModel: AdminViewModel = viewModel()) {
    val artifacts = viewModel.pendingArtifacts
    var showAuditDialog by remember { mutableStateOf<Artifact?>(null) }

    LaunchedEffect(Unit) { viewModel.loadPendingArtifacts() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("待审核文物", fontFamily = FontFamily.Serif) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        if (artifacts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("暂无待审核文物", color = Bronze) }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(artifacts) { artifact ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row {
                                AsyncImage(model = artifact.imageUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(artifact.name, fontWeight = FontWeight.Bold)
                                    Text("${artifact.era} · ${artifact.category}", fontSize = 12.sp, color = Bronze)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { showAuditDialog = artifact }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("审核") }
                            }
                        }
                    }
                }
            }
        }

        if (showAuditDialog != null) {
            var notes by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAuditDialog = null },
                title = { Text("审核：${showAuditDialog!!.name}") },
                text = {
                    Column {
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("审批备注") })
                    }
                },
                confirmButton = {
                    Row {
                        TextButton(onClick = { viewModel.auditArtifact(showAuditDialog!!.id, "REJECTED", notes) { showAuditDialog = null } }) { Text("拒绝", color = CinnabarRed) }
                        Button(onClick = { viewModel.auditArtifact(showAuditDialog!!.id, "APPROVED", notes) { showAuditDialog = null } }) { Text("通过") }
                    }
                }
            )
        }

        if (viewModel.isAuditingArtifact) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("请稍候") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(viewModel.operationHint.ifBlank { "正在审批..." })
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddEditArtifactScreen(navController: NavController, artifactId: Long? = null, viewModel: AdminViewModel = viewModel()) {
    var name by remember { mutableStateOf("") }
    var era by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLocalLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(artifactId) {
        if (artifactId != null) {
            isLocalLoading = true
            try {
                val response = NetworkModule.api.getArtifactDetail(artifactId)
                response.data?.let { a ->
                    name = a.name
                    era = a.era
                    location = a.location
                    description = a.description
                    selectedCategory = ArtifactCategoryCatalog.normalize(a.category)
                        ?: ArtifactCategoryCatalog.normalize(a.tags.firstOrNull())
                        ?: ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLocalLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSelectableCategories()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (artifactId == null) "新增文物" else "编辑文物") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (artifactId != null) {
                        IconButton(onClick = { viewModel.deleteArtifact(artifactId) { navController.popBackStack() } }) { Icon(Icons.Default.Delete, null, tint = CinnabarRed) }
                    }
                }
            )
        }
    ) { padding ->
        if (isLocalLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = era, onValueChange = { era = it }, label = { Text("年代") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("馆藏地") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("标签分类") },
                        placeholder = { Text("请选择（与首页筛选联动）") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        viewModel.selectableCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { launcher.launch("image/*") }) { Text("选择图片") }
                
                bitmap?.let { 
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        bitmap = it.asImageBitmap(), 
                        contentDescription = "预览图", 
                        modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    ) 
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val normalizedTags = selectedCategory.ifBlank { "青铜" }
                        val imageBase64 = bitmap?.let {
                            val maxSide = 1280
                            val scale = min(maxSide.toFloat() / it.width, maxSide.toFloat() / it.height)
                            val resized = if (scale < 1f) {
                                Bitmap.createScaledBitmap(it, (it.width * scale).toInt(), (it.height * scale).toInt(), true)
                            } else {
                                it
                            }
                            val output = ByteArrayOutputStream()
                            resized.compress(Bitmap.CompressFormat.JPEG, 80, output)
                            "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                        }
                        val req = AdminArtifactRequest(
                            name = name,
                            description = description,
                            imageBase64 = imageBase64,
                            tags = normalizedTags,
                            location = location,
                            era = era,
                            status = if (artifactId == null) "PENDING" else "APPROVED"
                        )
                        if (artifactId == null) {
                            viewModel.createArtifact(req) { navController.popBackStack() }
                        } else {
                            viewModel.updateArtifact(artifactId, req) { navController.popBackStack() }
                        }
                    }, 
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank()
                            && era.isNotBlank()
                            && selectedCategory.isNotBlank()
                            && (artifactId != null || bitmap != null)
                            && !viewModel.isSubmittingArtifact
                ) { 
                    Text("提交") 
                }

                if (viewModel.errorMsg.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(viewModel.errorMsg, color = CinnabarRed, fontSize = 12.sp)
                }

                if (viewModel.isSubmittingArtifact) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(viewModel.operationHint.ifBlank { "正在上传..." }, color = Bronze, fontSize = 12.sp)
                    }
                }
            }
        }

        if (viewModel.isSubmittingArtifact) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("请稍候") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(viewModel.operationHint.ifBlank { "正在上传..." })
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDialogHistoriesScreen(navController: NavController, viewModel: AdminViewModel = viewModel()) {
    val histories = viewModel.allDialogHistories
    LaunchedEffect(Unit) { viewModel.loadAllDialogHistories() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全站对话历史") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(histories) { history ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("提问：${history.userQuery}", fontWeight = FontWeight.Bold)
                        Text("回复：${history.aiResponse}", maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(history.createdAt, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))
                    }
                }
            }
        }
    }
}
