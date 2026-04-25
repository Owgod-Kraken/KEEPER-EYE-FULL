package com.keepereye.app.obstacle

import android.graphics.Rect

enum class ObstaclePosition {
    LEFT, CENTER, RIGHT
}

enum class ObstacleProximity(val label: String) {
    NEAR("cerca"),
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
            ObstacleProximity.NEAR -> "Obstáculo $posLabel, muy cerca"
            ObstacleProximity.MEDIUM -> "Objeto $posLabel"
            ObstacleProximity.FAR -> "Objeto detectado $posLabel, ${ proximity.label }"
        }
    }
}
