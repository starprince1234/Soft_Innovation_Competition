package com.example.jiguyunyu.data

import android.content.Context

class FavoriteRepository private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("favorites_store", Context.MODE_PRIVATE)

    fun getFavoriteIds(userKey: String): Set<Long> {
        return prefs.getStringSet(keyFor(userKey), emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    fun isFavorite(userKey: String, artifactId: Long): Boolean {
        return getFavoriteIds(userKey).contains(artifactId)
    }

    fun toggleFavorite(userKey: String, artifactId: Long): Boolean {
        val current = getFavoriteIds(userKey).toMutableSet()
        val nowFavorite = if (current.contains(artifactId)) {
            current.remove(artifactId)
            false
        } else {
            current.add(artifactId)
            true
        }
        save(userKey, current)
        return nowFavorite
    }

    private fun save(userKey: String, ids: Set<Long>) {
        prefs.edit().putStringSet(keyFor(userKey), ids.map { it.toString() }.toSet()).apply()
    }

    private fun keyFor(userKey: String): String = "favorites_$userKey"

    companion object {
        @Volatile
        private var instance: FavoriteRepository? = null

        fun getInstance(context: Context): FavoriteRepository {
            return instance ?: synchronized(this) {
                instance ?: FavoriteRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}