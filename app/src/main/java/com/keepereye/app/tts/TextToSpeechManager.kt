package com.keepereye.app.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false
    private var speechRate = 1.0f

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                val fallback = tts.setLanguage(Locale.US)
                if (fallback == TextToSpeech.LANG_MISSING_DATA ||
                    fallback == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e(TAG, "TTS language not supported")
                    return
                }
            }
            tts.setSpeechRate(speechRate)
            isReady = true
            Log.d(TAG, "TTS initialized successfully")
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
        }
    }

    fun speak(text: String) {
        if (!isReady || text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_${System.currentTimeMillis()}")
    }

    fun speakWithPriority(text: String) {
        if (!isReady || text.isBlank()) return
        tts.stop()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "priority_${System.currentTimeMillis()}")
    }

    fun stop() {
        if (isReady) {
            tts.stop()
        }
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.25f, 3.0f)
        if (isReady) {
            tts.setSpeechRate(speechRate)
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
        isReady = false
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
    }
}
