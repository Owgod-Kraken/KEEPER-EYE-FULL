package com.keepereye.app

import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.keepereye.app.assistant.SmartAssistantEngine
import com.keepereye.app.databinding.ActivityCameraModuleBinding
import com.keepereye.app.obstacle.ObstacleAlertManager
import com.keepereye.app.tts.TextToSpeechManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SmartAssistantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraModuleBinding
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var assistantEngine: SmartAssistantEngine
    private lateinit var obstacleAlertManager: ObstacleAlertManager
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraModuleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ttsManager = TextToSpeechManager(this)
        obstacleAlertManager = ObstacleAlertManager(this, ttsManager)
        assistantEngine = SmartAssistantEngine(this, ttsManager, obstacleAlertManager)
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.tvModuleTitle.text = getString(R.string.assistant_title)
        binding.tvStatus.text = getString(R.string.assistant_monitoring)
        binding.btnAction.visibility = View.GONE

        setupUI()
        startCamera()
        ttsManager.speak("Modo asistente inteligente activado. Monitoreando tu entorno de forma continua.")
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val resolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()

            val analyzer = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, assistantEngine)
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analyzer
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        assistantEngine.shutdown()
        ttsManager.shutdown()
    }

    companion object {
        private const val TAG = "SmartAssistantActivity"
    }
}
