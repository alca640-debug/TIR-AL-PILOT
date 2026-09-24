package com.tirai.pilot

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var endpointInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(6, 16, 26)
        setContentView(buildUi())
        refreshStatus()
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
        root.addView(text("Sürekli hazır sürüş ve görüşme asistanı • Prototip 0.1", 15f, Color.rgb(167,180,194), false).apply {
            setPadding(0, dp(4), 0, dp(18))
        })

        statusText = text("Durum kontrol ediliyor…", 18f, Color.rgb(40,209,124), true)
        root.addView(card(statusText))

        root.addView(section("1. İzinler"))
        root.addView(text(
            "Mikrofon: yalnızca senin 'Hey Pilot' komutlarını dinlemek için.\nTelefon durumu: görüşme başladığını/sona erdiğini anlamak için.\nBildirim: servis açıkken görünür kalmak için.",
            14f, Color.rgb(215,224,232), false
        ))

        root.addView(button("Gerekli izinleri ver") {
            requestNeededPermissions()
        })

        root.addView(section("2. Pilot servisi"))
        root.addView(button("▶ Pilot'u sürekli hazır başlat") {
            if (!hasMicPermission()) {
                Toast.makeText(this, "Önce mikrofon izni gerekli.", Toast.LENGTH_SHORT).show()
                requestNeededPermissions()
            } else {
                val i = Intent(this, PilotForegroundService::class.java).setAction(PilotForegroundService.ACTION_START)
                startForegroundService(i)
                refreshStatus(true)
            }
        })
        root.addView(button("■ Pilot'u durdur") {
            startService(Intent(this, PilotForegroundService::class.java).setAction(PilotForegroundService.ACTION_STOP))
            refreshStatus(false)
        })

        root.addView(section("3. AI sunucusu (isteğe bağlı)"))
        root.addView(text(
            "Gerçek yapay zekâ için kendi HTTPS ara sunucu adresini girebilirsin. Boş bırakırsan uygulama demo cevapları verir. API anahtarını APK içine koyma.",
            14f, Color.rgb(215,224,232), false
        ))
        endpointInput = EditText(this).apply {
            hint = "https://senin-sunucun.example/pilot"
            setHintTextColor(Color.rgb(120,140,160))
            setTextColor(Color.WHITE)
            setSingleLine(true)
            setBackgroundColor(Color.rgb(16,34,53))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setText(getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ENDPOINT, ""))
        }
        root.addView(endpointInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8)
        })
        root.addView(button("AI adresini kaydet") {
            val v = endpointInput.text.toString().trim()
            getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ENDPOINT, v).apply()
            Toast.makeText(this, "Kaydedildi.", Toast.LENGTH_SHORT).show()
        })

        root.addView(section("4. Kullanım"))
        root.addView(card(text(
            "Normal sürüşte:\n“Hey Pilot, Kapıkule nedir?”\n\nGörüşme sırasında:\nUygulama görüşmenin aktif olduğunu algılar. Mikrofon Android tarafından müsaitse 'Hey Pilot' komutunu dinler; değilse kalıcı bildirimden uygulamayı açabilirsin. Karşı tarafın sesi kaydedilmez.",
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
        val list = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE)
        if (android.os.Build.VERSION.SDK_INT >= 33) list += Manifest.permission.POST_NOTIFICATIONS
        requestPermissions(
            list.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }.toTypedArray(),
            REQ_PERMS
        )
    }

    private fun hasMicPermission() =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun refreshStatus(force: Boolean? = null) {
        val on = force ?: PilotForegroundService.isRunning
        statusText.text = if (on) "● PILOT AKTİF • Mikrofon servisi açık" else "○ PILOT KAPALI"
        statusText.setTextColor(if (on) Color.rgb(40,209,124) else Color.rgb(240,120,120))
    }

    override fun onResume() {
        super.onResume()
        if (::statusText.isInitialized) refreshStatus()
    }

    private fun section(title: String): TextView = text(title, 18f, Color.rgb(30,136,255), true).apply {
        setPadding(0, dp(22), 0, dp(8))
    }

    private fun button(label: String, click: () -> Unit): Button = Button(this).apply {
        text = label
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(30,136,255))
        setOnClickListener { click() }
    }.also {
        it.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)).apply {
            topMargin = dp(10)
        }
    }

    private fun card(child: android.view.View): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        setBackgroundColor(Color.rgb(16,34,53))
        addView(child)
    }

    private fun text(s: String, size: Float, color: Int, bold: Boolean): TextView = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
        gravity = Gravity.START
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    companion object {
        const val PREFS = "pilot_prefs"
        const val KEY_ENDPOINT = "ai_endpoint"
        private const val REQ_PERMS = 700
    }
}
