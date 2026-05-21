package com.keepereye.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.keepereye.app.databinding.ActivityMainBinding
import com.keepereye.app.tts.TextToSpeechManager
import com.keepereye.app.voice.VoiceCommandManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ttsManager: TextToSpeechManager
    private var voiceManager: VoiceCommandManager? = null
    private var hasGreeted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ttsManager = TextToSpeechManager(this) {
            runOnUiThread {
                if (!hasGreeted) {
                    hasGreeted = true
                    startWelcomeSequence()
                }
            }
        }

        setupUI()
    }

    private fun setupUI() {
        binding.btnStartScan.setOnClickListener {
            hapticFeedback()
            launchOCR()
        }

        binding.btnObstacles.setOnClickListener {
            hapticFeedback()
            launchObstacles()
        }

        binding.btnHistory.setOnClickListener {
            hapticFeedback()
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.fabVoice.setOnClickListener {
            hapticFeedback()
            startContinuousVoice()
        }
    }

    private fun startWelcomeSequence() {
        binding.tvVoiceStatus.visibility = View.VISIBLE
        binding.tvVoiceStatus.text = getString(R.string.welcome_message)

        ttsManager.speakAndThen(getString(R.string.welcome_message)) {
            runOnUiThread {
                binding.tvVoiceStatus.text = getString(R.string.voice_menu_full)
                ttsManager.speakAndThen(getString(R.string.voice_menu_full)) {
                    runOnUiThread { startContinuousVoice() }
                }
            }
        }
    }

    private fun startContinuousVoice() {
        binding.tvVoiceStatus.visibility = View.VISIBLE
        binding.tvVoiceStatus.text = getString(R.string.listening)

        voiceManager?.release()
        voiceManager = VoiceCommandManager(
            context = this,
            onCommand = { command -> runOnUiThread { handleVoiceCommand(command) } },
            onListeningStateChanged = { listening ->
                runOnUiThread {
                    if (listening) {
                        binding.tvVoiceStatus.visibility = View.VISIBLE
                        binding.tvVoiceStatus.text = getString(R.string.listening)
                    }
                }
            }
        )
        voiceManager?.startContinuousListening()
    }

    private fun handleVoiceCommand(command: VoiceCommandManager.VoiceCommand) {
        when (command) {
            VoiceCommandManager.VoiceCommand.OBSTACLES -> {
                hapticFeedback()
                voiceManager?.pause()
                launchObstacles()
            }
            VoiceCommandManager.VoiceCommand.OCR -> {
                hapticFeedback()
                voiceManager?.pause()
                launchOCR()
            }
            VoiceCommandManager.VoiceCommand.EXIT -> {
                hapticFeedback()
                ttsManager.speakAndThen(getString(R.string.goodbye_message)) {
                    runOnUiThread { finishAffinity() }
                }
            }
            VoiceCommandManager.VoiceCommand.GO_BACK -> {
                // Already on main screen
            }
            VoiceCommandManager.VoiceCommand.UNKNOWN -> {
                // Continuous mode auto-restarts
            }
        }
    }

    private fun launchObstacles() {
        ttsManager.speakAndThen(getString(R.string.obstacle_mode_activated)) {
            runOnUiThread { startActivity(Intent(this, ObstacleActivity::class.java)) }
        }
    }

    private fun launchOCR() {
        ttsManager.speakAndThen(getString(R.string.ocr_mode_activated)) {
            runOnUiThread { startActivity(Intent(this, ScanActivity::class.java)) }
        }
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
        if (hasGreeted) {
            binding.tvVoiceStatus.visibility = View.VISIBLE
            binding.tvVoiceStatus.text = getString(R.string.listening)
            ttsManager.speakAndThen(getString(R.string.voice_menu_short)) {
                runOnUiThread { startContinuousVoice() }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        voiceManager?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager?.release()
        ttsManager.shutdown()
    }
}
