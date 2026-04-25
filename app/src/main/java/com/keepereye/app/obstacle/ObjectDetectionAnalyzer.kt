package com.keepereye.app.obstacle

import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class ObjectDetectionAnalyzer(
    private val onObstaclesDetected: (List<DetectedObstacle>) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()

    private val detector = ObjectDetection.getClient(options)
    private var lastAnalysisTime = 0L
    private var imageWidth = 0
    private var imageHeight = 0

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < ANALYSIS_INTERVAL_MS) {
            imageProxy.close()
            return
        }
        lastAnalysisTime = currentTime

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        imageWidth = imageProxy.width
        imageHeight = imageProxy.height

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(image)
            .addOnSuccessListener { results ->
                val obstacles = results.mapNotNull { obj ->
                    processDetectedObject(obj)
                }
                onObstaclesDetected(obstacles)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Object detection failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun processDetectedObject(obj: DetectedObject): DetectedObstacle {
        val box = obj.boundingBox
        val position = analyzePosition(box)
        val proximity = analyzeProximity(box)

        val label = if (obj.labels.isNotEmpty()) {
            val mlLabel = obj.labels.first()
            classifyObject(mlLabel.text, mlLabel.index, proximity)
        } else {
            classifyBySize(proximity)
        }

        val confidence = if (obj.labels.isNotEmpty()) {
            obj.labels.first().confidence
        } else {
            0.5f
        }

        return DetectedObstacle(
            label = label,
            boundingBox = box,
            position = position,
            proximity = proximity,
            confidence = confidence,
            trackingId = obj.trackingId
        )
    }

    private fun classifyObject(
        mlLabel: String,
        categoryIndex: Int,
        proximity: ObstacleProximity
    ): String {
        return when (categoryIndex) {
            CATEGORY_FASHION_GOOD -> {
                if (proximity == ObstacleProximity.NEAR) "Persona" else "Persona u objeto"
            }
            CATEGORY_HOME_GOOD -> "Mueble"
            CATEGORY_FOOD -> "Objeto cercano"
            CATEGORY_PLACE -> "Estructura"
            CATEGORY_PLANT -> "Planta"
            else -> DetectedObstacle.translateLabel(mlLabel)
        }
    }

    private fun classifyBySize(proximity: ObstacleProximity): String {
        return when (proximity) {
            ObstacleProximity.NEAR -> "Obstáculo"
            ObstacleProximity.MEDIUM -> "Objeto"
            ObstacleProximity.FAR -> "Objeto detectado"
        }
    }

    private fun analyzePosition(box: Rect): ObstaclePosition {
        val centerX = box.centerX().toFloat()
        val frameWidth = if (imageWidth > 0) imageWidth.toFloat() else 480f
        val relativeX = centerX / frameWidth

        return when {
            relativeX < 0.33f -> ObstaclePosition.LEFT
            relativeX > 0.66f -> ObstaclePosition.RIGHT
            else -> ObstaclePosition.CENTER
        }
    }

    private fun analyzeProximity(box: Rect): ObstacleProximity {
        val frameArea = if (imageWidth > 0 && imageHeight > 0) {
            (imageWidth * imageHeight).toFloat()
        } else {
            (480 * 640).toFloat()
        }
        val boxArea = (box.width() * box.height()).toFloat()
        val ratio = boxArea / frameArea

        return when {
            ratio > NEAR_THRESHOLD -> ObstacleProximity.NEAR
            ratio > MEDIUM_THRESHOLD -> ObstacleProximity.MEDIUM
            else -> ObstacleProximity.FAR
        }
    }

    companion object {
        private const val TAG = "ObjectDetectionAnalyzer"
        private const val ANALYSIS_INTERVAL_MS = 500L
        private const val NEAR_THRESHOLD = 0.15f
        private const val MEDIUM_THRESHOLD = 0.05f

        private const val CATEGORY_FASHION_GOOD = 0
        private const val CATEGORY_FOOD = 1
        private const val CATEGORY_HOME_GOOD = 2
        private const val CATEGORY_PLACE = 3
        private const val CATEGORY_PLANT = 4
    }
}
