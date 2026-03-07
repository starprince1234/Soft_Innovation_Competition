package com.example.jiguyunyu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jiguyunyu.data.AuthRepository
import com.example.jiguyunyu.data.UserRole
import com.example.jiguyunyu.ui.navigation.Routes
import com.example.jiguyunyu.ui.screens.*
import com.example.jiguyunyu.ui.theme.Bronze
import com.example.jiguyunyu.ui.theme.CinnabarRed
import com.example.jiguyunyu.ui.theme.IvoryWhite
import com.example.jiguyunyu.ui.theme.JiguyunyuTheme
import com.example.jiguyunyu.data.NetworkModule

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkModule.authRepository = AuthRepository.getInstance(applicationContext)
        setContent {
            JiguyunyuTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val authRepo = remember { AuthRepository.getInstance(context) }
    val user by authRepo.currentUser.collectAsState()
    val requireLogin by authRepo.requireLogin.collectAsState()

    LaunchedEffect(requireLogin) {
        if (requireLogin) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0)
            }
            authRepo.resetRequireLogin()
        }
    }

    val isManager = user?.roles?.contains(UserRole.MANAGER) == true

    val bottomBarRoutes = listOf(Routes.HOME, Routes.SCAN, Routes.PROFILE, Routes.ADMIN_DASHBOARD, "chat")
    val showBottomBar = currentRoute?.substringBefore("/") in bottomBarRoutes.map { it.substringBefore("/") }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .shadow(8.dp, RoundedCornerShape(32.dp))
                        .clip(RoundedCornerShape(32.dp)),
                    containerColor = Color.White,
                    contentColor = Bronze
                ) {
                    val navItems = mutableListOf(
                        Triple(Routes.HOME, "首页", Icons.Default.Home),
                        Triple(Routes.SCAN, "识别", Icons.Default.ViewInAr)
                    )

                    if (isManager) {
                        navItems.add(Triple(Routes.ADMIN_DASHBOARD, "管理", Icons.Default.AdminPanelSettings))
                    } else {
                        navItems.add(Triple("chat", "对话", Icons.Default.ChatBubbleOutline))
                    }
                    navItems.add(Triple(Routes.PROFILE, "我的", Icons.Default.Person))

                    navItems.forEach { (route, label, icon) ->
                        val isSelected = currentRoute?.startsWith(route.substringBefore("/")) == true
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    navController.navigate(route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CinnabarRed,
                                selectedTextColor = CinnabarRed,
                                indicatorColor = CinnabarRed.copy(alpha = 0.1f),
                                unselectedIconColor = Bronze,
                                unselectedTextColor = Bronze
                            )
                        )
                    }
                }
            }
        },
        containerColor = IvoryWhite
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Routes.LOGIN
            ) {
                composable(Routes.LOGIN) { LoginScreen(navController) }
                composable(Routes.HOME) { HomeScreen(navController) }
                composable(Routes.SCAN) { DetectScreen(navController) }
                
                composable(
                    route = Routes.CHAT,
                    arguments = listOf(navArgument("artifactId") { nullable = true; defaultValue = null })
                ) { backStackEntry ->
                    val artifactId = backStackEntry.arguments?.getString("artifactId")
                    ChatScreen(navController, artifactId = artifactId)
                }

                composable(Routes.PROFILE) { ProfileScreen(navController) }

                composable(
                    route = Routes.DETAIL,
                    arguments = listOf(navArgument("artifactId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val artifactId = backStackEntry.arguments?.getString("artifactId")
                    DetailScreen(navController, artifactId)
                }

                composable(Routes.HISTORY) { HistoryScreen(navController) }
                composable(Routes.FEEDBACK) { FeedbackScreen(navController) }
                composable(Routes.ARCHAEOLOGY_UPLOAD) { ArchaeologyUploadScreen(navController) }
                
                // 管理员核心页面
                composable(Routes.ADMIN_DASHBOARD) { AdminDashboardScreen(navController) }
                composable(Routes.ADMIN_FEEDBACK) { AdminFeedbackScreen(navController) }
                composable(Routes.ADMIN_USERS) { AdminUserScreen(navController) }
                
                // 管理员新增的二级页面路由注册
                composable(Routes.ADMIN_PENDING_ARTIFACTS) { AdminPendingArtifactsScreen(navController) }
                composable(Routes.ADMIN_ADD_ARTIFACT) { AdminAddEditArtifactScreen(navController) }
                composable(
                    route = Routes.ADMIN_EDIT_ARTIFACT,
                    arguments = listOf(navArgument("artifactId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val artifactId = backStackEntry.arguments?.getLong("artifactId")
                    AdminAddEditArtifactScreen(navController, artifactId)
                }
                composable(Routes.ADMIN_DIALOG_HISTORIES) { AdminDialogHistoriesScreen(navController) }
            }
        }
    }
}
