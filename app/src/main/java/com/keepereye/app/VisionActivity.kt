package com.keepereye.app

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.keepereye.app.databinding.ActivityCameraModuleBinding
import com.keepereye.app.tts.TextToSpeechManager
import com.keepereye.app.vision.SceneInterpreter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VisionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraModuleBinding
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var sceneInterpreter: SceneInterpreter
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraModuleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ttsManager = TextToSpeechManager(this)
        sceneInterpreter = SceneInterpreter(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.tvModuleTitle.text = getString(R.string.vision_title)
        binding.tvStatus.text = getString(R.string.vision_analyzing)
        binding.btnAction.text = "Analizar"
        binding.btnAction.visibility = View.VISIBLE

        setupUI()
        startCamera()
        ttsManager.speak("Módulo qué estoy viendo. Toca analizar para una descripción completa del entorno.")
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAction.setOnClickListener { captureAndInterpret() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndInterpret() {
        val capture = imageCapture ?: return
        binding.tvStatus.text = getString(R.string.vision_analyzing)
        binding.tvStatus.visibility = View.VISIBLE
        binding.tvResult.visibility = View.GONE
        binding.btnAction.isEnabled = false

        capture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val interpretation = sceneInterpreter.interpretScene(image)
                image.close()

                runOnUiThread {
                    val resultText = if (interpretation.isNotBlank()) {
                        "${getString(R.string.vision_result_prefix)} $interpretation"
                    } else {
                        "No se pudo interpretar la escena"
                    }
                    binding.tvResult.text = resultText
                    binding.tvResult.visibility = View.VISIBLE
                    binding.tvStatus.visibility = View.GONE
                    binding.btnAction.isEnabled = true
                    ttsManager.speak(resultText)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Capture failed", exception)
                runOnUiThread {
                    binding.tvStatus.text = "Error al capturar imagen"
                    binding.btnAction.isEnabled = true
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        ttsManager.shutdown()
    }

    companion object {
        private const val TAG = "VisionActivity"
    }
}
