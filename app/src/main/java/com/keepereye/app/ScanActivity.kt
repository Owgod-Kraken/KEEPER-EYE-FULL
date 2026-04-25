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
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.keepereye.app.ai.KeywordDetector
import com.keepereye.app.databinding.ActivityScanBinding
import com.keepereye.app.history.HistoryRepository
import com.keepereye.app.obstacle.ObjectDetectionAnalyzer
import com.keepereye.app.obstacle.ObstacleAlertManager
import com.keepereye.app.ocr.TextRecognitionAnalyzer
import com.keepereye.app.tts.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class ScanMode {
    OCR_ONLY,
    ASSIST
}

class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var keywordDetector: KeywordDetector
    private lateinit var historyRepository: HistoryRepository
    private lateinit var obstacleAlertManager: ObstacleAlertManager
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var objectExecutor: ExecutorService

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastSpokenText = ""
    private var isProcessingOcr = false
    private var hasActiveText = false
    private var currentMode = ScanMode.OCR_ONLY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ttsManager = TextToSpeechManager(this)
        keywordDetector = KeywordDetector()
        historyRepository = HistoryRepository(this)
        obstacleAlertManager = ObstacleAlertManager(this, ttsManager)
        cameraExecutor = Executors.newSingleThreadExecutor()
        objectExecutor = Executors.newSingleThreadExecutor()

        val startInAssist = intent.getBooleanExtra(EXTRA_ASSIST_MODE, false)
        currentMode = if (startInAssist) ScanMode.ASSIST else ScanMode.OCR_ONLY

        setupUI()
        updateModeUI()
        startCamera()
    }

    private fun setupUI() {
        binding.btnRepeat.setOnClickListener {
            if (lastSpokenText.isNotEmpty()) {
                ttsManager.speak(lastSpokenText)
            }
        }

        binding.btnStop.setOnClickListener {
            ttsManager.stop()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnToggleMode.setOnClickListener {
            currentMode = when (currentMode) {
                ScanMode.OCR_ONLY -> ScanMode.ASSIST
                ScanMode.ASSIST -> ScanMode.OCR_ONLY
            }
            updateModeUI()
            restartCamera()
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

    private fun updateModeUI() {
        when (currentMode) {
            ScanMode.OCR_ONLY -> {
                binding.tvModeIndicator.text = getString(R.string.mode_ocr)
                binding.tvModeIndicator.setTextColor(
                    ContextCompat.getColor(this, R.color.accent_green)
                )
                binding.tvNoText.text = getString(R.string.scanning)
                binding.objectOverlay.visibility = View.GONE
                binding.textOverlay.visibility = View.VISIBLE
                binding.statusIndicators.visibility = View.GONE
            }
            ScanMode.ASSIST -> {
                binding.tvModeIndicator.text = getString(R.string.mode_assist)
                binding.tvModeIndicator.setTextColor(
                    ContextCompat.getColor(this, R.color.accent_orange)
                )
                binding.tvNoText.text = getString(R.string.scanning_assist)
                binding.objectOverlay.visibility = View.VISIBLE
                binding.textOverlay.visibility = View.VISIBLE
                binding.statusIndicators.visibility = View.VISIBLE
            }
        }
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

            val ocrAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, createOcrAnalyzer())
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                if (currentMode == ScanMode.ASSIST) {
                    val objectAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(objectExecutor, createObjectAnalyzer())
                        }

                    cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        ocrAnalyzer,
                        objectAnalyzer
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        ocrAnalyzer
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                if (currentMode == ScanMode.ASSIST) {
                    Log.w(TAG, "Falling back to OCR-only with object detection on same executor")
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            this,
                            cameraSelector,
                            preview,
                            ocrAnalyzer
                        )
                    } catch (fallbackEx: Exception) {
                        Log.e(TAG, "Fallback camera binding also failed", fallbackEx)
                    }
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun restartCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
            obstacleAlertManager.reset()
            hasActiveText = false
            startCamera()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun createOcrAnalyzer(): TextRecognitionAnalyzer {
        return TextRecognitionAnalyzer { result ->
            if (isProcessingOcr) return@TextRecognitionAnalyzer
            isProcessingOcr = true

            runOnUiThread {
                val detectedText = result.text
                binding.textOverlay.setTextBlocks(result.textBlocks)
                binding.textOverlay.invalidate()

                if (detectedText.isNotBlank()) {
                    binding.tvDetectedText.text = detectedText
                    binding.tvDetectedText.visibility = View.VISIBLE
                    binding.tvNoText.visibility = View.GONE
                    hasActiveText = true

                    if (currentMode == ScanMode.ASSIST) {
                        binding.tvTextStatus.visibility = View.VISIBLE
                    }

                    val keywords = keywordDetector.detectKeywords(detectedText)
                    if (keywords.isNotEmpty()) {
                        val priorityText = keywordDetector.buildPriorityMessage(keywords)
                        if (priorityText != lastSpokenText) {
                            lastSpokenText = priorityText
                            ttsManager.speakWithPriority(priorityText)
                            vibrateAlert()
                        }
                    } else if (detectedText != lastSpokenText && detectedText.length > 3) {
                        lastSpokenText = detectedText
                        ttsManager.speak(detectedText)
                        vibrateShort()
                    }

                    scope.launch(Dispatchers.IO) {
                        historyRepository.saveText(detectedText)
                    }
                } else {
                    binding.tvDetectedText.visibility = View.GONE
                    hasActiveText = false

                    if (currentMode == ScanMode.ASSIST) {
                        binding.tvTextStatus.visibility = View.GONE
                        binding.tvNoText.text = getString(R.string.scanning_assist)
                    } else {
                        binding.tvNoText.visibility = View.VISIBLE
                    }
                }

                isProcessingOcr = false
            }
        }
    }

    private fun createObjectAnalyzer(): ObjectDetectionAnalyzer {
        return ObjectDetectionAnalyzer { obstacles ->
            runOnUiThread {
                binding.objectOverlay.setObstacles(obstacles)

                if (obstacles.isNotEmpty()) {
                    binding.tvObstacleStatus.visibility = View.VISIBLE
                    binding.tvNoText.visibility = View.GONE
                    obstacleAlertManager.processObstacles(obstacles, hasActiveText)
                } else {
                    binding.tvObstacleStatus.visibility = View.GONE
                    if (!hasActiveText) {
                        binding.tvNoText.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun vibrateShort() {
        triggerVibration(100L)
    }

    private fun vibrateAlert() {
        triggerVibration(300L)
    }

    private fun triggerVibration(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        objectExecutor.shutdown()
        ttsManager.shutdown()
        scope.cancel()
    }

    companion object {
        private const val TAG = "ScanActivity"
        const val EXTRA_ASSIST_MODE = "extra_assist_mode"
    }
}
