package com.keepereye.app.braille

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

class BrailleRecognizer {

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun recognizeBraille(imageProxy: ImageProxy): String {
        val bitmap = imageProxyToBitmap(imageProxy) ?: return ""
        return processImage(bitmap)
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val planes = imageProxy.planes
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height), 80, out)
            val bytes = out.toByteArray()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun processImage(bitmap: Bitmap): String {
        val width = bitmap.width
        val height = bitmap.height

        val grayscale = Array(height) { y ->
            IntArray(width) { x ->
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
        }

        val threshold = computeOtsuThreshold(grayscale, width, height)
        val binary = Array(height) { y ->
            BooleanArray(width) { x ->
                grayscale[y][x] < threshold
            }
        }

        val dots = detectBrailleDots(binary, width, height)
        return decodeBrailleCells(dots)
    }

    private fun computeOtsuThreshold(gray: Array<IntArray>, width: Int, height: Int): Int {
        val histogram = IntArray(256)
        for (y in 0 until height) {
            for (x in 0 until width) {
                histogram[gray[y][x]]++
            }
        }

        val total = width * height
        var sum = 0.0
        for (i in 0 until 256) sum += i * histogram[i]

        var sumB = 0.0
        var wB = 0
        var maxVariance = 0.0
        var bestThreshold = 0

        for (t in 0 until 256) {
            wB += histogram[t]
            if (wB == 0) continue
            val wF = total - wB
            if (wF == 0) break

            sumB += t * histogram[t]
            val mB = sumB / wB
            val mF = (sum - sumB) / wF
            val variance = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)

            if (variance > maxVariance) {
                maxVariance = variance
                bestThreshold = t
            }
        }

        return bestThreshold
    }

    private fun detectBrailleDots(binary: Array<BooleanArray>, width: Int, height: Int): List<BrailleDot> {
        val dots = mutableListOf<BrailleDot>()
        val visited = Array(height) { BooleanArray(width) }
        val minDotSize = (width * height) / 10000
        val maxDotSize = (width * height) / 100

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (binary[y][x] && !visited[y][x]) {
                    val region = floodFill(binary, visited, x, y, width, height)
                    if (region.size in minDotSize..maxDotSize) {
                        val cx = region.sumOf { it.first } / region.size
                        val cy = region.sumOf { it.second } / region.size
                        dots.add(BrailleDot(cx, cy, region.size))
                    }
                }
            }
        }
        return dots
    }

    private fun floodFill(
        binary: Array<BooleanArray>,
        visited: Array<BooleanArray>,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int
    ): List<Pair<Int, Int>> {
        val region = mutableListOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(Pair(startX, startY))
        visited[startY][startX] = true

        while (queue.isNotEmpty() && region.size < 5000) {
            val (cx, cy) = queue.removeFirst()
            region.add(Pair(cx, cy))

            for ((dx, dy) in NEIGHBORS) {
                val nx = cx + dx
                val ny = cy + dy
                if (nx in 0 until width && ny in 0 until height &&
                    binary[ny][nx] && !visited[ny][nx]
                ) {
                    visited[ny][nx] = true
                    queue.add(Pair(nx, ny))
                }
            }
        }
        return region
    }

    private fun decodeBrailleCells(dots: List<BrailleDot>): String {
        if (dots.size < 2) return ""

        val sortedDots = dots.sortedWith(compareBy({ it.y / 50 }, { it.x }))

        val cellWidth = estimateCellSize(sortedDots.map { it.x })
        val cellHeight = estimateCellSize(sortedDots.map { it.y })

        if (cellWidth <= 0 || cellHeight <= 0) return ""

        val cells = mutableMapOf<Pair<Int, Int>, MutableList<BrailleDot>>()
        for (dot in sortedDots) {
            val cellCol = dot.x / cellWidth
            val cellRow = dot.y / cellHeight
            val key = Pair(cellRow, cellCol)
            cells.getOrPut(key) { mutableListOf() }.add(dot)
        }

        val result = StringBuilder()
        val sortedCells = cells.entries.sortedWith(compareBy({ it.key.first }, { it.key.second }))

        for ((_, cellDots) in sortedCells) {
            val char = decodeSingleCell(cellDots, cellWidth, cellHeight)
            if (char != null) result.append(char)
        }

        return result.toString()
    }

    private fun estimateCellSize(positions: List<Int>): Int {
        if (positions.size < 2) return 1
        val sorted = positions.sorted()
        val diffs = mutableListOf<Int>()
        for (i in 1 until sorted.size) {
            val diff = sorted[i] - sorted[i - 1]
            if (diff > 5) diffs.add(diff)
        }
        return if (diffs.isNotEmpty()) diffs.sorted()[diffs.size / 2] * 3 else 50
    }

    private fun decodeSingleCell(dots: List<BrailleDot>, cellWidth: Int, cellHeight: Int): Char? {
        if (dots.isEmpty()) return null

        val minX = dots.minOf { it.x }
        val minY = dots.minOf { it.y }

        var pattern = 0
        for (dot in dots) {
            val relX = dot.x - minX
            val relY = dot.y - minY
            val col = if (relX < cellWidth / 3) 0 else 1
            val row = when {
                relY < cellHeight / 4 -> 0
                relY < cellHeight / 2 -> 1
                else -> 2
            }
            val bitPos = row + col * 3
            pattern = pattern or (1 shl bitPos)
        }

        return BRAILLE_MAP[pattern]
    }

    data class BrailleDot(val x: Int, val y: Int, val size: Int)

    companion object {
        private val NEIGHBORS = listOf(
            Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
        )

        private val BRAILLE_MAP = mapOf(
            0b000001 to 'a', 0b000011 to 'b', 0b001001 to 'c',
            0b011001 to 'd', 0b010001 to 'e', 0b001011 to 'f',
            0b011011 to 'g', 0b010011 to 'h', 0b001010 to 'i',
            0b011010 to 'j', 0b000101 to 'k', 0b000111 to 'l',
            0b001101 to 'm', 0b011101 to 'n', 0b010101 to 'o',
            0b001111 to 'p', 0b011111 to 'q', 0b010111 to 'r',
            0b001110 to 's', 0b011110 to 't', 0b100101 to 'u',
            0b100111 to 'v', 0b111010 to 'w', 0b101101 to 'x',
            0b101111 to 'y', 0b100111 to 'z'
        )
    }
}
