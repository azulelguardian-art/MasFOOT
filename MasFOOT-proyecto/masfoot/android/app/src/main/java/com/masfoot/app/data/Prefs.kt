package com.masfoot.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore(name = "masfoot")

/** Ajustes y caché local. Las claves del usuario se guardan en el dispositivo, nunca en el APK. */
class Prefs(context: Context) {
    private val ctx = context.applicationContext

    private object K {
        val server = stringPreferencesKey("server")
        val token = stringPreferencesKey("token")
        val fdKey = stringPreferencesKey("fd_key")
        val country = stringPreferencesKey("country")
        val dark = booleanPreferencesKey("dark")
        val favs = stringSetPreferencesKey("favs")
    }

    val serverUrl: Flow<String> = ctx.store.data.map { it[K.server] ?: "" }
    val token: Flow<String> = ctx.store.data.map { it[K.token] ?: "" }
    val fdKey: Flow<String> = ctx.store.data.map { it[K.fdKey] ?: "" }
    val country: Flow<String> = ctx.store.data.map { it[K.country] ?: "ES" }
    val dark: Flow<Boolean> = ctx.store.data.map { it[K.dark] ?: false }
    val favorites: Flow<Set<String>> = ctx.store.data.map { it[K.favs] ?: emptySet() }

    suspend fun saveConnection(server: String, token: String, fdKey: String, country: String) {
        ctx.store.edit {
            it[K.server] = server.trim()
            it[K.token] = token.trim()
            it[K.fdKey] = fdKey.trim()
            it[K.country] = country.trim().uppercase().ifBlank { "ES" }
        }
    }

    suspend fun setDark(value: Boolean) {
        ctx.store.edit { it[K.dark] = value }
    }

    suspend fun toggleFavorite(key: String) {
        ctx.store.edit {
            val cur = it[K.favs] ?: emptySet()
            it[K.favs] = if (key in cur) cur - key else cur + key
        }
    }

    suspend fun cacheGet(key: String): String? =
        ctx.store.data.first()[stringPreferencesKey("cache_$key")]

    suspend fun cachePut(key: String, json: String) {
        ctx.store.edit { it[stringPreferencesKey("cache_$key")] = json }
    }
}
