package com.keepereye.app.voice

class VoiceCommandProcessor {

    enum class Action {
        OBSTACLES,
        OCR,
        DESCRIBE,
        BRAILLE,
        COLORS,
        VISION,
        HELP,
        HOME,
        EXIT,
        EMERGENCY,
        NONE
    }

    fun processCommand(input: String): Action {
        val normalized = input.lowercase().trim()

        return when {
            normalized.contains("uno") || normalized.contains("1") ||
                normalized.contains("obstáculo") || normalized.contains("obstaculo") -> Action.OBSTACLES

            normalized.contains("dos") || normalized.contains("2") ||
                normalized.contains("ocr") || normalized.contains("leer texto") -> Action.OCR

            normalized.contains("tres") || normalized.contains("3") ||
                normalized.contains("describir") || normalized.contains("descripción") -> Action.DESCRIBE

            normalized.contains("cuatro") || normalized.contains("4") ||
                normalized.contains("braille") -> Action.BRAILLE

            normalized.contains("cinco") || normalized.contains("5") ||
                normalized.contains("color") || normalized.contains("colores") -> Action.COLORS

            normalized.contains("seis") || normalized.contains("6") ||
                normalized.contains("qué estoy viendo") || normalized.contains("que estoy viendo") ||
                normalized.contains("viendo") -> Action.VISION

            normalized.contains("ayuda") || normalized.contains("help") -> Action.HELP

            normalized.contains("inicio") || normalized.contains("menú") ||
                normalized.contains("menu") || normalized.contains("principal") -> Action.HOME

            normalized.contains("salir") || normalized.contains("cerrar") ||
                normalized.contains("exit") -> Action.EXIT

            normalized.contains("emergencia") || normalized.contains("socorro") ||
                normalized.contains("auxilio") -> Action.EMERGENCY

            else -> Action.NONE
        }
    }
}
