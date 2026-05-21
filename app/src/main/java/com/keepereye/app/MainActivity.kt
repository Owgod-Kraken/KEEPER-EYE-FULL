package com.keepereye.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.keepereye.app.databinding.ActivityMainBinding
import com.keepereye.app.tts.TextToSpeechManager
import com.keepereye.app.voice.VoiceCommandManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ttsManager: TextToSpeechManager
    private var voiceManager: VoiceCommandManager? = null
    private var hasGreeted = false
    private var permissionsReady = false

    private val allPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionsReady = true
        onPermissionsReady()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ttsManager = TextToSpeechManager(this) {
            runOnUiThread {
                if (permissionsReady && !hasGreeted) {
                    hasGreeted = true
                    startWelcomeSequence()
                }
            }
        }

        setupUI()
        setupAccessibility()
        requestAllPermissions()
    }

    private fun requestAllPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }

        if (needed.isEmpty()) {
            permissionsReady = true
            onPermissionsReady()
        } else {
            allPermissionsLauncher.launch(needed.toTypedArray())
        }
    }

    private fun onPermissionsReady() {
        if (!hasGreeted) {
            hasGreeted = true
            startWelcomeSequence()
        }
    }

    private fun setupUI() {
        binding.btnStartScan.setOnClickListener {
            hapticFeedback()
            ttsManager.speakAndThen(getString(R.string.ocr_mode_activated)) {
                runOnUiThread { navigateTo(ScanActivity::class.java) }
            }
        }

        binding.btnObstacles.setOnClickListener {
            hapticFeedback()
            ttsManager.speakAndThen(getString(R.string.obstacle_mode_activated)) {
                runOnUiThread { navigateTo(ObstacleActivity::class.java) }
            }
        }

        binding.btnHistory.setOnClickListener {
            hapticFeedback()
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.fabVoice.setOnClickListener {
            hapticFeedback()
            startVoiceInteraction()
        }

        binding.btnStartScan.contentDescription = getString(R.string.start_scan_description)
        binding.btnObstacles.contentDescription = getString(R.string.obstacle_detector_description)
        binding.btnHistory.contentDescription = getString(R.string.history_description)
    }

    private fun setupAccessibility() {
        binding.btnObstacles.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        binding.btnStartScan.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        binding.btnHistory.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        binding.fabVoice.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    private fun startWelcomeSequence() {
        ttsManager.speakAndThen(getString(R.string.welcome_message)) {
            runOnUiThread {
                ttsManager.speakAndThen(getString(R.string.voice_menu)) {
                    runOnUiThread { startVoiceInteraction() }
                }
            }
        }
    }

    private fun startVoiceInteraction() {
        binding.tvVoiceStatus.visibility = View.VISIBLE
        binding.tvVoiceStatus.text = getString(R.string.listening)

        voiceManager?.release()
        voiceManager = VoiceCommandManager(
            context = this,
            onCommand = { command -> runOnUiThread { handleVoiceCommand(command) } },
            onListeningStateChanged = { listening ->
                runOnUiThread {
                    binding.tvVoiceStatus.visibility = if (listening) View.VISIBLE else View.GONE
                }
            }
        )
        voiceManager?.startListening()
    }

    private fun handleVoiceCommand(command: VoiceCommandManager.VoiceCommand) {
        binding.tvVoiceStatus.visibility = View.GONE
        when (command) {
            VoiceCommandManager.VoiceCommand.OBSTACLES -> {
                hapticFeedback()
                ttsManager.speakAndThen(getString(R.string.obstacle_mode_activated)) {
                    runOnUiThread { navigateTo(ObstacleActivity::class.java) }
                }
            }
            VoiceCommandManager.VoiceCommand.OCR -> {
                hapticFeedback()
                ttsManager.speakAndThen(getString(R.string.ocr_mode_activated)) {
                    runOnUiThread { navigateTo(ScanActivity::class.java) }
                }
            }
            VoiceCommandManager.VoiceCommand.UNKNOWN -> {
                ttsManager.speakAndThen(getString(R.string.voice_not_recognized)) {
                    runOnUiThread {
                        ttsManager.speakAndThen(getString(R.string.voice_menu)) {
                            runOnUiThread { startVoiceInteraction() }
                        }
                    }
                }
            }
        }
    }

    private fun navigateTo(target: Class<*>) {
        startActivity(Intent(this, target))
    }

    private fun hapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50L)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.tvVoiceStatus.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager?.release()
        ttsManager.shutdown()
    }
}
