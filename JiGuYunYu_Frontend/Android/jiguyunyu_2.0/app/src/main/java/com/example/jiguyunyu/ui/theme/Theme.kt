package com.example.jiguyunyu.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 核心配色方案 - 新中式优化
val IvoryWhite = Color(0xFFFDFBF5) // 背景色-象牙白/羊皮纸
val IndigoInk = Color(0xFF2B3A55)  // 主色调-黛蓝 (保持深邃)
val CinnabarRed = Color(0xFFB22222) // 强调色-朱砂 (更正统的红)
val Parchment = Color(0xFFF4F1EB)  // 辅助背景 (保持)
val Bronze = Color(0xFF7D6B52)     // 古铜色/青铜褐 (用于次要文字)
val Charcoal = Color(0xFF333333)   // 文本色

private val LightColorScheme = lightColorScheme(
    primary = IndigoInk,
    onPrimary = Color.White,
    secondary = CinnabarRed,
    onSecondary = Color.White,
    tertiary = Bronze,
    background = IvoryWhite,
    surface = IvoryWhite,
    onBackground = Charcoal,
    onSurface = Charcoal,
    surfaceVariant = Parchment,
    onSurfaceVariant = IndigoInk
)

@Composable
fun JiguyunyuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 强制使用亮色模式以保持设计图的"纸张感"和"古风"
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assuming default typography for brevity
        content = content
    )
}
