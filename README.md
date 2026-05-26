# KEEPER-EYE 3.0

**"Tu guía segura en cada paso"**

Asistente inteligente de accesibilidad para personas con discapacidad visual. Utiliza visión artificial, inteligencia artificial y comandos de voz para interpretar el entorno de forma autónoma, segura e independiente.

## Características Principales

- **Asistente por Voz Automático** - Escucha continua sin necesidad de tocar la pantalla
- **Detección de Obstáculos** - ML Kit Object Detection con alertas por voz y vibración
- **Lectura OCR en Tiempo Real** - Detección y lectura automática de texto
- **Descripción de Imágenes** - IA visual para describir escenas completas
- **Lector de Braille** - Reconocimiento de texto Braille mediante cámara
- **Detector de Colores** - Identificación precisa de colores y variantes
- **¿Qué Estoy Viendo?** - Interpretación completa del entorno (OCR + objetos + colores)
- **Modo Asistente Inteligente** - Monitoreo continuo con priorización de riesgos
- **Modo Emergencia** - Envío de alertas GPS por SMS a contactos de emergencia
- **Accesibilidad Total** - Compatible con TalkBack, alto contraste, vibración inteligente

## Módulos y Comandos de Voz

| Comando | Módulo | Función |
|---------|--------|---------|
| "Uno" | Obstáculos | Detecta personas, vehículos, escaleras, muebles |
| "Dos" | OCR | Lee texto en tiempo real |
| "Tres" | Describir | Describe escenas con IA |
| "Cuatro" | Braille | Reconoce y traduce Braille |
| "Cinco" | Colores | Identifica colores de objetos |
| "Seis" | Visión | Interpreta todo el entorno |
| "Ayuda" | — | Explica funciones disponibles |
| "Inicio" | — | Vuelve al menú principal |
| "Salir" | — | Cierra el módulo actual |
| "Emergencia" | — | Envía alerta con ubicación |

## Stack Tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Material Design 3 |
| Cámara | CameraX |
| OCR | Google ML Kit Text Recognition |
| Objetos | Google ML Kit Object Detection |
| Etiquetas | Google ML Kit Image Labeling |
| Braille | Procesamiento de imagen propio |
| Voz TTS | Android TextToSpeech API |
| Voz STT | Android SpeechRecognizer |
| Ubicación | Google Play Services Location |
| Min SDK | API 24 (Android 7.0) |
| Target SDK | API 34 (Android 14) |

## Estructura del Proyecto

```
app/src/main/
├── java/com/keepereye/app/
│   ├── MainActivity.kt              # Pantalla principal + voz
│   ├── ScanActivity.kt              # Módulo 2: OCR
│   ├── ObstacleActivity.kt          # Módulo 1: Obstáculos
│   ├── DescribeActivity.kt          # Módulo 3: Descripción
│   ├── BrailleActivity.kt           # Módulo 4: Braille
│   ├── ColorDetectionActivity.kt    # Módulo 5: Colores
│   ├── VisionActivity.kt            # Módulo 6: Visión completa
│   ├── SmartAssistantActivity.kt    # Modo Asistente
│   ├── EmergencyActivity.kt         # Modo Emergencia
│   ├── HistoryActivity.kt           # Historial
│   ├── voice/
│   │   └── VoiceCommandProcessor.kt # Procesamiento de comandos
│   ├── tts/
│   │   └── TextToSpeechManager.kt   # Gestión de voz
│   ├── ocr/
│   │   ├── TextRecognitionAnalyzer.kt
│   │   └── TextOverlayView.kt
│   ├── obstacle/
│   │   ├── ObjectDetectionAnalyzer.kt
│   │   ├── ObstacleAlertManager.kt
│   │   ├── DetectedObstacle.kt
│   │   └── ObjectOverlayView.kt
│   ├── describe/
│   │   └── ImageDescriber.kt
│   ├── braille/
│   │   └── BrailleRecognizer.kt
│   ├── color/
│   │   └── ColorAnalyzer.kt
│   ├── vision/
│   │   └── SceneInterpreter.kt
│   ├── assistant/
│   │   └── SmartAssistantEngine.kt
│   ├── emergency/
│   │   └── EmergencyContactManager.kt
│   ├── ai/
│   │   └── KeywordDetector.kt
│   └── history/
│       ├── HistoryEntry.kt
│       ├── HistoryDatabaseHelper.kt
│       ├── HistoryRepository.kt
│       └── HistoryAdapter.kt
└── res/
    ├── layout/
    ├── values/
    └── drawable/
```

## Compilación

### Requisitos
- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17
- Android SDK 34

### Generar APK Debug
```bash
./gradlew assembleDebug
```

### Generar APK Release
```bash
./gradlew assembleRelease
```

## Permisos

| Permiso | Uso |
|---------|-----|
| `CAMERA` | Todos los módulos de visión |
| `RECORD_AUDIO` | Reconocimiento de voz continuo |
| `VIBRATE` | Alertas táctiles de obstáculos |
| `ACCESS_FINE_LOCATION` | Modo emergencia (GPS) |
| `SEND_SMS` | Modo emergencia (alerta) |
| `INTERNET` | Descarga de modelos ML Kit |

## Paleta de Colores

- **Primario:** `#6A5AE0` (Morado)
- **Secundario:** `#9C27B0` (Violeta)
- **Terciario:** `#B388FF` (Lila)
- **Fondo:** `#0D0B1A` (Negro profundo)
- **Superficie:** `#1A1530` (Gris oscuro)

## Licencia

Proyecto privado - Todos los derechos reservados.
