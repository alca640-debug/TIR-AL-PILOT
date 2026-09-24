package com.tirai.pilot

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class PilotApiClient(private val context: Context) {
    private val executor = Executors.newSingleThreadExecutor()

    fun ask(question: String, inCall: Boolean, callback: (String) -> Unit) {
        val endpoint = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
            .getString(MainActivity.KEY_ENDPOINT, "")
            .orEmpty()
            .trim()

        if (endpoint.isBlank()) {
            callback(demoAnswer(question, inCall))
            return
        }

        executor.execute {
            val answer = runCatching {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 8000
                    readTimeout = 15000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                val body = JSONObject()
                    .put("question", question)
                    .put("mode", if (inCall) "call" else "drive")
                    .put("language", "tr")
                    .toString()
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val raw = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
                    .bufferedReader().use { it.readText() }
                conn.disconnect()
                JSONObject(raw).optString("answer").ifBlank { "Sunucudan cevap alınamadı." }
            }.getOrElse { "AI bağlantısı başarısız: ${it.javaClass.simpleName}" }

            context.mainExecutor.execute { callback(answer) }
        }
    }

    private fun demoAnswer(q: String, inCall: Boolean): String {
        val x = q.lowercase()
        return when {
            "merhaba" in x -> "Buradayım. Pilot hazır."
            "kapıkule" in x -> "Kapıkule canlı verisi bu prototipte henüz bağlı değil. AI sunucusu bağlanınca güncel bilgi çekilebilir."
            "çevir" in x || "almanca" in x -> "Çeviri motoru AI sunucusu bağlandığında devreye girecek."
            "görüşme" in x -> if (inCall) "Görüşmenin aktif olduğunu algıladım. Karşı tarafın sesini kaydetmiyorum." else "Şu anda telefon görüşmesi algılanmıyor."
            else -> "Sorunu aldım: $q. Bu prototip demo modunda. Gerçek AI sunucusunu bağladığında bilinmeyen konularda yanıt üreteceğim."
        }
    }
}
