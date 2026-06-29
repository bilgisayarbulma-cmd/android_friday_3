package com.friday.assistant

import org.json.JSONArray
import org.json.JSONObject

/**
 * Friday'in mobilde destekledigi araclarin (tool/function) tanimlari.
 * Windows surumundeki command_engine.py icindeki _ai_tools listesinin
 * mobile uyarlanmis hali. Masaustune ozel araclar (notepad ac, dosya
 * gezgini vb.) cikarildi, mobile anlamli olanlar (uygulama acma, SMS,
 * arama vb.) eklendi.
 */
object FridayTools {

    val declarations: JSONArray by lazy { buildDeclarations() }

    private fun buildDeclarations(): JSONArray {
        val list = JSONArray()

        list.put(simpleTool("get_time", "Şu anki saati söyler."))

        list.put(simpleTool("get_weather", "Tanımlı şehir için güncel hava durumunu söyler."))

        list.put(
            toolWithParams(
                "open_app",
                "Telefondaki bir uygulamayı açar (örn. whatsapp, youtube, spotify, chrome, ayarlar, kamera, telefon).",
                paramString("app_name", "Açılacak uygulamanın adı", required = true)
            )
        )

        list.put(
            toolWithParams(
                "web_search",
                "Google'da bir şey arar ve tarayıcıda açar.",
                paramString("query", "Aranacak metin", required = true)
            )
        )

        list.put(
            toolWithParams(
                "play_media",
                "YouTube veya Spotify'da şarkı/video arar ve açar.",
                paramString("query", "Aranacak şarkı/video adı", required = true),
                paramStringEnum("provider", "youtube veya spotify", listOf("youtube", "spotify"))
            )
        )

        list.put(
            toolWithParams(
                "save_memory",
                "Kullanıcı hakkında hatırlanması gereken bir bilgiyi kaydeder.",
                paramString("key", "Bilginin kısa anahtarı", required = true),
                paramString("value", "Hatırlanacak değer", required = true)
            )
        )

        list.put(
            toolWithParams(
                "calculate",
                "Matematiksel bir ifadeyi hesaplar (toplama, çarpma, kare kök, vb.).",
                paramString("expression", "Hesaplanacak ifade, örn. 'sqrt(144) + 5'", required = true)
            )
        )

        list.put(
            toolWithParams(
                "send_sms",
                "Bir telefon numarasına SMS göndermek için mesajlaşma uygulamasını mesaj hazır şekilde açar. Otomatik göndermez, kullanıcı onaylayıp göndermesi gerekir.",
                paramString("phone_number", "Alıcının telefon numarası", required = true),
                paramString("message", "Gönderilecek mesaj", required = true)
            )
        )

        list.put(
            toolWithParams(
                "make_call",
                "Bir telefon numarasını arama ekranını açar (otomatik aramaz, kullanıcı onaylaması gerekir).",
                paramString("phone_number", "Aranacak telefon numarası", required = true)
            )
        )

        return list
    }

    private fun simpleTool(name: String, description: String): JSONObject {
        return JSONObject()
            .put("name", name)
            .put("description", description)
            .put("parameters", JSONObject().put("type", "OBJECT").put("properties", JSONObject()))
    }

    private fun toolWithParams(name: String, description: String, vararg params: Pair<String, JSONObject>): JSONObject {
        val properties = JSONObject()
        val required = JSONArray()
        for ((paramName, paramDef) in params) {
            properties.put(paramName, paramDef.getJSONObject("schema"))
            if (paramDef.getBoolean("required")) required.put(paramName)
        }
        val parameters = JSONObject().put("type", "OBJECT").put("properties", properties)
        if (required.length() > 0) parameters.put("required", required)

        return JSONObject().put("name", name).put("description", description).put("parameters", parameters)
    }

    private fun paramString(name: String, description: String, required: Boolean = false): Pair<String, JSONObject> {
        val schema = JSONObject().put("type", "STRING").put("description", description)
        return name to JSONObject().put("schema", schema).put("required", required)
    }

    private fun paramStringEnum(name: String, description: String, options: List<String>): Pair<String, JSONObject> {
        val schema = JSONObject().put("type", "STRING").put("description", description)
            .put("enum", JSONArray(options))
        return name to JSONObject().put("schema", schema).put("required", false)
    }
}

/** Bir tool calistirildiginda donen sonuc. */
data class ToolResult(val success: Boolean, val message: String)

/** Tool cagrilarini gercekten calistiran arayuz. MainActivity bunu implemente eder
 * cunku bazi araclar (uygulama acma, SMS, arama) Android Context'e ihtiyac duyar. */
interface ToolExecutor {
    fun execute(toolName: String, args: JSONObject): ToolResult
}
