package com.friday.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenWeatherMap API'sinden hava durumu bilgisi ceken basit istemci.
 * Windows surumundeki command_engine.py icindeki _hava_durumu fonksiyonunun
 * Kotlin karsiligidir.
 */
class WeatherClient(private val apiKey: String) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchWeather(city: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "Hava durumu API anahtarı tanımlı değil."

        try {
            val url = "https://api.openweathermap.org/data/2.5/weather" +
                "?q=$city&appid=$apiKey&units=metric&lang=tr"
            val request = Request.Builder().url(url).build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext "Hava durumu alınamadı."
                val json = JSONObject(body)

                if (!response.isSuccessful) {
                    val message = json.optString("message", "bilinmeyen hata")
                    return@withContext "Hava durumu alınamadı: $message"
                }

                val temp = json.getJSONObject("main").getDouble("temp").toInt()
                val description = json.getJSONArray("weather").getJSONObject(0).getString("description")
                "$city için hava $temp derece, $description"
            }
        } catch (e: Exception) {
            "Hava durumu bilgisine erişemedim: ${e.message}"
        }
    }
}
