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
import com.keepereye.app.braille.BrailleRecognizer
import com.keepereye.app.databinding.ActivityCameraModuleBinding
import com.keepereye.app.tts.TextToSpeechManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BrailleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraModuleBinding
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var brailleRecognizer: BrailleRecognizer
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraModuleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ttsManager = TextToSpeechManager(this)
        brailleRecognizer = BrailleRecognizer()
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.tvModuleTitle.text = getString(R.string.braille_title)
        binding.tvStatus.text = getString(R.string.braille_scanning)
        binding.btnAction.text = getString(R.string.braille_capture)
        binding.btnAction.visibility = View.VISIBLE

        setupUI()
        startCamera()
        ttsManager.speak("Módulo de lectura Braille. Apunta la cámara al texto Braille y toca capturar.")
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAction.setOnClickListener { captureAndRecognize() }
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

    private fun captureAndRecognize() {
        val capture = imageCapture ?: return
        binding.tvStatus.text = getString(R.string.braille_scanning)
        binding.tvStatus.visibility = View.VISIBLE
        binding.btnAction.isEnabled = false

        capture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
            @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
            override fun onCaptureSuccess(image: ImageProxy) {
                val text = brailleRecognizer.recognizeBraille(image)
                image.close()

                runOnUiThread {
                    if (text.isNotBlank()) {
                        val result = "Texto Braille: $text"
                        binding.tvResult.text = result
                        binding.tvResult.visibility = View.VISIBLE
                        binding.tvStatus.visibility = View.GONE
                        ttsManager.speak(result)
                    } else {
                        binding.tvStatus.text = getString(R.string.braille_no_text)
                        binding.tvResult.visibility = View.GONE
                        ttsManager.speak(getString(R.string.braille_no_text))
                    }
                    binding.btnAction.isEnabled = true
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Capture failed", exception)
                runOnUiThread {
                    binding.tvStatus.text = getString(R.string.braille_no_text)
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
        private const val TAG = "BrailleActivity"
    }
}
