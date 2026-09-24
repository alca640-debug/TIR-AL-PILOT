package com.tirai.pilot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class VoiceCommandEngine(
    private val context: Context,
    private val onStatus: (String) -> Unit,
    private val onQuestion: (String) -> Unit
) : RecognitionListener {

    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var running = false
    private var awaitingQuestion = false

    fun start() {
        if (running) return
        running = true
        main.post { createAndListen() }
    }

    fun stop() {
        running = false
        awaitingQuestion = false
        main.removeCallbacksAndMessages(null)
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun createAndListen() {
        if (!running) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onStatus("Bu telefonda konuşma tanıma servisi bulunamadı.")
            return
        }

        if (recognizer == null) {
            recognizer = try {
                if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }
            } catch (_: Throwable) {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
            recognizer?.setRecognitionListener(this)
        }

        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        runCatching { recognizer?.startListening(i) }
            .onFailure { scheduleRestart(1200) }
    }

    private fun handle(text: String) {
        val clean = text.trim()
        val lower = clean.lowercase(Locale("tr", "TR"))
        val wakeCandidates = listOf("hey pilot", "hey paylot", "hey pılot", "pilot")
        val matched = wakeCandidates.firstOrNull { lower.contains(it) }

        if (awaitingQuestion && clean.isNotBlank()) {
            awaitingQuestion = false
            onQuestion(clean)
            return
        }

        if (matched != null) {
            val idx = lower.indexOf(matched)
            val rest = clean.substring((idx + matched.length).coerceAtMost(clean.length))
                .trim(' ', ',', ':', ';', '-')

            if (rest.isBlank()) {
                awaitingQuestion = true
                onStatus("Dinliyorum…")
            } else {
                onQuestion(rest)
            }
        }
    }

    private fun scheduleRestart(delay: Long = 650) {
        if (!running) return
        main.postDelayed({ createAndListen() }, delay)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        onStatus("Hey Pilot dinleniyor")
    }

    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        scheduleRestart(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1400 else 700)
    }

    override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.let(::handle)
        scheduleRestart()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull() ?: return
        val l = t.lowercase(Locale("tr", "TR"))
        if (l.contains("hey pilot") || l.contains("hey paylot")) handle(t)
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
