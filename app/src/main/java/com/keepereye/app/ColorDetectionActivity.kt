package com.keepereye.app

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.keepereye.app.color.ColorAnalyzer
import com.keepereye.app.databinding.ActivityCameraModuleBinding
import com.keepereye.app.tts.TextToSpeechManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ColorDetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraModuleBinding
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var cameraExecutor: ExecutorService
    private var lastSpokenColor = ""
    private var lastSpeakTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraModuleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ttsManager = TextToSpeechManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.tvModuleTitle.text = getString(R.string.color_title)
        binding.tvStatus.text = getString(R.string.color_scanning)
        binding.btnAction.visibility = View.GONE

        setupUI()
        startCamera()
        ttsManager.speak("Módulo de detección de colores. Apunta la cámara a un objeto para identificar su color.")
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

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ColorAnalyzer { colorName ->
                        runOnUiThread { onColorDetected(colorName) }
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun onColorDetected(colorName: String) {
        val currentTime = System.currentTimeMillis()
        val resultText = getString(R.string.color_detected_format, colorName)
        binding.tvResult.text = resultText
        binding.tvResult.visibility = View.VISIBLE
        binding.tvStatus.visibility = View.GONE

        if (colorName != lastSpokenColor && currentTime - lastSpeakTime > 3000L) {
            ttsManager.speak(resultText)
            lastSpokenColor = colorName
            lastSpeakTime = currentTime
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        ttsManager.shutdown()
    }

    companion object {
        private const val TAG = "ColorDetectionActivity"
    }
}
