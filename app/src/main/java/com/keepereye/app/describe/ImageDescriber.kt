package com.keepereye.app.describe

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ImageDescriber(private val context: Context) {

    private val labelerOptions = ImageLabelerOptions.Builder()
        .setConfidenceThreshold(0.6f)
        .build()
    private val labeler = ImageLabeling.getClient(labelerOptions)

    private val objectOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()
    private val objectDetector = ObjectDetection.getClient(objectOptions)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun describeImage(imageProxy: ImageProxy): String {
        val mediaImage = imageProxy.image ?: return ""
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        val labels = mutableListOf<String>()
        val objects = mutableListOf<String>()
        val latch = CountDownLatch(2)

        labeler.process(image)
            .addOnSuccessListener { imageLabels ->
                imageLabels.forEach { label ->
                    labels.add(translateLabel(label.text))
                }
            }
            .addOnFailureListener { e -> Log.e(TAG, "Labeling failed", e) }
            .addOnCompleteListener { latch.countDown() }

        objectDetector.process(image)
            .addOnSuccessListener { detectedObjects ->
                detectedObjects.forEach { obj ->
                    obj.labels.forEach { label ->
                        objects.add(translateLabel(label.text))
                    }
                }
            }
            .addOnFailureListener { e -> Log.e(TAG, "Object detection failed", e) }
            .addOnCompleteListener { latch.countDown() }

        latch.await(5, TimeUnit.SECONDS)

        return buildDescription(labels, objects)
    }

    private fun buildDescription(labels: List<String>, objects: List<String>): String {
        val allItems = (objects + labels).distinct().take(5)
        if (allItems.isEmpty()) return ""

        val sb = StringBuilder()
        when {
            allItems.size == 1 -> sb.append("${allItems[0]}.")
            allItems.size == 2 -> sb.append("${allItems[0]} y ${allItems[1]}.")
            else -> {
                allItems.dropLast(1).forEachIndexed { index, item ->
                    if (index > 0) sb.append(", ")
                    sb.append(item)
                }
                sb.append(" y ${allItems.last()}.")
            }
        }
        return sb.toString()
    }

    private fun translateLabel(label: String): String {
        return LABEL_TRANSLATIONS[label.lowercase()] ?: label.lowercase()
    }

    companion object {
        private const val TAG = "ImageDescriber"

        private val LABEL_TRANSLATIONS = mapOf(
            "person" to "una persona",
            "people" to "personas",
            "car" to "un vehículo",
            "vehicle" to "un vehículo",
            "bicycle" to "una bicicleta",
            "dog" to "un perro",
            "cat" to "un gato",
            "chair" to "una silla",
            "table" to "una mesa",
            "desk" to "un escritorio",
            "computer" to "una computadora",
            "laptop" to "una computadora portátil",
            "phone" to "un teléfono",
            "book" to "un libro",
            "tree" to "un árbol",
            "plant" to "una planta",
            "flower" to "una flor",
            "food" to "comida",
            "building" to "un edificio",
            "door" to "una puerta",
            "window" to "una ventana",
            "street" to "una calle",
            "road" to "una carretera",
            "sky" to "el cielo",
            "grass" to "césped",
            "water" to "agua",
            "animal" to "un animal",
            "bird" to "un pájaro",
            "furniture" to "muebles",
            "clothing" to "ropa",
            "shoe" to "un zapato",
            "bag" to "una mochila",
            "backpack" to "una mochila",
            "bottle" to "una botella",
            "cup" to "una taza",
            "glasses" to "lentes",
            "hat" to "un sombrero",
            "umbrella" to "un paraguas",
            "stairs" to "escaleras",
            "wall" to "una pared",
            "floor" to "el piso",
            "ceiling" to "el techo",
            "light" to "una luz",
            "sign" to "un letrero"
        )
    }
}
