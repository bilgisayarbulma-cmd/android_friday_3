package com.friday.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Gemini REST API'sine baglanip kullanicinin soyledigini anlayan, gerekirse
 * arac (tool/function) cagiran istemci. Windows surumundeki
 * command_engine.py'nin _ai_ile_calistir / _gemini_istek_gonder
 * fonksiyonlarinin Kotlin karsiligidir.
 *
 * Birden fazla API anahtari verilebilir; biri kota/rate-limit hatasi (429)
 * verirse otomatik olarak siradaki anahtara gecilir.
 */
class GeminiClient(
    private val apiKeys: List<String>,
    private val toolExecutor: ToolExecutor
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var currentKeyIndex = 0
    private val conversationHistory = mutableListOf<JSONObject>()

    data class Result(val success: Boolean, val displayText: String, val spokenText: String)

    /** Kullanicinin soyledigi metni Gemini'ye gonderir, gerekirse tool cagirir,
     * dogal bir cevap uretip dondurur. */
    suspend fun sendMessage(userText: String): Result = withContext(Dispatchers.IO) {
        if (apiKeys.isEmpty()) {
            return@withContext Result(false, "Gemini API anahtarı tanımlı değil.", "API anahtarı eksik.")
        }

        val userPart = JSONObject().put("parts", JSONArray().put(JSONObject().put("text", userText)))
            .put("role", "user")
        conversationHistory.add(userPart)
        trimHistory()

        val response = sendWithFailover() ?: return@withContext Result(
            false, "Bağlantı kurulamadı.", "Yapay zekaya bağlanamadım."
        )

        if (response.has("error")) {
            val message = response.getJSONObject("error").optString("message", "bilinmeyen hata")
            return@withContext Result(false, "AI hatası: $message", "Bir sorun oluştu.")
        }

        return@withContext processResponse(response)
    }

    /** Tum anahtarlari sirayla dener, biri 429 (kota) hatasi verirse siradakine gecer. */
    private fun sendWithFailover(): JSONObject? {
        var attempts = 0
        while (attempts < apiKeys.size) {
            val key = apiKeys[currentKeyIndex]
            val result = sendSingleRequest(key)

            if (result != null && result.has("error")) {
                val message = result.getJSONObject("error").optString("message", "")
                val isQuotaError = message.contains("quota", ignoreCase = true) ||
                    message.contains("rate", ignoreCase = true)
                if (isQuotaError) {
                    currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size
                    attempts++
                    continue
                }
            }
            return result
        }
        return JSONObject().put("error", JSONObject().put("message", "Tüm anahtarlar limit doldu."))
    }

    private fun sendSingleRequest(apiKey: String): JSONObject? {
        val systemPrompt = buildSystemPrompt()

        val body = JSONObject().apply {
            put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
            put("contents", JSONArray(conversationHistory))
            put("tools", JSONArray().put(JSONObject().put("function_declarations", FridayTools.declarations)))
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            httpClient.newCall(request).execute().use { resp ->
                val text = resp.body?.string() ?: return null
                JSONObject(text)
            }
        } catch (e: Exception) {
            JSONObject().put("error", JSONObject().put("message", e.message ?: "ağ hatası"))
        }
    }

    /** Gemini'nin cevabini isler: tool cagrisi varsa calistirir ve sonucu
     * tekrar gonderir, yoksa dogal metni dondurur. */
    private suspend fun processResponse(response: JSONObject): Result {
        val candidates = response.optJSONArray("candidates") ?: return Result(false, "Boş yanıt.", "Cevap alamadım.")
        if (candidates.length() == 0) return Result(false, "Boş yanıt.", "Cevap alamadım.")

        val content = candidates.getJSONObject(0).getJSONObject("content")
        conversationHistory.add(content)

        val parts = content.getJSONArray("parts")
        val functionCalls = mutableListOf<JSONObject>()
        val textParts = mutableListOf<String>()

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("functionCall")) functionCalls.add(part.getJSONObject("functionCall"))
            if (part.has("text")) textParts.add(part.getString("text"))
        }

        if (functionCalls.isEmpty()) {
            val combined = textParts.joinToString(" ").trim().ifEmpty { "Tamam." }
            return Result(true, combined, combined)
        }

        // Tool cagrilarini calistir
        val functionResponses = JSONArray()
        val summaries = mutableListOf<String>()
        var allSucceeded = true

        for (call in functionCalls) {
            val name = call.getString("name")
            val args = call.optJSONObject("args") ?: JSONObject()
            val toolResult = toolExecutor.execute(name, args)
            summaries.add(toolResult.message)
            if (!toolResult.success) allSucceeded = false

            functionResponses.put(
                JSONObject().put(
                    "functionResponse",
                    JSONObject().put("name", name).put(
                        "response",
                        JSONObject().put("result", toolResult.message)
                    )
                )
            )
        }

        conversationHistory.add(JSONObject().put("role", "user").put("parts", functionResponses))
        trimHistory()

        // Tool sonucunu Gemini'ye bildirip dogal bir cevap istiyoruz
        val followUp = sendWithFailover()
        if (followUp != null && !followUp.has("error")) {
            val followCandidates = followUp.optJSONArray("candidates")
            if (followCandidates != null && followCandidates.length() > 0) {
                val followContent = followCandidates.getJSONObject(0).getJSONObject("content")
                conversationHistory.add(followContent)
                val followParts = followContent.getJSONArray("parts")
                val followTextList = mutableListOf<String>()
                for (idx in 0 until followParts.length()) {
                    val text = followParts.getJSONObject(idx).optString("text", "")
                    if (text.isNotEmpty()) followTextList.add(text)
                }
                val followText = followTextList.joinToString(" ").trim()
                if (followText.isNotEmpty()) {
                    return Result(allSucceeded, summaries.joinToString(" / "), followText)
                }
            }
        }

        val summary = summaries.joinToString(" / ")
        return Result(allSucceeded, summary, summary)
    }

    private fun buildSystemPrompt(): String {
        return "Sen FRIDAY adlı bir Türkçe sesli asistansın. Kullanıcının söylediği her şeyi " +
            "anlamaya çalış. Eğer söylenen şey listedeki araçlardan birini gerektiriyorsa o aracı " +
            "çağır. Gerektirmiyorsa sadece doğal, kısa, konuşma dilinde bir Türkçe cevap ver. " +
            "Cevapların sesli okunacağı için kısa ve doğal cümleler kullan, madde işaretleri veya " +
            "Markdown kullanma."
    }

    /** Konusma gecmisini cok buyumesin diye son N mesajla sinirlar. */
    private fun trimHistory() {
        val maxMessages = 20
        while (conversationHistory.size > maxMessages) {
            conversationHistory.removeAt(0)
        }
    }

    fun resetConversation() {
        conversationHistory.clear()
    }
}
