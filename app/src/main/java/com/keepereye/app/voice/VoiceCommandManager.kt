package com.keepereye.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private var isContinuous = false
    private var isPaused = false
    private val handler = Handler(Looper.getMainLooper())
    private var restartCount = 0

    enum class VoiceCommand {
        OBSTACLES, OCR, GO_BACK, EXIT, UNKNOWN
    }

    fun startContinuousListening() {
        isContinuous = true
        isPaused = false
        restartCount = 0
        startListeningInternal()
    }

    fun startListening() {
        isContinuous = false
        isPaused = false
        startListeningInternal()
    }

    fun pause() {
        isPaused = true
        stopListening()
    }

    fun resume() {
        isPaused = false
        if (isContinuous) {
            restartCount = 0
            startListeningInternal()
        }
    }

    private fun startListeningInternal() {
        if (isPaused) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available")
            scheduleRestart()
            return
        }

        release()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    restartCount = 0
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
                    Log.w(TAG, "Recognition error: $error")

                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            if (isContinuous && !isPaused) {
                                scheduleRestart()
                            } else {
                                onCommand(VoiceCommand.UNKNOWN)
                            }
                        }
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            handler.postDelayed({
                                if (isContinuous && !isPaused) {
                                    release()
                                    scheduleRestart()
                                }
                            }, 500)
                        }
                        else -> {
                            if (isContinuous && !isPaused) {
                                restartCount++
                                val delay = (restartCount * 500L).coerceAtMost(3000L)
                                handler.postDelayed({
                                    if (!isPaused) startListeningInternal()
                                }, delay)
                            } else {
                                onCommand(VoiceCommand.UNKNOWN)
                            }
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    onListeningStateChanged(false)

                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    val command = parseCommand(matches)

                    if (command != VoiceCommand.UNKNOWN) {
                        onCommand(command)
                    } else if (isContinuous && !isPaused) {
                        scheduleRestart()
                    } else {
                        onCommand(command)
                    }
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
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            if (isContinuous && !isPaused) scheduleRestart()
        }
    }

    private fun scheduleRestart() {
        if (isPaused) return
        handler.postDelayed({
            if (isContinuous && !isPaused) {
                startListeningInternal()
            }
        }, 300)
    }

    private fun parseCommand(matches: List<String>?): VoiceCommand {
        if (matches.isNullOrEmpty()) return VoiceCommand.UNKNOWN

        Log.d(TAG, "Speech matches: $matches")

        for (match in matches) {
            val lower = match.lowercase().trim()
            val words = lower.split("\\s+".toRegex())

            // Priority 1: exact number/word matches for modules
            if (words.any { it == "1" || it == "uno" }) {
                return VoiceCommand.OBSTACLES
            }
            if (words.any { it == "2" || it == "dos" || it == "do" || it == "segundo" }) {
                return VoiceCommand.OCR
            }

            // Priority 2: navigation commands
            if (lower.contains("regresar") || lower.contains("volver") ||
                lower.contains("atrás") || lower.contains("atras") ||
                lower.contains("menú") || lower.contains("menu")
            ) {
                return VoiceCommand.GO_BACK
            }

            if (lower.contains("salir") || lower.contains("cerrar") ||
                lower.contains("terminar")
            ) {
                return VoiceCommand.EXIT
            }

            // Priority 3: keyword matches for modules
            if (lower.contains("obstáculo") || lower.contains("obstaculo") ||
                lower.contains("detección") || lower.contains("deteccion") ||
                lower.contains("obstacle")
            ) {
                return VoiceCommand.OBSTACLES
            }

            if (lower.contains("ocr") || lower.contains("lectura") ||
                lower.contains("leer") || lower.contains("texto") ||
                lower.contains("escanear") || lower.contains("escaneo")
            ) {
                return VoiceCommand.OCR
            }

            // Priority 4: fuzzy digit matching anywhere in text
            if (lower.contains("2")) {
                return VoiceCommand.OCR
            }
            if (lower.contains("1")) {
                return VoiceCommand.OBSTACLES
            }
        }
        return VoiceCommand.UNKNOWN
    }

    fun stopListening() {
        handler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        isListening = false
        onListeningStateChanged(false)
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        isContinuous = false
        isPaused = false
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        isListening = false
    }

    companion object {
        private const val TAG = "VoiceCommandManager"
    }
}
