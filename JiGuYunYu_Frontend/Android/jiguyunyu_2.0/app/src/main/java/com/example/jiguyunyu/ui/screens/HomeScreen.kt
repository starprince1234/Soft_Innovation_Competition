package com.example.jiguyunyu.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.jiguyunyu.data.Artifact
import com.example.jiguyunyu.data.AuthRepository
import com.example.jiguyunyu.data.UserRole
import com.example.jiguyunyu.ui.navigation.Routes
import com.example.jiguyunyu.ui.theme.*
import com.example.jiguyunyu.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    val artifacts by viewModel.artifacts.collectAsState()
    val categories by viewModel.availableCategories.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val authRepo = remember { AuthRepository.getInstance(context) }
    val user by authRepo.currentUser.collectAsState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = viewModel.isRefreshing,
        onRefresh = { viewModel.loadArtifacts(refresh = true) }
    )

    DisposableEffect(lifecycleOwner, navController) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadArtifacts(refresh = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val destinationListener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.route?.startsWith(Routes.HOME) == true) {
                viewModel.loadArtifacts(refresh = true)
            }
        }
        navController.addOnDestinationChangedListener(destinationListener)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            navController.removeOnDestinationChangedListener(destinationListener)
        }
    }

    Scaffold(
        floatingActionButton = {
            if (user?.role == UserRole.ARCHAEOLOGIST || user?.role == UserRole.MANAGER) {
                ExtendedFloatingActionButton(
                    text = {
                        Text(
                            if (user?.role == UserRole.MANAGER) "上传文物到OSS" else "录入文物",
                            fontFamily = FontFamily.Serif
                        )
                    },
                    icon = { Icon(Icons.Default.Add, null) },
                    onClick = {
                        if (user?.role == UserRole.MANAGER) {
                            navController.navigate(Routes.ADMIN_ADD_ARTIFACT)
                        } else {
                            navController.navigate(Routes.ARCHAEOLOGY_UPLOAD)
                        }
                    },
                    containerColor = CinnabarRed,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(32.dp)
                )
            }
        },
        containerColor = IvoryWhite
    ) { padding ->
        // 核心修复1：用Box包裹可滑动区域，确保LazyVerticalStaggeredGrid占满剩余空间
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
                .background(Brush.verticalGradient(listOf(IvoryWhite, Parchment.copy(alpha = 0.3f))))
                .padding(padding) // 将Scaffold的padding移到最外层Box，避免内容被遮挡
        ) {
            // 核心修复2：LazyVerticalStaggeredGrid 作为根可滑动容器，整合所有内容
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. 顶部标题区（跨整行）
                item(span = StaggeredGridItemSpan.FullLine) {
                    HeaderSection(
                        searchQuery = viewModel.searchQuery,
                        onSearch = { viewModel.updateSearch(it) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // 2. 筛选标签（跨整行）
                item(span = StaggeredGridItemSpan.FullLine) {
                    FilterSection(
                        categories = categories,
                        selected = viewModel.selectedCategory,
                        onCategorySelect = { viewModel.updateCategory(it) },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // 3. 加载/空数据/文物列表
                if (viewModel.isLoading && artifacts.isEmpty()) {
                    // 加载中（跨整行）
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp), // 固定高度保证加载动画居中
                            contentAlignment = Alignment.Center
                        ) {
                            BronzeLoadingSpinner()
                        }
                    }
                } else if (artifacts.isEmpty()) {
                    // 无数据（跨整行）
                    item(span = StaggeredGridItemSpan.FullLine) {
                        NoArtifactsView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                        )
                    }
                } else {
                    // 4. 今日推荐（跨整行）
                    item(span = StaggeredGridItemSpan.FullLine) {
                        FeaturedArtifactCard(artifacts[0]) {
                            navController.navigate(Routes.detail(artifacts[0].id))
                        }
                    }

                    // 5. 文物列表（交错网格）
                    itemsIndexed(artifacts.drop(1)) { _, artifact ->
                        ArtifactGalleryItem(artifact) {
                            navController.navigate(Routes.detail(artifact.id))
                        }
                    }

                    // 补充：底部留白，避免最后一项被FAB遮挡
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = viewModel.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = CinnabarRed
            )
        }
    }
}

// 修复：给HeaderSection添加modifier参数，方便布局控制
@Composable
fun HeaderSection(
    searchQuery: String,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "云语藏珍",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = IndigoInk,
                    letterSpacing = 2.sp
                )
                Text("探寻千年文明的数字回响", fontSize = 12.sp, color = Bronze, fontFamily = FontFamily.Serif)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            placeholder = { Text("搜索文物、时代、地域...", fontSize = 14.sp, color = Bronze.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Bronze) },
            shape = RoundedCornerShape(26.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Bronze,
                unfocusedBorderColor = Bronze.copy(alpha = 0.2f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White.copy(alpha = 0.7f)
            ),
            singleLine = true
        )
    }
}

// 修复：给FilterSection添加modifier参数，方便布局控制
@Composable
fun FilterSection(
    categories: List<String>,
    selected: String,
    onCategorySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            val isSelected = selected == cat
            Surface(
                onClick = { onCategorySelect(cat) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) IndigoInk else Color.White,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Bronze.copy(alpha = 0.1f)),
                modifier = Modifier.height(32.dp).defaultMinSize(minWidth = 56.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = cat,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.White else Charcoal,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun FeaturedArtifactCard(artifact: Artifact, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(240.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CachedArtifactImage(
                imageUrl = artifact.imageUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
            ))
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Surface(color = CinnabarRed, shape = RoundedCornerShape(4.dp)) {
                    Text("今日推荐", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(artifact.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text("${artifact.era} · ${artifact.category}", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                if (artifact.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = artifact.description,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ArtifactGalleryItem(artifact: Artifact, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Bronze.copy(alpha = 0.1f))
    ) {
        Column {
            CachedArtifactImage(
                imageUrl = artifact.imageUrl,
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 220.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    artifact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Serif,
                    color = Charcoal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(artifact.era, fontSize = 11.sp, color = Bronze)
                    Text(" · ", fontSize = 11.sp, color = Bronze)
                    Text(artifact.category, fontSize = 11.sp, color = Bronze)
                }
                if (artifact.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        artifact.description,
                        fontSize = 12.sp,
                        color = Charcoal.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun BronzeLoadingSpinner() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing)
        ), label = "angle"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .rotate(angle)
            .border(4.dp, Brush.sweepGradient(listOf(IvoryWhite, Bronze, CinnabarRed, IvoryWhite)), CircleShape)
    )
}

// 修复：给NoArtifactsView添加modifier参数，适配布局
@Composable
fun NoArtifactsView(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Inbox, null, modifier = Modifier.size(64.dp), tint = Bronze.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("暂无相关文物", color = Bronze.copy(alpha = 0.5f), fontFamily = FontFamily.Serif)
        }
    }
}

@Composable
private fun CachedArtifactImage(
    imageUrl: String,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val context = LocalContext.current
    val request = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
    )
}