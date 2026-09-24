package com.tirai.pilot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class PilotApiClient(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private val secureStore = SecureKeyStore(context)

    fun ask(question: String, inCall: Boolean, callback: (String) -> Unit) {
        val apiKey = secureStore.getApiKey()

        if (apiKey.isNullOrBlank()) {
            callback("OpenAI API anahtarı kayıtlı değil. TIR AI Pilot uygulamasında API anahtarını kaydet.")
            return
        }

        executor.execute {
            val answer = runCatching {
                requestOpenAi(apiKey, question, inCall)
            }.getOrElse {
                "AI bağlantısı başarısız: ${it.javaClass.simpleName}"
            }

            context.mainExecutor.execute { callback(answer) }
        }
    }

    private fun requestOpenAi(apiKey: String, question: String, inCall: Boolean): String {
        val conn = (URL(RESPONSES_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 45000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Accept", "application/json")
        }

        val instructions = if (inCall) {
            """
            Sen TIR AI Pilot'sun. Kullanıcı uluslararası tır sürücüsü ve şu anda telefon görüşmesinde olabilir.
            Türkçe cevap ver. Cevabı çok kısa, net ve sürüşte okunabilir tut. En fazla 4 kısa cümle kullan.
            Karşı tarafın sesini duymadığını varsay; yalnızca kullanıcının sana söylediği soruyu yanıtla.
            Güncel bilgi gerekiyorsa web aramasını kullan. Belirsiz bilgi uydurma.
            """.trimIndent()
        } else {
            """
            Sen TIR AI Pilot'sun. Kullanıcı uluslararası tır sürücüsü.
            Türkçe, kısa ve pratik cevap ver. Trafik, sınır kapısı, dozvola, yol yasağı, hava veya güncel operasyon bilgisi
            sorulursa web aramasını kullan ve güncel olmayan bilgiyi kesinmiş gibi söyleme.
            Sürüş sırasında okunabilir, sade cevaplar üret.
            """.trimIndent()
        }

        val body = JSONObject()
            .put("model", MODEL)
            .put("instructions", instructions)
            .put("input", question)
            .put("store", false)
            .put("max_output_tokens", if (inCall) 180 else 320)

        if (shouldUseWeb(question)) {
            body.put(
                "tools",
                JSONArray().put(JSONObject().put("type", "web_search"))
            )
        }

        conn.outputStream.use {
            it.write(body.toString().toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()

        conn.disconnect()

        if (code !in 200..299) {
            return when (code) {
                401 -> "OpenAI API anahtarı kabul edilmedi. Anahtarı kontrol et veya yeni anahtar oluştur."
                429 -> "OpenAI kullanım limiti veya bakiye sınırı aşıldı. Platform hesabını kontrol et."
                else -> {
                    val message = runCatching {
                        JSONObject(raw)
                            .optJSONObject("error")
                            ?.optString("message")
                            .orEmpty()
                    }.getOrDefault("")

                    if (message.isNotBlank()) "OpenAI hatası: $message"
                    else "OpenAI bağlantısı hata verdi (HTTP $code)."
                }
            }
        }

        return extractOutputText(JSONObject(raw))
            .ifBlank { "OpenAI cevap üretti ama metin okunamadı." }
    }

    private fun extractOutputText(root: JSONObject): String {
        val direct = root.optString("output_text")
        if (direct.isNotBlank()) return direct

        val out = root.optJSONArray("output") ?: return ""

        val parts = mutableListOf<String>()
        for (i in 0 until out.length()) {
            val item = out.optJSONObject(i) ?: continue
            val content = item.optJSONArray("content") ?: continue

            for (j in 0 until content.length()) {
                val c = content.optJSONObject(j) ?: continue
                if (c.optString("type") == "output_text") {
                    val text = c.optString("text")
                    if (text.isNotBlank()) parts += text
                }
            }
        }

        return parts.joinToString("\n").trim()
    }

    private fun shouldUseWeb(question: String): Boolean {
        val q = question.lowercase(Locale("tr", "TR"))
        val triggers = listOf(
            "güncel", "son durum", "bugün", "yarın", "şimdi", "şu an",
            "kapıkule", "frigo", "dozvola", "sıra", "yoğunluk", "bekleme",
            "trafik", "yol yasağı", "hava", "feribot", "fiyat", "kur",
            "açık mı", "kaç saat", "son dakika"
        )
        return triggers.any { it in q }
    }

    companion object {
        private const val RESPONSES_URL = "https://api.openai.com/v1/responses"
        private const val MODEL = "gpt-5.6-luna"
    }
}
