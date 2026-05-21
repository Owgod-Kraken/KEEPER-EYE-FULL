package com.keepereye.app

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.keepereye.app.databinding.ActivityObstacleBinding
import com.keepereye.app.obstacle.ObstacleAlertManager
import com.keepereye.app.obstacle.ObjectDetectionAnalyzer
import com.keepereye.app.tts.TextToSpeechManager
import com.keepereye.app.voice.VoiceCommandManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ObstacleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityObstacleBinding
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var obstacleAlertManager: ObstacleAlertManager
    private lateinit var cameraExecutor: ExecutorService
    private var voiceManager: VoiceCommandManager? = null

    private var lastAlertMessage = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityObstacleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ttsManager = TextToSpeechManager(this) {
            runOnUiThread {
                ttsManager.speak(getString(R.string.obstacle_mode_activated_voice))
            }
        }
        obstacleAlertManager = ObstacleAlertManager(this, ttsManager)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        startCamera()
        startVoiceCommands()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            hapticFeedback()
            finish()
        }

        binding.btnRepeat.setOnClickListener {
            hapticFeedback()
            if (lastAlertMessage.isNotEmpty()) {
                ttsManager.speak(lastAlertMessage)
            }
        }

        binding.btnStop.setOnClickListener {
            hapticFeedback()
            ttsManager.stop()
        }

        binding.speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = 0.5f + (progress / 100f) * 1.5f
                ttsManager.setSpeechRate(speed)
                binding.tvSpeedLabel.text = getString(R.string.speed_format, speed)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.speedSeekBar.progress = 33
    }

    private fun startVoiceCommands() {
        voiceManager = VoiceCommandManager(
            context = this,
            onCommand = { command ->
                runOnUiThread {
                    when (command) {
                        VoiceCommandManager.VoiceCommand.GO_BACK -> {
                            hapticFeedback()
                            ttsManager.speakAndThen(getString(R.string.returning_to_menu)) {
                                runOnUiThread { finish() }
                            }
                        }
                        VoiceCommandManager.VoiceCommand.EXIT -> {
                            hapticFeedback()
                            ttsManager.speakAndThen(getString(R.string.closing_mode)) {
                                runOnUiThread { finish() }
                            }
                        }
                        VoiceCommandManager.VoiceCommand.OCR -> {
                            hapticFeedback()
                            ttsManager.speak(getString(R.string.ocr_mode_activated))
                        }
                        else -> { }
                    }
                }
            },
            onListeningStateChanged = { }
        )
        voiceManager?.startContinuousListening()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            val resolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()

            val objectAnalyzer = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, createObjectAnalyzer())
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    objectAnalyzer
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun createObjectAnalyzer(): ObjectDetectionAnalyzer {
        return ObjectDetectionAnalyzer { obstacles, imageSize ->
            runOnUiThread {
                binding.objectOverlay.setObstacles(obstacles, imageSize)

                if (obstacles.isNotEmpty()) {
                    binding.tvNoObstacle.visibility = View.GONE

                    val sorted = obstacles.sortedByDescending {
                        it.boundingBox.width() * it.boundingBox.height()
                    }
                    val mostImportant = sorted.first()
                    val alertMsg = mostImportant.buildAlertMessage()
                    binding.tvDetectedObstacle.text = alertMsg
                    binding.tvDetectedObstacle.visibility = View.VISIBLE
                    binding.tvObstacleCount.text = getString(
                        R.string.obstacle_count_format, obstacles.size
                    )
                    binding.tvObstacleCount.visibility = View.VISIBLE
                    lastAlertMessage = alertMsg

                    obstacleAlertManager.processObstacles(sorted, false)
                } else {
                    binding.tvDetectedObstacle.visibility = View.GONE
                    binding.tvObstacleCount.visibility = View.GONE
                    binding.tvNoObstacle.visibility = View.VISIBLE
                    binding.tvNoObstacle.text = getString(R.string.scanning_obstacles)
                }
            }
        }
    }

    private fun hapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager?.release()
        cameraExecutor.shutdown()
        ttsManager.shutdown()
    }

    companion object {
        private const val TAG = "ObstacleActivity"
    }
}
