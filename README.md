# KEEPER EYE

Aplicacion movil Android nativa enfocada en accesibilidad para personas con discapacidad visual. Utiliza OCR en tiempo real, deteccion de obstaculos y navegacion por comandos de voz, funcionando completamente offline sin necesidad de backend.

## Caracteristicas

- **Sistema de bienvenida por voz** - Saludo automatico con Text-to-Speech al abrir la app
- **Comandos de voz** - Di "1" para deteccion de obstaculos o "2" para lectura OCR
- **OCR en tiempo real** - Deteccion de texto con la camara usando Google ML Kit (on-device)
- **Deteccion de obstaculos** - Deteccion de objetos con alerta de direccion y proximidad
- **Lectura por voz** - Reproduccion automatica del texto detectado usando Android TTS
- **Deteccion de palabras clave** - Prioriza la lectura de palabras como "peligro", "alto", "salida"
- **Accesibilidad total** - Botones grandes, alto contraste, vibracion, navegacion por voz
- **Retroalimentacion haptica** - Vibracion al interactuar y al detectar objetos/texto
- **Historial** - Guarda los ultimos textos detectados en SQLite local
- **100% Offline** - Todo el procesamiento es local, sin envio de datos a internet
- **Privacidad** - Ningun dato sale del dispositivo

## Stack Tecnologico

| Componente | Tecnologia |
|---|---|
| Lenguaje | Kotlin |
| UI | Material Design 3 |
| Camara | CameraX |
| OCR | Google ML Kit Text Recognition (on-device) |
| Deteccion Objetos | Google ML Kit Object Detection (on-device) |
| Voz TTS | Android TextToSpeech API |
| Reconocimiento Voz | Android SpeechRecognizer |
| Base de datos | SQLite |
| Min SDK | API 24 (Android 7.0) |
| Target SDK | API 34 (Android 14) |

## Estructura del Proyecto

```
app/src/main/
├── java/com/keepereye/app/
│   ├── SplashActivity.kt          # Pantalla splash con animacion
│   ├── MainActivity.kt            # Pantalla principal con comandos de voz
│   ├── ScanActivity.kt            # Camara + OCR + TTS
│   ├── ObstacleActivity.kt        # Camara + Deteccion objetos + Alertas
│   ├── HistoryActivity.kt         # Historial de textos
│   ├── voice/
│   │   └── VoiceCommandManager.kt # Reconocimiento de comandos de voz
│   ├── ocr/
│   │   ├── TextRecognitionAnalyzer.kt  # Procesamiento ML Kit OCR
│   │   └── TextOverlayView.kt         # Bounding boxes OCR
│   ├── obstacle/
│   │   ├── ObjectDetectionAnalyzer.kt  # Procesamiento ML Kit Objects
│   │   ├── ObjectOverlayView.kt       # Bounding boxes objetos
│   │   ├── DetectedObstacle.kt        # Modelo de obstaculos
│   │   └── ObstacleAlertManager.kt    # Alertas voz + vibracion
│   ├── tts/
│   │   └── TextToSpeechManager.kt     # Gestion de voz con callbacks
│   ├── ai/
│   │   └── KeywordDetector.kt         # Deteccion de palabras clave
│   └── history/
│       ├── HistoryEntry.kt            # Modelo de datos
│       ├── HistoryDatabaseHelper.kt   # SQLite helper
│       ├── HistoryRepository.kt       # Repositorio
│       └── HistoryAdapter.kt          # RecyclerView adapter
└── res/
    ├── layout/         # Layouts XML
    ├── values/         # Strings, colors, themes
    └── drawable/       # Iconos vectoriales
```

## Compilacion

### Requisitos
- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17
- Android SDK 34

### Generar APK Debug
```bash
./gradlew assembleDebug
```
El APK se generara en `app/build/outputs/apk/debug/app-debug.apk`

### Generar APK Release
```bash
./gradlew assembleRelease
```

## Permisos

- `CAMERA` - Para escaneo OCR y deteccion de obstaculos en tiempo real
- `VIBRATE` - Para feedback tactil
- `RECORD_AUDIO` - Para reconocimiento de comandos de voz

## Palabras Clave Detectadas

| Palabra | Prioridad | Tipo |
|---|---|---|
| peligro, danger, cuidado, emergencia | CRITICA | Alerta inmediata con vibracion larga |
| alto, stop, pare, no pasar, prohibido, precaucion | ALTA | Alerta con vibracion |
| salida, exit, entrada, escalera, ascensor, bano | MEDIA | Aviso informativo |

## Identidad Visual

- **Colores principales**: Morado (#7C3AED), Violeta (#8B5CF6), Blanco
- **Estilo**: Moderno, tecnologico, accesible, minimalista
- **Logo**: Vision artificial + accesibilidad

## Licencia

Proyecto privado - Todos los derechos reservados.
