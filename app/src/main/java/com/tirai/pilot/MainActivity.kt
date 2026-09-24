package com.tirai.pilot

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var apiKeyInput: EditText
    private lateinit var apiKeyStatus: TextView
    private lateinit var secureStore: SecureKeyStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        secureStore = SecureKeyStore(this)
        window.statusBarColor = Color.rgb(6, 16, 26)
        setContentView(buildUi())
        refreshStatus()
        refreshApiKeyStatus()
    }

    private fun buildUi(): ScrollView {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
            setBackgroundColor(Color.rgb(8, 19, 31))
        }
        scroll.addView(root)

        root.addView(text("TIR AI PILOT", 30f, Color.rgb(244,247,251), true))
        root.addView(text("Sürekli hazır sürüş ve görüşme asistanı • v0.2", 15f, Color.rgb(167,180,194), false).apply {
            setPadding(0, dp(4), 0, dp(18))
        })

        statusText = text("Durum kontrol ediliyor…", 18f, Color.rgb(40,209,124), true)
        root.addView(card(statusText))

        root.addView(section("1. İzinler"))
        root.addView(text(
            "Mikrofon: yalnızca senin 'Hey Pilot' komutlarını dinlemek için.\nTelefon durumu: görüşme başladığını/sona erdiğini anlamak için.\nBildirim: servis açıkken görünür kalmak için.",
            14f, Color.rgb(215,224,232), false
        ))

        root.addView(button("GEREKLİ İZİNLERİ VER") {
            requestNeededPermissions()
        })

        root.addView(section("2. Pilot servisi"))
        root.addView(button("▶ PİLOT'U SÜREKLİ HAZIR BAŞLAT") {
            if (!hasMicPermission()) {
                Toast.makeText(this, "Önce mikrofon izni gerekli.", Toast.LENGTH_SHORT).show()
                requestNeededPermissions()
            } else {
                val i = Intent(this, PilotForegroundService::class.java)
                    .setAction(PilotForegroundService.ACTION_START)
                startForegroundService(i)
                refreshStatus(true)
            }
        })
        root.addView(button("■ PİLOT'U DURDUR") {
            startService(
                Intent(this, PilotForegroundService::class.java)
                    .setAction(PilotForegroundService.ACTION_STOP)
            )
            refreshStatus(false)
        })

        root.addView(section("3. OpenAI API anahtarı"))
        root.addView(text(
            "Gerçek yapay zekâ için OpenAI API anahtarını buraya gir. Anahtar APK'ya veya GitHub'a yazılmaz; bu telefonda Android Keystore ile şifrelenmiş olarak saklanır. Anahtarın kullanım ücreti OpenAI Platform hesabına yansır.",
            14f, Color.rgb(215,224,232), false
        ))

        apiKeyStatus = text("", 14f, Color.rgb(167,180,194), true)
        apiKeyStatus.setPadding(0, dp(8), 0, dp(8))
        root.addView(apiKeyStatus)

        apiKeyInput = EditText(this).apply {
            hint = "sk-..."
            setHintTextColor(Color.rgb(120,140,160))
            setTextColor(Color.WHITE)
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setBackgroundColor(Color.rgb(16,34,53))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        root.addView(
            apiKeyInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        )

        root.addView(button("OPENAI ANAHTARINI KAYDET") {
            val key = apiKeyInput.text.toString().trim()
            if (key.length < 20) {
                Toast.makeText(this, "Geçerli API anahtarını yapıştır.", Toast.LENGTH_SHORT).show()
            } else {
                runCatching {
                    secureStore.saveApiKey(key)
                    apiKeyInput.setText("")
                    refreshApiKeyStatus()
                    Toast.makeText(this, "API anahtarı telefona şifreli kaydedildi.", Toast.LENGTH_LONG).show()
                }.onFailure {
                    Toast.makeText(this, "Anahtar kaydedilemedi.", Toast.LENGTH_SHORT).show()
                }
            }
        })

        root.addView(button("API ANAHTARINI SİL") {
            secureStore.deleteApiKey()
            apiKeyInput.setText("")
            refreshApiKeyStatus()
            Toast.makeText(this, "API anahtarı silindi.", Toast.LENGTH_SHORT).show()
        })

        root.addView(section("4. Kullanım"))
        root.addView(card(text(
            "Normal sürüşte:\n“Hey Pilot, merhaba”\n“Hey Pilot, Kapıkule son durum”\n\nGüncel sorularda Pilot web araması kullanabilir. Telefon görüşmesinde Android mikrofonu Pilot'a vermeyebilir; bu durumda görüşme algılanır ama sesli komut alınamayabilir. Karşı tarafın sesi kaydedilmez.",
            15f, Color.rgb(230,236,242), false
        )))

        root.addView(section("5. Gizlilik freni"))
        root.addView(text(
            "Pilot çalışırken Android bildiriminde mikrofon kullanımı görünür. Bildirimdeki DURDUR düğmesiyle tek dokunuşta mikrofon servisini kapatabilirsin.",
            14f, Color.rgb(215,224,232), false
        ))

        return scroll
    }

    private fun requestNeededPermissions() {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE
        )
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            list += Manifest.permission.POST_NOTIFICATIONS
        }

        requestPermissions(
            list.filter {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray(),
            REQ_PERMS
        )
    }

    private fun hasMicPermission() =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun refreshStatus(force: Boolean? = null) {
        val on = force ?: PilotForegroundService.isRunning
        statusText.text =
            if (on) "● PİLOT AKTİF • Mikrofon servisi açık"
            else "○ PİLOT KAPALI"

        statusText.setTextColor(
            if (on) Color.rgb(40,209,124)
            else Color.rgb(240,120,120)
        )
    }

    private fun refreshApiKeyStatus() {
        apiKeyStatus.text =
            if (secureStore.hasApiKey())
                "● OpenAI API anahtarı kayıtlı"
            else
                "○ OpenAI API anahtarı henüz kayıtlı değil"

        apiKeyStatus.setTextColor(
            if (secureStore.hasApiKey())
                Color.rgb(40,209,124)
            else
                Color.rgb(240,180,100)
        )
    }

    override fun onResume() {
        super.onResume()
        if (::statusText.isInitialized) refreshStatus()
        if (::apiKeyStatus.isInitialized) refreshApiKeyStatus()
    }

    private fun section(title: String): TextView =
        text(title, 18f, Color.rgb(30,136,255), true).apply {
            setPadding(0, dp(22), 0, dp(8))
        }

    private fun button(label: String, click: () -> Unit): Button =
        Button(this).apply {
            text = label
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(30,136,255))
            setOnClickListener { click() }
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54)
            ).apply {
                topMargin = dp(10)
            }
        }

    private fun card(child: android.view.View): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(Color.rgb(16,34,53))
            addView(child)
        }

    private fun text(
        s: String,
        size: Float,
        color: Int,
        bold: Boolean
    ): TextView =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            gravity = Gravity.START
            if (bold) setTypeface(
                typeface,
                android.graphics.Typeface.BOLD
            )
        }

    private fun dp(v: Int) =
        (v * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQ_PERMS = 700
    }
}
