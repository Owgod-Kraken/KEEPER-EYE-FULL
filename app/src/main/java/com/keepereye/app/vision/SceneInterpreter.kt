package com.keepereye.app.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SceneInterpreter(private val context: Context) {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(0.55f).build()
    )

    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun interpretScene(imageProxy: ImageProxy): String {
        val mediaImage = imageProxy.image ?: return ""
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        val labels = mutableListOf<String>()
        val objects = mutableListOf<String>()
        var detectedText = ""
        val latch = CountDownLatch(3)

        labeler.process(image)
            .addOnSuccessListener { result ->
                result.take(5).forEach { labels.add(it.text) }
            }
            .addOnCompleteListener { latch.countDown() }

        objectDetector.process(image)
            .addOnSuccessListener { result ->
                result.forEach { obj ->
                    obj.labels.firstOrNull()?.let { objects.add(it.text) }
                }
            }
            .addOnCompleteListener { latch.countDown() }

        textRecognizer.process(image)
            .addOnSuccessListener { result ->
                detectedText = result.text.take(200)
            }
            .addOnCompleteListener { latch.countDown() }

        latch.await(8, TimeUnit.SECONDS)

        return buildCompleteDescription(labels, objects, detectedText)
    }

    private fun buildCompleteDescription(
        labels: List<String>,
        objects: List<String>,
        text: String
    ): String {
        val parts = mutableListOf<String>()

        val translatedObjects = objects.distinct().map { translateToSpanish(it) }
        val translatedLabels = labels.distinct().map { translateToSpanish(it) }

        val allItems = (translatedObjects + translatedLabels).distinct().take(4)

        if (allItems.isNotEmpty()) {
            val itemsStr = when {
                allItems.size == 1 -> allItems[0]
                allItems.size == 2 -> "${allItems[0]} y ${allItems[1]}"
                else -> allItems.dropLast(1).joinToString(", ") + " y ${allItems.last()}"
            }
            parts.add(itemsStr)
        }

        if (text.isNotBlank()) {
            val cleanText = text.replace("\n", " ").trim()
            if (cleanText.length > 3) {
                parts.add("También se detecta texto que dice: \"$cleanText\"")
            }
        }

        return if (parts.isEmpty()) {
            ""
        } else {
            parts.joinToString(". ") + "."
        }
    }

    private fun translateToSpanish(label: String): String {
        return TRANSLATIONS[label.lowercase()] ?: label.lowercase()
    }

    companion object {
        private const val TAG = "SceneInterpreter"

        private val TRANSLATIONS = mapOf(
            "person" to "una persona",
            "people" to "varias personas",
            "car" to "un vehículo",
            "vehicle" to "un vehículo",
            "bicycle" to "una bicicleta",
            "motorcycle" to "una motocicleta",
            "bus" to "un autobús",
            "truck" to "un camión",
            "dog" to "un perro",
            "cat" to "un gato",
            "bird" to "un pájaro",
            "chair" to "una silla",
            "table" to "una mesa",
            "desk" to "un escritorio",
            "bed" to "una cama",
            "couch" to "un sofá",
            "computer" to "una computadora",
            "laptop" to "una computadora portátil",
            "phone" to "un teléfono",
            "book" to "un libro",
            "bottle" to "una botella",
            "cup" to "una taza",
            "bag" to "una bolsa",
            "backpack" to "una mochila",
            "tree" to "un árbol",
            "plant" to "una planta",
            "flower" to "flores",
            "food" to "comida",
            "building" to "un edificio",
            "house" to "una casa",
            "door" to "una puerta",
            "window" to "una ventana",
            "wall" to "una pared",
            "street" to "una calle",
            "road" to "un camino",
            "sign" to "un letrero",
            "traffic light" to "un semáforo",
            "stairs" to "escaleras",
            "clothing" to "ropa",
            "shoe" to "zapatos",
            "glasses" to "lentes",
            "hat" to "un sombrero",
            "fashion good" to "una prenda",
            "home good" to "un objeto del hogar",
            "food" to "comida",
            "place" to "un lugar"
        )
    }
}
