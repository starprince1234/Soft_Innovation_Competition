package com.example.jiguyunyu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.jiguyunyu.data.Artifact
import com.example.jiguyunyu.ui.navigation.Routes
import com.example.jiguyunyu.ui.theme.Bronze
import com.example.jiguyunyu.ui.theme.Charcoal
import com.example.jiguyunyu.ui.theme.CinnabarRed
import com.example.jiguyunyu.ui.theme.IndigoInk
import com.example.jiguyunyu.ui.theme.IvoryWhite
import com.example.jiguyunyu.viewmodel.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavController, viewModel: FavoritesViewModel = viewModel()) {
    LaunchedEffect(Unit) {
        viewModel.loadFavorites()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("我的收藏", color = IndigoInk, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = IndigoInk)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = IvoryWhite)
            )
        },
        containerColor = IvoryWhite
    ) { padding ->
        when {
            viewModel.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(IvoryWhite)
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Bronze)
                }
            }

            viewModel.favorites.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(IvoryWhite)
                        .padding(padding)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = Bronze)
                    Text(
                        text = "暂无收藏，去详情页点亮小红心吧。",
                        color = Charcoal,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(IvoryWhite)
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(viewModel.favorites, key = { it.id }) { artifact ->
                        FavoriteArtifactCard(
                            artifact = artifact,
                            onOpen = { navController.navigate(Routes.detail(artifact.id)) },
                            onRemove = { viewModel.removeFavorite(artifact.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteArtifactCard(artifact: Artifact, onOpen: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = artifact.imageUrl,
                contentDescription = artifact.name,
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = CinnabarRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("已收藏", color = CinnabarRed, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(artifact.name, fontWeight = FontWeight.Bold, color = Charcoal)
                Text(
                    text = "${artifact.era.ifBlank { "年代待考" }} · ${artifact.category.ifBlank { "文物" }}",
                    color = Bronze,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = onRemove,
                colors = IconButtonDefaults.iconButtonColors(contentColor = Bronze)
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "移除收藏")
            }
        }
    }
}
