package com.keepereye.app.obstacle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class ObjectOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var obstacles: List<DetectedObstacle> = emptyList()

    private val nearPaint = Paint().apply {
        color = Color.parseColor("#FFEF5350")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val nearFillPaint = Paint().apply {
        color = Color.parseColor("#40EF5350")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val mediumPaint = Paint().apply {
        color = Color.parseColor("#FFFFB74D")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val mediumFillPaint = Paint().apply {
        color = Color.parseColor("#30FFB74D")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val farPaint = Paint().apply {
        color = Color.parseColor("#FF42A5F5")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val farFillPaint = Paint().apply {
        color = Color.parseColor("#2042A5F5")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val labelPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        isAntiAlias = true
        isFakeBoldText = true
    }

    private val labelBgPaint = Paint().apply {
        color = Color.parseColor("#CC000000")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun setObstacles(detectedObstacles: List<DetectedObstacle>) {
        obstacles = detectedObstacles
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (obstacle in obstacles) {
            val box = obstacle.boundingBox
            val scaleX = width.toFloat() / PREVIEW_WIDTH
            val scaleY = height.toFloat() / PREVIEW_HEIGHT

            val scaledRect = RectF(
                box.left * scaleX,
                box.top * scaleY,
                box.right * scaleX,
                box.bottom * scaleY
            )

            val (strokePaint, fillPaint) = when (obstacle.proximity) {
                ObstacleProximity.NEAR -> nearPaint to nearFillPaint
                ObstacleProximity.MEDIUM -> mediumPaint to mediumFillPaint
                ObstacleProximity.FAR -> farPaint to farFillPaint
            }

            canvas.drawRoundRect(scaledRect, 12f, 12f, fillPaint)
            canvas.drawRoundRect(scaledRect, 12f, 12f, strokePaint)

            val label = "${obstacle.label} - ${obstacle.proximity.label}"
            val labelWidth = labelPaint.measureText(label)
            val labelX = scaledRect.left + 8f
            val labelY = scaledRect.top - 8f

            if (labelY > 40f) {
                canvas.drawRoundRect(
                    labelX - 4f, labelY - 32f,
                    labelX + labelWidth + 8f, labelY + 8f,
                    6f, 6f, labelBgPaint
                )
                canvas.drawText(label, labelX, labelY, labelPaint)
            }
        }
    }

    companion object {
        private const val PREVIEW_WIDTH = 480f
        private const val PREVIEW_HEIGHT = 640f
    }
}
