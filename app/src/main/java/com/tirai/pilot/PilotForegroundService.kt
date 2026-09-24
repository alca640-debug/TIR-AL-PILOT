package com.tirai.pilot

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.speech.tts.TextToSpeech
import java.util.Locale

class PilotForegroundService : Service(), TextToSpeech.OnInitListener {

    private lateinit var voice: VoiceCommandEngine
    private lateinit var calls: CallStateMonitor
    private lateinit var api: PilotApiClient
    private var inCall = false
    private var tts: TextToSpeech? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createChannel()
        api = PilotApiClient(this)
        tts = TextToSpeech(this, this)

        calls = CallStateMonitor(this) { active ->
            inCall = active
            if (active) {
                updateNotification("Görüşme algılandı • Pilot hazır (mikrofon erişimi cihaza bağlı)")
            } else {
                updateNotification("Hey Pilot dinleniyor")
            }
        }.also { it.start() }

        voice = VoiceCommandEngine(
            context = this,
            onStatus = { status ->
                updateNotification(if (inCall) "Görüşme aktif • $status" else status)
            },
            onQuestion = { question ->
                updateNotification("Soru: $question")
                api.ask(question, inCall) { answer ->
                    updateNotification(answer)
                    if (!inCall) speak(answer)
                }
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val notification = buildNotification("Hey Pilot dinleniyor")
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    startForeground(
                        NOTIF_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIF_ID, notification)
                }
                voice.start()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        voice.stop()
        calls.stop()
        tts?.stop()
        tts?.shutdown()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("tr", "TR")
        }
    }

    private fun speak(text: String) {
        val short = if (text.length > 260) text.take(260) + "…" else text
        tts?.speak(short, TextToSpeech.QUEUE_FLUSH, null, "pilot_answer")
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "TIR AI Pilot",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Pilot mikrofon ve görüşme asistanı durumu"
                setShowBadge(false)
            }
        )
    }

    private fun buildNotification(text: String): Notification {
        val openPi = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopPi = PendingIntent.getService(
            this,
            2,
            Intent(this, PilotForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(
                if (inCall) "TIR AI Pilot • Görüşme Modu"
                else "TIR AI Pilot • Aktif"
            )
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(openPi)
            .addAction(Notification.Action.Builder(null, "DURDUR", stopPi).build())
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildNotification(text))
    }

    companion object {
        const val ACTION_START = "com.tirai.pilot.START"
        const val ACTION_STOP = "com.tirai.pilot.STOP"
        private const val CHANNEL_ID = "pilot_foreground"
        private const val NOTIF_ID = 1107

        @Volatile
        var isRunning: Boolean = false
    }
}
