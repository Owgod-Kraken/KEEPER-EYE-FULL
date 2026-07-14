package com.keepereye.app.color

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class ColorAnalyzer(
    private val onColorDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAnalysisTime = 0L

    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < 1000) {
            image.close()
            return
        }
        lastAnalysisTime = currentTime

        val colorName = detectDominantColor(image)
        if (colorName.isNotBlank()) {
            onColorDetected(colorName)
        }
        image.close()
    }

    private fun detectDominantColor(image: ImageProxy): String {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        val centerX = width / 2
        val centerY = height / 2
        val sampleSize = 50

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var count = 0

        for (dy in -sampleSize..sampleSize step 5) {
            for (dx in -sampleSize..sampleSize step 5) {
                val x = centerX + dx
                val y = centerY + dy
                if (x in 0 until width && y in 0 until height) {
                    val offset = y * rowStride + x * pixelStride
                    if (offset + 2 < buffer.capacity()) {
                        val r = buffer.get(offset).toInt() and 0xFF
                        val g = buffer.get(offset + 1).toInt() and 0xFF
                        val b = buffer.get(offset + 2).toInt() and 0xFF
                        totalR += r
                        totalG += g
                        totalB += b
                        count++
                    }
                }
            }
        }

        if (count == 0) return ""

        val avgR = (totalR / count).toInt()
        val avgG = (totalG / count).toInt()
        val avgB = (totalB / count).toInt()

        return classifyColor(avgR, avgG, avgB)
    }

    private fun classifyColor(r: Int, g: Int, b: Int): String {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(r, g, b, hsv)
        val hue = hsv[0]
        val saturation = hsv[1]
        val value = hsv[2]

        return when {
            value < 0.15f -> "Negro"
            value > 0.85f && saturation < 0.15f -> "Blanco"
            saturation < 0.15f && value < 0.5f -> "Gris oscuro"
            saturation < 0.15f -> "Gris"

            hue < 10f || hue >= 350f -> when {
                saturation > 0.6f && value > 0.5f -> "Rojo"
                saturation > 0.3f && value < 0.4f -> "Vino"
                saturation < 0.5f && value > 0.7f -> "Rosa claro"
                else -> "Rojo oscuro"
            }
            hue in 10f..25f -> when {
                saturation > 0.7f -> "Naranja"
                value < 0.5f -> "Café"
                else -> "Naranja claro"
            }
            hue in 25f..45f -> when {
                saturation > 0.5f && value > 0.7f -> "Amarillo"
                saturation < 0.4f -> "Beige"
                value < 0.5f -> "Café"
                else -> "Dorado"
            }
            hue in 45f..75f -> when {
                value < 0.4f -> "Verde oliva"
                else -> "Amarillo verdoso"
            }
            hue in 75f..160f -> when {
                saturation > 0.6f && value > 0.6f -> "Verde"
                saturation > 0.3f && value < 0.4f -> "Verde oscuro"
                saturation < 0.3f -> "Verde grisáceo"
                else -> "Verde claro"
            }
            hue in 160f..190f -> "Turquesa"
            hue in 190f..220f -> when {
                value > 0.7f && saturation > 0.5f -> "Azul cielo"
                value < 0.4f -> "Azul marino"
                else -> "Azul"
            }
            hue in 220f..260f -> when {
                value > 0.6f && saturation > 0.6f -> "Azul"
                value < 0.3f -> "Azul marino"
                else -> "Azul oscuro"
            }
            hue in 260f..290f -> when {
                saturation > 0.5f -> "Morado"
                value > 0.7f -> "Lila"
                else -> "Violeta"
            }
            hue in 290f..330f -> when {
                saturation > 0.6f && value > 0.5f -> "Rosa"
                saturation > 0.4f -> "Magenta"
                else -> "Rosa pálido"
            }
            hue in 330f..350f -> when {
                saturation > 0.5f -> "Rosa fuerte"
                else -> "Rosa"
            }
            else -> "Indeterminado"
        }
    }
}
