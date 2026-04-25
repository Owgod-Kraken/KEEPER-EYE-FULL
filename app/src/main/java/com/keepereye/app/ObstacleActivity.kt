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
import com.keepereye.app.databinding.ActivityObstacleBinding
import com.keepereye.app.obstacle.ObstacleAlertManager
import com.keepereye.app.obstacle.ObjectDetectionAnalyzer
import com.keepereye.app.tts.TextToSpeechManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ObstacleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityObstacleBinding
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var obstacleAlertManager: ObstacleAlertManager
    private lateinit var cameraExecutor: ExecutorService

    private var lastAlertMessage = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityObstacleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ttsManager = TextToSpeechManager(this)
        obstacleAlertManager = ObstacleAlertManager(this, ttsManager)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        startCamera()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRepeat.setOnClickListener {
            if (lastAlertMessage.isNotEmpty()) {
                ttsManager.speak(lastAlertMessage)
            }
        }

        binding.btnStop.setOnClickListener {
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

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            val objectAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
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
        return ObjectDetectionAnalyzer { obstacles ->
            runOnUiThread {
                binding.objectOverlay.setObstacles(obstacles)

                if (obstacles.isNotEmpty()) {
                    binding.tvNoObstacle.visibility = View.GONE
                    val mostImportant = obstacles.first()
                    val alertMsg = mostImportant.buildAlertMessage()
                    binding.tvDetectedObstacle.text = alertMsg
                    binding.tvDetectedObstacle.visibility = View.VISIBLE
                    binding.tvObstacleCount.text = getString(
                        R.string.obstacle_count_format, obstacles.size
                    )
                    binding.tvObstacleCount.visibility = View.VISIBLE
                    lastAlertMessage = alertMsg

                    obstacleAlertManager.processObstacles(obstacles, false)
                } else {
                    binding.tvNoObstacle.visibility = View.VISIBLE
                    binding.tvDetectedObstacle.visibility = View.GONE
                    binding.tvObstacleCount.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        ttsManager.shutdown()
    }

    companion object {
        private const val TAG = "ObstacleActivity"
    }
}
