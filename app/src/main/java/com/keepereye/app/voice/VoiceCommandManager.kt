package com.keepereye.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class VoiceCommandManager(
    private val context: Context,
    private val onCommand: (VoiceCommand) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    enum class VoiceCommand {
        OBSTACLES, OCR, UNKNOWN
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available")
            onCommand(VoiceCommand.UNKNOWN)
            return
        }

        release()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    onListeningStateChanged(true)
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    onListeningStateChanged(false)
                }

                override fun onError(error: Int) {
                    isListening = false
                    onListeningStateChanged(false)
                    Log.e(TAG, "Recognition error: $error")
                    onCommand(VoiceCommand.UNKNOWN)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    onListeningStateChanged(false)

                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    val command = parseCommand(matches)
                    onCommand(command)
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("es", "ES").toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
        }

        speechRecognizer?.startListening(intent)
    }

    private fun parseCommand(matches: List<String>?): VoiceCommand {
        if (matches.isNullOrEmpty()) return VoiceCommand.UNKNOWN

        for (match in matches) {
            val lower = match.lowercase().trim()
            if (lower.contains("1") || lower.contains("uno") ||
                lower.contains("obstáculo") || lower.contains("obstaculo") ||
                lower.contains("detección") || lower.contains("deteccion")
            ) {
                return VoiceCommand.OBSTACLES
            }
            if (lower.contains("2") || lower.contains("dos") ||
                lower.contains("ocr") || lower.contains("lectura") ||
                lower.contains("leer") || lower.contains("texto")
            ) {
                return VoiceCommand.OCR
            }
        }
        return VoiceCommand.UNKNOWN
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        onListeningStateChanged(false)
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }

    companion object {
        private const val TAG = "VoiceCommandManager"
    }
}
