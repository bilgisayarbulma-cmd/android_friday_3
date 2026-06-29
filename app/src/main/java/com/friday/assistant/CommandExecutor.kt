package com.friday.assistant

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Friday'in tool cagrilarini GERCEKTEN calistiran sinif. Windows surumundeki
 * command_engine.py icindeki _ai_tool_haritasi fonksiyonlarinin Android
 * karsiligidir.
 *
 * ONEMLI GUVENLIK SINIRI: send_sms ve make_call asla otomatik gondermez/aramaz.
 * Sadece ilgili sistem uygulamasini (Mesajlar/Telefon) onceden doldurulmus
 * sekilde acar, son onay/tiklama kullaniciya birakilir -- Windows surumundeki
 * WhatsApp entegrasyonunda aldigimiz ayni guvenlik karari.
 */
class CommandExecutor(
    private val context: Context,
    private val weatherCity: String,
    private val weatherApiKey: String,
    private val onMemorySave: (String, String) -> Unit
) : ToolExecutor {

    private val weatherClient = WeatherClient(weatherApiKey)

    override fun execute(toolName: String, args: JSONObject): ToolResult {
        return try {
            when (toolName) {
                "get_time" -> getTime()
                "get_weather" -> getWeather()
                "open_app" -> openApp(args.optString("app_name", ""))
                "web_search" -> webSearch(args.optString("query", ""))
                "play_media" -> playMedia(args.optString("query", ""), args.optString("provider", "youtube"))
                "save_memory" -> saveMemory(args.optString("key", ""), args.optString("value", ""))
                "calculate" -> calculate(args.optString("expression", ""))
                "send_sms" -> sendSms(args.optString("phone_number", ""), args.optString("message", ""))
                "make_call" -> makeCall(args.optString("phone_number", ""))
                else -> ToolResult(false, "Bilinmeyen araç: $toolName")
            }
        } catch (e: Exception) {
            ToolResult(false, "Araç çalıştırılırken hata: ${e.message}")
        }
    }

    private fun getTime(): ToolResult {
        val formatted = SimpleDateFormat("HH:mm", Locale("tr")).format(Date())
        return ToolResult(true, "Saat şu an $formatted")
    }

    /** ToolExecutor arayuzu senkron oldugu icin, coroutine tabanli WeatherClient'i
     * runBlocking ile cagiriyoruz. Bu cagri GeminiClient.sendMessage icinde zaten
     * Dispatchers.IO uzerinde calistigi icin ana thread'i bloke etmez. */
    private fun getWeather(): ToolResult {
        if (weatherApiKey.isBlank()) {
            return ToolResult(false, "Hava durumu API anahtarı tanımlı değil.")
        }
        val result = kotlinx.coroutines.runBlocking { weatherClient.fetchWeather(weatherCity) }
        val success = !result.contains("alınamadı") && !result.contains("erişemedim")
        return ToolResult(success, result)
    }

    private fun openApp(appName: String): ToolResult {
        val packageMap = mapOf(
            "whatsapp" to "com.whatsapp",
            "youtube" to "com.google.android.youtube",
            "spotify" to "com.spotify.music",
            "chrome" to "com.android.chrome",
        )

        val normalized = appName.trim().lowercase()

        try {
            when (normalized) {
                "kamera", "camera" -> {
                    context.startActivity(Intent("android.media.action.IMAGE_CAPTURE").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return ToolResult(true, "Kamera açıldı.")
                }
                "ayarlar", "settings" -> {
                    context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return ToolResult(true, "Ayarlar açıldı.")
                }
                "telefon", "phone" -> {
                    context.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return ToolResult(true, "Telefon uygulaması açıldı.")
                }
            }

            val packageName = packageMap[normalized]
            if (packageName != null) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ToolResult(true, "$appName açıldı.")
                }
                return ToolResult(false, "$appName telefonda kurulu değil.")
            }

            return ToolResult(false, "'$appName' adlı uygulama tanınmadı.")
        } catch (e: ActivityNotFoundException) {
            return ToolResult(false, "$appName açılamadı: uygulama bulunamadı.")
        }
    }

    private fun webSearch(query: String): ToolResult {
        if (query.isBlank()) return ToolResult(false, "Aranacak bir şey belirtilmedi.")
        val encoded = Uri.encode(query)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return ToolResult(true, "Google'da '$query' aranıyor.")
    }

    private fun playMedia(query: String, provider: String): ToolResult {
        if (query.isBlank()) return ToolResult(false, "Ne çalmamı istediğin belirtilmedi.")
        val encoded = Uri.encode(query)
        val url = if (provider == "spotify") {
            "https://open.spotify.com/search/$encoded"
        } else {
            "https://www.youtube.com/results?search_query=$encoded"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return ToolResult(true, "'$query' için $provider'da arama yapılıyor.")
    }

    private fun saveMemory(key: String, value: String): ToolResult {
        if (key.isBlank() || value.isBlank()) {
            return ToolResult(false, "Hafızaya kaydetmek için anahtar ve değer gerekli.")
        }
        onMemorySave(key, value)
        return ToolResult(true, "Hafızaya kaydedildi: $key = $value")
    }

    private fun calculate(expression: String): ToolResult {
        val (success, result) = SafeCalculator.calculate(expression)
        return if (success) ToolResult(true, "$expression = $result")
        else ToolResult(false, result)
    }

    /** GUVENLIK: SMS uygulamasini mesaj hazir sekilde acar, OTOMATIK GONDERMEZ. */
    private fun sendSms(phoneNumber: String, message: String): ToolResult {
        if (phoneNumber.isBlank() || message.isBlank()) {
            return ToolResult(false, "Telefon numarası veya mesaj belirtilmedi.")
        }
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ToolResult(
            true,
            "$phoneNumber için mesaj hazırlandı. Göndermek için açılan uygulamada onaylaman gerekiyor."
        )
    }

    /** GUVENLIK: Arama ekranini acar, OTOMATIK ARAMAZ. */
    private fun makeCall(phoneNumber: String): ToolResult {
        if (phoneNumber.isBlank()) return ToolResult(false, "Aranacak numara belirtilmedi.")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return ToolResult(true, "$phoneNumber için arama ekranı açıldı, aramayı sen başlatmalısın.")
    }
}
