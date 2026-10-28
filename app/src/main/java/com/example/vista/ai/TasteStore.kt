package com.example.vista.ai

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tasteData by preferencesDataStore("vista_taste")
data class TastePreferences(val genres: Set<String> = emptySet(), val disliked: Set<String> = emptySet(), val resetAt: Long = 0, val enabled: Boolean = true)

@Singleton
class TasteStore @Inject constructor(@ApplicationContext private val context: Context, private val auth: FirebaseAuth, private val profile: com.example.vista.data.local.ProfileScope) {
    private fun owner() = profile.current()
    val changes = profile.owners.flatMapLatest { owner ->
        context.tasteData.data.map {
            TastePreferences(it[stringSetPreferencesKey("${owner}_genres")] ?: emptySet(),
                it[stringSetPreferencesKey("${owner}_disliked")] ?: emptySet(),
                it[longPreferencesKey("${owner}_reset")] ?: 0,
                it[booleanPreferencesKey("${owner}_enabled")] ?: true)
        }
    }
    suspend fun read(): TastePreferences = changes.first()
    suspend fun toggleGenre(genre: String) {
        val key = stringSetPreferencesKey("${owner()}_genres")
        context.tasteData.edit { val old = it[key] ?: emptySet(); it[key] = if (genre in old) old - genre else old + genre }
    }
    suspend fun dislike(key: String) {
        val pref = stringSetPreferencesKey("${owner()}_disliked")
        context.tasteData.edit { it[pref] = (it[pref] ?: emptySet()) + key }
    }
    suspend fun setEnabled(value: Boolean) {
        val key = booleanPreferencesKey("${owner()}_enabled")
        context.tasteData.edit { it[key] = value }
    }
    suspend fun reset() {
        val owner = owner()
        context.tasteData.edit {
            it.remove(stringSetPreferencesKey("${owner}_genres"))
            it.remove(stringSetPreferencesKey("${owner}_disliked"))
            it[longPreferencesKey("${owner}_reset")] = System.currentTimeMillis()
        }
    }
}
