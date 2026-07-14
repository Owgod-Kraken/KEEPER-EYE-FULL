package com.keepereye.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.keepereye.app.databinding.ActivityMainBinding
import com.keepereye.app.tts.TextToSpeechManager
import com.keepereye.app.voice.VoiceCommandProcessor

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var voiceProcessor: VoiceCommandProcessor
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var pendingTarget: Class<*>? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingTarget?.let { navigateTo(it) }
        } else {
            ttsManager.speak(getString(R.string.camera_permission_denied))
        }
        pendingTarget = null
    }

    private val multiPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            pendingTarget?.let { navigateTo(it) }
        }
        pendingTarget = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ttsManager = TextToSpeechManager(this) {
            speakWelcomeMessage()
        }
        voiceProcessor = VoiceCommandProcessor()

        setupUI()
        setupSpeechRecognizer()
    }

    private fun speakWelcomeMessage() {
        ttsManager.speakSequence(
            listOf(
                getString(R.string.welcome_message),
                getString(R.string.voice_menu)
            )
        ) {
            startListening()
        }
    }

    private fun setupUI() {
        binding.btnObstacles.setOnClickListener {
            checkCameraPermissionAndStart(ObstacleActivity::class.java)
        }

        binding.btnStartScan.setOnClickListener {
            checkCameraPermissionAndStart(ScanActivity::class.java)
        }

        binding.btnDescribe.setOnClickListener {
            checkCameraPermissionAndStart(DescribeActivity::class.java)
        }

        binding.btnBraille.setOnClickListener {
            checkCameraPermissionAndStart(BrailleActivity::class.java)
        }

        binding.btnColors.setOnClickListener {
            checkCameraPermissionAndStart(ColorDetectionActivity::class.java)
        }

        binding.btnVision.setOnClickListener {
            checkCameraPermissionAndStart(VisionActivity::class.java)
        }

        binding.btnAssistant.setOnClickListener {
            checkCameraPermissionAndStart(SmartAssistantActivity::class.java)
        }

        binding.btnEmergency.setOnClickListener {
            launchEmergency()
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            binding.tvListeningStatus.text = getString(R.string.voice_not_available)
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    binding.tvListeningStatus.text = getString(R.string.listening)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        processVoiceCommand(matches[0].lowercase())
                    }
                    restartListening()
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onEndOfSpeech() { isListening = false }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onRmsChanged(rmsdB: Float) {}

                override fun onError(error: Int) {
                    isListening = false
                    if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                        error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    ) {
                        Log.e(TAG, "Speech recognition error: $error")
                    }
                    restartListening()
                }
            })
        }
    }

    private fun startListening() {
        if (isListening) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            multiPermissionLauncher.launch(
                arrayOf(Manifest.permission.RECORD_AUDIO)
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
        }
    }

    private fun restartListening() {
        binding.root.postDelayed({ startListening() }, 500)
    }

    private fun processVoiceCommand(command: String) {
        val action = voiceProcessor.processCommand(command)
        when (action) {
            VoiceCommandProcessor.Action.OBSTACLES -> {
                ttsManager.speak("Abriendo detector de obstáculos")
                checkCameraPermissionAndStart(ObstacleActivity::class.java)
            }
            VoiceCommandProcessor.Action.OCR -> {
                ttsManager.speak("Abriendo lectura OCR")
                checkCameraPermissionAndStart(ScanActivity::class.java)
            }
            VoiceCommandProcessor.Action.DESCRIBE -> {
                ttsManager.speak("Abriendo descripción de imágenes")
                checkCameraPermissionAndStart(DescribeActivity::class.java)
            }
            VoiceCommandProcessor.Action.BRAILLE -> {
                ttsManager.speak("Abriendo lector Braille")
                checkCameraPermissionAndStart(BrailleActivity::class.java)
            }
            VoiceCommandProcessor.Action.COLORS -> {
                ttsManager.speak("Abriendo detector de colores")
                checkCameraPermissionAndStart(ColorDetectionActivity::class.java)
            }
            VoiceCommandProcessor.Action.VISION -> {
                ttsManager.speak("Abriendo qué estoy viendo")
                checkCameraPermissionAndStart(VisionActivity::class.java)
            }
            VoiceCommandProcessor.Action.HELP -> {
                ttsManager.speakSequence(listOf(getString(R.string.voice_menu))) {
                    startListening()
                }
            }
            VoiceCommandProcessor.Action.HOME -> {
                ttsManager.speak("Estás en el menú principal")
            }
            VoiceCommandProcessor.Action.EXIT -> {
                ttsManager.speak("Cerrando aplicación")
                finish()
            }
            VoiceCommandProcessor.Action.EMERGENCY -> {
                launchEmergency()
            }
            VoiceCommandProcessor.Action.NONE -> {}
        }
    }

    private fun launchEmergency() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS
        )
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            startActivity(Intent(this, EmergencyActivity::class.java))
        } else {
            pendingTarget = EmergencyActivity::class.java
            multiPermissionLauncher.launch(permissions)
        }
    }

    private fun checkCameraPermissionAndStart(target: Class<*>) {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                navigateTo(target)
            }
            else -> {
                pendingTarget = target
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun navigateTo(target: Class<*>) {
        stopListening()
        startActivity(Intent(this, target))
    }

    private fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping listener", e)
        }
        isListening = false
    }

    override fun onResume() {
        super.onResume()
        binding.root.postDelayed({ startListening() }, 1000)
    }

    override fun onPause() {
        super.onPause()
        stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        ttsManager.shutdown()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
