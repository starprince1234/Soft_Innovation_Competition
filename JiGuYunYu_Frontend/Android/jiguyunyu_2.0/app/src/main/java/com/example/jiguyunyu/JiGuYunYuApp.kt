package com.example.jiguyunyu

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class JiGuYunYuApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val imageHttpCacheDir = File(cacheDir, "http_image_cache")
        val okHttpClient = OkHttpClient.Builder()
            .cache(Cache(imageHttpCacheDir, HTTP_CACHE_BYTES))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addNetworkInterceptor(cacheControlInterceptor)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "coil_image_cache"))
                    .maxSizeBytes(COIL_DISK_CACHE_BYTES)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }

    private val cacheControlInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)

        val hasImageLikePath = request.url.encodedPath
            .lowercase()
            .let { path ->
                path.endsWith(".png") ||
                    path.endsWith(".jpg") ||
                    path.endsWith(".jpeg") ||
                    path.endsWith(".webp") ||
                    path.contains("/detect/")
            }

        if (request.method == "GET" && hasImageLikePath) {
            response.newBuilder()
                .removeHeader("Pragma")
                .header("Cache-Control", "public, max-age=86400")
                .build()
        } else {
            response
        }
    }

    private companion object {
        const val HTTP_CACHE_BYTES = 120L * 1024L * 1024L
        const val COIL_DISK_CACHE_BYTES = 200L * 1024L * 1024L
    }
}
