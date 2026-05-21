package com.keepereye.app.obstacle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Size
import android.view.View

class ObjectOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var obstacles: List<DetectedObstacle> = emptyList()
    private var sourceSize: Size = Size(480, 640)

    private val nearPaint = Paint().apply {
        color = Color.parseColor("#FFEF4444")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val nearFillPaint = Paint().apply {
        color = Color.parseColor("#40EF4444")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val mediumPaint = Paint().apply {
        color = Color.parseColor("#FFFBBF24")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val mediumFillPaint = Paint().apply {
        color = Color.parseColor("#30FBBF24")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val farPaint = Paint().apply {
        color = Color.parseColor("#FFA78BFA")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val farFillPaint = Paint().apply {
        color = Color.parseColor("#20A78BFA")
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

    fun setObstacles(detectedObstacles: List<DetectedObstacle>, imageSize: Size) {
        obstacles = detectedObstacles
        sourceSize = imageSize
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (obstacles.isEmpty() || width == 0 || height == 0) return

        val scaleX = width.toFloat() / sourceSize.width.toFloat()
        val scaleY = height.toFloat() / sourceSize.height.toFloat()

        for (obstacle in obstacles) {
            val box = obstacle.boundingBox

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
}
