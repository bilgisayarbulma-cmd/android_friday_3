package com.friday.assistant

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * Friday'in ayarlarini (Gemini API anahtari, secilen ses, sehir vb.)
 * kalici olarak saklayan yardimci sinif. Windows surumundeki .env
 * dosyasinin Android karsiligi -- DataStore kullanir (SharedPreferences'in
 * modern, coroutine-uyumlu yerini alan cozumu).
 */
private val Context.dataStore by preferencesDataStore(name = "friday_settings")

class SettingsManager(private val context: Context) {

    companion object {
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val GEMINI_API_KEY_2 = stringPreferencesKey("gemini_api_key_2")
        private val GEMINI_API_KEY_3 = stringPreferencesKey("gemini_api_key_3")
        private val SELECTED_VOICE = stringPreferencesKey("selected_voice")
        private val WEATHER_CITY = stringPreferencesKey("weather_city")
        private val WEATHER_API_KEY = stringPreferencesKey("weather_api_key")
    }

    suspend fun getWeatherApiKey(): String {
        return context.dataStore.data.first()[WEATHER_API_KEY] ?: ""
    }

    suspend fun saveWeatherApiKey(key: String) {
        context.dataStore.edit { prefs -> prefs[WEATHER_API_KEY] = key }
    }

    suspend fun getGeminiApiKeys(): List<String> {
        val prefs = context.dataStore.data.first()
        return listOfNotNull(
            prefs[GEMINI_API_KEY],
            prefs[GEMINI_API_KEY_2],
            prefs[GEMINI_API_KEY_3]
        ).filter { it.isNotBlank() }
    }

    suspend fun saveGeminiApiKey(key: String, slot: Int = 1) {
        context.dataStore.edit { prefs ->
            val target = when (slot) {
                2 -> GEMINI_API_KEY_2
                3 -> GEMINI_API_KEY_3
                else -> GEMINI_API_KEY
            }
            prefs[target] = key
        }
    }

    suspend fun getSelectedVoice(): String {
        return context.dataStore.data.first()[SELECTED_VOICE] ?: "Charon"
    }

    suspend fun saveSelectedVoice(voiceName: String) {
        context.dataStore.edit { prefs -> prefs[SELECTED_VOICE] = voiceName }
    }

    suspend fun getWeatherCity(): String {
        return context.dataStore.data.first()[WEATHER_CITY] ?: "Antalya"
    }

    suspend fun saveWeatherCity(city: String) {
        context.dataStore.edit { prefs -> prefs[WEATHER_CITY] = city }
    }
}
