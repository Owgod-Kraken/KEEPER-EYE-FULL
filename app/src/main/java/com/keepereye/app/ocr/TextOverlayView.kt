package com.keepereye.app.ocr

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.text.Text

class TextOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var textBlocks: List<Text.TextBlock> = emptyList()

    private val boundingBoxPaint = Paint().apply {
        color = Color.parseColor("#A78BFA")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#40A78BFA")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun setTextBlocks(blocks: List<Text.TextBlock>) {
        textBlocks = blocks
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (block in textBlocks) {
            val boundingBox = block.boundingBox ?: continue
            val rect = RectF(boundingBox)

            val scaleX = width.toFloat() / PREVIEW_WIDTH
            val scaleY = height.toFloat() / PREVIEW_HEIGHT

            val scaledRect = RectF(
                rect.left * scaleX,
                rect.top * scaleY,
                rect.right * scaleX,
                rect.bottom * scaleY
            )

            canvas.drawRoundRect(scaledRect, 8f, 8f, backgroundPaint)
            canvas.drawRoundRect(scaledRect, 8f, 8f, boundingBoxPaint)
        }
    }

    companion object {
        private const val PREVIEW_WIDTH = 480f
        private const val PREVIEW_HEIGHT = 640f
    }
}
