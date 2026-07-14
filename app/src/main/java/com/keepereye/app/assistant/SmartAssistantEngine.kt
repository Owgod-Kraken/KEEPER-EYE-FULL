package com.keepereye.app.assistant

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.keepereye.app.obstacle.DetectedObstacle
import com.keepereye.app.obstacle.ObstacleAlertManager
import com.keepereye.app.obstacle.ObstaclePosition
import com.keepereye.app.obstacle.ObstacleProximity
import com.keepereye.app.tts.TextToSpeechManager

class SmartAssistantEngine(
    private val context: Context,
    private val ttsManager: TextToSpeechManager,
    private val obstacleAlertManager: ObstacleAlertManager
) : ImageAnalysis.Analyzer {

    private val objectOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()
    private val objectDetector = ObjectDetection.getClient(objectOptions)
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

    private var isProcessing = false
    private var frameCount = 0
    private var lastTextAlert = 0L
    private var lastImportantText = ""

    private val importantKeywords = listOf(
        "peligro", "danger", "cuidado", "emergencia", "alto", "stop",
        "salida", "exit", "entrada", "prohibido", "precaución", "no pasar"
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        frameCount++
        if (isProcessing || frameCount % 3 != 0) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing = true
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        val imgWidth = if (rotationDegrees == 90 || rotationDegrees == 270) {
            imageProxy.height
        } else {
            imageProxy.width
        }
        val imgHeight = if (rotationDegrees == 90 || rotationDegrees == 270) {
            imageProxy.width
        } else {
            imageProxy.height
        }

        objectDetector.process(image)
            .addOnSuccessListener { results ->
                val obstacles = results.map { obj ->
                    val box = obj.boundingBox
                    val label = obj.labels.firstOrNull()?.text ?: "Objeto"
                    val confidence = obj.labels.firstOrNull()?.confidence ?: 0.5f

                    val centerX = box.centerX().toFloat() / imgWidth.toFloat()
                    val position = when {
                        centerX < 0.33f -> ObstaclePosition.LEFT
                        centerX > 0.66f -> ObstaclePosition.RIGHT
                        else -> ObstaclePosition.CENTER
                    }

                    val areaRatio = (box.width() * box.height()).toFloat() /
                        (imgWidth * imgHeight).toFloat()
                    val proximity = when {
                        areaRatio > 0.15f -> ObstacleProximity.NEAR
                        areaRatio > 0.05f -> ObstacleProximity.MEDIUM
                        else -> ObstacleProximity.FAR
                    }

                    DetectedObstacle(
                        label = DetectedObstacle.translateLabel(label),
                        boundingBox = box,
                        position = position,
                        proximity = proximity,
                        confidence = confidence,
                        trackingId = obj.trackingId
                    )
                }

                if (obstacles.isNotEmpty()) {
                    obstacleAlertManager.processObstacles(obstacles, ttsManager.isSpeaking())
                }
            }
            .addOnCompleteListener {
                processText(image, imageProxy)
            }
    }

    private fun processText(image: InputImage, imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTextAlert < 5000L) {
            isProcessing = false
            imageProxy.close()
            return
        }

        textRecognizer.process(image)
            .addOnSuccessListener { result ->
                val text = result.text
                if (text.isNotBlank()) {
                    val hasImportant = importantKeywords.any {
                        text.lowercase().contains(it)
                    }
                    if (hasImportant && text != lastImportantText) {
                        val importantWord = importantKeywords.first {
                            text.lowercase().contains(it)
                        }
                        ttsManager.speakWithPriority("Texto importante detectado: $importantWord")
                        lastImportantText = text
                        lastTextAlert = currentTime
                    }
                }
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }

    fun shutdown() {
        objectDetector.close()
        textRecognizer.close()
    }

    companion object {
        private const val TAG = "SmartAssistantEngine"
    }
}
