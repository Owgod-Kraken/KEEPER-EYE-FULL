package com.keepereye.app.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.LinkedList

class TextToSpeechManager(
    context: Context,
    private val onReady: (() -> Unit)? = null
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false
    private var speechRate = 1.0f
    private val pendingQueue = LinkedList<String>()
    private var sequenceCallback: (() -> Unit)? = null

    constructor(context: Context) : this(context, null)

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
            setupUtteranceListener()
            isReady = true
            Log.d(TAG, "TTS initialized successfully")
            onReady?.invoke()
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
        }
    }

    private fun setupUtteranceListener() {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (pendingQueue.isNotEmpty()) {
                    val next = pendingQueue.poll()
                    if (next != null) {
                        speakInternal(next, "seq_${System.currentTimeMillis()}")
                    }
                } else {
                    sequenceCallback?.invoke()
                    sequenceCallback = null
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                pendingQueue.clear()
                sequenceCallback = null
            }
        })
    }

    fun speak(text: String) {
        if (!isReady || text.isBlank()) return
        pendingQueue.clear()
        sequenceCallback = null
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_${System.currentTimeMillis()}")
    }

    fun speakQueued(text: String) {
        if (!isReady || text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "queued_${System.currentTimeMillis()}")
    }

    fun speakWithPriority(text: String) {
        if (!isReady || text.isBlank()) return
        pendingQueue.clear()
        sequenceCallback = null
        tts.stop()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "priority_${System.currentTimeMillis()}")
    }

    fun speakSequence(texts: List<String>, onComplete: (() -> Unit)? = null) {
        if (!isReady || texts.isEmpty()) return
        pendingQueue.clear()
        sequenceCallback = onComplete

        val first = texts.first()
        texts.drop(1).forEach { pendingQueue.add(it) }
        speakInternal(first, "seq_${System.currentTimeMillis()}")
    }

    private fun speakInternal(text: String, utteranceId: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        if (isReady) {
            pendingQueue.clear()
            sequenceCallback = null
            tts.stop()
        }
    }

    fun isSpeaking(): Boolean = tts.isSpeaking

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.25f, 3.0f)
        if (isReady) {
            tts.setSpeechRate(speechRate)
        }
    }

    fun shutdown() {
        pendingQueue.clear()
        sequenceCallback = null
        tts.stop()
        tts.shutdown()
        isReady = false
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
    }
}
