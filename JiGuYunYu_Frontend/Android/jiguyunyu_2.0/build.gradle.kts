// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // 修正 AGP 版本为稳定的 8.2.2 (原 8.13.2 不存在)
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    // Kotlin 2.0 必须启用的 Compose 编译器插件
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
}