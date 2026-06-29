package com.friday.assistant

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONObject

private val Context.memoryDataStore by preferencesDataStore(name = "friday_memory")

/**
 * Friday'in kalici hafizasi (Android). Windows surumundeki
 * memory_manager.py'nin Kotlin karsiligi -- DataStore'da tek bir JSON
 * string olarak saklanir (basitlik icin).
 */
class MemoryStore(private val context: Context) {

    companion object {
        private val MEMORY_JSON = stringPreferencesKey("memory_json")
    }

    suspend fun getAll(): JSONObject {
        val raw = context.memoryDataStore.data.first()[MEMORY_JSON] ?: "{}"
        return try { JSONObject(raw) } catch (e: Exception) { JSONObject() }
    }

    suspend fun save(key: String, value: String) {
        val current = getAll()
        current.put(key, value)
        context.memoryDataStore.edit { prefs -> prefs[MEMORY_JSON] = current.toString() }
    }

    suspend fun delete(key: String) {
        val current = getAll()
        current.remove(key)
        context.memoryDataStore.edit { prefs -> prefs[MEMORY_JSON] = current.toString() }
    }

    /** Hafizayi Gemini'ye sistem promptu icinde verilecek okunabilir metne cevirir. */
    suspend fun formatForPrompt(): String {
        val memory = getAll()
        if (memory.length() == 0) return ""

        val lines = StringBuilder("[FRIDAY'İN HATIRLADIKLARI]\n")
        for (key in memory.keys()) {
            lines.append("- $key: ${memory.getString(key)}\n")
        }
        return lines.toString()
    }
}
