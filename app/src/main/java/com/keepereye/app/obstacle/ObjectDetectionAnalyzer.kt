package com.keepereye.app.obstacle

import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class ObjectDetectionAnalyzer(
    private val onObstaclesDetected: (List<DetectedObstacle>, Size) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()

    private val detector = ObjectDetection.getClient(options)
    private var isDetecting = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isDetecting) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isDetecting = true

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val imageWidth: Int
        val imageHeight: Int
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            imageWidth = imageProxy.height
            imageHeight = imageProxy.width
        } else {
            imageWidth = imageProxy.width
            imageHeight = imageProxy.height
        }

        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { results ->
                val obstacles = results.map { obj ->
                    processDetectedObject(obj, imageWidth, imageHeight)
                }
                onObstaclesDetected(obstacles, Size(imageWidth, imageHeight))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Object detection failed", e)
                onObstaclesDetected(emptyList(), Size(imageWidth, imageHeight))
            }
            .addOnCompleteListener {
                isDetecting = false
                imageProxy.close()
            }
    }

    private fun processDetectedObject(
        obj: DetectedObject,
        imgWidth: Int,
        imgHeight: Int
    ): DetectedObstacle {
        val box = obj.boundingBox
        val position = analyzePosition(box, imgWidth)
        val proximity = analyzeProximity(box, imgWidth, imgHeight)

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

    private fun analyzePosition(box: Rect, imgWidth: Int): ObstaclePosition {
        val centerX = box.centerX().toFloat()
        val frameWidth = imgWidth.toFloat().coerceAtLeast(1f)
        val relativeX = centerX / frameWidth

        return when {
            relativeX < 0.33f -> ObstaclePosition.LEFT
            relativeX > 0.66f -> ObstaclePosition.RIGHT
            else -> ObstaclePosition.CENTER
        }
    }

    private fun analyzeProximity(box: Rect, imgWidth: Int, imgHeight: Int): ObstacleProximity {
        val frameArea = (imgWidth * imgHeight).toFloat().coerceAtLeast(1f)
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
        private const val NEAR_THRESHOLD = 0.12f
        private const val MEDIUM_THRESHOLD = 0.04f

        private const val CATEGORY_FASHION_GOOD = 0
        private const val CATEGORY_FOOD = 1
        private const val CATEGORY_HOME_GOOD = 2
        private const val CATEGORY_PLACE = 3
        private const val CATEGORY_PLANT = 4
    }
}
