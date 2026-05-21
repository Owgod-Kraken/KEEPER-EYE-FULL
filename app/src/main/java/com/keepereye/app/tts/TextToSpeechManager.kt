package com.keepereye.app.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TextToSpeechManager(
    context: Context,
    private val onReady: (() -> Unit)? = null
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false
    private var speechRate = 1.0f
    private var onDone: (() -> Unit)? = null

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

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    onDone?.invoke()
                    onDone = null
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {}
            })

            isReady = true
            Log.d(TAG, "TTS initialized successfully")
            onReady?.invoke()
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
        }
    }

    fun speak(text: String) {
        if (!isReady || text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_${System.currentTimeMillis()}")
    }

    fun speakAndThen(text: String, callback: () -> Unit) {
        if (!isReady || text.isBlank()) {
            callback()
            return
        }
        onDone = callback
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "chain_${System.currentTimeMillis()}")
    }

    fun speakQueued(text: String) {
        if (!isReady || text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "queued_${System.currentTimeMillis()}")
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

    fun isSpeaking(): Boolean {
        return isReady && tts.isSpeaking
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
