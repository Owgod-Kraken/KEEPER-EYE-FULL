package com.keepereye.app.ai

data class DetectedKeyword(
    val keyword: String,
    val priority: KeywordPriority,
    val originalContext: String
)

enum class KeywordPriority(val level: Int, val label: String) {
    CRITICAL(3, "ALERTA CRITICA"),
    HIGH(2, "ALERTA"),
    MEDIUM(1, "AVISO")
}

class KeywordDetector {

    private val keywordMap = mapOf(
        "peligro" to KeywordPriority.CRITICAL,
        "danger" to KeywordPriority.CRITICAL,
        "warning" to KeywordPriority.CRITICAL,
        "cuidado" to KeywordPriority.CRITICAL,
        "alto" to KeywordPriority.HIGH,
        "stop" to KeywordPriority.HIGH,
        "pare" to KeywordPriority.HIGH,
        "no pasar" to KeywordPriority.HIGH,
        "prohibido" to KeywordPriority.HIGH,
        "salida" to KeywordPriority.MEDIUM,
        "exit" to KeywordPriority.MEDIUM,
        "entrada" to KeywordPriority.MEDIUM,
        "escalera" to KeywordPriority.MEDIUM,
        "ascensor" to KeywordPriority.MEDIUM,
        "baño" to KeywordPriority.MEDIUM,
        "emergencia" to KeywordPriority.CRITICAL,
        "emergency" to KeywordPriority.CRITICAL,
        "precaución" to KeywordPriority.HIGH,
        "caution" to KeywordPriority.HIGH
    )

    fun detectKeywords(text: String): List<DetectedKeyword> {
        val lowerText = text.lowercase()
        val detected = mutableListOf<DetectedKeyword>()

        for ((keyword, priority) in keywordMap) {
            if (lowerText.contains(keyword)) {
                val context = extractContext(text, keyword)
                detected.add(DetectedKeyword(keyword, priority, context))
            }
        }

        return detected.sortedByDescending { it.priority.level }
    }

    fun buildPriorityMessage(keywords: List<DetectedKeyword>): String {
        if (keywords.isEmpty()) return ""

        val highestPriority = keywords.first()
        val sb = StringBuilder()

        sb.append("${highestPriority.priority.label}: ")
        sb.append(keywords.joinToString(", ") { it.keyword })

        return sb.toString()
    }

    private fun extractContext(text: String, keyword: String): String {
        val lowerText = text.lowercase()
        val index = lowerText.indexOf(keyword)
        if (index == -1) return text

        val start = maxOf(0, index - 20)
        val end = minOf(text.length, index + keyword.length + 20)

        return text.substring(start, end).trim()
    }
}
