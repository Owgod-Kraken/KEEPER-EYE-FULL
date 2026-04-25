package com.keepereye.app.obstacle

import android.graphics.Rect

enum class ObstaclePosition {
    LEFT, CENTER, RIGHT
}

enum class ObstacleProximity(val label: String) {
    NEAR("muy cerca"),
    MEDIUM("a media distancia"),
    FAR("lejos")
}

data class DetectedObstacle(
    val label: String,
    val boundingBox: Rect,
    val position: ObstaclePosition,
    val proximity: ObstacleProximity,
    val confidence: Float,
    val trackingId: Int?
) {
    fun buildAlertMessage(): String {
        val posLabel = when (position) {
            ObstaclePosition.LEFT -> "a la izquierda"
            ObstaclePosition.CENTER -> "al frente"
            ObstaclePosition.RIGHT -> "a la derecha"
        }
        return when (proximity) {
            ObstacleProximity.NEAR -> "$label $posLabel, muy cerca"
            ObstacleProximity.MEDIUM -> "$label $posLabel"
            ObstacleProximity.FAR -> "$label $posLabel, lejos"
        }
    }

    companion object {
        private val LABEL_MAP = mapOf(
            "Fashion good" to "Persona u objeto",
            "Home good" to "Mueble",
            "Food" to "Objeto",
            "Place" to "Estructura",
            "Plant" to "Planta"
        )

        fun translateLabel(englishLabel: String): String {
            return LABEL_MAP[englishLabel] ?: "Obstáculo"
        }
    }
}
