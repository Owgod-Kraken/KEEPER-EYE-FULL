package com.keepereye.app.obstacle

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.keepereye.app.tts.TextToSpeechManager

class ObstacleAlertManager(
    private val context: Context,
    private val ttsManager: TextToSpeechManager
) {

    private var lastAlertTime = 0L
    private var lastAlertMessage = ""

    fun processObstacles(obstacles: List<DetectedObstacle>, hasActiveText: Boolean) {
        if (obstacles.isEmpty()) return

        val currentTime = System.currentTimeMillis()

        val sortedObstacles = obstacles.sortedWith(
            compareByDescending<DetectedObstacle> { it.proximity == ObstacleProximity.NEAR }
                .thenByDescending { it.proximity == ObstacleProximity.MEDIUM }
                .thenByDescending { it.position == ObstaclePosition.CENTER }
        )

        val mostImportant = sortedObstacles.first()

        val cooldownMs = when (mostImportant.proximity) {
            ObstacleProximity.NEAR -> COOLDOWN_NEAR_MS
            ObstacleProximity.MEDIUM -> COOLDOWN_MEDIUM_MS
            ObstacleProximity.FAR -> COOLDOWN_FAR_MS
        }

        if (currentTime - lastAlertTime < cooldownMs) return

        val alertMessage = mostImportant.buildAlertMessage()

        if (alertMessage == lastAlertMessage &&
            currentTime - lastAlertTime < cooldownMs * 2
        ) return

        if (hasActiveText && mostImportant.proximity != ObstacleProximity.NEAR) return

        lastAlertTime = currentTime
        lastAlertMessage = alertMessage

        vibrateForProximity(mostImportant.proximity)

        if (mostImportant.proximity == ObstacleProximity.NEAR) {
            ttsManager.speakWithPriority(alertMessage)
        } else {
            ttsManager.speak(alertMessage)
        }
    }

    private fun vibrateForProximity(proximity: ObstacleProximity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = when (proximity) {
                ObstacleProximity.NEAR -> {
                    longArrayOf(0, 200, 100, 200, 100, 200)
                }
                ObstacleProximity.MEDIUM -> {
                    longArrayOf(0, 150, 150, 150)
                }
                ObstacleProximity.FAR -> {
                    longArrayOf(0, 80)
                }
            }
            val amplitudes = when (proximity) {
                ObstacleProximity.NEAR -> {
                    intArrayOf(0, 255, 0, 255, 0, 255)
                }
                ObstacleProximity.MEDIUM -> {
                    intArrayOf(0, 150, 0, 150)
                }
                ObstacleProximity.FAR -> {
                    intArrayOf(0, 60)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            }
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val durationMs = when (proximity) {
                ObstacleProximity.NEAR -> 500L
                ObstacleProximity.MEDIUM -> 250L
                ObstacleProximity.FAR -> 80L
            }
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    fun reset() {
        lastAlertTime = 0L
        lastAlertMessage = ""
    }

    companion object {
        private const val COOLDOWN_NEAR_MS = 1500L
        private const val COOLDOWN_MEDIUM_MS = 3000L
        private const val COOLDOWN_FAR_MS = 5000L
    }
}
